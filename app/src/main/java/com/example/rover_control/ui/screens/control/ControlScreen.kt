package com.example.rover_control.ui.screens.control

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rover_control.R
import com.example.rover_control.data.bluetooth.ConnectionState
import com.example.rover_control.data.bluetooth.isConnected
import com.example.rover_control.data.settings.InputMode
import com.example.rover_control.ui.components.ConnectionStatusPill
import com.example.rover_control.ui.components.Direction
import com.example.rover_control.ui.components.HudBottomTabBar
import com.example.rover_control.ui.components.HudTab
import com.example.rover_control.ui.components.HudCardTitle
import com.example.rover_control.ui.components.HudCircleButton
import com.example.rover_control.ui.components.HudDirectionalPad
import com.example.rover_control.ui.components.HudJoystick
import com.example.rover_control.ui.components.HudPanel
import com.example.rover_control.ui.components.HudPillButton
import com.example.rover_control.ui.components.HudVerticalSpeedSlider
import com.example.rover_control.ui.components.SteeringModeSwitch
import com.example.rover_control.ui.components.toRoverCommand
import com.example.rover_control.ui.theme.ErrorRed
import com.example.rover_control.ui.theme.HudAccentCyan
import com.example.rover_control.ui.theme.HudBackground
import com.example.rover_control.ui.theme.HudLightRed
import com.example.rover_control.ui.theme.HudRedSurface
import com.example.rover_control.ui.theme.HudTextPrimary
import com.example.rover_control.ui.theme.HudTextSecondary
import com.example.rover_control.ui.theme.HudTealSurface
import com.example.rover_control.ui.theme.PrimaryCyan

@Composable
fun ControlScreen(
    viewModel: ControlViewModel = viewModel(),
    onOpenTelemetry: () -> Unit = {},
    onOpenConnection: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    var activeDirection by remember { mutableStateOf<Direction?>(null) }

    val connected = uiState.connectionState.isConnected
    val controlsEnabled = connected && uiState.isEngineOn && !uiState.isStopped
    val lightsEnabled = connected && !uiState.isStopped

    val hapticTap: () -> Unit = {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
    val hapticLong: () -> Unit = {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    LaunchedEffect(controlsEnabled) {
        if (!controlsEnabled) activeDirection = null
    }

    val handleDirectionChange: (Direction?) -> Unit = { direction ->
        if (direction != activeDirection) hapticTap()
        activeDirection = direction
        viewModel.onDirectionChange(direction?.toRoverCommand())
    }
    val handleRelease: () -> Unit = {
        activeDirection = null
        viewModel.onRelease()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBackground)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF17212A), HudBackground),
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                    radius = size.maxDimension * 0.72f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            ControlHeader(
                connectionState = uiState.connectionState,
                stopped = uiState.isStopped,
                onOpenConnection = {
                    hapticTap()
                    onOpenConnection()
                },
                onEmergencyStop = {
                    hapticLong()
                    activeDirection = null
                    viewModel.emergencyStop()
                }
            )

            if (uiState.isStopped) {
                Spacer(Modifier.height(4.dp))
                StoppedBar(
                    onRearm = {
                        hapticTap()
                        activeDirection = null
                        viewModel.rearm()
                    }
                )
                Spacer(Modifier.height(4.dp))
            } else {
                Spacer(Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DirectionPanel(
                    inputMode = uiState.inputMode,
                    enabled = controlsEnabled,
                    onModeChange = { isPadMode ->
                        hapticTap()
                        activeDirection = null
                        viewModel.setInputMode(if (isPadMode) InputMode.DPAD else InputMode.JOYSTICK)
                    },
                    onDirectionChange = handleDirectionChange,
                    onRelease = handleRelease,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                SpeedPanel(
                    speedPercent = uiState.speedPercent,
                    headlightsOn = uiState.headlightsOn,
                    isEngineOn = uiState.isEngineOn,
                    isTurboActive = uiState.isTurboActive,
                    controlsEnabled = controlsEnabled,
                    lightsEnabled = lightsEnabled,
                    connected = connected,
                    stopped = uiState.isStopped,
                    onSpeedChange = viewModel::onSpeedChange,
                    onEngineClick = {
                        hapticTap()
                        activeDirection = null
                        viewModel.toggleEngine()
                    },
                    onLightsClick = {
                        hapticTap()
                        viewModel.toggleHeadlights(!uiState.headlightsOn)
                    },
                    onTurboClick = {
                        hapticTap()
                        viewModel.toggleTurbo()
                    },
                    onBrakeClick = {
                        hapticLong()
                        activeDirection = null
                        viewModel.brake()
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                contentAlignment = Alignment.Center
            ) {
                HudBottomTabBar(
                    selectedTab = HudTab.CONTROLS,
                    controlsEnabled = true,
                    telemetryEnabled = connected,
                    onControlsClick = {},
                    onTelemetryClick = {
                        hapticTap()
                        onOpenTelemetry()
                    },
                    modifier = Modifier.width(340.dp)
                )
            }
        }
    }
}

@Composable
private fun ControlHeader(
    connectionState: ConnectionState,
    stopped: Boolean,
    onOpenConnection: () -> Unit,
    onEmergencyStop: () -> Unit
) {
    val openConnectionLabel = stringResource(R.string.content_description_open_connection)
    val ledColor = when (connectionState) {
        is ConnectionState.Connected -> if (connectionState.isDemo) com.example.rover_control.ui.theme.WarningAmber else com.example.rover_control.ui.theme.SuccessGreen
        is ConnectionState.Connecting -> com.example.rover_control.ui.theme.WarningAmber
        is ConnectionState.Disconnected -> ErrorRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(com.example.rover_control.ui.theme.HudSurfaceControl)
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                .clickable(
                    onClickLabel = openConnectionLabel,
                    onClick = onOpenConnection
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.nav_back),
                tint = HudTextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(ledColor, CircleShape)
            )
        }

        Text(
            text = stringResource(R.string.control_hud_title),
            color = HudAccentCyan,
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 72.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(CircleShape)
                .background(ErrorRed.copy(alpha = if (stopped) 0.05f else 0.14f))
                .border(
                    width = 1.dp,
                    color = ErrorRed.copy(alpha = if (stopped) 0.2f else 0.55f),
                    shape = CircleShape
                )
                .clickable(enabled = !stopped, onClick = onEmergencyStop)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                tint = ErrorRed.copy(alpha = if (stopped) 0.4f else 1f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.control_estop),
                color = ErrorRed.copy(alpha = if (stopped) 0.4f else 1f),
                fontFamily = FontFamily.SansSerif,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StoppedBar(onRearm: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ErrorRed.copy(alpha = 0.12f))
            .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.control_stopped),
            color = ErrorRed,
            fontFamily = FontFamily.SansSerif,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.control_stopped_hint),
            color = HudTextSecondary,
            fontFamily = FontFamily.SansSerif,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.control_rearm),
            color = PrimaryCyan,
            fontFamily = FontFamily.SansSerif,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onRearm)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DirectionPanel(
    inputMode: InputMode,
    enabled: Boolean,
    onModeChange: (Boolean) -> Unit,
    onDirectionChange: (Direction?) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    HudPanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudCardTitle(
                icon = Icons.Filled.Explore,
                title = stringResource(R.string.control_direction_title),
                modifier = Modifier.weight(1f)
            )
            SteeringModeSwitch(
                isPadMode = inputMode == InputMode.DPAD,
                enabled = enabled,
                onModeChange = onModeChange
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = inputMode,
                label = "steeringMode"
            ) { mode ->
                when (mode) {
                    InputMode.DPAD -> HudDirectionalPad(
                        enabled = enabled,
                        onDirectionChange = onDirectionChange,
                        onRelease = onRelease,
                        modifier = Modifier.fillMaxSize()
                    )

                    InputMode.JOYSTICK -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        HudJoystick(
                            enabled = enabled,
                            onDirectionChange = onDirectionChange,
                            onRelease = onRelease,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedPanel(
    speedPercent: Float,
    headlightsOn: Boolean,
    isEngineOn: Boolean,
    isTurboActive: Boolean,
    controlsEnabled: Boolean,
    lightsEnabled: Boolean,
    connected: Boolean,
    stopped: Boolean,
    onSpeedChange: (Float) -> Unit,
    onEngineClick: () -> Unit,
    onLightsClick: () -> Unit,
    onTurboClick: () -> Unit,
    onBrakeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HudPanel(modifier = modifier) {
        HudCardTitle(
            icon = Icons.Filled.Speed,
            title = stringResource(R.string.control_speed_title)
        )

        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val circleSize = minOf(68.dp, (maxHeight * 0.36f).coerceAtLeast(48.dp))
            val pillHeight = minOf(44.dp, (maxHeight * 0.23f).coerceAtLeast(34.dp))

            Row(Modifier.fillMaxSize()) {
                HudVerticalSpeedSlider(
                    value = speedPercent,
                    enabled = controlsEnabled,
                    onValueChange = onSpeedChange,
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight()
                )

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier
                        .weight(0.60f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HudCircleButton(
                            icon = Icons.Filled.PowerSettingsNew,
                            title = stringResource(R.string.control_engine),
                            subtitle = stringResource(
                                if (isEngineOn) {
                                    R.string.control_engine_on
                                } else {
                                    R.string.control_engine_start
                                }
                            ),
                            active = isEngineOn,
                            dashedBorder = true,
                            enabled = connected && !stopped,
                            onClick = onEngineClick,
                            modifier = Modifier.size(circleSize)
                        )
                        HudCircleButton(
                            icon = Icons.Filled.WbSunny,
                            title = stringResource(R.string.control_lights_label),
                            subtitle = stringResource(
                                if (headlightsOn) {
                                    R.string.control_engine_on
                                } else {
                                    R.string.control_off
                                }
                            ),
                            active = headlightsOn,
                            dashedBorder = false,
                            enabled = lightsEnabled,
                            onClick = onLightsClick,
                            modifier = Modifier.size(circleSize)
                        )
                    }

                    HudPillButton(
                        icon = Icons.Filled.RocketLaunch,
                        label = stringResource(R.string.control_turbo),
                        containerColor = HudTealSurface,
                        contentColor = HudTextPrimary,
                        active = isTurboActive,
                        enabled = controlsEnabled,
                        onClick = onTurboClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pillHeight)
                    )

                    HudPillButton(
                        icon = Icons.Filled.ReportProblem,
                        label = stringResource(R.string.control_brake),
                        containerColor = HudRedSurface,
                        contentColor = HudLightRed,
                        active = true,
                        enabled = connected && !stopped,
                        onClick = onBrakeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pillHeight)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}
