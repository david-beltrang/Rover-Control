package com.example.rover_control.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.rover_control.ui.theme.ErrorRed
import com.example.rover_control.ui.theme.PrimaryCyan
import com.example.rover_control.ui.theme.SurfaceVariantDark
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Touch pad that behaves like a D-pad but with a single pointer handler, so a
 * finger can slide from one direction to another without lifting
 * (Implementation_plan.md §6.3).
 */
@Composable
fun DirectionalPad(
    enabled: Boolean,
    onDirectionChange: (Direction?) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDirectionChange by rememberUpdatedState(onDirectionChange)
    val currentOnRelease by rememberUpdatedState(onRelease)
    var activeDirection by remember { mutableStateOf<Direction?>(null) }
    var pressed by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                fun directionAt(position: Offset): Direction? {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = position.x - center.x
                    val dy = position.y - center.y
                    val deadzone = size.width * 0.16f
                    if (hypot(dx, dy) < deadzone) return null
                    return if (abs(dx) > abs(dy)) {
                        if (dx > 0f) Direction.RIGHT else Direction.LEFT
                    } else {
                        if (dy > 0f) Direction.BACKWARD else Direction.FORWARD
                    }
                }

                fun update(position: Offset) {
                    val direction = directionAt(position)
                    if (direction != activeDirection) {
                        activeDirection = direction
                        currentOnDirectionChange(direction)
                    }
                }

                try {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        update(down.position)
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!pointer.pressed) {
                                activeDirection = null
                                pressed = false
                                currentOnRelease()
                                break
                            }
                            update(pointer.position)
                            pointer.consume()
                        }
                    }
                } finally {
                    pressed = false
                    if (activeDirection != null) {
                        activeDirection = null
                        currentOnRelease()
                    }
                }
            }
    ) {
        val gap = 10.dp
        val buttonSize: Dp = (maxWidth - gap * 2) / 3

        PadFace(
            icon = Icons.Filled.KeyboardArrowUp,
            active = pressed && activeDirection == Direction.FORWARD,
            size = buttonSize,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        PadFace(
            icon = Icons.Filled.KeyboardArrowDown,
            active = pressed && activeDirection == Direction.BACKWARD,
            size = buttonSize,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        PadFace(
            icon = Icons.Filled.KeyboardArrowLeft,
            active = pressed && activeDirection == Direction.LEFT,
            size = buttonSize,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        PadFace(
            icon = Icons.Filled.KeyboardArrowRight,
            active = pressed && activeDirection == Direction.RIGHT,
            size = buttonSize,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        PadFace(
            icon = Icons.Filled.Stop,
            active = pressed && activeDirection == null,
            size = buttonSize,
            tint = ErrorRed,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun PadFace(
    icon: ImageVector,
    active: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color = PrimaryCyan
) {
    val background = if (active) {
        PrimaryCyan.copy(alpha = 0.18f)
    } else {
        SurfaceVariantDark
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (active) PrimaryCyan.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) tint else tint.copy(alpha = 0.75f)
        )
    }
}
