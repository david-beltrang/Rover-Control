package com.example.rover_control.data.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandEncoderTest {

    @Test
    fun `movement commands encode to a single-letter line`() {
        assertArrayEquals("F\n".toByteArray(Charsets.US_ASCII), CommandEncoder.encode(RoverCommand.Forward))
        assertArrayEquals("B\n".toByteArray(Charsets.US_ASCII), CommandEncoder.encode(RoverCommand.Backward))
        assertArrayEquals("L\n".toByteArray(Charsets.US_ASCII), CommandEncoder.encode(RoverCommand.Left))
        assertArrayEquals("R\n".toByteArray(Charsets.US_ASCII), CommandEncoder.encode(RoverCommand.Right))
        assertArrayEquals("S\n".toByteArray(Charsets.US_ASCII), CommandEncoder.encode(RoverCommand.Stop))
        assertArrayEquals("X\n".toByteArray(Charsets.US_ASCII), CommandEncoder.encode(RoverCommand.EmergencyStop))
    }

    @Test
    fun `speed is encoded as V plus value`() {
        assertEquals("V0\n", CommandEncoder.encodeToString(RoverCommand.SetSpeed(0)))
        assertEquals("V128\n", CommandEncoder.encodeToString(RoverCommand.SetSpeed(128)))
        assertEquals("V255\n", CommandEncoder.encodeToString(RoverCommand.SetSpeed(255)))
    }

    @Test
    fun `speed is clamped to the protocol range`() {
        assertEquals("V0\n", CommandEncoder.encodeToString(RoverCommand.SetSpeed(-10)))
        assertEquals("V255\n", CommandEncoder.encodeToString(RoverCommand.SetSpeed(300)))
    }

    @Test
    fun `headlights encode to W1 and W0`() {
        assertEquals("W1\n", CommandEncoder.encodeToString(RoverCommand.Headlights(true)))
        assertEquals("W0\n", CommandEncoder.encodeToString(RoverCommand.Headlights(false)))
    }

    @Test
    fun `buzzer encodes to B1 and B0`() {
        assertEquals("B1\n", CommandEncoder.encodeToString(RoverCommand.Buzzer(true)))
        assertEquals("B0\n", CommandEncoder.encodeToString(RoverCommand.Buzzer(false)))
    }
}
