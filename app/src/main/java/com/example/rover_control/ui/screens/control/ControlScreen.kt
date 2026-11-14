package com.example.rover_control.ui.screens.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rover_control.R
import com.example.rover_control.data.settings.InputMode
import com.example.rover_control.ui.components.ConnectionStatusPill
import com.example.rover_control.ui.components.Direction
import com.example.rover_control.ui.components.DirectionLabel
import com.example.rover_control.ui.components.DirectionalPad
import com.example.rover_control.ui.components.EmergencyStopBanner
import com.example.rover_control.ui.components.EmergencyStopFab
import com.example.rover_control.ui.components.InputModeSheet
import com.example.rover_control.ui.components.Joystick
import com.example.rover_control.ui.components.SpeedSlider
import com.example.rover_control.ui.components.toRoverCommand
import com.example.rover_control.ui.theme.Background
import com.example.rover_control.ui.theme.PrimaryCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    viewModel: ControlViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showInputModeSheet by remember { mutableStateOf(false) }
    var activeDirection by remember { mutableStateOf<Direction?>(null) }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.control_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    ConnectionStatusPill(
                        state = uiState.connectionState,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(onClick = { showInputModeSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.content_description_open_settings)
                        )
                    }
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
                .padding(horizontal = 20.dp)
        ) {
            if (uiState.isStopped) {
                EmergencyStopBanner(
                    onRearm = {
                        activeDirection = null
                        viewModel.rearm()
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DirectionLabel(direction = activeDirection)
                    Spacer(Modifier.height(8.dp))

                    val handleDirectionChange: (Direction?) -> Unit = { direction ->
                        activeDirection = direction
                        viewModel.onDirectionChange(direction?.toRoverCommand())
                    }
                    val handleRelease: () -> Unit = {
                        activeDirection = null
                        viewModel.onRelease()
                    }

                    when (uiState.inputMode) {
                        InputMode.JOYSTICK -> Joystick(
                            enabled = !uiState.isStopped,
                            onDirectionChange = handleDirectionChange,
                            onRelease = handleRelease,
                            modifier = Modifier.fillMaxWidth(0.68f)
                        )

                        InputMode.DPAD -> DirectionalPad(
                            enabled = !uiState.isStopped,
                            onDirectionChange = handleDirectionChange,
                            onRelease = handleRelease,
                            modifier = Modifier.fillMaxWidth(0.72f)
                        )
                    }
                }
            }

            SpeedSlider(
                value = uiState.speedPercent,
                enabled = !uiState.isStopped,
                onValueChange = viewModel::onSpeedChange
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = stringResource(R.string.content_description_headlights),
                        tint = if (uiState.headlightsOn) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.control_headlights),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = uiState.headlightsOn,
                        onCheckedChange = viewModel::toggleHeadlights,
                        enabled = !uiState.isStopped
                    )
                }
                OutlinedButton(
                    onClick = viewModel::resetSpeed,
                    enabled = !uiState.isStopped
                ) {
                    Text(stringResource(R.string.control_reset_speed))
                }
            }

            Spacer(Modifier.height(104.dp))
        }
    }

    if (showInputModeSheet) {
        InputModeSheet(
            current = uiState.inputMode,
            onSelect = {
                viewModel.setInputMode(it)
                showInputModeSheet = false
            },
            onDismiss = { showInputModeSheet = false }
        )
    }
}
