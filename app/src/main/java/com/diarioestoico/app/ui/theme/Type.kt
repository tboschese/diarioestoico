package com.diarioestoico.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.diarioestoico.app.R

// Editorial serif — Lora (quotes, commentary body, phrase cards)
val LoraFamily = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_bold,    FontWeight.Bold),
    Font(R.font.lora_italic,  FontWeight.Normal, FontStyle.Italic)
)

// Modern sans — system Roboto for titles and all UI chrome
val SansFamily: FontFamily = FontFamily.SansSerif

val StoicTypography = Typography(
    // Screen section titles ("Favoritos")
    headlineLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.5).sp
    ),
    // Day title in reading screen
    headlineMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W600,
        fontSize = 31.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp
    ),
    // Smaller heading
    headlineSmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W600,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    // Quote text — Lora italic
    bodyLarge = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 20.sp,
        lineHeight = 33.sp,
        letterSpacing = 0.sp
    ),
    // Commentary body — Lora regular
    bodyMedium = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    // Smaller body (phrase cards, card quotes)
    bodySmall = TextStyle(
        fontFamily = LoraFamily,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 22.sp
    ),
    // Button / action labels
    labelLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W600,
        fontSize = 14.5.sp
    ),
    // Overlines, author attribution
    labelMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.8.sp
    ),
    // Tiny labels (dates, section headers)
    labelSmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W600,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.sp
    )
)
