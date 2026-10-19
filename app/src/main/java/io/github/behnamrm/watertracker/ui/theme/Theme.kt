package io.github.behnamrm.watertracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC6F1F5),
    onSecondaryContainer = Color(0xFF002022),
    tertiary = Color(0xFF6A5AA8),
    background = Color(0xFFF7F9FF),
    surface = Color(0xFFF7F9FF),
    surfaceContainerLow = Color(0xFFF0F4FC),
    surfaceContainer = Color(0xFFEAEFF8),
    surfaceContainerHigh = Color(0xFFE4E9F3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003063),
    primaryContainer = Color(0xFF00468B),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF82D3DD),
    onSecondary = Color(0xFF00363B),
    secondaryContainer = Color(0xFF004F56),
    onSecondaryContainer = Color(0xFFC6F1F5),
    tertiary = Color(0xFFCBBEFF),
    background = Color(0xFF111318),
    surface = Color(0xFF111318),
)

@Composable
fun WaterTrackerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
