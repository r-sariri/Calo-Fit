package com.example.calofit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

data class CaloFitColors(
    val mainBackground: Color,
    val secondaryBackground: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val cardSurface: Color,
    val cardSurfaceSubtle: Color,
    val cardBorder: Color,
    val accentTextOnLight: Color,
    val onPrimaryAccent: Color,
    val isDark: Boolean
)

val LightCaloFitColors = CaloFitColors(
    mainBackground = LightMainBackground,
    secondaryBackground = LightSecondaryBackground,
    primaryAccent = LightPrimaryAccent,
    secondaryAccent = LightSecondaryAccent,
    primaryText = LightPrimaryText,
    secondaryText = LightSecondaryText,
    cardSurface = LightCardSurface,
    cardSurfaceSubtle = LightCardSurfaceSubtle,
    cardBorder = LightCardBorder,
    accentTextOnLight = Color(0xFF5D46B8),
    onPrimaryAccent = Color.White,
    isDark = false
)

val DarkCaloFitColors = CaloFitColors(
    mainBackground = DarkMainBackground,
    secondaryBackground = DarkSecondaryBackground,
    primaryAccent = DarkPrimaryAccent,
    secondaryAccent = DarkSecondaryAccent,
    primaryText = DarkPrimaryText,
    secondaryText = DarkSecondaryText,
    cardSurface = DarkCardSurface,
    cardSurfaceSubtle = DarkCardSurfaceSubtle,
    cardBorder = DarkCardBorder,
    accentTextOnLight = Color(0xFFB8A9F2),
    onPrimaryAccent = Color(0xFF141222),
    isDark = true
)

val LocalCaloFitColors = staticCompositionLocalOf { LightCaloFitColors }

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryAccent,
    onPrimary = Color.White,
    secondary = LightSecondaryAccent,
    onSecondary = Color.White,
    background = LightMainBackground,
    surface = LightCardSurface,
    onSurface = LightPrimaryText,
    error = CaloFitError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryAccent,
    onPrimary = Color(0xFF141222),
    secondary = DarkSecondaryAccent,
    onSecondary = Color(0xFF141222),
    background = DarkMainBackground,
    surface = DarkCardSurface,
    onSurface = DarkPrimaryText,
    error = CaloFitError
)

@Composable
fun CaloFitTheme(
    themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    isPersian: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val caloFitColors = if (isDark) DarkCaloFitColors else LightCaloFitColors
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val typography = caloFitTypography(isPersian = isPersian)
    val fontFamily = if (isPersian) VazirmatnFontFamily else PlusJakartaFontFamily

    CompositionLocalProvider(LocalCaloFitColors provides caloFitColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            ProvideTextStyle(value = TextStyle(fontFamily = fontFamily)) {
                content()
            }
        }
    }
}
