package com.burrow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.burrow.app.ui.ICON_KEYS
import com.burrow.app.ui.iconFor
import com.burrow.app.ui.theme.Burrow
import com.burrow.app.ui.theme.FolderColors
import com.burrow.app.viewmodel.BurrowViewModel
import com.burrow.app.viewmodel.FormMode
import com.burrow.app.viewmodel.Sheet
import com.burrow.app.viewmodel.UiState

private fun sheetTitle(sheet: Sheet): String = when (sheet) {
    is Sheet.FolderForm -> if (sheet.mode == FormMode.ADD) "New folder" else "Rename folder"
    is Sheet.LinkForm -> if (sheet.mode == FormMode.ADD) "New link" else "Edit link"
    is Sheet.VariableForm -> if (sheet.mode == FormMode.ADD) "New variable" else "Edit variable"
    is Sheet.FolderActions -> sheet.item.name
    is Sheet.LinkActions -> sheet.item.name
    is Sheet.VariableActions -> sheet.item.key
    Sheet.Settings -> "Settings"
    is Sheet.ChangePinForm -> "Change PIN"
}

@Composable
fun SheetContent(state: UiState, viewModel: BurrowViewModel) {
    val sheet = state.sheet ?: return
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(sheetTitle(sheet), style = MaterialTheme.typography.titleMedium, color = Burrow.Text)
            IconButton(onClick = { viewModel.closeSheet() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Burrow.Neutral600)
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))

        when (sheet) {
            is Sheet.FolderForm -> {
                LabeledField("Folder name", sheet.name, viewModel::updateFolderFormName, "e.g. Kaspersky")
                androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
                Text("Color", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral700)
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FolderColors.forEach { c ->
                        val selected = sheet.color == c.key
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(c.bg, CircleShape)
                                .border(
                                    if (selected) 3.dp else 2.dp,
                                    if (selected) Burrow.Accent700 else Burrow.Neutral300,
                                    CircleShape,
                                )
                                .clickable { viewModel.updateFolderFormColor(c.key) },
                        )
                    }
                }
                FormActions(viewModel)
            }
            is Sheet.LinkForm -> {
                LabeledField("Name", sheet.name, viewModel::updateLinkFormName, "e.g. Swagger API")
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                LabeledField("URL", sheet.url, viewModel::updateLinkFormUrl, "https://…")
                IconPicker(sheet.icon, viewModel::updateLinkFormIcon)
                FormActions(viewModel)
            }
            is Sheet.VariableForm -> {
                LabeledField("Key", sheet.key, viewModel::updateVariableFormKey, "e.g. SSH VPS")
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                LabeledField("Value", sheet.value, viewModel::updateVariableFormValue, "value or secret", monospace = true, multiline = true)
                IconPicker(sheet.icon, viewModel::updateVariableFormIcon)
                FormActions(viewModel)
            }
            is Sheet.FolderActions -> {
                ActionButton("Rename") { viewModel.openEditFolder(sheet.item) }
                ActionButton("Delete", danger = true) { viewModel.requestDeleteFolder(sheet.item) }
            }
            is Sheet.LinkActions -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                ActionButton("Open link") {
                    runCatching {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(sheet.item.url)))
                    }
                    viewModel.closeSheet()
                }
                ActionButton("Copy link") {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(sheet.item.url))
                    viewModel.showToast("Link copied")
                    viewModel.closeSheet()
                }
                ActionButton("Edit") { viewModel.openEditLink(sheet.item) }
                ActionButton("Delete", danger = true) { viewModel.requestDeleteLink(sheet.item) }
            }
            is Sheet.VariableActions -> {
                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                ActionButton("Copy key") {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(sheet.item.key))
                    viewModel.showToast("Key copied")
                    viewModel.closeSheet()
                }
                ActionButton("Copy value") {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(sheet.item.value))
                    viewModel.showToast("Value copied")
                    viewModel.closeSheet()
                }
                ActionButton("Edit") { viewModel.openEditVariable(sheet.item) }
                ActionButton("Delete", danger = true) { viewModel.requestDeleteVariable(sheet.item) }
            }
            Sheet.Settings -> {
                ActionButton("Change PIN") { viewModel.openChangePin() }
            }
            is Sheet.ChangePinForm -> {
                LabeledField("Current PIN", sheet.current, viewModel::updatePinCurrent, "••••", keyboardType = KeyboardType.NumberPassword, password = true)
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                LabeledField("New PIN", sheet.next, viewModel::updatePinNext, "••••", keyboardType = KeyboardType.NumberPassword, password = true)
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                LabeledField("Confirm new PIN", sheet.confirm, viewModel::updatePinConfirm, "••••", keyboardType = KeyboardType.NumberPassword, password = true)
                FormActions(viewModel)
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    monospace: Boolean = false,
    multiline: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral700)
        androidx.compose.foundation.layout.Spacer(Modifier.height(5.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder) },
            singleLine = !multiline,
            minLines = if (multiline) 3 else 1,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = if (monospace) MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Burrow.Surface,
                unfocusedContainerColor = Burrow.Surface,
                focusedBorderColor = Burrow.Accent,
                unfocusedBorderColor = Burrow.Divider,
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun IconPicker(current: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
    Text("Icon", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral700)
    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ICON_KEYS.forEach { key ->
            val selected = current == key
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Burrow.Neutral100, CircleShape)
                    .border(2.5.dp, if (selected) Burrow.Accent700 else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconFor(key), contentDescription = key, tint = if (selected) Burrow.Accent700 else Burrow.Text)
            }
        }
    }
}

@Composable
private fun FormActions(viewModel: BurrowViewModel) {
    androidx.compose.foundation.layout.Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { viewModel.closeSheet() },
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f),
        ) { Text("Cancel") }
        Button(
            onClick = { viewModel.saveForm() },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Burrow.Accent, contentColor = Burrow.Bg),
            modifier = Modifier.weight(1f),
        ) { Text("Save") }
    }
}

@Composable
private fun ActionButton(label: String, danger: Boolean = false, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(contentColor = if (danger) Burrow.Accent700 else Burrow.Accent),
    ) {
        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
    }
}
