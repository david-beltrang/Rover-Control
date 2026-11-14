package com.example.rover_control.data.protocol

/**
 * Encodes [RoverCommand]s into the ASCII protocol used by the rover firmware.
 *
 * Every command is terminated with `\n` (Implementation_plan.md §5.2).
 */
object CommandEncoder {

    private const val TERMINATOR = "\n"

    fun encode(command: RoverCommand): ByteArray =
        encodeToString(command).toByteArray(Charsets.US_ASCII)

    fun encodeToString(command: RoverCommand): String {
        val payload = when (command) {
            RoverCommand.Forward -> "F"
            RoverCommand.Backward -> "B"
            RoverCommand.Left -> "L"
            RoverCommand.Right -> "R"
            RoverCommand.Stop -> "S"
            is RoverCommand.SetSpeed -> "V${command.value.coerceIn(0, 255)}"
            is RoverCommand.Headlights -> if (command.on) "W1" else "W0"
            RoverCommand.EmergencyStop -> "X"
        }
        return payload + TERMINATOR
    }
}
