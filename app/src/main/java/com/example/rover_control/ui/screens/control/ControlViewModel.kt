package com.example.rover_control.ui.screens.control

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rover_control.RoverApplication
import com.example.rover_control.data.bluetooth.ConnectionState
import com.example.rover_control.data.protocol.RoverCommand
import com.example.rover_control.data.settings.DEFAULT_SPEED
import com.example.rover_control.data.settings.InputMode
import com.example.rover_control.util.speedByteToPercent
import com.example.rover_control.util.speedPercentToByte
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ControlUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected(),
    val inputMode: InputMode = InputMode.DPAD,
    val speedPercent: Float = speedByteToPercent(DEFAULT_SPEED),
    val headlightsOn: Boolean = false,
    val isBuzzerActive: Boolean = false,
    val isStopped: Boolean = false,
    val isEngineOn: Boolean = true,
    val isTurboActive: Boolean = false
)

class ControlViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RoverApplication
    private val repository = app.roverRepository
    private val settingsRepository = app.settingsRepository

    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    private var speedJob: Job? = null
    private var speedBeforeStop: Float = speedByteToPercent(DEFAULT_SPEED)
    private var speedBeforeTurbo: Float = speedByteToPercent(DEFAULT_SPEED)

    init {
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
        viewModelScope.launch {
            repository.emergencyStopped.collect { stopped ->
                if (stopped) {
                    if (!_uiState.value.isStopped) {
                        speedBeforeStop = _uiState.value.speedPercent
                        _uiState.update {
                            it.copy(
                                isStopped = true,
                                speedPercent = 0f,
                                headlightsOn = false,
                                isTurboActive = false
                            )
                        }
                        settingsRepository.setHeadlightsOn(false)
                    }
                } else {
                    if (_uiState.value.isStopped) {
                        _uiState.update {
                            it.copy(isStopped = false, speedPercent = speedBeforeStop)
                        }
                    } else {
                        _uiState.update { it.copy(isStopped = false) }
                    }
                }
            }
        }
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            speedBeforeStop = speedByteToPercent(settings.lastSpeed)
            _uiState.update { state ->
                if (state.isStopped) {
                    state.copy(inputMode = settings.inputMode, headlightsOn = false)
                } else {
                    state.copy(
                        inputMode = settings.inputMode,
                        speedPercent = speedByteToPercent(settings.lastSpeed),
                        headlightsOn = settings.headlightsOn
                    )
                }
            }
        }
    }

    fun onDirectionChange(command: RoverCommand?) {
        val state = _uiState.value
        if (state.isStopped || !state.isEngineOn) return
        repository.sendCommand(command ?: RoverCommand.Stop)
    }

    fun onRelease() {
        val state = _uiState.value
        if (state.isStopped || !state.isEngineOn) return
        repository.sendCommand(RoverCommand.Stop)
    }

    fun onSpeedChange(percent: Float) {
        val state = _uiState.value
        if (state.isStopped || !state.isEngineOn) return
        val clamped = percent.coerceIn(0f, 100f)
        _uiState.update { it.copy(speedPercent = clamped, isTurboActive = false) }

        speedJob?.cancel()
        speedJob = viewModelScope.launch {
            delay(SPEED_DEBOUNCE_MS)
            persistAndSendSpeed(clamped)
        }
    }

    /**
     * Engine is an app-level arming state: the firmware protocol has no engine
     * command. Turning it off stops movement locally and on the rover; turning
     * it on re-sends the selected speed.
     */
    fun toggleEngine() {
        val state = _uiState.value
        if (state.isStopped) return

        if (state.isEngineOn) {
            speedJob?.cancel()
            _uiState.update { it.copy(isEngineOn = false, isTurboActive = false) }
            repository.sendCommand(RoverCommand.Stop)
        } else {
            _uiState.update { it.copy(isEngineOn = true) }
            repository.setSpeed(speedPercentToByte(state.speedPercent))
        }
    }

    fun toggleTurbo() {
        val state = _uiState.value
        if (state.isStopped || !state.isEngineOn) return

        if (state.isTurboActive) {
            _uiState.update {
                it.copy(isTurboActive = false, speedPercent = speedBeforeTurbo)
            }
            speedJob?.cancel()
            viewModelScope.launch { persistAndSendSpeed(speedBeforeTurbo) }
        } else {
            speedBeforeTurbo = state.speedPercent
            _uiState.update { it.copy(isTurboActive = true, speedPercent = 100f) }
            speedJob?.cancel()
            viewModelScope.launch { persistAndSendSpeed(100f) }
        }
    }

    /** Immediate non-latching brake: speed to 0 %, then stop the motors. */
    fun brake() {
        if (_uiState.value.isStopped) return
        speedJob?.cancel()
        _uiState.update { it.copy(speedPercent = 0f, isTurboActive = false) }
        repository.setSpeed(0)
        repository.sendCommand(RoverCommand.Stop)
        viewModelScope.launch { settingsRepository.setLastSpeed(0) }
    }

    fun toggleHeadlights(on: Boolean) {
        if (_uiState.value.isStopped) return
        _uiState.update { it.copy(headlightsOn = on) }
        repository.sendCommand(RoverCommand.Headlights(on))
        viewModelScope.launch { settingsRepository.setHeadlightsOn(on) }
    }

    fun setBuzzer(on: Boolean) {
        if (_uiState.value.isStopped) return
        _uiState.update { it.copy(isBuzzerActive = on) }
        repository.sendCommand(RoverCommand.Buzzer(on))
    }

    fun setInputMode(mode: InputMode) {
        if (_uiState.value.inputMode == mode) return
        _uiState.update { it.copy(inputMode = mode) }
        viewModelScope.launch { settingsRepository.setInputMode(mode) }
    }

    fun emergencyStop() {
        speedJob?.cancel()
        repository.emergencyStop()
    }

    fun rearm() {
        val restore = speedBeforeStop
        val speedByte = speedPercentToByte(restore)
        repository.rearm(speedByte)
        _uiState.update {
            it.copy(isStopped = false, speedPercent = restore, isTurboActive = false)
        }

        speedJob?.cancel()
        viewModelScope.launch {
            settingsRepository.setLastSpeed(speedByte)
        }
    }

    private suspend fun persistAndSendSpeed(percent: Float) {
        val speedByte = speedPercentToByte(percent)
        repository.setSpeed(speedByte)
        settingsRepository.setLastSpeed(speedByte)
    }

    override fun onCleared() {
        speedJob?.cancel()
        repository.sendCommand(RoverCommand.Stop)
    }

    private companion object {
        const val SPEED_DEBOUNCE_MS = 200L
    }
}
