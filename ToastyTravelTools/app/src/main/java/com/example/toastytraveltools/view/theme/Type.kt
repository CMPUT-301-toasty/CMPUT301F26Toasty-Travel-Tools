package com.example.toastytraveltools.view.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.toastytraveltools.R

// Lato, bundled from Google Fonts (OFL). Google's Lato has no ExtraBold (800) cut,
// so ExtraBold and Black both resolve to Lato Black.
val Lato = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_bold, FontWeight.Bold),
    Font(R.font.lato_black, FontWeight.ExtraBold),
    Font(R.font.lato_black, FontWeight.Black),
)

private fun lato(weight: FontWeight, size: TextUnit, lineHeight: TextUnit) = TextStyle(
    fontFamily = Lato,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = 0.sp,
)

val Typography = Typography(
    displayLarge = lato(FontWeight.Black, 57.sp, 64.sp),
    displayMedium = lato(FontWeight.Black, 45.sp, 52.sp),
    displaySmall = lato(FontWeight.Black, 36.sp, 44.sp),
    headlineLarge = lato(FontWeight.ExtraBold, 28.sp, 32.sp),
    headlineMedium = lato(FontWeight.ExtraBold, 24.sp, 30.sp),
    headlineSmall = lato(FontWeight.ExtraBold, 22.sp, 28.sp),
    titleLarge = lato(FontWeight.Bold, 20.sp, 27.sp),
    titleMedium = lato(FontWeight.Bold, 16.sp, 22.sp),
    titleSmall = lato(FontWeight.Bold, 14.sp, 19.sp),
    bodyLarge = lato(FontWeight.Normal, 15.sp, 20.sp),
    bodyMedium = lato(FontWeight.Normal, 14.sp, 19.sp),
    bodySmall = lato(FontWeight.Normal, 12.sp, 16.sp),
    labelLarge = lato(FontWeight.Bold, 15.sp, 20.sp),
    labelMedium = lato(FontWeight.Bold, 12.sp, 16.sp),
    labelSmall = lato(FontWeight.Bold, 11.sp, 14.sp),
)
