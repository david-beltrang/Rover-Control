package com.example.rover_control.data.bluetooth

data class PairedDevice(
    val name: String,
    val address: String
) {
    val displayName: String
        get() = name.ifBlank { address }
}
