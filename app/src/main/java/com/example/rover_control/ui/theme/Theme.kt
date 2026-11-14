package com.example.rover_control.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RoverDarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = PrimaryOnDark,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = PrimaryCyan,
    secondary = SecondaryPurple,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = OnBackground,
    background = Background,
    onBackground = OnBackground,
    surface = SurfaceDark,
    onSurface = OnBackground,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceVariantDark,
    error = ErrorRed,
    onError = Color.White,
    outline = OnSurfaceVariant,
    outlineVariant = SurfaceVariantDark
)

val RoverShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * App theme. The app is dark-only by design (see Implementation_plan.md §6.6);
 * dynamic color is intentionally not used so the rover palette stays stable.
 */
@Composable
fun RoverControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RoverDarkColorScheme,
        typography = Typography,
        shapes = RoverShapes,
        content = content
    )
}
