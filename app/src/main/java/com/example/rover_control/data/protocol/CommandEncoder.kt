package com.example.rover_control.data.protocol


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
            is RoverCommand.Buzzer -> if (command.on) "B1" else "B0"
            RoverCommand.EmergencyStop -> "X"
        }
        return payload + TERMINATOR
    }
}
