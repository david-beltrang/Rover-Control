package com.example.rover_control.ui.screens.connection

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rover_control.R
import com.example.rover_control.RoverApplication
import com.example.rover_control.data.bluetooth.ConnectionState
import com.example.rover_control.data.bluetooth.PairedDevice
import com.example.rover_control.data.settings.RoverSettings
import com.example.rover_control.util.hasBluetoothPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectionUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected(),
    val pairedDevices: List<PairedDevice> = emptyList(),
    val isBluetoothSupported: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothPermission: Boolean = true,
    val demoMode: Boolean = false,
    val lastDeviceMac: String? = null,
    val errorMessage: String? = null,
    @param:StringRes val errorResId: Int? = null
)

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RoverApplication
    private val repository = app.roverRepository
    private val settingsRepository = app.settingsRepository
    private val bluetoothManager = app.bluetoothManager

    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    private var latestSettings = RoverSettings()
    private var lastReportedError: String? = null

    init {
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
                val error = (state as? ConnectionState.Disconnected)?.lastError
                when {
                    error != null && error != lastReportedError -> {
                        lastReportedError = error
                        _uiState.update { it.copy(errorMessage = error) }
                    }

                    state !is ConnectionState.Disconnected -> lastReportedError = null
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                latestSettings = settings
                _uiState.update { it.copy(lastDeviceMac = settings.lastDeviceMac) }
            }
        }
        viewModelScope.launch {
            repository.demoMode.collect { demo ->
                _uiState.update { it.copy(demoMode = demo) }
            }
        }
        refresh()
    }

    /** Re-reads permissions, adapter state and bonded devices. */
    fun refresh() {
        val context = getApplication<Application>()
        val hasPermission = context.hasBluetoothPermissions()
        val supported = bluetoothManager.isSupported
        val enabled = bluetoothManager.isEnabled
        val devices = if (hasPermission && enabled) {
            bluetoothManager.bondedDevices()
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                hasBluetoothPermission = hasPermission,
                isBluetoothSupported = supported,
                isBluetoothEnabled = enabled,
                pairedDevices = devices
            )
        }
    }

    fun onPermissionResult() {
        refresh()
    }

    fun connect(device: PairedDevice) {
        val bluetoothDevice = bluetoothManager.findBondedDevice(device.address)
        if (bluetoothDevice == null) {
            _uiState.update { it.copy(errorResId = R.string.connection_device_unavailable) }
            return
        }
        repository.connect(
            device = bluetoothDevice,
            initialSpeed = latestSettings.lastSpeed,
            headlightsOn = latestSettings.headlightsOn
        )
        viewModelScope.launch {
            settingsRepository.setLastDeviceMac(device.address)
        }
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun setDemoMode(enabled: Boolean) {
        repository.setDemoMode(enabled)
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null, errorResId = null) }
    }
}
