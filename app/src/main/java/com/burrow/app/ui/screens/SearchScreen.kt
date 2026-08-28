package com.burrow.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.burrow.app.data.FileItem
import com.burrow.app.data.FileStorage
import com.burrow.app.data.ResultType
import com.burrow.app.data.flattenSearch
import com.burrow.app.data.formatFileSize
import com.burrow.app.data.maskedValue
import com.burrow.app.ui.components.Tag
import com.burrow.app.ui.theme.Burrow
import com.burrow.app.viewmodel.BurrowViewModel
import com.burrow.app.viewmodel.UiState

@Composable
fun SearchScreen(state: UiState, viewModel: BurrowViewModel) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val query = state.searchQuery
    val hasQuery = query.trim().isNotEmpty()
    val results = remember(state.tree, query) { if (hasQuery) flattenSearch(state.tree, query) else emptyList() }
    val focusRequester = remember { FocusRequester() }

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

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconButton(onClick = { viewModel.closeSearch() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search", tint = Burrow.Text)
            }
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search links, environments, and files") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Burrow.Surface,
                    unfocusedContainerColor = Burrow.Surface,
                    focusedBorderColor = Burrow.Accent,
                    unfocusedBorderColor = Burrow.Divider,
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
            )
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        when {
            !hasQuery -> Box(Modifier.fillMaxSize()) {
                Text(
                    "Type to search across every folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Burrow.Neutral600,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp, start = 24.dp, end = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            results.isEmpty() -> Box(Modifier.fillMaxSize()) {
                Text(
                    "No results for \"$query\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = Burrow.Neutral600,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp, start = 24.dp, end = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 24.dp)),
            ) {
                results.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when (r.type) {
                            ResultType.LINK -> Tag("LINK", Burrow.Accent100, Burrow.Accent800)
                            ResultType.VARIABLE -> {
                                val label = if (r.variable!!.isSecret) "SECRET" else "ENV"
                                Tag(label, Burrow.Accent2_100, Burrow.Accent2_800)
                            }
                            ResultType.FILE -> Tag("FILE", Burrow.Accent100, Burrow.Accent800)
                        }
                        Column(
                            Modifier.weight(1f).clickable {
                                when (r.type) {
                                    ResultType.LINK -> {
                                        val link = r.link!!
                                        runCatching {
                                            context.startActivity(
                                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link.url)),
                                            )
                                        }
                                    }
                                    ResultType.VARIABLE -> {
                                        val v = r.variable!!
                                        if (v.isSecret) viewModel.toggleReveal(v.id)
                                    }
                                    ResultType.FILE -> {
                                        val file = r.file!!
                                        pendingDownloadFile = file
                                        downloadLauncher.launch(file.originalFileName.ifBlank { file.name })
                                    }
                                }
                            },
                        ) {
                            val primary = when (r.type) {
                                ResultType.LINK -> r.link!!.name
                                ResultType.VARIABLE -> r.variable!!.key
                                ResultType.FILE -> r.file!!.name
                            }
                            val secondary = when (r.type) {
                                ResultType.LINK -> r.link!!.url
                                ResultType.VARIABLE -> {
                                    val v = r.variable!!
                                    if (!v.isSecret || state.revealed.contains(v.id)) v.value else maskedValue(v.value)
                                }
                                ResultType.FILE -> {
                                    val f = r.file!!
                                    "${f.originalFileName} · ${formatFileSize(f.sizeBytes)}"
                                }
                            }
                            Text(primary, style = MaterialTheme.typography.bodyMedium, color = Burrow.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(secondary, style = MaterialTheme.typography.bodySmall, color = Burrow.Neutral600, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "in ${r.breadcrumb}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Burrow.Accent700,
                                modifier = Modifier.clickable { viewModel.navigateToSearchResult(r.pathIds) },
                            )
                        }
                        if (r.type == ResultType.FILE) {
                            IconButton(
                                onClick = {
                                    val file = r.file!!
                                    pendingDownloadFile = file
                                    downloadLauncher.launch(file.originalFileName.ifBlank { file.name })
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = "Download", tint = Burrow.Neutral600)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val text = if (r.type == ResultType.LINK) r.link!!.url else r.variable!!.value
                                    clipboard.setText(AnnotatedString(text))
                                    viewModel.showToast(if (r.type == ResultType.LINK) "Link copied" else "Value copied")
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Burrow.Neutral600)
                            }
                            IconButton(
                                onClick = {
                                    val text = if (r.type == ResultType.LINK) {
                                        val link = r.link!!
                                        "${link.name}\n${link.url}"
                                    } else {
                                        val v = r.variable!!
                                        "${v.key}: ${v.value}"
                                    }
                                    shareText(context, text)
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Burrow.Neutral600)
                            }
                        }
                    }
                }
            }
        }
    }
}
