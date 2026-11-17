package com.example.rover_control.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rover_control.R
import com.example.rover_control.ui.theme.HudAccentCyan
import com.example.rover_control.ui.theme.HudBackground
import com.example.rover_control.ui.theme.HudCyanGlow
import com.example.rover_control.ui.theme.HudLightRed
import com.example.rover_control.ui.theme.HudRedBorder
import com.example.rover_control.ui.theme.HudRedSurface
import com.example.rover_control.ui.theme.HudSurface
import com.example.rover_control.ui.theme.HudSurfaceControl
import com.example.rover_control.ui.theme.HudSurfacePressed
import com.example.rover_control.ui.theme.HudTealSurface
import com.example.rover_control.ui.theme.HudTextPrimary
import com.example.rover_control.ui.theme.HudTextSecondary
import com.example.rover_control.ui.theme.HudTrack
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private val HudShape = RoundedCornerShape(24.dp)
private val HudFont = FontFamily.SansSerif
private val HudMono = FontFamily.Monospace

/** Soft, layered halo used instead of hardware BlurMaskFilter (API 26-safe). */
private fun Modifier.hudGlow(
    color: Color,
    radius: Dp = 14.dp,
    corner: Dp = 20.dp
): Modifier = if (color.alpha == 0f) {
    this
} else {
    drawBehind {
        val glow = radius.toPx()
        val cornerRadius = CornerRadius(corner.toPx())
        for (step in 4 downTo 1) {
            val expansion = glow * step / 4f
            drawRoundRect(
                color = color.copy(alpha = 0.035f),
                topLeft = Offset(-expansion, -expansion),
                size = Size(size.width + expansion * 2f, size.height + expansion * 2f),
                cornerRadius = CornerRadius(cornerRadius.x + expansion)
            )
        }
    }
}

@Composable
internal fun HudPanel(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(8.dp, HudShape, clip = false)
            .clip(HudShape)
            .background(HudSurface)
            .border(1.dp, Color.White.copy(alpha = 0.04f), HudShape)
            .padding(16.dp),
        content = content
    )
}

@Composable
internal fun HudCardTitle(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HudAccentCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = HudTextPrimary,
            fontFamily = HudFont,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            maxLines = 1
        )
    }
}

@Composable
internal fun SteeringModeSwitch(
    isPadMode: Boolean,
    enabled: Boolean,
    onModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF23272D))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeSegment(
            icon = Icons.Filled.GridView,
            label = stringResource(R.string.control_mode_pad),
            selected = isPadMode,
            enabled = enabled,
            onClick = { onModeChange(true) }
        )
        ModeSegment(
            icon = Icons.Filled.SportsEsports,
            label = stringResource(R.string.control_mode_stick),
            selected = !isPadMode,
            enabled = enabled,
            onClick = { onModeChange(false) }
        )
    }
}

@Composable
private fun ModeSegment(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) HudTealSurface else Color.Transparent,
        label = "modeBackground"
    )
    val contentColor = if (selected) HudTextPrimary else HudTextSecondary
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .hudGlow(
                color = if (selected) HudCyanGlow else Color.Transparent,
                radius = 10.dp,
                corner = 16.dp
            )
            .clip(CircleShape)
            .background(background)
            .then(
                if (selected) {
                    Modifier.border(1.dp, HudAccentCyan.copy(alpha = 0.55f), CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) HudAccentCyan else contentColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = contentColor,
            fontFamily = HudFont,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            maxLines = 1
        )
    }
}

/**
 * Five-button HUD D-pad. A single pointer handler covers the whole pad so the
 * finger can slide between directions without lifting, matching the original
 * edge-triggered control behaviour.
 */
@Composable
internal fun HudDirectionalPad(
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
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val side = minOf(maxWidth, maxHeight)
        Box(
            modifier = Modifier
                .size(side)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput

                    fun directionAt(position: Offset): Direction? {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = position.x - center.x
                        val dy = position.y - center.y
                        if (hypot(dx, dy) < size.width * 0.16f) return null
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
            DirectionFace(
                icon = Icons.Filled.ArrowDropUp,
                contentDescription = stringResource(R.string.control_forward),
                active = pressed && activeDirection == Direction.FORWARD,
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(0.27f).aspectRatio(1f)
            )
            DirectionFace(
                icon = Icons.Filled.ArrowDropUp,
                contentDescription = stringResource(R.string.control_backward),
                active = pressed && activeDirection == Direction.BACKWARD,
                rotation = 180f,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.27f).aspectRatio(1f)
            )
            DirectionFace(
                icon = Icons.Filled.ArrowDropUp,
                contentDescription = stringResource(R.string.control_left),
                active = pressed && activeDirection == Direction.LEFT,
                rotation = -90f,
                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.27f).aspectRatio(1f)
            )
            DirectionFace(
                icon = Icons.Filled.ArrowDropUp,
                contentDescription = stringResource(R.string.control_right),
                active = pressed && activeDirection == Direction.RIGHT,
                rotation = 90f,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.27f).aspectRatio(1f)
            )
            CenterStopFace(
                active = pressed && activeDirection == null,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.24f).aspectRatio(1f)
            )
        }
    }
}

@Composable
private fun DirectionFace(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    rotation: Float = 0f
) {
    val background by animateColorAsState(
        targetValue = if (active) HudSurfacePressed else HudSurfaceControl,
        label = "directionBackground"
    )
    Box(
        modifier = modifier
            .hudGlow(
                color = if (active) HudCyanGlow else Color.Transparent,
                radius = 14.dp,
                corner = 28.dp
            )
            .clip(CircleShape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (active) HudAccentCyan.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.05f),
                shape = CircleShape
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) HudAccentCyan else HudTextPrimary.copy(alpha = 0.85f),
            modifier = Modifier.size(26.dp).rotate(rotation)
        )
    }
}

@Composable
private fun CenterStopFace(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .hudGlow(
                color = if (active) HudAccentCyan.copy(alpha = 0.8f) else HudCyanGlow,
                radius = 16.dp,
                corner = 28.dp
            )
            .clip(CircleShape)
            .background(HudSurfacePressed)
            .border(1.dp, HudAccentCyan.copy(alpha = if (active) 0.9f else 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = center
            val glowRadius = size.minDimension * 0.34f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(HudAccentCyan.copy(alpha = 0.55f), Color.Transparent),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )
            drawCircle(
                color = HudAccentCyan,
                radius = size.minDimension * 0.10f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = size.minDimension * 0.035f,
                center = center
            )
        }
    }
}

/** HUD analog joystick used by the STICK segment. */
@Composable
internal fun HudJoystick(
    enabled: Boolean,
    onDirectionChange: (Direction?) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDirectionChange by rememberUpdatedState(onDirectionChange)
    val currentOnRelease by rememberUpdatedState(onRelease)
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var activeDirection by remember { mutableStateOf<Direction?>(null) }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                fun update(position: Offset) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = min(size.width, size.height) / 2f * 0.9f
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
        val baseRadius = size.minDimension / 2f * 0.9f
        val travel = baseRadius * 0.55f
        val center = center

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(HudCyanGlow, Color.Transparent),
                center = center,
                radius = baseRadius * 1.22f
            ),
            radius = baseRadius * 1.22f,
            center = center
        )
        drawCircle(color = HudBackground, radius = baseRadius, center = center)
        drawCircle(color = HudSurfaceControl, radius = baseRadius * 0.92f, center = center)
        drawCircle(
            color = HudAccentCyan.copy(alpha = 0.45f),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = HudAccentCyan.copy(alpha = 0.18f),
            radius = baseRadius * 0.72f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        val tickStart = baseRadius * 0.77f
        val tickEnd = baseRadius * 0.88f
        listOf(
            Offset(center.x, center.y - tickStart) to Offset(center.x, center.y - tickEnd),
            Offset(center.x, center.y + tickStart) to Offset(center.x, center.y + tickEnd),
            Offset(center.x - tickStart, center.y) to Offset(center.x - tickEnd, center.y),
            Offset(center.x + tickStart, center.y) to Offset(center.x + tickEnd, center.y)
        ).forEach { (start, end) ->
            drawLine(
                color = HudTextSecondary.copy(alpha = 0.5f),
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        var knobCenter = center + knobOffset
        val knobDistance = (knobCenter - center).getDistance()
        if (knobDistance > travel && knobDistance > 0f) {
            knobCenter = center + (knobCenter - center) * (travel / knobDistance)
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(HudAccentCyan.copy(alpha = 0.35f), Color.Transparent),
                center = knobCenter,
                radius = baseRadius * 0.42f
            ),
            radius = baseRadius * 0.42f,
            center = knobCenter
        )
        drawCircle(
            color = Color(0xFF162B2E),
            radius = baseRadius * 0.29f,
            center = knobCenter
        )
        drawCircle(
            color = HudAccentCyan.copy(alpha = 0.7f),
            radius = baseRadius * 0.29f,
            center = knobCenter,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = HudAccentCyan,
            radius = baseRadius * 0.11f,
            center = knobCenter
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = baseRadius * 0.04f,
            center = knobCenter
        )
    }
}

@Composable
internal fun HudVerticalSpeedSlider(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val description = stringResource(R.string.content_description_speed_slider)

    Column(
        modifier = modifier.semantics {
            contentDescription = description
            progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..100f)
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.control_speed_percent, value.roundToInt()),
            color = if (enabled) HudAccentCyan else HudTextSecondary,
            fontFamily = HudMono,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        val thumbRadius = 18.dp.toPx()

                        fun update(positionY: Float) {
                            val top = thumbRadius
                            val bottom = size.height - thumbRadius
                            val usable = (bottom - top).coerceAtLeast(1f)
                            val fraction = ((bottom - positionY) / usable).coerceIn(0f, 1f)
                            currentOnValueChange(fraction * 100f)
                        }

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            update(down.position.y)
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!pointer.pressed) break
                                update(pointer.position.y)
                                pointer.consume()
                            }
                        }
                    }
            ) {
                val thumbRadius = 18.dp.toPx()
                val trackWidth = 10.dp.toPx()
                val top = thumbRadius
                val bottom = size.height - thumbRadius
                val usable = (bottom - top).coerceAtLeast(1f)
                val fraction = value.coerceIn(0f, 100f) / 100f
                val thumbY = bottom - usable * fraction
                val centerX = size.width / 2f
                val trackAlpha = if (enabled) 1f else 0.45f

                drawRoundRect(
                    color = HudTrack.copy(alpha = trackAlpha),
                    topLeft = Offset(centerX - trackWidth / 2f, top),
                    size = Size(trackWidth, bottom - top),
                    cornerRadius = CornerRadius(trackWidth / 2f)
                )
                if (bottom - thumbY > 0f) {
                    drawRoundRect(
                        color = HudAccentCyan.copy(alpha = trackAlpha),
                        topLeft = Offset(centerX - trackWidth / 2f, thumbY),
                        size = Size(trackWidth, bottom - thumbY),
                        cornerRadius = CornerRadius(trackWidth / 2f)
                    )
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HudAccentCyan.copy(alpha = 0.42f * trackAlpha),
                            Color.Transparent
                        ),
                        center = Offset(centerX, thumbY),
                        radius = thumbRadius * 1.8f
                    ),
                    radius = thumbRadius * 1.8f,
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = Color(0xFF162B2E),
                    radius = thumbRadius,
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = HudAccentCyan.copy(alpha = 0.7f * trackAlpha),
                    radius = thumbRadius,
                    center = Offset(centerX, thumbY),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = HudAccentCyan.copy(alpha = trackAlpha),
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f * trackAlpha),
                    radius = 2.2.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
            }
        }
        Text(
            text = stringResource(R.string.control_control_speed),
            color = HudTextSecondary,
            fontFamily = HudFont,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
internal fun HudCircleButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    dashedBorder: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "circleScale"
    )
    val glow = active

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hudGlow(
                color = if (glow) HudCyanGlow else Color.Transparent,
                radius = 18.dp,
                corner = 34.dp
            )
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF141E26), Color(0xFF0E151B)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            if (dashedBorder) {
                drawCircle(
                    color = HudAccentCyan.copy(alpha = if (active) 0.8f else 0.35f),
                    radius = radius - 1.dp.toPx(),
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                        )
                    )
                )
            } else {
                drawCircle(
                    color = if (active) HudAccentCyan.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.08f),
                    radius = radius - 0.5.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (glow) HudAccentCyan else HudTextSecondary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = title,
                color = HudTextPrimary,
                fontFamily = HudFont,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                lineHeight = 9.sp,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = if (glow) HudAccentCyan else HudTextSecondary,
                fontFamily = HudFont,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                lineHeight = 8.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun HudPillButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "pillScale"
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            }
            .hudGlow(
                color = if (active) {
                    if (contentColor == HudLightRed) {
                        HudRedBorder.copy(alpha = 0.65f)
                    } else {
                        HudCyanGlow
                    }
                } else {
                    Color.Transparent
                },
                radius = 14.dp,
                corner = 22.dp
            )
            .clip(CircleShape)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (active) {
                    if (contentColor == HudLightRed) {
                        HudRedBorder
                    } else {
                        HudAccentCyan.copy(alpha = 0.65f)
                    }
                } else {
                    Color.White.copy(alpha = 0.04f)
                },
                shape = CircleShape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = contentColor,
            fontFamily = HudFont,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            maxLines = 1
        )
    }
}

internal enum class HudTab {
    CONTROLS,
    TELEMETRY
}

@Composable
internal fun HudBottomTabBar(
    selectedTab: HudTab,
    controlsEnabled: Boolean,
    telemetryEnabled: Boolean,
    onControlsClick: () -> Unit,
    onTelemetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .shadow(8.dp, RoundedCornerShape(26.dp), clip = false)
            .clip(RoundedCornerShape(26.dp))
            .background(HudSurface)
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(26.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HudTabItem(
            icon = Icons.Filled.SportsEsports,
            label = stringResource(R.string.control_nav_controls),
            selected = selectedTab == HudTab.CONTROLS,
            enabled = controlsEnabled,
            onClick = onControlsClick,
            modifier = Modifier.weight(1f)
        )
        HudTabItem(
            icon = Icons.Filled.MonitorHeart,
            label = stringResource(R.string.control_nav_telemetry),
            selected = selectedTab == HudTab.TELEMETRY,
            enabled = telemetryEnabled,
            onClick = onTelemetryClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HudTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .hudGlow(
                color = if (selected) HudCyanGlow else Color.Transparent,
                radius = 14.dp,
                corner = 22.dp
            )
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) HudTealSurface else Color.Transparent)
            .then(
                if (selected) {
                    Modifier.border(1.dp, HudAccentCyan.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled && !selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) HudAccentCyan else HudTextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = if (selected) HudAccentCyan else HudTextSecondary,
            fontFamily = HudFont,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            letterSpacing = 1.sp,
            maxLines = 1
        )
    }
}
