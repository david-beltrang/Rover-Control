package com.example.rover_control.data.protocol

/**
 * One telemetry sample received from the rover.
 *
 * Fields are nullable because the firmware may omit keys; the parser keeps the
 * last known value in that case (Implementation_plan.md §5.3).
 */
data class Telemetry(
    val temperatureC: Float? = null,
    val humidityPercent: Float? = null,
    val lightRaw: Int? = null,
    val distanceCm: Int? = null
) {
    val isEmpty: Boolean
        get() = temperatureC == null &&
            humidityPercent == null &&
            lightRaw == null &&
            distanceCm == null

    companion object {
        val EMPTY = Telemetry()
    }
}
