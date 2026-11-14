package com.example.rover_control.ui.screens.telemetry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rover_control.R
import com.example.rover_control.data.protocol.Telemetry
import com.example.rover_control.ui.components.ConnectionStatusPill
import com.example.rover_control.ui.components.EmergencyStopFab
import com.example.rover_control.ui.components.StaleBadge
import com.example.rover_control.ui.components.TelemetryCard
import com.example.rover_control.ui.theme.Background
import com.example.rover_control.ui.theme.BrightYellow
import com.example.rover_control.ui.theme.ErrorRed
import com.example.rover_control.ui.theme.InfoBlue
import com.example.rover_control.ui.theme.OnBackground
import com.example.rover_control.ui.theme.WarningAmber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryScreen(
    viewModel: TelemetryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.telemetry_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    ConnectionStatusPill(
                        state = uiState.connectionState,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            EmergencyStopFab(onClick = viewModel::emergencyStop)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp)
        ) {
            UpdateIndicator(
                hasReceivedTelemetry = uiState.hasReceivedTelemetry,
                ageMillis = uiState.ageMillis,
                isStale = uiState.isStale
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TemperatureCard(
                    telemetry = uiState.telemetry,
                    stale = uiState.isStale,
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                )
                HumidityCard(
                    telemetry = uiState.telemetry,
                    stale = uiState.isStale,
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LightCard(
                    telemetry = uiState.telemetry,
                    stale = uiState.isStale,
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                )
                DistanceCard(
                    telemetry = uiState.telemetry,
                    stale = uiState.isStale,
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            InclinationCard(
                telemetry = uiState.telemetry,
                stale = uiState.isStale,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun UpdateIndicator(
    hasReceivedTelemetry: Boolean,
    ageMillis: Long?,
    isStale: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val text = when {
            !hasReceivedTelemetry -> stringResource(R.string.telemetry_never_updated)
            ageMillis != null -> stringResource(R.string.telemetry_age_seconds, ageMillis / 1000f)
            else -> stringResource(R.string.telemetry_waiting)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isStale) MaterialTheme.colorScheme.onSurfaceVariant else OnBackground
        )
        StaleBadge(visible = isStale)
    }
}

@Composable
private fun TemperatureCard(
    telemetry: Telemetry,
    stale: Boolean,
    modifier: Modifier = Modifier
) {
    val value = telemetry.temperatureC
    TelemetryCard(
        icon = Icons.Filled.Thermostat,
        title = stringResource(R.string.telemetry_temperature),
        value = value?.let(::formatOneDecimal) ?: stringResource(R.string.value_placeholder),
        unit = stringResource(R.string.unit_celsius),
        hint = if (value != null && value > 40f) {
            stringResource(R.string.telemetry_hint_hot)
        } else {
            null
        },
        hintColor = ErrorRed,
        stale = stale,
        modifier = modifier
    )
}

@Composable
private fun HumidityCard(
    telemetry: Telemetry,
    stale: Boolean,
    modifier: Modifier = Modifier
) {
    val value = telemetry.humidityPercent
    TelemetryCard(
        icon = Icons.Filled.WaterDrop,
        title = stringResource(R.string.telemetry_humidity),
        value = value?.let(::formatOneDecimal) ?: stringResource(R.string.value_placeholder),
        unit = stringResource(R.string.unit_percent),
        hint = if (value != null && value > 80f) {
            stringResource(R.string.telemetry_hint_humid)
        } else {
            null
        },
        hintColor = InfoBlue,
        stale = stale,
        modifier = modifier
    )
}

@Composable
private fun LightCard(
    telemetry: Telemetry,
    stale: Boolean,
    modifier: Modifier = Modifier
) {
    val value = telemetry.lightRaw
    val hint = when {
        value == null -> null
        value < 200 -> stringResource(R.string.telemetry_hint_dark)
        value < 700 -> stringResource(R.string.telemetry_hint_dim)
        else -> stringResource(R.string.telemetry_hint_bright)
    }
    val color: Color = when {
        value == null -> OnBackground
        value < 200 -> MaterialTheme.colorScheme.onSurfaceVariant
        value < 700 -> WarningAmber
        else -> BrightYellow
    }

    TelemetryCard(
        icon = Icons.Filled.WbSunny,
        title = stringResource(R.string.telemetry_light),
        value = value?.toString() ?: stringResource(R.string.value_placeholder),
        unit = stringResource(R.string.unit_raw_adc),
        hint = hint,
        hintColor = color,
        stale = stale,
        modifier = modifier
    )
}

@Composable
private fun DistanceCard(
    telemetry: Telemetry,
    stale: Boolean,
    modifier: Modifier = Modifier
) {
    val distance = telemetry.distanceCm?.takeIf { it > 0 }
    val hint = when {
        distance == null -> null
        distance < 20 -> stringResource(R.string.telemetry_hint_danger)
        distance < 50 -> stringResource(R.string.telemetry_hint_near)
        else -> null
    }
    val color: Color = when {
        distance == null -> OnBackground
        distance < 20 -> ErrorRed
        distance < 50 -> WarningAmber
        else -> OnBackground
    }

    TelemetryCard(
        icon = Icons.Filled.Radar,
        title = stringResource(R.string.telemetry_distance),
        value = distance?.toString() ?: stringResource(R.string.value_placeholder),
        unit = stringResource(R.string.unit_centimeters),
        hint = hint,
        hintColor = color,
        stale = stale,
        modifier = modifier
    )
}

@Composable
private fun InclinationCard(
    telemetry: Telemetry,
    stale: Boolean,
    modifier: Modifier = Modifier
) {
    val value = telemetry.inclinationDeg
    TelemetryCard(
        icon = Icons.Filled.ScreenRotation,
        title = stringResource(R.string.telemetry_inclination),
        value = value?.let(::formatOneDecimal) ?: stringResource(R.string.value_placeholder),
        unit = stringResource(R.string.unit_degrees),
        hint = if (value != null && value > 25f) {
            stringResource(R.string.telemetry_hint_tilt)
        } else {
            null
        },
        hintColor = ErrorRed,
        stale = stale,
        modifier = modifier
    )
}

private fun formatOneDecimal(value: Float): String =
    String.format(Locale.getDefault(), "%.1f", value)
