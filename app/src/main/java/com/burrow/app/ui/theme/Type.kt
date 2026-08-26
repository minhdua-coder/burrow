package com.burrow.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val HeadingFamily = FontFamily.SansSerif
val BodyFamily = FontFamily.SansSerif

val BurrowTypography = Typography(
    displayLarge = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Black, fontSize = 42.sp, lineHeight = 47.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Black, fontSize = 25.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Black, fontSize = 20.sp, lineHeight = 23.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 17.sp),
    labelMedium = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, lineHeight = 15.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.9.sp),
    bodyLarge = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
)
