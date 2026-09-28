package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import com.example.data.model.DarkVariant
import com.example.data.model.TextFontFamily
import com.example.data.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

private val DarkGoldColorScheme = darkColorScheme(
    primary = DarkGoldPrimary,
    onPrimary = DarkGoldOnPrimary,
    primaryContainer = DarkGoldPrimaryContainer,
    onPrimaryContainer = DarkGoldOnPrimaryContainer,
    background = DarkGoldBackground,
    onBackground = DarkGoldOnBackground,
    surface = DarkGoldSurface,
    onSurface = DarkGoldOnSurface,
    surfaceVariant = DarkGoldSurfaceVariant,
    onSurfaceVariant = DarkGoldOnSurfaceVariant,
    outline = DarkGoldOutline
)

private val DarkBlueColorScheme = darkColorScheme(
    primary = DarkBluePrimary,
    onPrimary = DarkBlueOnPrimary,
    primaryContainer = DarkBluePrimaryContainer,
    onPrimaryContainer = DarkBlueOnPrimaryContainer,
    background = DarkBlueBackground,
    onBackground = DarkBlueOnBackground,
    surface = DarkBlueSurface,
    onSurface = DarkBlueOnSurface,
    surfaceVariant = DarkBlueSurfaceVariant,
    onSurfaceVariant = DarkBlueOnSurfaceVariant,
    outline = DarkBlueOutline
)

fun getFontFamily(font: TextFontFamily): FontFamily {
    return when (font) {
        TextFontFamily.SERIF -> FontFamily.Serif
        TextFontFamily.SANS -> FontFamily.SansSerif
        TextFontFamily.MONO -> FontFamily.Monospace
    }
}

@Composable
fun RBibliaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkVariant: DarkVariant = DarkVariant.GOLD,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) {
        when (darkVariant) {
            DarkVariant.GOLD -> DarkGoldColorScheme
            DarkVariant.BLUE -> DarkBlueColorScheme
        }
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
