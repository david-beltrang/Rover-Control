package com.example.rover_control.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.content.Context
import android.util.Log
import com.example.rover_control.util.hasBluetoothPermissions
import java.io.IOException

/**
 * Thin wrapper around the Android Bluetooth Classic APIs.
 *
 * Only already-bonded devices are exposed: pairing happens in Android settings
 * and the app intentionally does not implement a scan/pair flow.
 */
class BluetoothManager(context: Context) {

    private val appContext: Context = context.applicationContext

    private val adapter: BluetoothAdapter? by lazy {
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager)?.adapter
    }

    val isSupported: Boolean
        get() = adapter != null

    val isEnabled: Boolean
        @SuppressLint("MissingPermission")
        get() = isSupported && appContext.hasBluetoothPermissions() && adapter?.isEnabled == true

    /** Bonded devices, sorted by display name. Empty when permission is missing. */
    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<PairedDevice> {
        val adapter = adapter ?: return emptyList()
        if (!appContext.hasBluetoothPermissions()) return emptyList()
        return runCatching {
            adapter.bondedDevices.orEmpty()
                .map { PairedDevice(name = it.name.orEmpty(), address = it.address) }
                .sortedBy { it.displayName.lowercase() }
        }.onFailure {
            Log.w(TAG, "Unable to read bonded devices", it)
        }.getOrDefault(emptyList())
    }

    @SuppressLint("MissingPermission")
    fun findBondedDevice(address: String): BluetoothDevice? {
        if (address.isBlank() || !appContext.hasBluetoothPermissions()) return null
        return runCatching {
            adapter?.bondedDevices?.firstOrNull { it.address.equals(address, ignoreCase = true) }
        }.getOrNull()
    }

    /**
     * Opens an RFCOMM socket using the three strategies required by the plan:
     * secure SDP lookup, insecure SDP lookup and finally a reflective channel-1
     * fallback for HC-05 clones that reject SDP.
     */
    @SuppressLint("MissingPermission")
    fun openSocket(device: BluetoothDevice): BluetoothSocket {
        runCatching { adapter?.cancelDiscovery() }

        var secureFailure: Exception? = null
        try {
            return connectSocket(device, secure = true)
        } catch (e: IOException) {
            secureFailure = e
            Log.w(TAG, "Secure RFCOMM socket failed for ${device.address}", e)
        }

        var insecureFailure: Exception? = null
        try {
            return connectSocket(device, secure = false)
        } catch (e: IOException) {
            insecureFailure = e
            Log.w(TAG, "Insecure RFCOMM socket failed for ${device.address}", e)
        }

        return try {
            connectSocketViaReflection(device)
        } catch (e: Exception) {
            Log.e(TAG, "RFCOMM channel fallback failed for ${device.address}", e)
            throw IOException(
                "Unable to open Bluetooth socket",
                insecureFailure ?: secureFailure ?: e
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun enableBluetooth(): Boolean {
        // Only used as a last-resort fallback; the UI prefers ACTION_REQUEST_ENABLE.
        @Suppress("DEPRECATION")
        return runCatching { adapter?.enable() == true }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    private fun connectSocket(device: BluetoothDevice, secure: Boolean): BluetoothSocket {
        val socket = if (secure) {
            device.createRfcommSocketToServiceRecord(BluetoothConstants.SPP_UUID)
        } else {
            device.createInsecureRfcommSocketToServiceRecord(BluetoothConstants.SPP_UUID)
        }
        return try {
            socket.connect()
            socket
        } catch (e: IOException) {
            runCatching { socket.close() }
            throw e
        }
    }

    private fun connectSocketViaReflection(device: BluetoothDevice): BluetoothSocket {
        val method = device.javaClass.getMethod(
            "createRfcommSocket",
            Int::class.javaPrimitiveType!!
        )
        val socket = method.invoke(device, BluetoothConstants.RFCOMM_CHANNEL) as BluetoothSocket
        return try {
            socket.connect()
            socket
        } catch (e: Exception) {
            runCatching { socket.close() }
            throw e
        }
    }

    private companion object {
        const val TAG = "BluetoothManager"
    }
}
