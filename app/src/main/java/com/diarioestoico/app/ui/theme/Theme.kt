package com.diarioestoico.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    background       = PaperBg,          // warm parchment
    surface          = PaperSurface,     // near-white warm surface
    surfaceVariant   = PaperQuoteTint,   // quote card tint
    onBackground     = InkPrimary,       // main text
    onSurface        = InkSecondary,     // secondary text
    onSurfaceVariant = InkTertiary,      // tertiary / captions
    primary          = AccentSienna,     // sienna accent
    onPrimary        = PaperSurface,
    secondary        = InkSecondary,
    onSecondary      = PaperSurface,
    outline          = InkLine,          // dividers, borders
    outlineVariant   = PaperSurface2,    // segmented control bg
)

private val DarkColors = darkColorScheme(
    background       = NightBg,
    surface          = NightSurface,
    surfaceVariant   = NightSurface2,
    onBackground     = NightInk,
    onSurface        = NightInk2,
    onSurfaceVariant = NightInk3,
    primary          = NightAccent,
    onPrimary        = NightBg,
    secondary        = NightInk2,
    onSecondary      = NightBg,
    outline          = NightLine,
    outlineVariant   = NightSurface2,
)

@Composable
fun DiarioEstoicoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography  = StoicTypography,
        content     = content
    )
}
