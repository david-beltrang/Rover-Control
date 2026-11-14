package com.example.rover_control.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rover_control.R
import com.example.rover_control.RoverApplication
import com.example.rover_control.data.bluetooth.isConnected
import com.example.rover_control.ui.screens.connection.ConnectionScreen
import com.example.rover_control.ui.screens.control.ControlScreen
import com.example.rover_control.ui.screens.telemetry.TelemetryScreen
import com.example.rover_control.ui.theme.Background
import com.example.rover_control.ui.theme.SurfaceDark

object Routes {
    const val CONNECTION = "connection"
    const val CONTROL = "control"
    const val TELEMETRY = "telemetry"
}

@Composable
fun RoverNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as RoverApplication
    val connectionState by app.roverRepository.connectionState.collectAsStateWithLifecycle()
    val connected = connectionState.isConnected

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentRoute == Routes.CONTROL,
                    onClick = {
                        if (connected) navController.navigateTopLevel(Routes.CONTROL)
                    },
                    enabled = connected,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.SportsEsports,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.nav_control)) }
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.TELEMETRY,
                    onClick = {
                        if (connected) navController.navigateTopLevel(Routes.TELEMETRY)
                    },
                    enabled = connected,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Sensors,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.nav_telemetry)) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CONNECTION,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.CONNECTION) {
                ConnectionScreen(
                    onConnected = {
                        navController.navigate(Routes.CONTROL) {
                            launchSingleTop = true
                            popUpTo(Routes.CONNECTION) { inclusive = false }
                        }
                    }
                )
            }
            composable(Routes.CONTROL) {
                ControlScreen()
            }
            composable(Routes.TELEMETRY) {
                TelemetryScreen()
            }
        }
    }
}

private fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.CONTROL) { inclusive = false }
        launchSingleTop = true
        restoreState = false
    }
}
