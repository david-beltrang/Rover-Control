package com.example.rover_control.ui.components

import com.example.rover_control.data.protocol.RoverCommand

enum class Direction {
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT
}

fun Direction.toRoverCommand(): RoverCommand = when (this) {
    Direction.FORWARD -> RoverCommand.Forward
    Direction.BACKWARD -> RoverCommand.Backward
    Direction.LEFT -> RoverCommand.Left
    Direction.RIGHT -> RoverCommand.Right
}
