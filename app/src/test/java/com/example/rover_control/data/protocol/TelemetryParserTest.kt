package com.example.rover_control.data.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelemetryParserTest {

    private val parser = TelemetryParser()

    @Test
    fun `parses a complete line`() {
        val telemetry = parser.parse("""{"t":25.3,"h":60.2,"l":412,"d":35,"i":12}""")

        assertEquals(25.3f, telemetry!!.temperatureC!!, 0.001f)
        assertEquals(60.2f, telemetry.humidityPercent!!, 0.001f)
        assertEquals(412, telemetry.lightRaw)
        assertEquals(35, telemetry.distanceCm)
        assertEquals(12f, telemetry.inclinationDeg!!, 0.001f)
    }

    @Test
    fun `missing fields keep the last known values`() {
        parser.parse("""{"t":25.3,"h":60.2,"l":412,"d":35,"i":12}""")
        val telemetry = parser.parse("""{"t":26.0}""")!!

        assertEquals(26.0f, telemetry.temperatureC!!, 0.001f)
        assertEquals(60.2f, telemetry.humidityPercent!!, 0.001f)
        assertEquals(412, telemetry.lightRaw)
        assertEquals(35, telemetry.distanceCm)
        assertEquals(12f, telemetry.inclinationDeg!!, 0.001f)
    }

    @Test
    fun `first partial line leaves unknown fields empty`() {
        val telemetry = parser.parse("""{"t":20.0}""")!!

        assertEquals(20f, telemetry.temperatureC!!, 0.001f)
        assertNull(telemetry.humidityPercent)
        assertNull(telemetry.lightRaw)
        assertNull(telemetry.distanceCm)
        assertNull(telemetry.inclinationDeg)
    }

    @Test
    fun `unknown fields are ignored`() {
        val telemetry = parser.parse("""{"t":20.0,"a":["HOT"],"ts":123456}""")!!

        assertEquals(20f, telemetry.temperatureC!!, 0.001f)
        assertEquals(0, parser.invalidLineCount)
    }

    @Test
    fun `malformed json is ignored and counted`() {
        assertNull(parser.parse("""{"t":20.0"""))
        assertNull(parser.parse("""not json"""))
        assertEquals(2, parser.invalidLineCount)
    }

    @Test
    fun `empty line is ignored without being counted`() {
        assertNull(parser.parse(""))
        assertNull(parser.parse("   \r"))
        assertEquals(0, parser.invalidLineCount)
    }

    @Test
    fun `non numeric values keep the last known value`() {
        parser.parse("""{"t":20.0}""")
        val telemetry = parser.parse("""{"t":"oops"}""")!!

        assertEquals(20f, telemetry.temperatureC!!, 0.001f)
        assertEquals(0, parser.invalidLineCount)
    }

    @Test
    fun `decimal values are rounded for integer fields`() {
        val telemetry = parser.parse("""{"l":412.6,"d":34.4}""")!!

        assertEquals(413, telemetry.lightRaw)
        assertEquals(34, telemetry.distanceCm)
    }

    @Test
    fun `null values are treated as missing`() {
        parser.parse("""{"d":35}""")
        val telemetry = parser.parse("""{"d":null}""")!!

        assertEquals(35, telemetry.distanceCm)
    }

    @Test
    fun `reset clears last values and counters`() {
        parser.parse("""{"t":20.0}""")
        parser.parse("broken")
        parser.reset()

        assertEquals(0, parser.invalidLineCount)
        assertNull(parser.parse("""{"h":50.0}""")!!.temperatureC)
    }
}
