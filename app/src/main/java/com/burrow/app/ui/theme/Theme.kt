package com.burrow.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BurrowColorScheme = lightColorScheme(
    primary = Burrow.Accent,
    onPrimary = Burrow.Bg,
    primaryContainer = Burrow.Accent100,
    onPrimaryContainer = Burrow.Accent800,
    secondary = Burrow.Accent2,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Burrow.Accent2_100,
    onSecondaryContainer = Burrow.Accent2_800,
    background = Burrow.Neutral200,
    onBackground = Burrow.Text,
    surface = Burrow.Bg,
    onSurface = Burrow.Text,
    surfaceVariant = Burrow.Surface,
    onSurfaceVariant = Burrow.Neutral700,
    outline = Burrow.Divider,
    error = Burrow.Accent700,
)

val BurrowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun BurrowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BurrowColorScheme,
        typography = BurrowTypography,
        shapes = BurrowShapes,
        content = content,
    )
}
