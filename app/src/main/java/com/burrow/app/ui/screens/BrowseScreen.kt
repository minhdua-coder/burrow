package com.burrow.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.burrow.app.data.FileItem
import com.burrow.app.data.FileStorage
import com.burrow.app.data.FolderNode
import com.burrow.app.data.LinkItem
import com.burrow.app.data.Variable
import com.burrow.app.data.findNode
import com.burrow.app.data.formatFileSize
import com.burrow.app.data.maskedValue
import com.burrow.app.data.pathNames
import com.burrow.app.ui.components.DragHandle
import com.burrow.app.ui.components.ReorderableColumn
import com.burrow.app.ui.components.RowIconBadge
import com.burrow.app.ui.components.SectionHeader
import com.burrow.app.ui.iconFor
import com.burrow.app.ui.theme.Burrow
import com.burrow.app.ui.theme.folderColor
import com.burrow.app.viewmodel.BurrowViewModel
import com.burrow.app.viewmodel.ListKind
import com.burrow.app.viewmodel.UiState

@Composable
fun BrowseScreen(state: UiState, viewModel: BurrowViewModel) {
    val node = remember(state.tree, state.path) { findNode(state.tree, state.path) }
    val clipboard = LocalClipboardManager.current

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BrowseTopBar(state, viewModel)
            BrowseContent(state, node, viewModel, clipboard)
        }

        if (state.fabOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        viewModel.closeFab()
                    },
            )
        }

        FabCluster(state, viewModel, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

@Composable
private fun BrowseTopBar(state: UiState, viewModel: BurrowViewModel) {
    val canGoBack = state.path.isNotEmpty()
    val title = if (canGoBack) pathNames(state.tree, state.path).joinToString(" / ") else "Warren"

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp, start = 6.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canGoBack) {
            IconButton(onClick = { viewModel.goBack() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Burrow.Text)
            }
        } else {
            Spacer(Modifier.width(36.dp))
        }

        Box(Modifier.weight(1f).padding(start = 4.dp)) {
            if (title.length > 18) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Burrow.Text,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
            } else {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Burrow.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        IconButton(onClick = { viewModel.openIdGenerator() }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Fingerprint, contentDescription = "Generate random ID suffix", tint = Burrow.Text)
        }
        IconButton(onClick = { viewModel.openSettings() }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Burrow.Text)
        }
        IconButton(onClick = { viewModel.openSearch() }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = Burrow.Text)
        }
    }
}

@Composable
private fun BrowseContent(
    state: UiState,
    node: FolderNode,
    viewModel: BurrowViewModel,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
) {
    val hasFolders = node.folders.isNotEmpty()
    val hasLinks = node.links.isNotEmpty()
    val hasVariables = node.variables.isNotEmpty()
    val hasFiles = node.files.isNotEmpty()
    val context = androidx.compose.ui.platform.LocalContext.current

    var pendingDownloadFile by remember { mutableStateOf<FileItem?>(null) }
    val downloadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val file = pendingDownloadFile
        if (uri != null && file != null) {
            val ok = FileStorage.exportTo(context, file.id, uri)
            viewModel.showToast(if (ok) "Downloaded" else "Download failed")
        }
        pendingDownloadFile = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 120.dp)),
    ) {
        if (!hasFolders && !hasLinks && !hasVariables && !hasFiles) {
            EmptyState()
            return@Column
        }

        if (hasFolders) {
            Spacer(Modifier.height(10.dp))
            SectionHeader("Folders")
            Spacer(Modifier.height(2.dp))
            ReorderableColumn(
                items = node.folders,
                listKind = ListKind.FOLDERS,
                dragState = state.drag,
                onDragStateChange = viewModel::setDrag,
                onMove = { from, to -> viewModel.reorderList(ListKind.FOLDERS, from, to) },
            ) { item, _, isDragging, handleMod, rowMod ->
                FolderRow(
                    item = item,
                    isDragging = isDragging,
                    rowModifier = rowMod,
                    dragHandleModifier = handleMod,
                    onOpen = { viewModel.openFolder(item.id) },
                    onKebab = { viewModel.openFolderActions(item) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader("Links")
        Spacer(Modifier.height(2.dp))
        if (hasLinks) {
            ReorderableColumn(
                items = node.links,
                listKind = ListKind.LINKS,
                dragState = state.drag,
                onDragStateChange = viewModel::setDrag,
                onMove = { from, to -> viewModel.reorderList(ListKind.LINKS, from, to) },
            ) { item, _, isDragging, handleMod, rowMod ->
                LinkRow(
                    item = item,
                    isDragging = isDragging,
                    rowModifier = rowMod,
                    dragHandleModifier = handleMod,
                    onOpen = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(item.url)),
                            )
                        }
                    },
                    onCopy = {
                        clipboard.setText(AnnotatedString(item.url))
                        viewModel.showToast("Link copied")
                    },
                    onKebab = { viewModel.openLinkActions(item) },
                )
            }
        } else {
            Text("No links yet", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral500, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader("Variables")
        Spacer(Modifier.height(2.dp))
        if (hasVariables) {
            ReorderableColumn(
                items = node.variables,
                listKind = ListKind.VARIABLES,
                dragState = state.drag,
                onDragStateChange = viewModel::setDrag,
                onMove = { from, to -> viewModel.reorderList(ListKind.VARIABLES, from, to) },
            ) { item, _, isDragging, handleMod, rowMod ->
                VariableRow(
                    item = item,
                    revealed = state.revealed.contains(item.id),
                    isDragging = isDragging,
                    rowModifier = rowMod,
                    dragHandleModifier = handleMod,
                    onToggleReveal = { viewModel.toggleReveal(item.id) },
                    onCopyValue = {
                        clipboard.setText(AnnotatedString(item.value))
                        viewModel.showToast("Value copied")
                    },
                    onKebab = { viewModel.openVariableActions(item) },
                )
            }
        } else {
            Text("No variables yet", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral500, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader("Files")
        Spacer(Modifier.height(2.dp))
        if (hasFiles) {
            ReorderableColumn(
                items = node.files,
                listKind = ListKind.FILES,
                dragState = state.drag,
                onDragStateChange = viewModel::setDrag,
                onMove = { from, to -> viewModel.reorderList(ListKind.FILES, from, to) },
            ) { item, _, isDragging, handleMod, rowMod ->
                FileRow(
                    item = item,
                    isDragging = isDragging,
                    rowModifier = rowMod,
                    dragHandleModifier = handleMod,
                    onDownload = {
                        pendingDownloadFile = item
                        downloadLauncher.launch(item.originalFileName.ifBlank { item.name })
                    },
                    onKebab = { viewModel.openFileActions(item) },
                )
            }
        } else {
            Text("No files yet", style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral500, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 72.dp, bottom = 20.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp).background(Burrow.Accent100, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = Burrow.Accent700)
        }
        Spacer(Modifier.height(10.dp))
        Text("Nothing here yet", style = MaterialTheme.typography.titleSmall, color = Burrow.Text)
        Spacer(Modifier.height(4.dp))
        Text(
            "Add a folder, a link, a variable, or a file with the + button.",
            style = MaterialTheme.typography.bodySmall,
            color = Burrow.Neutral600,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun FolderRow(
    item: FolderNode,
    isDragging: Boolean,
    rowModifier: Modifier,
    dragHandleModifier: Modifier,
    onOpen: () -> Unit,
    onKebab: () -> Unit,
) {
    val c = folderColor(item.color)
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .background(if (isDragging) Burrow.Accent100 else Color.Transparent, RoundedCornerShape(28.dp))
            .padding(vertical = 9.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(dragHandleModifier.size(28.dp), contentAlignment = Alignment.Center) { DragHandle() }
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RowIconBadge(Icons.Filled.Folder, c.bg, c.fg, 34)
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Burrow.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Burrow.Neutral400)
        }
        IconButton(onClick = onKebab, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Burrow.Neutral600)
        }
    }
}

@Composable
private fun LinkRow(
    item: LinkItem,
    isDragging: Boolean,
    rowModifier: Modifier,
    dragHandleModifier: Modifier,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onKebab: () -> Unit,
) {
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .background(if (isDragging) Burrow.Accent100 else Color.Transparent, RoundedCornerShape(28.dp))
            .padding(vertical = 9.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(dragHandleModifier.size(28.dp), contentAlignment = Alignment.Center) { DragHandle() }
        RowIconBadge(iconFor(item.icon), Burrow.Accent100, Burrow.Accent700, 32)
        Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, color = Burrow.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.url, style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral600, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy link", tint = Burrow.Neutral600)
        }
        IconButton(onClick = onKebab, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Burrow.Neutral600)
        }
    }
}

@Composable
private fun VariableRow(
    item: Variable,
    revealed: Boolean,
    isDragging: Boolean,
    rowModifier: Modifier,
    dragHandleModifier: Modifier,
    onToggleReveal: () -> Unit,
    onCopyValue: () -> Unit,
    onKebab: () -> Unit,
) {
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .background(if (isDragging) Burrow.Accent2_100 else Color.Transparent, RoundedCornerShape(28.dp))
            .padding(vertical = 9.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(dragHandleModifier.size(28.dp), contentAlignment = Alignment.Center) { DragHandle() }
        RowIconBadge(iconFor(item.icon), Burrow.Accent2_100, Burrow.Accent2_700, 32)
        Column(Modifier.weight(1f).clickable(onClick = onToggleReveal)) {
            Text(item.key, style = MaterialTheme.typography.bodyMedium, color = Burrow.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (revealed) item.value else maskedValue(item.value),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Burrow.Neutral600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onToggleReveal, modifier = Modifier.size(32.dp)) {
            Icon(
                if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (revealed) "Hide value" else "Reveal value",
                tint = Burrow.Neutral600,
            )
        }
        IconButton(onClick = onCopyValue, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy value", tint = Burrow.Neutral600)
        }
        IconButton(onClick = onKebab, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Burrow.Neutral600)
        }
    }
}

@Composable
private fun FileRow(
    item: FileItem,
    isDragging: Boolean,
    rowModifier: Modifier,
    dragHandleModifier: Modifier,
    onDownload: () -> Unit,
    onKebab: () -> Unit,
) {
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .background(if (isDragging) Burrow.Accent100 else Color.Transparent, RoundedCornerShape(28.dp))
            .padding(vertical = 9.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(dragHandleModifier.size(28.dp), contentAlignment = Alignment.Center) { DragHandle() }
        RowIconBadge(Icons.Filled.InsertDriveFile, Burrow.Accent100, Burrow.Accent700, 32)
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, color = Burrow.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${item.originalFileName} · ${formatFileSize(item.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = Burrow.Neutral600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDownload, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Download, contentDescription = "Download", tint = Burrow.Neutral600)
        }
        IconButton(onClick = onKebab, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Burrow.Neutral600)
        }
    }
}

@Composable
private fun FabCluster(state: UiState, viewModel: BurrowViewModel, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(visible = state.fabOpen) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FabMiniButton("File") { viewModel.openAddForm(ListKind.FILES) }
                FabMiniButton("Variable") { viewModel.openAddForm(ListKind.VARIABLES) }
                FabMiniButton("Link") { viewModel.openAddForm(ListKind.LINKS) }
                FabMiniButton("Folder") { viewModel.openAddForm(ListKind.FOLDERS) }
            }
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Burrow.Accent, CircleShape)
                .clickable { viewModel.toggleFab() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add",
                tint = Color.White,
                modifier = Modifier.rotate(if (state.fabOpen) 45f else 0f),
            )
        }
    }
}

@Composable
private fun FabMiniButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        color = Burrow.Text,
        modifier = Modifier
            .height(42.dp)
            .background(Burrow.Bg, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    )
}
