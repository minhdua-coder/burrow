package com.burrow.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

val ICON_KEYS = listOf(
    "link", "globe", "key", "database", "terminal", "shield",
    "lock", "cloud", "code", "mail", "person", "creditcard", "wifi", "bolt", "file",
)

fun iconFor(key: String?): ImageVector = when (key) {
    "globe" -> Icons.Filled.Public
    "key" -> Icons.Filled.VpnKey
    "database" -> Icons.Filled.Storage
    "terminal" -> Icons.Filled.Terminal
    "shield" -> Icons.Filled.Shield
    "lock" -> Icons.Filled.Lock
    "cloud" -> Icons.Filled.Cloud
    "code" -> Icons.Filled.Code
    "mail" -> Icons.Filled.Email
    "person" -> Icons.Filled.Person
    "creditcard" -> Icons.Filled.CreditCard
    "wifi" -> Icons.Filled.Wifi
    "bolt" -> Icons.Filled.Bolt
    "file" -> Icons.Filled.InsertDriveFile
    else -> Icons.Filled.Link
}
