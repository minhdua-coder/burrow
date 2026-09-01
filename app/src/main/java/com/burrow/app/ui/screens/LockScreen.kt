package com.burrow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.burrow.app.ui.theme.Burrow
import com.burrow.app.viewmodel.BurrowViewModel
import com.burrow.app.viewmodel.UiState

private sealed interface PadKey {
    data class Digit(val d: String) : PadKey
    data object Empty : PadKey
    data object Back : PadKey
}

private val PAD_KEYS: List<PadKey> =
    (1..9).map { PadKey.Digit(it.toString()) } + listOf(PadKey.Empty, PadKey.Digit("0"), PadKey.Back)

@Composable
fun LockScreen(state: UiState, viewModel: BurrowViewModel, onBiometricClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(58.dp).background(Burrow.Accent100, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Burrow.Accent700)
        }
        Spacer(Modifier.height(20.dp))
        Text("Enter PIN", style = MaterialTheme.typography.headlineSmall, color = Burrow.Text)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { i ->
                val filled = i < state.pinInput.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(if (filled) Burrow.Accent700 else Color.Transparent, CircleShape)
                        .border(1.5.dp, Burrow.Accent700, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.height(16.dp)) {
            if (state.pinError) {
                Text("Incorrect PIN", style = MaterialTheme.typography.bodySmall, color = Burrow.Accent700)
            }
        }
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PAD_KEYS.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .let {
                                    when (key) {
                                        is PadKey.Digit -> it
                                            .background(Burrow.Neutral100, CircleShape)
                                            .clickable { viewModel.pressPin(key.d) }
                                        is PadKey.Back -> it
                                            .clickable { viewModel.backspacePin() }
                                        PadKey.Empty -> it
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            when (key) {
                                is PadKey.Digit -> Text(
                                    key.d,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Burrow.Text,
                                )
                                is PadKey.Back -> Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = Burrow.Neutral600,
                                )
                                PadKey.Empty -> {}
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (onBiometricClick != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onBiometricClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = "Unlock with biometrics", tint = Burrow.Accent700)
            }
            Spacer(Modifier.height(8.dp))
        }
        Text("Hint: 1234", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral500)
    }
}
