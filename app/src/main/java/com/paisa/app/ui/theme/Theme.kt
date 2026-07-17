package com.paisa.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PaisaGreen,
    onPrimary = Color.White,
    primaryContainer = PaisaGreenContainer,
    onPrimaryContainer = Color(0xFF002114),
    secondary = PaisaGreenLight,
    secondaryContainer = PaisaMint,
    onSecondaryContainer = Color(0xFF0C1F15),
    tertiary = PaisaGold,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PaisaGreenLight,
    onPrimary = Color(0xFF003820),
    primaryContainer = Color(0xFF005231),
    onPrimaryContainer = PaisaGreenContainer,
    secondary = PaisaMint,
    secondaryContainer = Color(0xFF374B3F),
    onSecondaryContainer = PaisaMint,
    tertiary = PaisaGold,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

@Composable
fun PaisaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
