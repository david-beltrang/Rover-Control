package com.example.rover_control.ui.screens.telemetry

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rover_control.RoverApplication
import com.example.rover_control.data.bluetooth.ConnectionState
import com.example.rover_control.data.protocol.Telemetry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TelemetryUiState(
    val telemetry: Telemetry = Telemetry.EMPTY,
    val hasReceivedTelemetry: Boolean = false,
    val ageMillis: Long? = null,
    val isStale: Boolean = true,
    val connectionState: ConnectionState = ConnectionState.Disconnected()
)

class TelemetryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as RoverApplication).roverRepository

    private val _uiState = MutableStateFlow(TelemetryUiState())
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                val lastAt = repository.lastTelemetryAt.value
                val age = lastAt?.let { SystemClock.elapsedRealtime() - it }
                _uiState.value = TelemetryUiState(
                    telemetry = repository.telemetry.value,
                    hasReceivedTelemetry = lastAt != null,
                    ageMillis = age,
                    isStale = age == null || age > STALE_AFTER_MS,
                    connectionState = repository.connectionState.value
                )
                delay(TICK_MS)
            }
        }
    }

    fun emergencyStop() {
        repository.emergencyStop()
    }

    private companion object {
        const val TICK_MS = 500L
        const val STALE_AFTER_MS = 3_000L
    }
}
