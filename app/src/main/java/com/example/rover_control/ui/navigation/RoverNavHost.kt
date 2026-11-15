package com.example.rover_control.ui.navigation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

    ApplyRouteChrome(route = currentRoute)

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // The Control screen owns the HUD bottom pill and immersive
            // landscape layout. The shared bar stays for Telemetry/Connection.
            if (currentRoute != Routes.CONTROL) {
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
                ControlScreen(
                    onOpenTelemetry = {
                        if (connected) navController.navigateTopLevel(Routes.TELEMETRY)
                    },
                    onOpenConnection = {
                        navController.navigate(Routes.CONNECTION) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.TELEMETRY) {
                TelemetryScreen()
            }
        }
    }
}

/**
 * Control is a full-screen landscape HUD. Connection and Telemetry remain
 * portrait, so orientation and system bars are applied per destination rather
 * than locked globally in the manifest.
 */
@SuppressLint("SourceLockedOrientationActivity")
@Composable
private fun ApplyRouteChrome(route: String?) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = rememberActivity(context)

    LaunchedEffect(route, activity) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        when (route) {
            Routes.CONTROL -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }

            Routes.CONNECTION, Routes.TELEMETRY -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

@Composable
private fun rememberActivity(context: Context): Activity? =
    remember(context) { context.findActivity() }

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

private fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.CONTROL) { inclusive = false }
        launchSingleTop = true
        restoreState = false
    }
}
