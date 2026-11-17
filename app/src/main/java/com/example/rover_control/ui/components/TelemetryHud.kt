package com.example.rover_control.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rover_control.R
import com.example.rover_control.data.bluetooth.ConnectionState
import com.example.rover_control.ui.theme.ErrorRed
import com.example.rover_control.ui.theme.HudLightRed
import com.example.rover_control.ui.theme.HudRedBorder
import com.example.rover_control.ui.theme.HudRedSurface
import com.example.rover_control.ui.theme.TelemetryHudAccent
import com.example.rover_control.ui.theme.TelemetryHudBackground
import com.example.rover_control.ui.theme.TelemetryHudGrid
import com.example.rover_control.ui.theme.TelemetryHudIconCircle
import com.example.rover_control.ui.theme.TelemetryHudSurface
import com.example.rover_control.ui.theme.TelemetryHudSurfaceInner
import com.example.rover_control.ui.theme.TelemetryHudTealSurface
import com.example.rover_control.ui.theme.TelemetryHudTextPrimary
import com.example.rover_control.ui.theme.TelemetryHudTextSecondary
import com.example.rover_control.ui.theme.TelemetryHudTopBar
import com.example.rover_control.ui.theme.TelemetryHudTrack
import com.example.rover_control.ui.theme.WarningAmber
import kotlin.math.ceil
import kotlin.math.min

private val TelemetryPanelShape = RoundedCornerShape(20.dp)
private val TelemetryInsetShape = RoundedCornerShape(14.dp)
private val TelemetryHudFont = FontFamily.SansSerif
internal val TelemetryNumberFont = FontFamily.Monospace

@Composable
internal fun TelemetryTopBar(
    connectionState: ConnectionState,
    isStale: Boolean,
    isStopped: Boolean,
    onOpenConnection: () -> Unit,
    onEmergencyStop: () -> Unit
) {
    val connectionLabel = when (connectionState) {
        is ConnectionState.Connected -> stringResource(R.string.connection_connected)
        is ConnectionState.Connecting -> stringResource(R.string.status_connecting)
        is ConnectionState.Disconnected -> stringResource(R.string.status_disconnected)
    }
    val ledColor = when {
        connectionState is ConnectionState.Disconnected -> ErrorRed
        connectionState is ConnectionState.Connecting -> WarningAmber
        isStale -> WarningAmber
        else -> TelemetryHudAccent
    }
    val openConnectionLabel = stringResource(R.string.content_description_open_connection)
    val emergencyStopLabel = stringResource(R.string.content_description_emergency_stop)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TelemetryHudTopBar)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenConnection)
                    .semantics {
                        contentDescription = openConnectionLabel
                        role = Role.Button
                    }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusLed(color = ledColor, description = connectionLabel)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.telemetry_title),
                        color = TelemetryHudTextPrimary,
                        fontFamily = TelemetryHudFont,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = connectionLabel.uppercase(),
                        color = ledColor,
                        fontFamily = TelemetryHudFont,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isStopped) TelemetryHudSurfaceInner else HudRedSurface)
                    .border(
                        1.dp,
                        if (isStopped) Color.White.copy(alpha = 0.06f) else HudRedBorder,
                        CircleShape
                    )
                    .clickable(enabled = !isStopped, onClick = onEmergencyStop)
                    .semantics {
                        contentDescription = emergencyStopLabel
                        role = Role.Button
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (isStopped) TelemetryHudTextSecondary else HudLightRed,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = stringResource(R.string.control_estop),
                    color = if (isStopped) TelemetryHudTextSecondary else HudLightRed,
                    fontFamily = TelemetryHudFont,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatusLed(color: Color, description: String) {
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(300),
        label = "telemetryStatusLed"
    )
    Canvas(
        modifier = Modifier
            .size(18.dp)
            .semantics { contentDescription = description }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f
        drawCircle(animatedColor.copy(alpha = 0.08f), radius, center)
        drawCircle(animatedColor.copy(alpha = 0.16f), radius * 0.68f, center)
        drawCircle(animatedColor.copy(alpha = 0.30f), radius * 0.44f, center)
        drawCircle(animatedColor, radius * 0.24f, center)
    }
}

@Composable
internal fun TelemetryUpdateStrip(
    hasReceivedTelemetry: Boolean,
    ageMillis: Long?,
    isStale: Boolean
) {
    val text = when {
        !hasReceivedTelemetry -> stringResource(R.string.telemetry_never_updated)
        ageMillis != null -> stringResource(R.string.telemetry_age_seconds, ageMillis / 1000f)
        else -> stringResource(R.string.telemetry_waiting)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (isStale) WarningAmber else TelemetryHudTextSecondary,
            fontFamily = TelemetryHudFont,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            maxLines = 2
        )
        if (isStale) {
            Spacer(Modifier.width(8.dp))
            TelemetryBadge(
                text = stringResource(R.string.telemetry_stale).uppercase(),
                color = WarningAmber,
                containerColor = WarningAmber.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
internal fun TelemetryStopBanner(
    onRearm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rearmLabel = stringResource(R.string.control_rearm)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HudRedSurface.copy(alpha = 0.72f))
            .border(1.dp, HudRedBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = HudLightRed,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.control_stopped),
                color = HudLightRed,
                fontFamily = TelemetryHudFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = stringResource(R.string.control_stopped_hint),
                color = TelemetryHudTextSecondary,
                fontFamily = TelemetryHudFont,
                fontSize = 10.sp
            )
        }
        Text(
            text = rearmLabel.uppercase(),
            modifier = Modifier
                .clip(CircleShape)
                .background(HudRedBorder.copy(alpha = 0.32f))
                .clickable(onClick = onRearm)
                .semantics {
                    role = Role.Button
                    contentDescription = rearmLabel
                }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            color = HudLightRed,
            fontFamily = TelemetryHudFont,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
internal fun TelemetryPanel(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(4.dp, TelemetryPanelShape, clip = false)
            .clip(TelemetryPanelShape)
            .background(TelemetryHudSurface)
            .border(1.dp, Color.White.copy(alpha = 0.04f), TelemetryPanelShape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
internal fun TelemetrySectionHeader(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    iconCircleSize: Dp = 40.dp,
    iconGlyphSize: Dp = 20.dp,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelemetryIconCircle(
            icon = icon,
            size = iconCircleSize,
            glyphSize = iconGlyphSize
        )
        Spacer(Modifier.width(12.dp))
        if (title != null) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TelemetryHudTextPrimary,
                    fontFamily = TelemetryHudFont,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = TelemetryHudTextSecondary,
                        fontFamily = TelemetryHudFont,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.1.sp,
                        maxLines = 1
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        trailing()
    }
}

@Composable
private fun TelemetryIconCircle(
    icon: ImageVector,
    size: Dp,
    glyphSize: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(TelemetryHudIconCircle)
            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TelemetryHudAccent,
            modifier = Modifier.size(glyphSize)
        )
    }
}

@Composable
internal fun TelemetryBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TelemetryHudAccent,
    containerColor: Color = TelemetryHudTealSurface
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, color.copy(alpha = 0.32f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = TelemetryHudFont,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            maxLines = 1
        )
    }
}

@Composable
internal fun TelemetryInsetBox(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 12.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(TelemetryInsetShape)
            .background(TelemetryHudSurfaceInner)
            .border(1.dp, Color.White.copy(alpha = 0.03f), TelemetryInsetShape)
            .padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
internal fun TelemetrySegmentedBar(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = TelemetryHudAccent,
    segments: Int = 5
) {
    val activeCount = ceil(progress.coerceIn(0f, 1f) * segments).toInt()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(segments) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < activeCount) {
                            activeColor
                        } else {
                            TelemetryHudTrack
                        }
                    )
            )
        }
    }
}

@Composable
internal fun TelemetryGyroRadar(
    pitch: Float,
    roll: Float,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val targetX = (roll / 45f).coerceIn(-1f, 1f)
    val targetY = (-pitch / 45f).coerceIn(-1f, 1f)
    val bubbleX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = tween(300),
        label = "telemetryRadarX"
    )
    val bubbleY by animateFloatAsState(
        targetValue = targetY,
        animationSpec = tween(300),
        label = "telemetryRadarY"
    )
    val bubbleColor = if (active) {
        TelemetryHudAccent
    } else {
        TelemetryHudTextSecondary.copy(alpha = 0.55f)
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = min(size.width, size.height) * 0.38f

        drawCircle(
            color = TelemetryHudGrid,
            radius = outerRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = TelemetryHudGrid.copy(alpha = 0.55f),
            radius = outerRadius * 0.55f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawLine(
            color = TelemetryHudGrid.copy(alpha = 0.65f),
            start = Offset(center.x - outerRadius, center.y),
            end = Offset(center.x + outerRadius, center.y),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = TelemetryHudGrid.copy(alpha = 0.65f),
            start = Offset(center.x, center.y - outerRadius),
            end = Offset(center.x, center.y + outerRadius),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )

        val bubbleCenter = Offset(
            x = center.x + bubbleX * outerRadius * 0.72f,
            y = center.y + bubbleY * outerRadius * 0.72f
        )
        val bubbleRadius = 6.dp.toPx()

        drawCircle(bubbleColor.copy(alpha = 0.08f), bubbleRadius * 2.8f, bubbleCenter)
        drawCircle(bubbleColor.copy(alpha = 0.14f), bubbleRadius * 1.9f, bubbleCenter)
        drawCircle(bubbleColor.copy(alpha = 0.24f), bubbleRadius * 1.35f, bubbleCenter)
        drawCircle(bubbleColor, bubbleRadius, bubbleCenter)
        drawCircle(TelemetryHudBackground, bubbleRadius * 0.42f, bubbleCenter)
    }
}

@Composable
internal fun TelemetryMetricLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TelemetryHudTextSecondary,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = TelemetryHudFont,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        lineHeight = 10.sp,
        textAlign = textAlign,
        maxLines = 2
    )
}
