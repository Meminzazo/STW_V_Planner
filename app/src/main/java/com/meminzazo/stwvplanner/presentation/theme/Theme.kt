package com.meminzazo.stwvplanner.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores inspirada en Fortnite / Save The World.
 * Se prioriza un modo oscuro elegante con acentos vibrantes.
 */
private val StwColorScheme = darkColorScheme(
    primary = FortPurple,
    secondary = FortBlue,
    tertiary = FortAccent,
    background = StwBackground,
    surface = StwCardSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = SpendRed,
    primaryContainer = FortDarkBlue,
    onPrimaryContainer = Color.White
)

@Composable
fun STWVPlannerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StwColorScheme,
        typography = Typography,
        content = content
    )
}
