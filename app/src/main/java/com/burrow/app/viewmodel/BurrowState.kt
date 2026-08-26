package com.burrow.app.viewmodel

import com.burrow.app.data.FolderNode
import com.burrow.app.data.LinkItem
import com.burrow.app.data.Variable
import com.burrow.app.data.seedTree

enum class Screen { BROWSE, SEARCH }
enum class FormMode { ADD, EDIT }
enum class ListKind { FOLDERS, LINKS, VARIABLES }
enum class DeleteKind { FOLDER, LINK, VARIABLE }

sealed interface Sheet {
    data class FolderForm(val mode: FormMode, val editId: String? = null, val name: String = "", val color: String = "sand") : Sheet
    data class LinkForm(val mode: FormMode, val editId: String? = null, val name: String = "", val url: String = "", val icon: String = "link") : Sheet
    data class VariableForm(val mode: FormMode, val editId: String? = null, val key: String = "", val value: String = "", val icon: String = "key") : Sheet
    data class FolderActions(val item: FolderNode) : Sheet
    data class LinkActions(val item: LinkItem) : Sheet
    data class VariableActions(val item: Variable) : Sheet
    data object Settings : Sheet
    data class ChangePinForm(val current: String = "", val next: String = "", val confirm: String = "") : Sheet
}

data class ConfirmDeleteState(val kind: DeleteKind, val id: String, val name: String)

data class DragState(val listKey: ListKind? = null, val index: Int = -1, val offset: Float = 0f)

data class UiState(
    val tree: FolderNode = seedTree(),
    val path: List<String> = emptyList(),
    val screen: Screen = Screen.BROWSE,
    val searchQuery: String = "",
    val fabOpen: Boolean = false,
    val sheet: Sheet? = null,
    val confirmDelete: ConfirmDeleteState? = null,
    val toast: String? = null,
    val revealed: Set<String> = emptySet(),
    val drag: DragState = DragState(),
    val locked: Boolean = true,
    val pinInput: String = "",
    val pinError: Boolean = false,
    val pin: String = "1234",
)
