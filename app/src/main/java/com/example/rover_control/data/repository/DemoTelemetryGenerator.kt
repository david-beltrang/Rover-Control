package com.example.rover_control.data.repository

import com.example.rover_control.data.protocol.Telemetry
import kotlin.random.Random

/**
 * Produces plausible telemetry for Demo Mode when no hardware is available
 * (Implementation_plan.md §12).
 */
class DemoTelemetryGenerator(private val random: Random = Random.Default) {

    fun next(): Telemetry = Telemetry(
        temperatureC = random.nextDouble(18.0, 35.0).toFloat(),
        humidityPercent = random.nextDouble(30.0, 90.0).toFloat(),
        lightRaw = random.nextInt(100, 901),
        distanceCm = random.nextInt(5, 201)
    )
}
