package com.example.rover_control.data.bluetooth

import java.util.UUID

object BluetoothConstants {
    /** Standard Bluetooth Classic Serial Port Profile UUID. */
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /** RFCOMM channel used by HC-05 clones that reject the SDP lookup. */
    const val RFCOMM_CHANNEL = 1
}
