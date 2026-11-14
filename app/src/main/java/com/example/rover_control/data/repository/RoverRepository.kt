package com.example.rover_control.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.SystemClock
import android.util.Log
import com.example.rover_control.data.bluetooth.BluetoothManager
import com.example.rover_control.data.bluetooth.ConnectionState
import com.example.rover_control.data.protocol.CommandEncoder
import com.example.rover_control.data.protocol.RoverCommand
import com.example.rover_control.data.protocol.Telemetry
import com.example.rover_control.data.protocol.TelemetryParser
import com.example.rover_control.data.settings.DEFAULT_SPEED
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Single source of truth for the rover link and telemetry.
 *
 * Owns the Bluetooth socket, a single ordered writer coroutine and the reader
 * loop. ViewModels only observe [connectionState] / [telemetry] and call
 * [sendCommand] (Implementation_plan.md §4.2).
 */
class RoverRepository(
    private val bluetoothManager: BluetoothManager,
    private val demoTelemetryGenerator: DemoTelemetryGenerator = DemoTelemetryGenerator(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val telemetryParser = TelemetryParser()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetry = MutableStateFlow(Telemetry.EMPTY)
    val telemetry: StateFlow<Telemetry> = _telemetry.asStateFlow()

    private val _lastTelemetryAt = MutableStateFlow<Long?>(null)
    val lastTelemetryAt: StateFlow<Long?> = _lastTelemetryAt.asStateFlow()

    private val _demoMode = MutableStateFlow(false)
    val demoMode: StateFlow<Boolean> = _demoMode.asStateFlow()

    private val _emergencyStopped = MutableStateFlow(false)
    val emergencyStopped: StateFlow<Boolean> = _emergencyStopped.asStateFlow()

    @Volatile
    private var socket: BluetoothSocket? = null

    @Volatile
    private var writerChannel: Channel<RoverCommand>? = null

    @Volatile
    private var connectionJob: Job? = null

    @Volatile
    private var demoJob: Job? = null

    @Volatile
    private var manualDisconnect = false

    @Volatile
    private var currentSpeed: Int = DEFAULT_SPEED

    /**
     * Connects to a bonded device. Reconnecting after a link loss is retried
     * once automatically; the Connection screen exposes a manual retry.
     */
    @SuppressLint("MissingPermission")
    fun connect(
        device: BluetoothDevice,
        initialSpeed: Int = currentSpeed,
        headlightsOn: Boolean = false
    ) {
        stopDemoInternal()
        _demoMode.value = false
        _emergencyStopped.value = false
        manualDisconnect = false
        currentSpeed = initialSpeed.coerceIn(0, 255)
        resetTransport()
        resetTelemetry()

        val deviceName = runCatching { device.name }.getOrNull()
            .orEmpty()
            .ifBlank { device.address }

        connectionJob = scope.launch {
            connectionLoop(device, deviceName, currentSpeed, headlightsOn)
        }
    }

    fun disconnect() {
        manualDisconnect = true
        stopDemoInternal()
        _demoMode.value = false
        _emergencyStopped.value = false
        resetTransport()
        _connectionState.value = ConnectionState.Disconnected()
    }

    /**
     * Demo Mode bypasses Bluetooth entirely and feeds synthetic telemetry while
     * commands become no-ops logged to Logcat (Implementation_plan.md §12).
     */
    fun setDemoMode(enabled: Boolean) {
        if (enabled == _demoMode.value) return

        if (enabled) {
            manualDisconnect = true
            stopDemoInternal()
            resetTransport()
            _demoMode.value = true
            _emergencyStopped.value = false
            resetTelemetry()
            _connectionState.value = ConnectionState.Connected(DEMO_DEVICE_NAME, isDemo = true)
            demoJob = scope.launch {
                while (currentCoroutineContext().isActive) {
                    publishTelemetry(demoTelemetryGenerator.next())
                    delay(DEMO_INTERVAL_MS)
                }
            }
        } else {
            stopDemoInternal()
            _demoMode.value = false
            _emergencyStopped.value = false
            _connectionState.value = ConnectionState.Disconnected()
        }
    }

    /**
     * Hard stop: sends `X\n` and turns the headlights off. Movement stays
     * blocked until [rearm] is called (Implementation_plan.md §6.3).
     */
    fun emergencyStop() {
        _emergencyStopped.value = true
        sendCommand(RoverCommand.EmergencyStop)
        sendCommand(RoverCommand.Headlights(false))
        Log.d(TAG, "Emergency stop engaged")
    }

    /** Sends `S\n` so the firmware leaves its stopped state, then re-enables input. */
    fun rearm() {
        _emergencyStopped.value = false
        sendCommand(RoverCommand.Stop)
        Log.d(TAG, "Rearmed")
    }

    fun sendCommand(command: RoverCommand) {
        val state = _connectionState.value
        val encoded = CommandEncoder.encodeToString(command).trim()
        if (state !is ConnectionState.Connected) {
            Log.d(TAG, "Ignoring command $encoded: not connected")
            return
        }
        if (state.isDemo) {
            Log.d(TAG, "DEMO command $encoded")
            return
        }
        if (_emergencyStopped.value && command.isMovementCommand()) {
            Log.d(TAG, "Ignoring movement command $encoded: emergency stop engaged")
            return
        }
        val queue = writerChannel
        if (queue == null) {
            Log.w(TAG, "Unable to send $encoded: writer is not ready")
            return
        }
        val result = queue.trySend(command)
        if (result.isFailure) {
            Log.w(TAG, "Unable to queue command $encoded", result.exceptionOrNull())
        }
    }

    /** Updates the stored speed and forwards it to the firmware. */
    fun setSpeed(speed: Int) {
        currentSpeed = speed.coerceIn(0, 255)
        sendCommand(RoverCommand.SetSpeed(currentSpeed))
    }

    private suspend fun connectionLoop(
        device: BluetoothDevice,
        deviceName: String,
        initialSpeed: Int,
        headlightsOn: Boolean
    ) {
        var retried = false

        while (currentCoroutineContext().isActive) {
            _connectionState.value = ConnectionState.Connecting(deviceName)
            try {
                val newSocket = bluetoothManager.openSocket(device)
                socket = newSocket
                telemetryParser.reset()
                _connectionState.value = ConnectionState.Connected(deviceName, isDemo = false)
                runSession(newSocket, initialSpeed, headlightsOn)
                // A session should only end when the socket is gone.
                throw IOException("Connection closed")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                closeSocket()
                if (manualDisconnect || !currentCoroutineContext().isActive) return
                if (!retried) {
                    retried = true
                    _connectionState.value = ConnectionState.Connecting(deviceName)
                    delay(RETRY_DELAY_MS)
                    continue
                }
                _connectionState.value = ConnectionState.Disconnected(
                    e.message ?: e.javaClass.simpleName
                )
                return
            }
        }
    }

    private suspend fun runSession(
        socket: BluetoothSocket,
        initialSpeed: Int,
        headlightsOn: Boolean
    ) = coroutineScope {
        val queue = Channel<RoverCommand>(Channel.UNLIMITED)
        writerChannel = queue

        // Sync firmware state with the persisted UI state.
        queue.trySend(RoverCommand.SetSpeed(initialSpeed))
        queue.trySend(RoverCommand.Headlights(headlightsOn))

        val reader = launch(Dispatchers.IO) {
            try {
                readLoop(socket)
            } catch (e: Exception) {
                runCatching { socket.close() }
                throw e
            }
        }
        val writer = launch(Dispatchers.IO) {
            try {
                writeLoop(socket, queue)
            } catch (e: Exception) {
                runCatching { socket.close() }
                throw e
            }
        }

        reader.join()
        writer.join()
    }

    private fun readLoop(socket: BluetoothSocket) {
        val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.US_ASCII))
        while (true) {
            val line = reader.readLine() ?: throw IOException("Bluetooth socket closed")
            val telemetry = telemetryParser.parse(line) ?: continue
            publishTelemetry(telemetry)
        }
    }

    private suspend fun writeLoop(socket: BluetoothSocket, queue: Channel<RoverCommand>) {
        val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, Charsets.US_ASCII))
        try {
            for (command in queue) {
                writer.write(CommandEncoder.encodeToString(command))
                writer.flush()
            }
        } finally {
            runCatching { writer.close() }
        }
    }

    private fun publishTelemetry(telemetry: Telemetry) {
        _telemetry.value = telemetry
        _lastTelemetryAt.value = SystemClock.elapsedRealtime()
    }

    private fun resetTelemetry() {
        telemetryParser.reset()
        _telemetry.value = Telemetry.EMPTY
        _lastTelemetryAt.value = null
    }

    @Synchronized
    private fun resetTransport() {
        val job = connectionJob
        connectionJob = null
        job?.cancel()
        closeSocket()
    }

    @Synchronized
    private fun closeSocket() {
        writerChannel?.close()
        writerChannel = null
        runCatching { socket?.close() }
        socket = null
    }

    private fun stopDemoInternal() {
        demoJob?.cancel()
        demoJob = null
    }

    private fun RoverCommand.isMovementCommand(): Boolean = when (this) {
        RoverCommand.Forward,
        RoverCommand.Backward,
        RoverCommand.Left,
        RoverCommand.Right -> true

        else -> false
    }

    private companion object {
        const val TAG = "RoverRepository"
        const val RETRY_DELAY_MS = 1_000L
        const val DEMO_INTERVAL_MS = 500L
        const val DEMO_DEVICE_NAME = "Demo Mode"
    }
}
