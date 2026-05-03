package com.diarioestoico.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    background = Parchment,
    surface = ParchmentDark,
    onBackground = InkBrown,
    onSurface = InkMedium,
    primary = AccentGold,
    onPrimary = Parchment,
    secondary = InkLight,
    onSecondary = Parchment,
    outline = DividerColor,
    surfaceVariant = QuoteBackground,
    onSurfaceVariant = InkMedium
)

private val DarkColors = darkColorScheme(
    background = NightBackground,
    surface = NightSurface,
    onBackground = NightText,
    onSurface = NightText,
    primary = NightAccent,
    onPrimary = NightBackground,
    secondary = NightTextSecondary,
    onSecondary = NightBackground,
    outline = NightDivider,
    surfaceVariant = NightSurface,
    onSurfaceVariant = NightTextSecondary
)

@Composable
fun DiarioEstoicoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = StoicTypography,
        content = content
    )
}
