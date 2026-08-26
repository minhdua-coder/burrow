package com.burrow.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.graphics.vector.ImageVector

val ICON_KEYS = listOf("link", "globe", "key", "database", "terminal", "shield")

fun iconFor(key: String?): ImageVector = when (key) {
    "globe" -> Icons.Filled.Public
    "key" -> Icons.Filled.VpnKey
    "database" -> Icons.Filled.Storage
    "terminal" -> Icons.Filled.Terminal
    "shield" -> Icons.Filled.Shield
    else -> Icons.Filled.Link
}
