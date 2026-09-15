package com.meminzazo.stwvplanner.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StormColorScheme = darkColorScheme(
    primary = StormCyan,
    onPrimary = StormBackground,
    primaryContainer = StormCardElevated,
    onPrimaryContainer = StormTextMain,
    secondary = StormIndigo,
    onSecondary = StormTextMain,
    tertiary = StormAmber,
    onTertiary = StormBackground,
    background = StormBackground,
    onBackground = StormTextMain,
    surface = StormCardSurface,
    onSurface = StormTextMain,
    surfaceVariant = StormCardElevated,
    onSurfaceVariant = StormTextMuted,
    error = SpendRed,
    onError = StormTextMain,
    outline = StormBorder
)

@Composable
fun STWVPlannerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StormColorScheme,
        typography = Typography,
        content = content
    )
}
