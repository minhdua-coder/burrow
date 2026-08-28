package com.burrow.app.ui.theme

import androidx.compose.ui.graphics.Color

object Burrow {
    val Bg = Color(0xFFF5EAD8)
    val Surface = Color(0xFFEBDDC5)
    val Text = Color(0xFF201E1D)
    val Accent = Color(0xFFC67139)
    val Accent2 = Color(0xFF7A8A5E)
    val Divider = Text.copy(alpha = 0.16f)

    val Neutral100 = Color(0xFFF9F4ED)
    val Neutral200 = Color(0xFFEEE7DB)
    val Neutral300 = Color(0xFFDCD3C4)
    val Neutral400 = Color(0xFFC0B6A5)
    val Neutral500 = Color(0xFFA19786)
    val Neutral600 = Color(0xFF82796A)
    val Neutral700 = Color(0xFF645C50)
    val Neutral800 = Color(0xFF474238)
    val Neutral900 = Color(0xFF2E2B25)

    val Accent100 = Color(0xFFFFF2EB)
    val Accent200 = Color(0xFFFFE1D0)
    val Accent300 = Color(0xFFFFC6A5)
    val Accent400 = Color(0xFFF6A06B)
    val Accent500 = Color(0xFFD67F48)
    val Accent600 = Color(0xFFB2622D)
    val Accent700 = Color(0xFF8C491A)
    val Accent800 = Color(0xFF643312)
    val Accent900 = Color(0xFF402310)

    val Accent2_100 = Color(0xFFF0FAE1)
    val Accent2_200 = Color(0xFFE1EECC)
    val Accent2_300 = Color(0xFFCCDBB2)
    val Accent2_400 = Color(0xFFAEBF92)
    val Accent2_500 = Color(0xFF8FA073)
    val Accent2_600 = Color(0xFF728157)
    val Accent2_700 = Color(0xFF56633F)
    val Accent2_800 = Color(0xFF3D472B)
    val Accent2_900 = Color(0xFF272E1B)

    // Two extra earthy hues, same tonal pattern as Accent/Accent2, so new
    // folder colors stay in-family instead of introducing a clashing hue.
    val Ochre100 = Color(0xFFFAF1DC)
    val Ochre600 = Color(0xFF9C7A32)
    val Ochre700 = Color(0xFF7A5F27)

    val Clay100 = Color(0xFFFBEAE7)
    val Clay600 = Color(0xFF9C554B)
    val Clay700 = Color(0xFF7A423A)
}

data class FolderColor(val key: String, val bg: Color, val fg: Color)

val FolderColors = listOf(
    FolderColor("sand", Burrow.Neutral200, Burrow.Neutral700),
    FolderColor("terracotta", Burrow.Accent100, Burrow.Accent700),
    FolderColor("sage", Burrow.Accent2_100, Burrow.Accent2_700),
    FolderColor("ochre", Burrow.Ochre100, Burrow.Ochre700),
    FolderColor("clay", Burrow.Clay100, Burrow.Clay700),
    FolderColor("terracotta-deep", Burrow.Accent600, Color.White),
    FolderColor("sage-deep", Burrow.Accent2_600, Color.White),
    FolderColor("ochre-deep", Burrow.Ochre600, Color.White),
    FolderColor("clay-deep", Burrow.Clay600, Color.White),
    FolderColor("ink", Burrow.Neutral700, Color.White),
)

fun folderColor(key: String?): FolderColor =
    FolderColors.find { it.key == key } ?: FolderColors[0]
