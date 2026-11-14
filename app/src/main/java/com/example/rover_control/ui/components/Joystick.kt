package com.example.rover_control.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min

/**
 * Canvas joystick with edge-triggered cardinal directions and a 15 % deadzone.
 *
 * Only direction *changes* are reported, which keeps the HC-05 from being
 * flooded (Implementation_plan.md §6.3).
 */
@Composable
fun Joystick(
    enabled: Boolean,
    onDirectionChange: (Direction?) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDirectionChange by rememberUpdatedState(onDirectionChange)
    val currentOnRelease by rememberUpdatedState(onRelease)

    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var activeDirection by remember { mutableStateOf<Direction?>(null) }

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val ringColor = MaterialTheme.colorScheme.primary
    val knobColor = MaterialTheme.colorScheme.secondary
    val crosshairColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                fun update(position: Offset) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = min(size.width, size.height) / 2f * 0.92f
                    val travel = baseRadius * 0.55f
                    val deadzone = baseRadius * 0.15f

                    var delta = position - center
                    val distance = delta.getDistance()
                    if (distance > travel && distance > 0f) {
                        delta *= travel / distance
                    }
                    knobOffset = delta

                    val newDirection = when {
                        distance < deadzone -> null
                        abs(delta.x) > abs(delta.y) ->
                            if (delta.x > 0f) Direction.RIGHT else Direction.LEFT

                        else -> if (delta.y > 0f) Direction.BACKWARD else Direction.FORWARD
                    }

                    if (newDirection != activeDirection) {
                        activeDirection = newDirection
                        currentOnDirectionChange(newDirection)
                    }
                }

                try {
                    detectDragGestures(
                        onDragStart = { update(it) },
                        onDrag = { change, _ ->
                            change.consume()
                            update(change.position)
                        },
                        onDragEnd = {
                            activeDirection = null
                            knobOffset = Offset.Zero
                            currentOnRelease()
                        },
                        onDragCancel = {
                            activeDirection = null
                            knobOffset = Offset.Zero
                            currentOnRelease()
                        }
                    )
                } finally {
                    if (activeDirection != null || knobOffset != Offset.Zero) {
                        activeDirection = null
                        knobOffset = Offset.Zero
                        currentOnRelease()
                    }
                }
            }
    ) {
        val baseRadius = size.minDimension / 2f * 0.92f
        val travel = baseRadius * 0.55f
        val center = center

        drawCircle(color = baseColor, radius = baseRadius, center = center)
        drawCircle(
            color = ringColor.copy(alpha = 0.35f),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = ringColor.copy(alpha = 0.12f),
            radius = baseRadius * 0.7f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        val crosshairStart = baseRadius * 0.75f
        drawLine(
            color = crosshairColor,
            start = Offset(center.x - crosshairStart, center.y),
            end = Offset(center.x + crosshairStart, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = crosshairColor,
            start = Offset(center.x, center.y - crosshairStart),
            end = Offset(center.x, center.y + crosshairStart),
            strokeWidth = 1.dp.toPx()
        )

        var knobCenter = center + knobOffset
        val knobDistance = (knobCenter - center).getDistance()
        if (knobDistance > travel && knobDistance > 0f) {
            knobCenter = center + (knobCenter - center) * (travel / knobDistance)
        }

        drawCircle(
            color = knobColor.copy(alpha = 0.3f),
            radius = baseRadius * 0.34f,
            center = knobCenter
        )
        drawCircle(
            color = knobColor,
            radius = baseRadius * 0.24f,
            center = knobCenter
        )
        drawCircle(
            color = ringColor.copy(alpha = 0.8f),
            radius = baseRadius * 0.08f,
            center = knobCenter
        )
    }
}

/** Small label shown above the joystick while a direction is active. */
@Composable
fun DirectionLabel(
    direction: Direction?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = direction != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        androidx.compose.material3.Text(
            text = direction.label(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun Direction?.label(): String = when (this) {
    Direction.FORWARD -> androidx.compose.ui.res.stringResource(com.example.rover_control.R.string.control_direction_fwd)
    Direction.BACKWARD -> androidx.compose.ui.res.stringResource(com.example.rover_control.R.string.control_direction_back)
    Direction.LEFT -> androidx.compose.ui.res.stringResource(com.example.rover_control.R.string.control_direction_left)
    Direction.RIGHT -> androidx.compose.ui.res.stringResource(com.example.rover_control.R.string.control_direction_right)
    null -> ""
}
