package com.example.rover_control.util

import kotlin.math.roundToInt

/** Maps the 0..100 UI slider to the 0..255 value sent to the firmware. */
fun speedPercentToByte(percent: Float): Int =
    (percent.coerceIn(0f, 100f) * 255f / 100f).roundToInt()

/** Maps a persisted 0..255 speed back to the 0..100 slider. */
fun speedByteToPercent(speed: Int): Float =
    speed.coerceIn(0, 255) * 100f / 255f

/** Speed as a percentage rounded for labels. */
fun speedByteToPercentRounded(speed: Int): Int =
    speedByteToPercent(speed).roundToInt()
