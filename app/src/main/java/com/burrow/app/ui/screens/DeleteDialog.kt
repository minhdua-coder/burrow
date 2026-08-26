package com.burrow.app.ui.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.burrow.app.ui.theme.Burrow
import com.burrow.app.viewmodel.BurrowViewModel
import com.burrow.app.viewmodel.ConfirmDeleteState

@Composable
fun DeleteConfirmDialog(confirmDelete: ConfirmDeleteState, viewModel: BurrowViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.cancelDelete() },
        containerColor = Burrow.Bg,
        shape = RoundedCornerShape(32.dp),
        title = {
            Text("Delete \"${confirmDelete.name}\"?", style = MaterialTheme.typography.titleSmall, color = Burrow.Text)
        },
        text = {
            Text("This can't be undone.", style = MaterialTheme.typography.bodyMedium, color = Burrow.Neutral700)
        },
        confirmButton = {
            Button(
                onClick = { viewModel.confirmDeleteNow() },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Burrow.Accent700, contentColor = Burrow.Bg),
            ) { Text("Delete") }
        },
        dismissButton = {
            OutlinedButton(onClick = { viewModel.cancelDelete() }, shape = RoundedCornerShape(50)) { Text("Cancel") }
        },
    )
}
