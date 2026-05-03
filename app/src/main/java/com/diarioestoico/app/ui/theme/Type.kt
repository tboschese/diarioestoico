package com.diarioestoico.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.diarioestoico.app.R

// Lora — serif designed for screen reading; excellent legibility + classic feel
val LibreBaskerville = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_bold,    FontWeight.Bold),
    Font(R.font.lora_italic,  FontWeight.Normal, FontStyle.Italic)
)

val StoicTypography = Typography(
    // Day title — clear, prominent
    headlineLarge = TextStyle(
        fontFamily = LibreBaskerville,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 29.sp,
        letterSpacing = 0.2.sp
    ),
    // Date / section label (small caps style)
    headlineMedium = TextStyle(
        fontFamily = LibreBaskerville,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 1.8.sp
    ),
    // Stoic quote — italic, generous line height for comfortable reading
    bodyLarge = TextStyle(
        fontFamily = LibreBaskerville,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 17.sp,
        lineHeight = 30.sp,   // extra spacing = easier to follow lines
        letterSpacing = 0.1.sp
    ),
    // Commentary body — regular weight, optimised line length & height
    bodyMedium = TextStyle(
        fontFamily = LibreBaskerville,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.05.sp
    ),
    // Author attribution
    labelMedium = TextStyle(
        fontFamily = LibreBaskerville,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.8.sp
    ),
    // Small caps labels (date, section headers)
    labelSmall = TextStyle(
        fontFamily = LibreBaskerville,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.sp
    )
)
