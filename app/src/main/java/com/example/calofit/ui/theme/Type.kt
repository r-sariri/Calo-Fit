package com.example.calofit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn, FontWeight.Medium),
    Font(R.font.vazirmatn, FontWeight.SemiBold),
    Font(R.font.vazirmatn, FontWeight.Bold),
    Font(R.font.vazirmatn, FontWeight.ExtraBold)
)

val PlusJakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

fun caloFitTypography(isPersian: Boolean): Typography {
    val activeFamily = if (isPersian) VazirmatnFontFamily else PlusJakartaFontFamily
    return Typography(
        displayLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp
        ),
        labelLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.1.sp
        ),
        labelSmall = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp
        )
    )
}

val Typography = caloFitTypography(isPersian = false)
