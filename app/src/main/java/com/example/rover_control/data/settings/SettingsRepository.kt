package com.example.rover_control.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

const val DEFAULT_SPEED = 153 // 60 % of 255

enum class InputMode(val storageValue: String) {
    JOYSTICK("joystick"),
    DPAD("dpad");

    companion object {
        fun fromStorage(value: String?): InputMode =
            entries.firstOrNull { it.storageValue == value } ?: JOYSTICK
    }
}

data class RoverSettings(
    val inputMode: InputMode = InputMode.JOYSTICK,
    val lastSpeed: Int = DEFAULT_SPEED,
    val headlightsOn: Boolean = false,
    val lastDeviceMac: String? = null
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "rover_settings"
)

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<RoverSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { it.toRoverSettings() }

    suspend fun setInputMode(mode: InputMode) {
        dataStore.edit { it[KEY_INPUT_MODE] = mode.storageValue }
    }

    suspend fun setLastSpeed(speed: Int) {
        dataStore.edit { it[KEY_LAST_SPEED] = speed.coerceIn(0, 255) }
    }

    suspend fun setHeadlightsOn(on: Boolean) {
        dataStore.edit { it[KEY_HEADLIGHTS_ON] = on }
    }

    suspend fun setLastDeviceMac(mac: String?) {
        dataStore.edit { preferences ->
            if (mac.isNullOrBlank()) {
                preferences.remove(KEY_LAST_DEVICE_MAC)
            } else {
                preferences[KEY_LAST_DEVICE_MAC] = mac
            }
        }
    }

    private fun Preferences.toRoverSettings(): RoverSettings = RoverSettings(
        inputMode = InputMode.fromStorage(this[KEY_INPUT_MODE]),
        lastSpeed = (this[KEY_LAST_SPEED] ?: DEFAULT_SPEED).coerceIn(0, 255),
        headlightsOn = this[KEY_HEADLIGHTS_ON] ?: false,
        lastDeviceMac = this[KEY_LAST_DEVICE_MAC]
    )

    private companion object {
        val KEY_INPUT_MODE = stringPreferencesKey("input_mode")
        val KEY_LAST_SPEED = intPreferencesKey("last_speed")
        val KEY_HEADLIGHTS_ON = booleanPreferencesKey("headlights_on")
        val KEY_LAST_DEVICE_MAC = stringPreferencesKey("last_device_mac")
    }
}
