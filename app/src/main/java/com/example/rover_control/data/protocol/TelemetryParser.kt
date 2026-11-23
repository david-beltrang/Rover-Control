package com.example.rover_control.data.protocol

import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Parses newline-delimited JSON telemetry lines.
 *
 * Tolerance rules (Implementation_plan.md §5.3):
 *  - lines that are not `{ ... }` are ignored;
 *  - unknown fields are ignored;
 *  - malformed JSON is counted and ignored instead of crashing;
 *  - missing fields keep the last known value.
 *
 * The parser is not thread-safe on its own: it is designed to be driven by a
 * single reader coroutine.
 */
class TelemetryParser {

    var invalidLineCount: Int = 0
        private set

    private var last: Telemetry = Telemetry.EMPTY

    /**
     * @return the merged telemetry sample, or `null` when the line must be ignored.
     */
    fun parse(line: String): Telemetry? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) {
            invalidLineCount++
            return null
        }

        val json = try {
            JSONObject(trimmed)
        } catch (_: JSONException) {
            invalidLineCount++
            return null
        }

        val merged = Telemetry(
            temperatureC = json.optFloatOrNull(KEY_TEMPERATURE) ?: last.temperatureC,
            humidityPercent = json.optFloatOrNull(KEY_HUMIDITY) ?: last.humidityPercent,
            lightRaw = json.optIntOrNull(KEY_LIGHT) ?: last.lightRaw,
            distanceCm = json.optIntOrNull(KEY_DISTANCE) ?: last.distanceCm
        )
        last = merged
        return merged
    }

    fun reset() {
        last = Telemetry.EMPTY
        invalidLineCount = 0
    }

    private fun JSONObject.optFloatOrNull(key: String): Float? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key)
        return if (value.isFinite()) value.toFloat() else null
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key)
        return if (value.isFinite()) value.roundToInt() else null
    }

    private companion object {
        const val KEY_TEMPERATURE = "t"
        const val KEY_HUMIDITY = "h"
        const val KEY_LIGHT = "l"
        const val KEY_DISTANCE = "d"
    }
}
