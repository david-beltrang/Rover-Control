package com.example.rover_control.ui.screens.telemetry

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rover_control.R
import com.example.rover_control.data.bluetooth.isConnected
import com.example.rover_control.data.protocol.Telemetry
import com.example.rover_control.ui.components.HudBottomTabBar
import com.example.rover_control.ui.components.HudTab
import com.example.rover_control.ui.components.TelemetryBadge
import com.example.rover_control.ui.components.TelemetryGyroRadar
import com.example.rover_control.ui.components.TelemetryInsetBox
import com.example.rover_control.ui.components.TelemetryMetricLabel
import com.example.rover_control.ui.components.TelemetryNumberFont
import com.example.rover_control.ui.components.TelemetryPanel
import com.example.rover_control.ui.components.TelemetrySectionHeader
import com.example.rover_control.ui.components.TelemetrySegmentedBar
import com.example.rover_control.ui.components.TelemetryStopBanner
import com.example.rover_control.ui.components.TelemetryTopBar
import com.example.rover_control.ui.components.TelemetryUpdateStrip
import com.example.rover_control.ui.theme.ErrorRed
import com.example.rover_control.ui.theme.TelemetryHudAccent
import com.example.rover_control.ui.theme.TelemetryHudBackground
import com.example.rover_control.ui.theme.TelemetryHudSurfaceInner
import com.example.rover_control.ui.theme.TelemetryHudTextPrimary
import com.example.rover_control.ui.theme.TelemetryHudTextSecondary
import com.example.rover_control.ui.theme.TelemetryHudTrack
import com.example.rover_control.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.tan

@Composable
fun TelemetryScreen(
    viewModel: TelemetryViewModel = viewModel(),
    onOpenControl: () -> Unit = {},
    onOpenConnection: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connected = uiState.connectionState.isConnected
    val scrollState = rememberScrollState()

    // Keep the STOPPED/rearm banner visible even if the user pressed E-STOP
    // while scrolled down through the sensor panels.
    LaunchedEffect(uiState.isStopped) {
        if (uiState.isStopped) {
            scrollState.animateScrollTo(0)
        }
    }

    Scaffold(
        containerColor = TelemetryHudBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TelemetryTopBar(
                connectionState = uiState.connectionState,
                isStale = uiState.isStale,
                isStopped = uiState.isStopped,
                onOpenConnection = onOpenConnection,
                onEmergencyStop = viewModel::emergencyStop
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.isStopped) {
                    TelemetryStopBanner(onRearm = viewModel::rearm)
                }

                TelemetryUpdateStrip(
                    hasReceivedTelemetry = uiState.hasReceivedTelemetry,
                    ageMillis = uiState.ageMillis,
                    isStale = uiState.isStale
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TemperatureCard(
                        telemetry = uiState.telemetry,
                        stale = uiState.isStale,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    HumidityCard(
                        telemetry = uiState.telemetry,
                        stale = uiState.isStale,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                LightSensorCard(
                    telemetry = uiState.telemetry,
                    stale = uiState.isStale
                )

                DistanceSensorCard(
                    telemetry = uiState.telemetry,
                    stale = uiState.isStale
                )

                InclinationSensorCard(
                    telemetry = uiState.telemetry,
                    stale = uiState.isStale
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(118.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, TelemetryHudBackground)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                HudBottomTabBar(
                    selectedTab = HudTab.TELEMETRY,
                    controlsEnabled = connected,
                    telemetryEnabled = true,
                    onControlsClick = onOpenControl,
                    onTelemetryClick = {},
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TemperatureCard(
    telemetry: Telemetry,
    stale: Boolean,
    modifier: Modifier = Modifier
) {
    val value = telemetry.temperatureC
    val statusText = when {
        value == null -> stringResource(R.string.telemetry_status_no_data)
        value < 20f -> stringResource(R.string.telemetry_status_low)
        value > 35f -> stringResource(R.string.telemetry_status_high)
        else -> stringResource(R.string.telemetry_status_optimal)
    }
    val statusColor = when {
        value == null -> TelemetryHudTextSecondary
        value < 20f -> WarningAmber
        value > 35f -> ErrorRed
        else -> TelemetryHudAccent
    }
    val dimmed = stale || value == null

    TelemetryPanel(modifier = modifier) {
        TelemetrySectionHeader(
            icon = Icons.Filled.Thermostat,
            iconCircleSize = 36.dp,
            iconGlyphSize = 18.dp,
            trailing = {
                TelemetryBadge(text = stringResource(R.string.telemetry_badge_ambient))
            }
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.telemetry_label_temperature),
            color = TelemetryHudTextSecondary,
            fontFamily = TelemetryNumberFont,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            maxLines = 1
        )

        Spacer(Modifier.height(5.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value?.let(::formatOneDecimal)
                    ?: stringResource(R.string.value_placeholder),
                color = if (dimmed) {
                    TelemetryHudTextPrimary.copy(alpha = 0.4f)
                } else {
                    TelemetryHudTextPrimary
                },
                fontFamily = TelemetryNumberFont,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                maxLines = 1
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = stringResource(R.string.unit_celsius),
                modifier = Modifier.padding(bottom = 4.dp),
                color = if (dimmed) {
                    TelemetryHudAccent.copy(alpha = 0.4f)
                } else {
                    TelemetryHudAccent
                },
                fontFamily = TelemetryNumberFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(R.string.telemetry_temperature_range),
                color = TelemetryHudTextSecondary,
                fontFamily = TelemetryNumberFont,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                maxLines = 2
            )
            Text(
                text = statusText,
                color = if (dimmed) statusColor.copy(alpha = 0.55f) else statusColor,
                fontFamily = TelemetryNumberFont,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HumidityCard(
    telemetry: Telemetry,
    stale: Boolean,
    modifier: Modifier = Modifier
) {
    val value = telemetry.humidityPercent
    val fraction = ((value ?: 0f) / 100f).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(300),
        label = "telemetryHumidity"
    )
    val statusText = when {
        value == null -> stringResource(R.string.telemetry_status_no_data)
        value < 30f -> stringResource(R.string.telemetry_status_dry)
        value > 70f -> stringResource(R.string.telemetry_status_humid)
        else -> stringResource(R.string.telemetry_status_normal)
    }
    val statusColor = when {
        value == null -> TelemetryHudTextSecondary
        value < 30f -> WarningAmber
        value > 70f -> WarningAmber
        else -> TelemetryHudAccent
    }
    val dimmed = stale || value == null

    TelemetryPanel(modifier = modifier) {
        TelemetrySectionHeader(
            icon = Icons.Filled.WaterDrop,
            iconCircleSize = 36.dp,
            iconGlyphSize = 18.dp,
            trailing = {
                TelemetryBadge(text = stringResource(R.string.telemetry_badge_humidity))
            }
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.telemetry_label_humidity),
            color = TelemetryHudTextSecondary,
            fontFamily = TelemetryNumberFont,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            maxLines = 1
        )

        Spacer(Modifier.height(5.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value?.roundToInt()?.toString()
                    ?: stringResource(R.string.value_placeholder),
                color = if (dimmed) {
                    TelemetryHudTextPrimary.copy(alpha = 0.4f)
                } else {
                    TelemetryHudTextPrimary
                },
                fontFamily = TelemetryNumberFont,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                maxLines = 1
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = stringResource(R.string.unit_percent),
                modifier = Modifier.padding(bottom = 4.dp),
                color = if (dimmed) {
                    TelemetryHudAccent.copy(alpha = 0.4f)
                } else {
                    TelemetryHudAccent
                },
                fontFamily = TelemetryNumberFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TelemetryHudTrack)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = fraction,
                            range = 0f..1f
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedFraction)
                        .clip(CircleShape)
                        .background(
                            if (dimmed) {
                                TelemetryHudAccent.copy(alpha = 0.4f)
                            } else {
                                TelemetryHudAccent
                            }
                        )
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = statusText,
                color = if (dimmed) statusColor.copy(alpha = 0.55f) else statusColor,
                fontFamily = TelemetryNumberFont,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LightSensorCard(
    telemetry: Telemetry,
    stale: Boolean,
    modifier: Modifier = Modifier
) {
    val raw = telemetry.lightRaw
    val progress = if (raw == null) 0f else (raw / 1023f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "telemetryLight"
    )
    val percentage = (progress * 100f).roundToInt()
    val dimmed = stale || raw == null

    val badgeText = when {
        raw == null -> stringResource(R.string.telemetry_status_no_data)
        raw < 200 -> stringResource(R.string.telemetry_badge_dark)
        raw < 700 -> stringResource(R.string.telemetry_badge_dim)
        else -> stringResource(R.string.telemetry_badge_bright)
    }
    val badgeColor = when {
        raw == null -> TelemetryHudTextSecondary
        raw < 200 -> TelemetryHudTextSecondary
        raw < 700 -> WarningAmber
        else -> TelemetryHudAccent
    }
    val exposureText = when {
        raw == null -> stringResource(R.string.telemetry_status_no_data)
        percentage >= 65 -> stringResource(R.string.telemetry_light_exposure_high)
        percentage >= 30 -> stringResource(R.string.telemetry_light_exposure_medium)
        else -> stringResource(R.string.telemetry_light_exposure_low)
    }

    TelemetryPanel(modifier = modifier) {
        TelemetrySectionHeader(
            icon = Icons.Filled.WbSunny,
            title = stringResource(R.string.telemetry_light_sensor),
            subtitle = stringResource(R.string.telemetry_light_subtitle),
            trailing = {
                TelemetryBadge(
                    text = badgeText,
                    color = badgeColor,
                    containerColor = badgeColor.copy(alpha = 0.12f)
                )
            }
        )

        Spacer(Modifier.height(14.dp))

        TelemetryInsetBox(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 14.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    TelemetryMetricLabel(
                        text = stringResource(R.string.telemetry_light_intensity)
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = raw?.toString()
                                ?: stringResource(R.string.value_placeholder),
                            color = if (dimmed) {
                                TelemetryHudAccent.copy(alpha = 0.4f)
                            } else {
                                TelemetryHudAccent
                            },
                            fontFamily = TelemetryNumberFont,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.8).sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.unit_lux),
                            modifier = Modifier.padding(bottom = 3.dp),
                            color = if (dimmed) {
                                TelemetryHudAccent.copy(alpha = 0.4f)
                            } else {
                                TelemetryHudAccent
                            },
                            fontFamily = TelemetryNumberFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.telemetry_light_level, percentage),
                        color = if (dimmed) {
                            TelemetryHudTextPrimary.copy(alpha = 0.4f)
                        } else {
                            TelemetryHudTextPrimary
                        },
                        fontFamily = TelemetryNumberFont,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = exposureText,
                        color = TelemetryHudTextSecondary,
                        fontFamily = TelemetryNumberFont,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        TelemetrySegmentedBar(
            progress = animatedProgress,
            modifier = Modifier.fillMaxWidth(),
            activeColor = if (dimmed) {
                TelemetryHudAccent.copy(alpha = 0.4f)
            } else {
                TelemetryHudAccent
            }
        )
    }
}

@Composable
private fun DistanceSensorCard(
    telemetry: Telemetry,
    stale: Boolean,
    modifier: Modifier = Modifier
) {
    val distance = telemetry.distanceCm?.takeIf { it > 0 }
    val statusText = when {
        distance == null -> stringResource(R.string.telemetry_status_no_data)
        distance < 20 -> stringResource(R.string.telemetry_status_danger)
        distance < 30 -> stringResource(R.string.telemetry_status_caution)
        else -> stringResource(R.string.telemetry_status_active)
    }
    val statusColor = when {
        distance == null -> TelemetryHudTextSecondary
        distance < 20 -> ErrorRed
        distance < 30 -> WarningAmber
        else -> TelemetryHudAccent
    }
    val detailText = when {
        distance == null -> stringResource(R.string.telemetry_distance_no_reading)
        distance < 20 -> stringResource(R.string.telemetry_distance_danger)
        distance < 30 -> stringResource(R.string.telemetry_distance_caution)
        else -> stringResource(R.string.telemetry_distance_clear)
    }
    val detailIcon: ImageVector = when {
        distance == null -> Icons.Filled.Warning
        distance < 20 -> Icons.Filled.Error
        distance < 30 -> Icons.Filled.Warning
        else -> Icons.Filled.CheckCircle
    }
    val dimmed = stale || distance == null

    TelemetryPanel(modifier = modifier) {
        TelemetrySectionHeader(
            icon = Icons.Filled.Radar,
            title = stringResource(R.string.telemetry_distance_sensor),
            subtitle = stringResource(R.string.telemetry_distance_subtitle),
            trailing = {
                TelemetryBadge(text = stringResource(R.string.telemetry_badge_ultrasonic))
            }
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryInsetBox(
                modifier = Modifier
                    .weight(0.47f)
                    .fillMaxHeight(),
                contentPadding = 14.dp,
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    TelemetryMetricLabel(
                        text = stringResource(R.string.telemetry_distance_label)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = distance?.toString()
                                ?: stringResource(R.string.value_placeholder),
                            color = if (dimmed) {
                                TelemetryHudAccent.copy(alpha = 0.4f)
                            } else {
                                TelemetryHudAccent
                            },
                            fontFamily = TelemetryNumberFont,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.unit_centimeters),
                            modifier = Modifier.padding(bottom = 4.dp),
                            color = if (dimmed) {
                                TelemetryHudAccent.copy(alpha = 0.4f)
                            } else {
                                TelemetryHudAccent
                            },
                            fontFamily = TelemetryNumberFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = distance?.let {
                            stringResource(R.string.telemetry_distance_meters, it / 100f)
                        } ?: stringResource(R.string.value_placeholder),
                        color = TelemetryHudTextSecondary,
                        fontFamily = TelemetryNumberFont,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.53f)
                    .fillMaxHeight()
            ) {
                TelemetryInsetBox(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = 14.dp,
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TelemetryMetricLabel(
                                text = stringResource(R.string.telemetry_detection)
                            )
                            Text(
                                text = statusText,
                                color = if (dimmed) {
                                    statusColor.copy(alpha = 0.55f)
                                } else {
                                    statusColor
                                },
                                fontFamily = TelemetryNumberFont,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                maxLines = 1
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = detailIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = detailText,
                                color = if (dimmed) {
                                    TelemetryHudTextSecondary
                                } else {
                                    TelemetryHudTextPrimary
                                },
                                fontFamily = TelemetryNumberFont,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 16.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InclinationSensorCard(
    telemetry: Telemetry,
    stale: Boolean,
    modifier: Modifier = Modifier
) {
    val inclination = telemetry.inclinationDeg
    // The current rover protocol exposes a single inclination value. Use it as
    // pitch and keep the roll axis level until the firmware reports both axes.
    val pitch = inclination
    val roll = 0f
    val slope = inclination?.let {
        (tan(it.toDouble() * PI / 180.0) * 100.0).toFloat()
    }
    val statusText = when {
        inclination == null -> stringResource(R.string.telemetry_status_no_data)
        abs(inclination) <= 15f -> stringResource(R.string.telemetry_status_stable)
        abs(inclination) <= 25f -> stringResource(R.string.telemetry_status_caution)
        else -> stringResource(R.string.telemetry_status_tilted)
    }
    val statusColor = when {
        inclination == null -> TelemetryHudTextSecondary
        abs(inclination) <= 15f -> TelemetryHudAccent
        abs(inclination) <= 25f -> WarningAmber
        else -> ErrorRed
    }
    val dimmed = stale || inclination == null
    val inclinationText = inclination?.let {
        stringResource(R.string.telemetry_degrees_value, formatOneDecimal(it))
    } ?: stringResource(R.string.value_placeholder)
    val pitchText = pitch?.let {
        stringResource(R.string.telemetry_degrees_value, formatSignedOneDecimal(it))
    } ?: stringResource(R.string.value_placeholder)
    val rollText = stringResource(
        R.string.telemetry_degrees_value,
        formatSignedOneDecimal(roll)
    )
    val slopeText = slope?.let {
        stringResource(R.string.telemetry_slope_percent, it)
    } ?: stringResource(R.string.value_placeholder)

    TelemetryPanel(modifier = modifier) {
        TelemetrySectionHeader(
            icon = Icons.Filled.ScreenRotation,
            title = stringResource(R.string.telemetry_tilt_sensor),
            subtitle = stringResource(R.string.telemetry_tilt_subtitle),
            trailing = {
                Text(
                    text = statusText,
                    color = if (dimmed) statusColor.copy(alpha = 0.55f) else statusColor,
                    fontFamily = TelemetryNumberFont,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp,
                    maxLines = 1
                )
            }
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryInsetBox(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                contentPadding = 10.dp,
                contentAlignment = Alignment.Center
            ) {
                TelemetryGyroRadar(
                    pitch = pitch ?: 0f,
                    roll = roll,
                    active = inclination != null && !stale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryInsetBox(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = 8.dp,
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryMetricLabel(
                            text = stringResource(R.string.telemetry_label_inclination)
                        )
                        Text(
                            text = inclinationText,
                            color = if (dimmed) {
                                TelemetryHudAccent.copy(alpha = 0.4f)
                            } else {
                                TelemetryHudAccent
                            },
                            fontFamily = TelemetryNumberFont,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryInsetBox(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = 8.dp,
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            TelemetryMetricLabel(
                                text = stringResource(R.string.telemetry_label_pitch)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = pitchText,
                                color = if (dimmed) {
                                    TelemetryHudTextPrimary.copy(alpha = 0.4f)
                                } else {
                                    TelemetryHudTextPrimary
                                },
                                fontFamily = TelemetryNumberFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                    TelemetryInsetBox(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = 8.dp,
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            TelemetryMetricLabel(
                                text = stringResource(R.string.telemetry_label_roll)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = rollText,
                                color = if (dimmed) {
                                    TelemetryHudAccent.copy(alpha = 0.4f)
                                } else {
                                    TelemetryHudAccent
                                },
                                fontFamily = TelemetryNumberFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                TelemetryInsetBox(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = 8.dp,
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryMetricLabel(
                            text = stringResource(R.string.telemetry_label_slope)
                        )
                        Text(
                            text = slopeText,
                            color = if (dimmed) {
                                TelemetryHudAccent.copy(alpha = 0.4f)
                            } else {
                                TelemetryHudAccent
                            },
                            fontFamily = TelemetryNumberFont,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun formatOneDecimal(value: Float): String =
    String.format(Locale.getDefault(), "%.1f", value)

private fun formatSignedOneDecimal(value: Float): String =
    if (value > 0f) {
        "+${formatOneDecimal(value)}"
    } else {
        formatOneDecimal(value)
    }
