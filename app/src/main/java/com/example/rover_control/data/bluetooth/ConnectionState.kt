package com.example.rover_control.data.bluetooth

/**
 * State of the link to the rover.
 *
 * [Connected.isDemo] marks the simulated connection used by Demo Mode so the UI
 * can show a distinct badge (Implementation_plan.md §12).
 */
sealed interface ConnectionState {
    data class Disconnected(val lastError: String? = null) : ConnectionState
    data class Connecting(val deviceName: String? = null) : ConnectionState
    data class Connected(val deviceName: String, val isDemo: Boolean = false) : ConnectionState
}

val ConnectionState.isConnected: Boolean
    get() = this is ConnectionState.Connected

val ConnectionState.isDemo: Boolean
    get() = this is ConnectionState.Connected && isDemo
