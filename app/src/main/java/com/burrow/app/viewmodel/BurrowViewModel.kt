package com.burrow.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.burrow.app.data.BurrowRepository
import com.burrow.app.data.FolderNode
import com.burrow.app.data.LinkItem
import com.burrow.app.data.Variable
import com.burrow.app.data.buildEnvFile
import com.burrow.app.data.findNode
import com.burrow.app.data.genId
import com.burrow.app.data.generateRandomSlug
import com.burrow.app.data.parseEnvFile
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
            showToast("No variables found in file")
            return
        }
        mutateNode { node ->
            val added = entries.map { e -> Variable(id = genId("v"), key = e.key, value = e.value, icon = "key") }
            node.copy(variables = node.variables + added)
        }
        showToast("Imported ${entries.size} variable(s)")
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
    private fun addVariable(key: String, value: String, icon: String) = mutateNode { node ->
        node.copy(variables = node.variables + Variable(id = genId("v"), key = key, value = value, icon = icon))
    }
    private fun editVariable(id: String, key: String, value: String, icon: String) = mutateNode { node ->
        node.copy(variables = node.variables.map { if (it.id == id) it.copy(key = key, value = value, icon = icon) else it })
    }

    // ---- sheet open/edit ----
    fun openAddForm(kind: ListKind) {
        val sheet = when (kind) {
            ListKind.FOLDERS -> Sheet.FolderForm(FormMode.ADD)
            ListKind.LINKS -> Sheet.LinkForm(FormMode.ADD)
            ListKind.VARIABLES -> Sheet.VariableForm(FormMode.ADD)
        }
        _state.update { it.copy(sheet = sheet, fabOpen = false) }
    }
    fun openEditFolder(item: FolderNode) =
        _state.update { it.copy(sheet = Sheet.FolderForm(FormMode.EDIT, item.id, item.name, item.color)) }
    fun openEditLink(item: LinkItem) =
        _state.update { it.copy(sheet = Sheet.LinkForm(FormMode.EDIT, item.id, item.name, item.url, item.icon)) }
    fun openEditVariable(item: Variable) =
        _state.update { it.copy(sheet = Sheet.VariableForm(FormMode.EDIT, item.id, item.key, item.value, item.icon)) }

    fun openFolderActions(item: FolderNode) = _state.update { it.copy(sheet = Sheet.FolderActions(item)) }
    fun openLinkActions(item: LinkItem) = _state.update { it.copy(sheet = Sheet.LinkActions(item)) }
    fun openVariableActions(item: Variable) = _state.update { it.copy(sheet = Sheet.VariableActions(item)) }
    fun openSettings() = _state.update { it.copy(sheet = Sheet.Settings) }
    fun openChangePin() = _state.update { it.copy(sheet = Sheet.ChangePinForm()) }
    fun closeSheet() = _state.update { it.copy(sheet = null) }

    fun openIdGenerator() {
        val length = 12
        _state.update { it.copy(sheet = Sheet.IdGenerator(length, generateRandomSlug(length))) }
    }
    fun regenerateId() = _state.update {
        val s = it.sheet as? Sheet.IdGenerator ?: return@update it
        it.copy(sheet = s.copy(value = generateRandomSlug(s.length)))
    }
    fun setIdGeneratorLength(length: Int) = _state.update {
        val s = it.sheet as? Sheet.IdGenerator ?: return@update it
        it.copy(sheet = s.copy(length = length, value = generateRandomSlug(length)))
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
                if (sheet.mode == FormMode.ADD) addVariable(key, sheet.value, sheet.icon) else editVariable(sheet.editId!!, key, sheet.value, sheet.icon)
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
            else -> {}
        }
        _state.update { it.copy(sheet = null) }
    }

    // ---- delete ----
    fun requestDeleteFolder(item: FolderNode) = _state.update { it.copy(confirmDelete = ConfirmDeleteState(DeleteKind.FOLDER, item.id, item.name), sheet = null) }
    fun requestDeleteLink(item: LinkItem) = _state.update { it.copy(confirmDelete = ConfirmDeleteState(DeleteKind.LINK, item.id, item.name), sheet = null) }
    fun requestDeleteVariable(item: Variable) = _state.update { it.copy(confirmDelete = ConfirmDeleteState(DeleteKind.VARIABLE, item.id, item.key), sheet = null) }
    fun cancelDelete() = _state.update { it.copy(confirmDelete = null) }
    fun confirmDeleteNow() {
        val cd = _state.value.confirmDelete ?: return
        when (cd.kind) {
            DeleteKind.FOLDER -> mutateNode { node -> node.copy(folders = node.folders.filter { it.id != cd.id }) }
            DeleteKind.LINK -> mutateNode { node -> node.copy(links = node.links.filter { it.id != cd.id }) }
            DeleteKind.VARIABLE -> mutateNode { node -> node.copy(variables = node.variables.filter { it.id != cd.id }) }
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
