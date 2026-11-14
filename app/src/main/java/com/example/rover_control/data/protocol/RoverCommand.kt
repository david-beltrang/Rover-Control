package com.example.rover_control.data.protocol

/**
 * Commands understood by the rover firmware.
 *
 * All commands are fire-and-forget and newline-terminated when encoded.
 * See Implementation_plan.md §5.2.
 */
sealed interface RoverCommand {
    data object Forward : RoverCommand
    data object Backward : RoverCommand
    data object Left : RoverCommand
    data object Right : RoverCommand
    data object Stop : RoverCommand
    data class SetSpeed(val value: Int) : RoverCommand
    data class Headlights(val on: Boolean) : RoverCommand
    data object EmergencyStop : RoverCommand
}
