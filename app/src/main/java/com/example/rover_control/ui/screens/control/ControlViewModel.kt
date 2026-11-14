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
    val inputMode: InputMode = InputMode.JOYSTICK,
    val speedPercent: Float = speedByteToPercent(DEFAULT_SPEED),
    val headlightsOn: Boolean = false,
    val isStopped: Boolean = false
)

class ControlViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RoverApplication
    private val repository = app.roverRepository
    private val settingsRepository = app.settingsRepository

    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    private var speedJob: Job? = null
    private var speedBeforeStop: Float = speedByteToPercent(DEFAULT_SPEED)

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
                            it.copy(isStopped = true, speedPercent = 0f, headlightsOn = false)
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
        if (_uiState.value.isStopped) return
        repository.sendCommand(command ?: RoverCommand.Stop)
    }

    fun onRelease() {
        if (_uiState.value.isStopped) return
        repository.sendCommand(RoverCommand.Stop)
    }

    fun onSpeedChange(percent: Float) {
        if (_uiState.value.isStopped) return
        val clamped = percent.coerceIn(0f, 100f)
        _uiState.update { it.copy(speedPercent = clamped) }

        speedJob?.cancel()
        speedJob = viewModelScope.launch {
            delay(SPEED_DEBOUNCE_MS)
            persistAndSendSpeed(clamped)
        }
    }

    fun resetSpeed() {
        onSpeedChange(DEFAULT_SPEED_PERCENT)
    }

    fun toggleHeadlights(on: Boolean) {
        if (_uiState.value.isStopped) return
        _uiState.update { it.copy(headlightsOn = on) }
        repository.sendCommand(RoverCommand.Headlights(on))
        viewModelScope.launch { settingsRepository.setHeadlightsOn(on) }
    }

    fun setInputMode(mode: InputMode) {
        if (_uiState.value.inputMode == mode) return
        _uiState.update { it.copy(inputMode = mode) }
        viewModelScope.launch { settingsRepository.setInputMode(mode) }
    }

    fun emergencyStop() {
        repository.emergencyStop()
    }

    fun rearm() {
        val restore = speedBeforeStop
        val speedByte = speedPercentToByte(restore)
        repository.rearm(speedByte)
        _uiState.update { it.copy(isStopped = false, speedPercent = restore) }

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
        const val DEFAULT_SPEED_PERCENT = 60f
    }
}
