package com.example.rover_control.ui.screens.connection

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rover_control.R
import com.example.rover_control.data.bluetooth.ConnectionState
import com.example.rover_control.data.bluetooth.PairedDevice
import com.example.rover_control.data.bluetooth.isConnected
import com.example.rover_control.ui.theme.Background
import com.example.rover_control.ui.theme.ErrorRed
import com.example.rover_control.ui.theme.OnSurfaceVariant
import com.example.rover_control.ui.theme.PrimaryCyan
import com.example.rover_control.ui.theme.SuccessGreen
import com.example.rover_control.ui.theme.WarningAmber
import com.example.rover_control.util.requiredBluetoothPermissions
import kotlinx.coroutines.delay

@Composable
fun ConnectionScreen(
    onConnected: () -> Unit,
    viewModel: ConnectionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.onPermissionResult() }

    val requestPermissions: () -> Unit = {
        val permissions = requiredBluetoothPermissions()
        if (permissions.isEmpty()) {
            viewModel.refresh()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refresh() }

    val openAppSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        runCatching { context.startActivity(intent) }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    LaunchedEffect(uiState.hasBluetoothPermission, uiState.isBluetoothSupported) {
        if (uiState.isBluetoothSupported && !uiState.hasBluetoothPermission) {
            showPermissionDialog = true
        }
    }

    val errorResId = uiState.errorResId
    val errorMessage = uiState.errorMessage
    val errorText = when {
        errorResId != null -> stringResource(errorResId)
        errorMessage != null ->
            stringResource(R.string.connection_failed, errorMessage)
        else -> null
    }
    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbarHostState.showSnackbar(errorText)
            viewModel.consumeError()
        }
    }

    var wasConnected by remember { mutableStateOf(uiState.connectionState.isConnected) }
    LaunchedEffect(uiState.connectionState) {
        val connected = uiState.connectionState.isConnected
        if (connected && !wasConnected) {
            // Let the tap that started the connection finish before swapping
            // screens, otherwise its release can land on the Control screen.
            delay(350)
            if (uiState.connectionState.isConnected) onConnected()
        }
        wasConnected = connected
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            StatusCard(
                state = uiState.connectionState,
                onDisconnect = viewModel::disconnect,
                onContinue = onConnected,
                onReconnect = uiState.pairedDevices
                    .firstOrNull { it.address == uiState.lastDeviceMac }
                    ?.let { device -> { viewModel.connect(device) } }
            )

            if (!uiState.isBluetoothSupported) {
                Spacer(Modifier.height(12.dp))
                Banner(color = ErrorRed) {
                    Text(
                        text = stringResource(R.string.connection_bluetooth_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (!uiState.hasBluetoothPermission) {
                Spacer(Modifier.height(12.dp))
                BannerBlock(color = ErrorRed) {
                    Text(
                        text = stringResource(R.string.connection_permission_denied),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = requestPermissions) {
                            Text(stringResource(R.string.connection_grant_permission))
                        }
                        TextButton(onClick = openAppSettings) {
                            Text(stringResource(R.string.connection_open_settings))
                        }
                    }
                }
            } else if (!uiState.isBluetoothEnabled) {
                Spacer(Modifier.height(12.dp))
                Banner(color = WarningAmber) {
                    Text(
                        text = stringResource(R.string.connection_bluetooth_off),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            runCatching {
                                enableBluetoothLauncher.launch(
                                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                )
                            }
                        }
                    ) {
                        Text(stringResource(R.string.connection_enable_bluetooth))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.connection_paired_devices),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::refresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.content_description_refresh_devices)
                    )
                }
            }

            if (uiState.pairedDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.connection_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.pairedDevices, key = { it.address }) { device ->
                        DeviceRow(
                            device = device,
                            isLastUsed = device.address == uiState.lastDeviceMac,
                            isConnectedToThisDevice = uiState.connectionState.let { state ->
                                state is ConnectionState.Connected &&
                                    !state.isDemo &&
                                    state.deviceName == device.displayName
                            },
                            canConnect = uiState.hasBluetoothPermission &&
                                uiState.isBluetoothEnabled &&
                                uiState.connectionState !is ConnectionState.Connecting,
                            onConnect = { viewModel.connect(device) }
                        )
                    }
                }
            }

            DemoCard(
                checked = uiState.demoMode,
                onCheckedChange = viewModel::setDemoMode
            )

            Text(
                text = stringResource(R.string.connection_tip),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.connection_permission_title)) },
            text = { Text(stringResource(R.string.connection_permission_rationale)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        requestPermissions()
                    }
                ) {
                    Text(stringResource(R.string.connection_grant_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }
}

@Composable
private fun StatusCard(
    state: ConnectionState,
    onDisconnect: () -> Unit,
    onContinue: () -> Unit,
    onReconnect: (() -> Unit)?
) {
    val (text, color) = when (state) {
        is ConnectionState.Connected -> if (state.isDemo) {
            stringResource(R.string.connection_demo_active) to WarningAmber
        } else {
            stringResource(R.string.status_connected_to, state.deviceName) to SuccessGreen
        }

        is ConnectionState.Connecting ->
            stringResource(R.string.status_connecting) to WarningAmber

        is ConnectionState.Disconnected ->
            stringResource(R.string.status_disconnected) to ErrorRed
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = color, modifier = Modifier.width(10.dp).height(10.dp)) {}
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        color = color
                    )
                    if (state is ConnectionState.Disconnected && state.lastError != null) {
                        Text(
                            text = state.lastError,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            if (state is ConnectionState.Connected) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDisconnect) {
                        Text(stringResource(R.string.connection_disconnect))
                    }
                    Button(onClick = onContinue) {
                        Text(stringResource(R.string.connection_continue))
                    }
                }
            } else if (state is ConnectionState.Disconnected && onReconnect != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onReconnect) {
                    Text(stringResource(R.string.connection_reconnect))
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: PairedDevice,
    isLastUsed: Boolean,
    isConnectedToThisDevice: Boolean,
    canConnect: Boolean,
    onConnect: () -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isLastUsed) {
                    Modifier.border(1.dp, PrimaryCyan.copy(alpha = 0.5f), shape)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Bluetooth,
                contentDescription = null,
                tint = PrimaryCyan
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                if (isLastUsed) {
                    Text(
                        text = stringResource(R.string.connection_last_used),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryCyan
                    )
                }
            }
            if (isConnectedToThisDevice) {
                Text(
                    text = stringResource(R.string.connection_connected),
                    style = MaterialTheme.typography.labelLarge,
                    color = SuccessGreen
                )
            } else {
                Button(onClick = onConnect, enabled = canConnect) {
                    Text(stringResource(R.string.connection_connect))
                }
            }
        }
    }
}

@Composable
private fun DemoCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.connection_demo_mode),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.connection_demo_mode_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun BannerBlock(
    color: androidx.compose.ui.graphics.Color,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            content = content
        )
    }
}

@Composable
private fun Banner(
    color: androidx.compose.ui.graphics.Color,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
