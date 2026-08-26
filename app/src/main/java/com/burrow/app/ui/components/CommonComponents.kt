package com.burrow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.burrow.app.ui.theme.Burrow

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = Burrow.Text,
    size: Int = 36,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(size.dp)) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
fun RowIconBadge(icon: ImageVector, bg: Color, fg: Color, size: Int = 32) {
    Box(
        modifier = Modifier.size(size.dp).background(bg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size((size * 0.47).dp))
    }
}

@Composable
fun DragHandle(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.DragIndicator,
        contentDescription = "Reorder",
        tint = Burrow.Neutral400,
        modifier = modifier,
    )
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Burrow.Neutral600,
    )
}

@Composable
fun Tag(text: String, container: Color, content: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = Modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
