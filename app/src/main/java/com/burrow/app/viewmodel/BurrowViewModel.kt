package com.burrow.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.burrow.app.data.BurrowRepository
import com.burrow.app.data.FileItem
import com.burrow.app.data.FileStorage
import com.burrow.app.data.FolderNode
import com.burrow.app.data.GithubAppAuth
import com.burrow.app.data.LinkItem
import com.burrow.app.data.Variable
import com.burrow.app.data.buildEnvFile
import com.burrow.app.data.detectGithubAppConfig
import com.burrow.app.data.findNode
import com.burrow.app.data.genId
import com.burrow.app.data.generateRandomBase64Key
import com.burrow.app.data.generateRandomSlug
import com.burrow.app.data.parseEnvFile
import com.burrow.app.data.pathNames
import com.burrow.app.data.updateAtPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BurrowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BurrowRepository(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var toastJobId = 0

    init {
        viewModelScope.launch {
            val tree = repository.treeFlow.first()
            val pin = repository.pinFlow.first()
            _state.update { it.copy(tree = tree, pin = pin) }
        }
    }

    fun currentNode(): FolderNode = findNode(_state.value.tree, _state.value.path)

    // ---- .env export/import (scoped to the current folder's variables) ----
    fun exportEnvContent(): String = buildEnvFile(currentNode().variables)

    fun importEnvContent(content: String) {
        val entries = parseEnvFile(content)
        if (entries.isEmpty()) {
            showToast("No environments found in file")
            return
        }
        mutateNode { node ->
            val added = entries.map { e -> Variable(id = genId("v"), key = e.key, value = e.value, icon = "key", isSecret = false) }
            node.copy(variables = node.variables + added)
        }
        showToast("Imported ${entries.size} environment(s)")
    }

    // ---- navigation ----
    fun openFolder(id: String) = _state.update { it.copy(path = it.path + id) }
    fun goBack() = _state.update { it.copy(path = it.path.dropLast(1)) }
    fun navigateToSearchResult(pathIds: List<String>) =
        _state.update { it.copy(path = pathIds, screen = Screen.BROWSE, searchQuery = "") }

    fun openSearch() = _state.update { it.copy(screen = Screen.SEARCH, searchQuery = "") }
    fun closeSearch() = _state.update { it.copy(screen = Screen.BROWSE) }
    fun setSearchQuery(q: String) = _state.update { it.copy(searchQuery = q) }

    // ---- reveal / toast ----
    fun toggleReveal(id: String) = _state.update {
        val next = it.revealed.toMutableSet()
        if (!next.add(id)) next.remove(id)
        it.copy(revealed = next)
    }

    fun showToast(msg: String) {
        toastJobId++
        val myId = toastJobId
        _state.update { it.copy(toast = msg) }
        viewModelScope.launch {
            delay(1600)
            if (myId == toastJobId) _state.update { it.copy(toast = null) }
        }
    }

    // ---- fab ----
    fun toggleFab() = _state.update { it.copy(fabOpen = !it.fabOpen) }
    fun closeFab() = _state.update { it.copy(fabOpen = false) }

    // ---- mutation core ----
    private fun mutateNode(updater: (FolderNode) -> FolderNode) {
        _state.update { it.copy(tree = updateAtPath(it.tree, it.path, updater)) }
        viewModelScope.launch { repository.saveTree(_state.value.tree) }
    }

    fun reorderList(listKey: ListKind, from: Int, to: Int) {
        if (from == to) return
        mutateNode { node ->
            when (listKey) {
                ListKind.FOLDERS -> {
                    val arr = node.folders.toMutableList()
                    val moved = arr.removeAt(from); arr.add(to, moved)
                    node.copy(folders = arr)
                }
                ListKind.LINKS -> {
                    val arr = node.links.toMutableList()
                    val moved = arr.removeAt(from); arr.add(to, moved)
                    node.copy(links = arr)
                }
                ListKind.VARIABLES -> {
                    val arr = node.variables.toMutableList()
                    val moved = arr.removeAt(from); arr.add(to, moved)
                    node.copy(variables = arr)
                }
                ListKind.FILES -> {
                    val arr = node.files.toMutableList()
                    val moved = arr.removeAt(from); arr.add(to, moved)
                    node.copy(files = arr)
                }
            }
        }
    }

    fun setDrag(drag: DragState) = _state.update { it.copy(drag = drag) }

    private fun addFolder(name: String, color: String) = mutateNode { node ->
        node.copy(folders = node.folders + FolderNode(id = genId("f"), name = name, color = color))
    }
    private fun editFolderMeta(id: String, name: String, color: String) = mutateNode { node ->
        node.copy(folders = node.folders.map { if (it.id == id) it.copy(name = name, color = color) else it })
    }
    private fun addLink(name: String, url: String, icon: String) = mutateNode { node ->
        node.copy(links = node.links + LinkItem(id = genId("l"), name = name, url = url, icon = icon))
    }
    private fun editLink(id: String, name: String, url: String, icon: String) = mutateNode { node ->
        node.copy(links = node.links.map { if (it.id == id) it.copy(name = name, url = url, icon = icon) else it })
    }
    private fun addVariable(key: String, value: String, icon: String, isSecret: Boolean) = mutateNode { node ->
        node.copy(variables = node.variables + Variable(id = genId("v"), key = key, value = value, icon = icon, isSecret = isSecret))
    }
    private fun editVariable(id: String, key: String, value: String, icon: String, isSecret: Boolean) = mutateNode { node ->
        node.copy(variables = node.variables.map { if (it.id == id) it.copy(key = key, value = value, icon = icon, isSecret = isSecret) else it })
    }
    private fun addFile(name: String, uri: Uri, originalFileName: String, mimeType: String, sizeBytes: Long, icon: String) {
        val id = genId("file")
        FileStorage.save(getApplication(), uri, id)
        mutateNode { node -> node.copy(files = node.files + FileItem(id, name, originalFileName, mimeType, sizeBytes, icon)) }
    }
    private fun editFileMeta(id: String, name: String, originalFileName: String, mimeType: String, sizeBytes: Long, icon: String) = mutateNode { node ->
        node.copy(files = node.files.map { if (it.id == id) it.copy(name = name, originalFileName = originalFileName, mimeType = mimeType, sizeBytes = sizeBytes, icon = icon) else it })
    }

    // ---- sheet open/edit ----
    fun openAddForm(kind: ListKind) {
        val sheet = when (kind) {
            ListKind.FOLDERS -> Sheet.FolderForm(FormMode.ADD)
            ListKind.LINKS -> Sheet.LinkForm(FormMode.ADD)
            ListKind.VARIABLES -> Sheet.VariableForm(FormMode.ADD)
            ListKind.FILES -> Sheet.FileForm(FormMode.ADD)
        }
        _state.update { it.copy(sheet = sheet, fabOpen = false) }
    }
    fun openEditFolder(item: FolderNode) =
        _state.update { it.copy(sheet = Sheet.FolderForm(FormMode.EDIT, item.id, item.name, item.color)) }
    fun openEditLink(item: LinkItem) =
        _state.update { it.copy(sheet = Sheet.LinkForm(FormMode.EDIT, item.id, item.name, item.url, item.icon)) }
    fun openEditVariable(item: Variable) =
        _state.update { it.copy(sheet = Sheet.VariableForm(FormMode.EDIT, item.id, item.key, item.value, item.icon, item.isSecret)) }
    fun openEditFile(item: FileItem) = _state.update {
        it.copy(sheet = Sheet.FileForm(FormMode.EDIT, item.id, item.name, null, item.originalFileName, item.mimeType, item.sizeBytes, item.icon))
    }

    fun openFolderActions(item: FolderNode) = _state.update { it.copy(sheet = Sheet.FolderActions(item)) }
    fun openLinkActions(item: LinkItem) = _state.update { it.copy(sheet = Sheet.LinkActions(item)) }
    fun openVariableActions(item: Variable) = _state.update { it.copy(sheet = Sheet.VariableActions(item)) }
    fun openFileActions(item: FileItem) = _state.update { it.copy(sheet = Sheet.FileActions(item)) }
    fun openSettings() = _state.update { it.copy(sheet = Sheet.Settings) }
    fun openChangePin() = _state.update { it.copy(sheet = Sheet.ChangePinForm()) }
    fun closeSheet() = _state.update { it.copy(sheet = null) }

    private fun generateForFormat(format: RandomFormat, length: Int): String =
        if (format == RandomFormat.SUFFIX) generateRandomSlug(length) else generateRandomBase64Key(length)

    fun openIdGenerator() {
        val length = 12
        _state.update { it.copy(sheet = Sheet.IdGenerator(RandomFormat.SUFFIX, length, generateRandomSlug(length))) }
    }
    fun setIdGeneratorFormat(format: RandomFormat) {
        val length = if (format == RandomFormat.SUFFIX) 12 else 32
        val value = generateForFormat(format, length)
        _state.update { it.copy(sheet = Sheet.IdGenerator(format, length, value)) }
        if (format == RandomFormat.KEY) saveGeneratedKeyAsVariable(value)
    }
    fun regenerateId() {
        val s = _state.value.sheet as? Sheet.IdGenerator ?: return
        val value = generateForFormat(s.format, s.length)
        _state.update {
            val cur = it.sheet as? Sheet.IdGenerator ?: return@update it
            it.copy(sheet = cur.copy(value = value))
        }
        if (s.format == RandomFormat.KEY) saveGeneratedKeyAsVariable(value)
    }
    fun setIdGeneratorLength(length: Int) {
        val s = _state.value.sheet as? Sheet.IdGenerator ?: return
        val value = generateForFormat(s.format, length)
        _state.update {
            val cur = it.sheet as? Sheet.IdGenerator ?: return@update it
            it.copy(sheet = cur.copy(length = length, value = value))
        }
        if (s.format == RandomFormat.KEY) saveGeneratedKeyAsVariable(value)
    }

    private fun saveGeneratedKeyAsVariable(value: String) {
        val name = "Key " + java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        mutateNode { node -> node.copy(variables = node.variables + Variable(id = genId("v"), key = name, value = value, icon = "key", isSecret = true)) }
        showToast("Saved as secret \"$name\"")
    }

    fun openEnvImportForm() = _state.update { it.copy(sheet = Sheet.EnvImportForm()) }
    fun updateEnvImportText(v: String) = _state.update {
        val s = it.sheet as? Sheet.EnvImportForm ?: return@update it
        it.copy(sheet = s.copy(text = v))
    }

    // ---- GitHub App installation token (auto-detected from the current folder's
    // APP_ID / INSTALLATION_ID variables and a .pem file) ----
    fun openGithubTokenTool() {
        val node = currentNode()
        val detected = detectGithubAppConfig(node)
        val label = if (_state.value.path.isEmpty()) "Warren" else pathNames(_state.value.tree, _state.value.path).joinToString(" / ")
        _state.update {
            it.copy(
                sheet = Sheet.GithubTokenTool(
                    folderLabel = label,
                    appId = detected.appId,
                    installationId = detected.installationId,
                    privateKeyFile = detected.privateKeyFile,
                ),
            )
        }
    }
    fun rescanGithubTokenFolder() = openGithubTokenTool()
    fun generateGithubToken() {
        val s = _state.value.sheet as? Sheet.GithubTokenTool ?: return
        val appId = s.appId
        val installationId = s.installationId
        val keyFile = s.privateKeyFile
        if (appId.isNullOrBlank() || installationId.isNullOrBlank()) {
            showToast("APP_ID / INSTALLATION_ID variable not found in this folder")
            return
        }
        if (keyFile == null) {
            showToast("No .pem file found in this folder")
            return
        }

        _state.update {
            val cur = it.sheet as? Sheet.GithubTokenTool ?: return@update it
            it.copy(sheet = cur.copy(isGenerating = true, error = null, token = null))
        }
        viewModelScope.launch {
            val pem = FileStorage.readText(getApplication(), keyFile.id)
            if (pem == null) {
                _state.update {
                    val cur = it.sheet as? Sheet.GithubTokenTool ?: return@update it
                    it.copy(sheet = cur.copy(isGenerating = false, error = "Could not read the private key file"))
                }
                return@launch
            }
            val result = GithubAppAuth.fetchInstallationToken(appId, installationId, pem)
            _state.update {
                val cur = it.sheet as? Sheet.GithubTokenTool ?: return@update it
                result.fold(
                    onSuccess = { r -> it.copy(sheet = cur.copy(isGenerating = false, token = r.token, tokenExpiresAt = r.expiresAt)) },
                    onFailure = { e -> it.copy(sheet = cur.copy(isGenerating = false, error = e.message ?: "Failed to generate token")) },
                )
            }
        }
    }

    // ---- sheet field updates ----
    fun updateFolderFormName(v: String) = _state.update {
        val s = it.sheet as? Sheet.FolderForm ?: return@update it
        it.copy(sheet = s.copy(name = v))
    }
    fun updateFolderFormColor(v: String) = _state.update {
        val s = it.sheet as? Sheet.FolderForm ?: return@update it
        it.copy(sheet = s.copy(color = v))
    }
    fun updateLinkFormName(v: String) = _state.update {
        val s = it.sheet as? Sheet.LinkForm ?: return@update it
        it.copy(sheet = s.copy(name = v))
    }
    fun updateLinkFormUrl(v: String) = _state.update {
        val s = it.sheet as? Sheet.LinkForm ?: return@update it
        it.copy(sheet = s.copy(url = v))
    }
    fun updateLinkFormIcon(v: String) = _state.update {
        val s = it.sheet as? Sheet.LinkForm ?: return@update it
        it.copy(sheet = s.copy(icon = v))
    }
    fun updateVariableFormKey(v: String) = _state.update {
        val s = it.sheet as? Sheet.VariableForm ?: return@update it
        it.copy(sheet = s.copy(key = v))
    }
    fun updateVariableFormValue(v: String) = _state.update {
        val s = it.sheet as? Sheet.VariableForm ?: return@update it
        it.copy(sheet = s.copy(value = v))
    }
    fun updateVariableFormIcon(v: String) = _state.update {
        val s = it.sheet as? Sheet.VariableForm ?: return@update it
        it.copy(sheet = s.copy(icon = v))
    }
    fun updateVariableFormIsSecret(v: Boolean) = _state.update {
        val s = it.sheet as? Sheet.VariableForm ?: return@update it
        it.copy(sheet = s.copy(isSecret = v))
    }
    fun updateFileFormName(v: String) = _state.update {
        val s = it.sheet as? Sheet.FileForm ?: return@update it
        it.copy(sheet = s.copy(name = v))
    }
    fun updateFileFormIcon(v: String) = _state.update {
        val s = it.sheet as? Sheet.FileForm ?: return@update it
        it.copy(sheet = s.copy(icon = v))
    }
    fun updateFileFormPicked(uri: Uri, originalFileName: String, mimeType: String, sizeBytes: Long) = _state.update {
        val s = it.sheet as? Sheet.FileForm ?: return@update it
        val name = if (s.name.isBlank()) originalFileName else s.name
        it.copy(sheet = s.copy(name = name, pickedUri = uri, originalFileName = originalFileName, mimeType = mimeType, sizeBytes = sizeBytes))
    }
    fun updatePinCurrent(v: String) = _state.update {
        val s = it.sheet as? Sheet.ChangePinForm ?: return@update it
        it.copy(sheet = s.copy(current = v.filter { c -> c.isDigit() }.take(4)))
    }
    fun updatePinNext(v: String) = _state.update {
        val s = it.sheet as? Sheet.ChangePinForm ?: return@update it
        it.copy(sheet = s.copy(next = v.filter { c -> c.isDigit() }.take(4)))
    }
    fun updatePinConfirm(v: String) = _state.update {
        val s = it.sheet as? Sheet.ChangePinForm ?: return@update it
        it.copy(sheet = s.copy(confirm = v.filter { c -> c.isDigit() }.take(4)))
    }

    fun saveForm() {
        when (val sheet = _state.value.sheet) {
            is Sheet.FolderForm -> {
                val name = sheet.name.trim()
                if (name.isEmpty()) return
                if (sheet.mode == FormMode.ADD) addFolder(name, sheet.color) else editFolderMeta(sheet.editId!!, name, sheet.color)
            }
            is Sheet.LinkForm -> {
                val name = sheet.name.trim(); val url = sheet.url.trim()
                if (name.isEmpty() || url.isEmpty()) return
                if (sheet.mode == FormMode.ADD) addLink(name, url, sheet.icon) else editLink(sheet.editId!!, name, url, sheet.icon)
            }
            is Sheet.VariableForm -> {
                val key = sheet.key.trim()
                if (key.isEmpty()) return
                if (sheet.mode == FormMode.ADD) {
                    addVariable(key, sheet.value, sheet.icon, sheet.isSecret)
                } else {
                    editVariable(sheet.editId!!, key, sheet.value, sheet.icon, sheet.isSecret)
                }
            }
            is Sheet.ChangePinForm -> {
                val cur = sheet.current.trim(); val next = sheet.next.trim(); val conf = sheet.confirm.trim()
                if (cur != _state.value.pin) { showToast("Current PIN incorrect"); return }
                if (next.length != 4) { showToast("PIN must be 4 digits"); return }
                if (next != conf) { showToast("PINs don't match"); return }
                _state.update { it.copy(pin = next) }
                viewModelScope.launch { repository.savePin(next) }
                showToast("PIN changed")
            }
            is Sheet.EnvImportForm -> {
                importEnvContent(sheet.text)
            }
            is Sheet.FileForm -> {
                val name = sheet.name.trim()
                if (name.isEmpty()) return
                if (sheet.mode == FormMode.ADD) {
                    val uri = sheet.pickedUri
                    if (uri == null) { showToast("Choose a file first"); return }
                    addFile(name, uri, sheet.originalFileName ?: name, sheet.mimeType, sheet.sizeBytes, sheet.icon)
                } else {
                    val editId = sheet.editId!!
                    sheet.pickedUri?.let { FileStorage.save(getApplication(), it, editId) }
                    editFileMeta(editId, name, sheet.originalFileName ?: name, sheet.mimeType, sheet.sizeBytes, sheet.icon)
                }
            }
            else -> {}
        }
        _state.update { it.copy(sheet = null) }
    }

    // ---- delete ----
    fun requestDeleteFolder(item: FolderNode) = _state.update { it.copy(confirmDelete = ConfirmDeleteState(DeleteKind.FOLDER, item.id, item.name), sheet = null) }
    fun requestDeleteLink(item: LinkItem) = _state.update { it.copy(confirmDelete = ConfirmDeleteState(DeleteKind.LINK, item.id, item.name), sheet = null) }
    fun requestDeleteVariable(item: Variable) = _state.update { it.copy(confirmDelete = ConfirmDeleteState(DeleteKind.VARIABLE, item.id, item.key), sheet = null) }
    fun requestDeleteFile(item: FileItem) = _state.update { it.copy(confirmDelete = ConfirmDeleteState(DeleteKind.FILE, item.id, item.name), sheet = null) }
    fun cancelDelete() = _state.update { it.copy(confirmDelete = null) }
    fun confirmDeleteNow() {
        val cd = _state.value.confirmDelete ?: return
        when (cd.kind) {
            DeleteKind.FOLDER -> mutateNode { node -> node.copy(folders = node.folders.filter { it.id != cd.id }) }
            DeleteKind.LINK -> mutateNode { node -> node.copy(links = node.links.filter { it.id != cd.id }) }
            DeleteKind.VARIABLE -> mutateNode { node -> node.copy(variables = node.variables.filter { it.id != cd.id }) }
            DeleteKind.FILE -> {
                mutateNode { node -> node.copy(files = node.files.filter { it.id != cd.id }) }
                FileStorage.delete(getApplication(), cd.id)
            }
        }
        _state.update { it.copy(confirmDelete = null) }
    }

    // ---- PIN lock ----
    fun pressPin(d: String) {
        val s = _state.value
        if (s.pinInput.length >= 4) return
        val next = s.pinInput + d
        _state.update { it.copy(pinInput = next, pinError = false) }
        if (next.length == 4) {
            viewModelScope.launch {
                delay(150)
                if (next == _state.value.pin) {
                    _state.update { it.copy(locked = false, pinInput = "") }
                } else {
                    _state.update { it.copy(pinError = true, pinInput = "") }
                }
            }
        }
    }
    fun backspacePin() = _state.update { it.copy(pinInput = it.pinInput.dropLast(1), pinError = false) }
}
