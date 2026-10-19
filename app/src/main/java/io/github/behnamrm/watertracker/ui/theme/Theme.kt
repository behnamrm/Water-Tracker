package io.github.behnamrm.watertracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.behnamrm.watertracker.R

/** Dark blue: buttons, selected items, text. */
val Navy = Color(0xFF0F2A4A)

/** Light blue: the accent for "today" and highlights. */
val SkyBlue = Color(0xFF4AAEEA)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEFFC),
    onPrimaryContainer = Navy,
    secondary = SkyBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEFFC),
    onSecondaryContainer = Navy,
    tertiary = SkyBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCEFFC),
    onTertiaryContainer = Navy,
    background = Color(0xFFF2F3F5),
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = Color(0xFFF1F3F6),
    onSurfaceVariant = Color(0xFF8A94A3),
    outline = Color(0xFFC9D0DA),
    outlineVariant = Color(0xFFE4E7EC),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainer = Color(0xFFF1F3F6),
    surfaceContainerHigh = Color(0xFFECEFF3),
    surfaceContainerHighest = Color(0xFFE4E7EC),
    inverseSurface = Navy,
    inverseOnSurface = Color.White,
    inversePrimary = SkyBlue,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE6EEF8),
    onPrimary = Navy,
    primaryContainer = Color(0xFF1D3A5F),
    onPrimaryContainer = Color(0xFFE6EEF8),
    secondary = SkyBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1D3A5F),
    onSecondaryContainer = Color(0xFFE6EEF8),
    tertiary = SkyBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF1D3A5F),
    onTertiaryContainer = Color(0xFFE6EEF8),
    background = Color(0xFF0B1422),
    onBackground = Color(0xFFE6EEF8),
    surface = Color(0xFF14213A),
    onSurface = Color(0xFFE6EEF8),
    surfaceVariant = Color(0xFF1B2A45),
    onSurfaceVariant = Color(0xFF8D9AAE),
    outline = Color(0xFF3A4A66),
    outlineVariant = Color(0xFF26344D),
    surfaceContainerLowest = Color(0xFF0E1829),
    surfaceContainerLow = Color(0xFF14213A),
    surfaceContainer = Color(0xFF1B2A45),
    surfaceContainerHigh = Color(0xFF223250),
    surfaceContainerHighest = Color(0xFF2A3B5B),
)

@OptIn(ExperimentalTextApi::class)
private fun dmSans(weight: Int) = Font(
    R.font.dm_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val DmSans = FontFamily(dmSans(400), dmSans(500), dmSans(600), dmSans(700), dmSans(800))

private val Base = Typography()

private fun TextStyle.dm() = copy(fontFamily = DmSans)

private val AppTypography = Typography(
    displayLarge = Base.displayLarge.dm().copy(fontWeight = FontWeight.Bold, letterSpacing = (-2).sp),
    displayMedium = Base.displayMedium.dm().copy(fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp),
    displaySmall = Base.displaySmall.dm().copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineLarge = Base.headlineLarge.dm().copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.dm().copy(fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    headlineSmall = Base.headlineSmall.dm().copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.dm().copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.dm().copy(fontWeight = FontWeight.Bold),
    titleSmall = Base.titleSmall.dm().copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.dm(),
    bodyMedium = Base.bodyMedium.dm(),
    bodySmall = Base.bodySmall.dm(),
    labelLarge = Base.labelLarge.dm().copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.dm().copy(fontWeight = FontWeight.Medium),
    labelSmall = Base.labelSmall.dm().copy(fontWeight = FontWeight.Medium),
)

/** Small, spaced-out capitals used as card captions ("TODAY", "GOAL"). */
val OverlineStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 1.4.sp,
)

/** Small monospaced figures ("250 ml"). */
val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    letterSpacing = 0.sp,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun WaterTrackerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
