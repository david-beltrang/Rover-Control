package com.example.rover_control

import android.app.Application
import com.example.rover_control.data.bluetooth.BluetoothManager
import com.example.rover_control.data.repository.RoverRepository
import com.example.rover_control.data.settings.SettingsRepository

/**
 * Manual dependency container. The repository lives for the whole process so
 * the Bluetooth socket survives configuration changes and screen navigation.
 */
class RoverApplication : Application() {

    lateinit var bluetoothManager: BluetoothManager
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var roverRepository: RoverRepository
        private set

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = BluetoothManager(this)
        settingsRepository = SettingsRepository(this)
        roverRepository = RoverRepository(bluetoothManager)
    }
}
