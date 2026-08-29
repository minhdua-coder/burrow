package com.burrow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.burrow.app.data.FolderNode
import com.burrow.app.data.ID_SUFFIX_LENGTHS
import com.burrow.app.data.findNode
import com.burrow.app.data.pathNames
import com.burrow.app.ui.ICON_KEYS
import com.burrow.app.ui.iconFor
import com.burrow.app.ui.theme.Burrow
import com.burrow.app.ui.theme.FolderColors
import com.burrow.app.viewmodel.BurrowViewModel
import com.burrow.app.viewmodel.DeleteKind
import com.burrow.app.viewmodel.FormMode
import com.burrow.app.viewmodel.Sheet
import com.burrow.app.viewmodel.UiState
import kotlinx.coroutines.launch

private fun sheetTitle(sheet: Sheet): String = when (sheet) {
    is Sheet.FolderForm -> if (sheet.mode == FormMode.ADD) "New folder" else "Rename folder"
    is Sheet.LinkForm -> if (sheet.mode == FormMode.ADD) "New link" else "Edit link"
    is Sheet.VariableForm -> if (sheet.mode == FormMode.ADD) "New environment" else "Edit environment"
    is Sheet.FolderActions -> sheet.item.name
    is Sheet.LinkActions -> sheet.item.name
    is Sheet.VariableActions -> sheet.item.key
    Sheet.Settings -> "Settings"
    is Sheet.ChangePinForm -> "Change PIN"
    is Sheet.IdGenerator -> if (sheet.format == com.burrow.app.viewmodel.RandomFormat.KEY) "Random key" else "Random ID suffix"
    is Sheet.EnvImportForm -> "Import environments from text"
    is Sheet.FileForm -> if (sheet.mode == FormMode.ADD) "New file" else "Rename file"
    is Sheet.FileActions -> sheet.item.name
    is Sheet.GithubTokenTool -> "GitHub App token"
    is Sheet.MoveItemPicker -> "Move \"${sheet.itemName}\""
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
                Row(
                    modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
                androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
                Text("Type", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral700)
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatToggleChip("Variable", !sheet.isSecret) { viewModel.updateVariableFormIsSecret(false) }
                    FormatToggleChip("Secret", sheet.isSecret) { viewModel.updateVariableFormIsSecret(true) }
                }
                IconPicker(sheet.icon, viewModel::updateVariableFormIcon)
                FormActions(viewModel)
            }
            is Sheet.FolderActions -> {
                ActionButton("Rename") { viewModel.openEditFolder(sheet.item) }
                ActionButton("Move to folder") { viewModel.openMoveFolder(sheet.item) }
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
                ActionButton("Share") {
                    shareText(context, "${sheet.item.name}\n${sheet.item.url}")
                    viewModel.closeSheet()
                }
                ActionButton("Edit") { viewModel.openEditLink(sheet.item) }
                ActionButton("Move to folder") { viewModel.openMoveLink(sheet.item) }
                ActionButton("Delete", danger = true) { viewModel.requestDeleteLink(sheet.item) }
            }
            is Sheet.VariableActions -> {
                val context = androidx.compose.ui.platform.LocalContext.current
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
                ActionButton("Share") {
                    shareText(context, "${sheet.item.key}: ${sheet.item.value}")
                    viewModel.closeSheet()
                }
                ActionButton("Edit") { viewModel.openEditVariable(sheet.item) }
                ActionButton("Move to folder") { viewModel.openMoveVariable(sheet.item) }
                ActionButton("Delete", danger = true) { viewModel.requestDeleteVariable(sheet.item) }
            }
            is Sheet.FileActions -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val downloadLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.CreateDocument("*/*"),
                ) { uri ->
                    if (uri != null) {
                        val ok = com.burrow.app.data.FileStorage.exportTo(context, sheet.item.id, uri)
                        viewModel.showToast(if (ok) "Downloaded" else "Download failed")
                    }
                }
                ActionButton("Download") {
                    downloadLauncher.launch(sheet.item.originalFileName.ifBlank { sheet.item.name })
                }
                ActionButton("Rename") { viewModel.openEditFile(sheet.item) }
                ActionButton("Move to folder") { viewModel.openMoveFile(sheet.item) }
                ActionButton("Delete", danger = true) { viewModel.requestDeleteFile(sheet.item) }
            }
            Sheet.Settings -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain"),
                ) { uri ->
                    if (uri != null) {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use {
                                it.write(viewModel.exportEnvContent().toByteArray())
                            }
                        }
                        viewModel.showToast("Exported")
                    }
                }
                val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                ) { uri ->
                    if (uri != null) {
                        val text = runCatching {
                            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        }.getOrNull()
                        if (text != null) viewModel.importEnvContent(text)
                    }
                }
                ActionButton("Export environments (.env)") { exportLauncher.launch("warren.env") }
                ActionButton("Import from file (.env)") { importLauncher.launch("*/*") }
                ActionButton("Import from pasted text") { viewModel.openEnvImportForm() }
                ActionButton("GitHub App token") { viewModel.openGithubTokenTool() }
                ActionButton("Check for updates") {
                    coroutineScope.launch {
                        when (val result = com.burrow.app.update.UpdateChecker.checkNow()) {
                            is com.burrow.app.update.UpdateCheckResult.UpdateAvailable -> {
                                val apkUrl = result.release.apkUrl ?: result.release.htmlUrl
                                com.burrow.app.update.UpdateInstaller.downloadAndInstall(context, apkUrl, result.release.tagName)
                            }
                            com.burrow.app.update.UpdateCheckResult.UpToDate ->
                                viewModel.showToast("You're on the latest version")
                            com.burrow.app.update.UpdateCheckResult.Failed ->
                                viewModel.showToast("Could not check for updates")
                        }
                    }
                }
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
            is Sheet.IdGenerator -> IdGeneratorContent(sheet, viewModel)
            is Sheet.FileForm -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val pickFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                ) { uri ->
                    if (uri != null) {
                        val displayName = com.burrow.app.data.FileStorage.queryDisplayName(context, uri) ?: "file"
                        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        val size = com.burrow.app.data.FileStorage.querySize(context, uri)
                        viewModel.updateFileFormPicked(uri, displayName, mimeType, size)
                    }
                }
                LabeledField("Name", sheet.name, viewModel::updateFileFormName, "e.g. Server backup")
                androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
                Text("File", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral700)
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Burrow.Surface, RoundedCornerShape(20.dp))
                        .clickable { pickFileLauncher.launch("*/*") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        sheet.originalFileName ?: "Choose a file…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sheet.originalFileName != null) Burrow.Text else Burrow.Neutral600,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.AttachFile, contentDescription = "Choose file", tint = Burrow.Neutral600)
                }
                IconPicker(sheet.icon, viewModel::updateFileFormIcon)
                FormActions(viewModel)
            }
            is Sheet.EnvImportForm -> {
                LabeledField(
                    "Paste .env content",
                    sheet.text,
                    viewModel::updateEnvImportText,
                    "KEY=\"value\"\nANOTHER_KEY=\"value\"",
                    monospace = true,
                    multiline = true,
                )
                FormActions(viewModel)
            }
            is Sheet.GithubTokenTool -> GithubTokenToolContent(sheet, viewModel)
            is Sheet.MoveItemPicker -> MoveItemPickerContent(sheet, viewModel, state.tree)
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
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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

fun shareText(context: android.content.Context, text: String) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(send, null))
}

@Composable
private fun IdGeneratorContent(sheet: Sheet.IdGenerator, viewModel: BurrowViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isKey = sheet.format == com.burrow.app.viewmodel.RandomFormat.KEY

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormatToggleChip("ID suffix", !isKey) { viewModel.setIdGeneratorFormat(com.burrow.app.viewmodel.RandomFormat.SUFFIX) }
        FormatToggleChip("Random key", isKey) { viewModel.setIdGeneratorFormat(com.burrow.app.viewmodel.RandomFormat.KEY) }
    }

    androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
    Text(if (isKey) "Bytes" else "Length", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral700)
    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val lengths = if (isKey) com.burrow.app.data.KEY_BYTE_LENGTHS else ID_SUFFIX_LENGTHS
        lengths.forEach { len ->
            val selected = sheet.length == len
            Text(
                text = len.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) Burrow.Bg else Burrow.Text,
                modifier = Modifier
                    .background(if (selected) Burrow.Accent else Burrow.Neutral100, RoundedCornerShape(50))
                    .clickable { viewModel.setIdGeneratorLength(len) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }

    androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Burrow.Surface, RoundedCornerShape(20.dp))
            .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            sheet.value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = Burrow.Text,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { viewModel.regenerateId() }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = "Regenerate", tint = Burrow.Neutral600)
        }
        IconButton(
            onClick = {
                clipboard.setText(AnnotatedString(sheet.value))
                viewModel.showToast("Copied")
            },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Burrow.Neutral600)
        }
        if (isKey) {
            IconButton(onClick = { shareText(context, sheet.value) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Burrow.Neutral600)
            }
        }
    }

    androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
    Text(
        if (isKey) {
            "Also saved as a new variable in this folder, named with the time it was generated."
        } else {
            "Append this to an object id in your URLs/APIs so it can't be guessed."
        },
        style = MaterialTheme.typography.bodySmall,
        color = Burrow.Neutral600,
    )
}

@Composable
private fun FormatToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) Burrow.Bg else Burrow.Text,
        modifier = Modifier
            .background(if (selected) Burrow.Accent else Burrow.Neutral100, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun GithubTokenToolContent(sheet: Sheet.GithubTokenTool, viewModel: BurrowViewModel) {
    val clipboard = LocalClipboardManager.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Reading from: ${sheet.folderLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = Burrow.Neutral700,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { viewModel.rescanGithubTokenFolder() }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = "Rescan folder", tint = Burrow.Neutral600)
        }
    }
    androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
    Text(
        "Put an APP_ID variable, an INSTALLATION_ID variable, and a .pem file in this folder - open the tool from there.",
        style = MaterialTheme.typography.bodySmall,
        color = Burrow.Neutral600,
    )

    androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
    DetectionStatusRow("APP_ID variable", sheet.appId)
    androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
    DetectionStatusRow("INSTALLATION_ID variable", sheet.installationId)
    androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
    DetectionStatusRow(".pem private key file", sheet.privateKeyFile?.originalFileName)

    androidx.compose.foundation.layout.Spacer(Modifier.height(18.dp))
    Button(
        onClick = { viewModel.generateGithubToken() },
        enabled = !sheet.isGenerating,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Burrow.Accent, contentColor = Burrow.Bg),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (sheet.isGenerating) "Generating…" else "Generate token") }

    val error = sheet.error
    if (error != null) {
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        Text(error, style = MaterialTheme.typography.bodySmall, color = Burrow.Accent700)
    }

    val token = sheet.token
    if (token != null) {
        androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
        Text("Installation token", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral700)
        androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Burrow.Surface, RoundedCornerShape(20.dp))
                .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                token,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Burrow.Text,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(token))
                    viewModel.showToast("Token copied")
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy token", tint = Burrow.Neutral600)
            }
        }
        val expiresAt = sheet.tokenExpiresAt
        if (!expiresAt.isNullOrBlank()) {
            androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
            Text("Expires: $expiresAt", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral600)
        }
    }
}

@Composable
private fun MoveItemPickerContent(sheet: Sheet.MoveItemPicker, viewModel: BurrowViewModel, tree: FolderNode) {
    val node = findNode(tree, sheet.browsePath)
    val crumb = if (sheet.browsePath.isEmpty()) "Warren" else pathNames(tree, sheet.browsePath).joinToString(" / ")
    val isSourceHere = sheet.browsePath == sheet.sourcePath

    Text("Moving \"${sheet.itemName}\"", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral600)
    androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (sheet.browsePath.isNotEmpty()) {
            IconButton(onClick = { viewModel.movePickerGoBack() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Burrow.Neutral600)
            }
        }
        Text(
            crumb,
            style = MaterialTheme.typography.bodyMedium,
            color = Burrow.Text,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        if (node.folders.isEmpty()) {
            Text(
                "No subfolders here.",
                style = MaterialTheme.typography.bodySmall,
                color = Burrow.Neutral600,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
        node.folders.forEach { f ->
            val disabled = sheet.kind == DeleteKind.FOLDER && f.id == sheet.itemId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !disabled) { viewModel.movePickerOpenFolder(f.id) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    f.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (disabled) Burrow.Neutral400 else Burrow.Text,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(">", style = MaterialTheme.typography.bodyMedium, color = Burrow.Neutral400)
            }
        }
    }

    androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
    Button(
        onClick = { viewModel.confirmMoveHere() },
        enabled = !isSourceHere,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Burrow.Accent, contentColor = Burrow.Bg),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (isSourceHere) "Already here" else "Move here") }
}

@Composable
private fun DetectionStatusRow(label: String, detectedValue: String?) {
    val found = !detectedValue.isNullOrBlank()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            if (found) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = if (found) "Found" else "Missing",
            tint = if (found) Burrow.Accent2_700 else Burrow.Neutral400,
            modifier = Modifier.size(18.dp),
        )
        Text(
            if (found) "$label: $detectedValue" else "$label: not found",
            style = MaterialTheme.typography.bodySmall,
            color = if (found) Burrow.Text else Burrow.Neutral600,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}
