package com.burrow.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Variable(
    val id: String,
    val key: String,
    val value: String,
    val icon: String = "key",
    // Defaults true so pre-existing data (saved before this field existed) keeps its
    // masked display; new items explicitly pass false from the add form.
    val isSecret: Boolean = true,
)

@Serializable
data class LinkItem(
    val id: String,
    val name: String,
    val url: String,
    val icon: String = "link",
)

@Serializable
data class FileItem(
    val id: String,
    val name: String,
    val originalFileName: String,
    val mimeType: String = "application/octet-stream",
    val sizeBytes: Long = 0,
    val icon: String = "file",
)

@Serializable
data class FolderNode(
    val id: String,
    val name: String,
    val color: String = "sand",
    val folders: List<FolderNode> = emptyList(),
    val links: List<LinkItem> = emptyList(),
    val variables: List<Variable> = emptyList(),
    val files: List<FileItem> = emptyList(),
)

fun genId(prefix: String): String = prefix + "_" + UUID.randomUUID().toString().take(8)

fun seedTree(): FolderNode = FolderNode(
    id = "root", name = "Warren", color = "sand",
    folders = listOf(
        FolderNode(
            id = "kaspersky", name = "Kaspersky", color = "terracotta",
            folders = listOf(
                FolderNode(
                    id = "develop", name = "Develop", color = "sage",
                    links = listOf(
                        LinkItem(id = "l1", name = "Swagger API", url = "http://160.22.161.35/kaperky/swagger/index.html", icon = "globe"),
                    ),
                    variables = listOf(
                        Variable(id = "v1", key = "SSH VPS", value = "ssh root@160.22.161.35 -p 22", icon = "terminal", isSecret = true),
                        Variable(id = "v2", key = "DB Connection String", value = "postgresql://kaspersky_user:S3cr3tPass@160.22.161.35:5432/kaspersky_db", icon = "database", isSecret = true),
                    ),
                ),
            ),
        ),
        FolderNode(id = "personal", name = "Personal", color = "sand"),
    ),
)

fun findNode(node: FolderNode, path: List<String>): FolderNode {
    if (path.isEmpty()) return node
    val child = node.folders.find { it.id == path.first() } ?: return node
    return findNode(child, path.drop(1))
}

fun updateAtPath(node: FolderNode, path: List<String>, updater: (FolderNode) -> FolderNode): FolderNode {
    if (path.isEmpty()) return updater(node)
    val id = path.first()
    return node.copy(folders = node.folders.map { if (it.id == id) updateAtPath(it, path.drop(1), updater) else it })
}

/** Moves a folder to a different parent. Returns the tree unchanged if destPath is the folder itself or inside it. */
fun moveFolder(tree: FolderNode, sourcePath: List<String>, folderId: String, destPath: List<String>): FolderNode {
    val forbiddenPrefix = sourcePath + folderId
    if (destPath.size >= forbiddenPrefix.size && destPath.subList(0, forbiddenPrefix.size) == forbiddenPrefix) return tree
    val item = findNode(tree, sourcePath).folders.find { it.id == folderId } ?: return tree
    val removed = updateAtPath(tree, sourcePath) { it.copy(folders = it.folders.filter { f -> f.id != folderId }) }
    return updateAtPath(removed, destPath) { it.copy(folders = it.folders + item) }
}

fun moveLink(tree: FolderNode, sourcePath: List<String>, linkId: String, destPath: List<String>): FolderNode {
    val item = findNode(tree, sourcePath).links.find { it.id == linkId } ?: return tree
    val removed = updateAtPath(tree, sourcePath) { it.copy(links = it.links.filter { l -> l.id != linkId }) }
    return updateAtPath(removed, destPath) { it.copy(links = it.links + item) }
}

fun moveVariable(tree: FolderNode, sourcePath: List<String>, variableId: String, destPath: List<String>): FolderNode {
    val item = findNode(tree, sourcePath).variables.find { it.id == variableId } ?: return tree
    val removed = updateAtPath(tree, sourcePath) { it.copy(variables = it.variables.filter { v -> v.id != variableId }) }
    return updateAtPath(removed, destPath) { it.copy(variables = it.variables + item) }
}

fun moveFile(tree: FolderNode, sourcePath: List<String>, fileId: String, destPath: List<String>): FolderNode {
    val item = findNode(tree, sourcePath).files.find { it.id == fileId } ?: return tree
    val removed = updateAtPath(tree, sourcePath) { it.copy(files = it.files.filter { f -> f.id != fileId }) }
    return updateAtPath(removed, destPath) { it.copy(files = it.files + item) }
}

/** Deep-copies a folder and everything nested inside it, minting fresh ids throughout. Old->new file ids are recorded in [fileIdMap] so callers can copy the underlying file bytes. */
private fun cloneFolderTree(node: FolderNode, fileIdMap: MutableMap<String, String>): FolderNode = node.copy(
    id = genId("f"),
    folders = node.folders.map { cloneFolderTree(it, fileIdMap) },
    links = node.links.map { it.copy(id = genId("l")) },
    variables = node.variables.map { it.copy(id = genId("v")) },
    files = node.files.map { f -> val newId = genId("file"); fileIdMap[f.id] = newId; f.copy(id = newId) },
)

/** Clones a folder (recursively, with fresh ids) into a different folder. Returns the new tree plus a map of old->new file ids for copying file bytes. */
fun cloneFolder(tree: FolderNode, sourcePath: List<String>, folderId: String, destPath: List<String>): Pair<FolderNode, Map<String, String>> {
    val item = findNode(tree, sourcePath).folders.find { it.id == folderId } ?: return tree to emptyMap()
    val fileIdMap = mutableMapOf<String, String>()
    val cloned = cloneFolderTree(item, fileIdMap)
    val newTree = updateAtPath(tree, destPath) { it.copy(folders = it.folders + cloned) }
    return newTree to fileIdMap
}

fun cloneLink(tree: FolderNode, sourcePath: List<String>, linkId: String, destPath: List<String>): FolderNode {
    val item = findNode(tree, sourcePath).links.find { it.id == linkId } ?: return tree
    return updateAtPath(tree, destPath) { it.copy(links = it.links + item.copy(id = genId("l"))) }
}

fun cloneVariable(tree: FolderNode, sourcePath: List<String>, variableId: String, destPath: List<String>): FolderNode {
    val item = findNode(tree, sourcePath).variables.find { it.id == variableId } ?: return tree
    return updateAtPath(tree, destPath) { it.copy(variables = it.variables + item.copy(id = genId("v"))) }
}

/** Clones a file into a different folder. Returns the new tree plus the new file's id so callers can copy the underlying file bytes. */
fun cloneFile(tree: FolderNode, sourcePath: List<String>, fileId: String, destPath: List<String>): Pair<FolderNode, String>? {
    val item = findNode(tree, sourcePath).files.find { it.id == fileId } ?: return null
    val newId = genId("file")
    val newTree = updateAtPath(tree, destPath) { it.copy(files = it.files + item.copy(id = newId)) }
    return newTree to newId
}

fun pathNames(tree: FolderNode, path: List<String>): List<String> {
    val names = mutableListOf<String>()
    var node = tree
    for (id in path) {
        val child = node.folders.find { it.id == id } ?: break
        names.add(child.name)
        node = child
    }
    return names
}

enum class ResultType { LINK, VARIABLE, FILE }

data class SearchResult(
    val type: ResultType,
    val link: LinkItem? = null,
    val variable: Variable? = null,
    val file: FileItem? = null,
    val pathIds: List<String>,
    val breadcrumb: String,
)

fun flattenSearch(
    tree: FolderNode,
    query: String,
    pathIds: List<String> = emptyList(),
    pathNames: List<String> = emptyList(),
): List<SearchResult> {
    val q = query.trim().lowercase()
    val results = mutableListOf<SearchResult>()
    val breadcrumb = if (pathNames.isNotEmpty()) pathNames.joinToString(" / ") else "Warren"

    tree.links.forEach { l ->
        if (l.name.lowercase().contains(q) || l.url.lowercase().contains(q)) {
            results.add(SearchResult(ResultType.LINK, link = l, pathIds = pathIds, breadcrumb = breadcrumb))
        }
    }
    tree.variables.forEach { v ->
        if (v.key.lowercase().contains(q) || v.value.lowercase().contains(q)) {
            results.add(SearchResult(ResultType.VARIABLE, variable = v, pathIds = pathIds, breadcrumb = breadcrumb))
        }
    }
    tree.files.forEach { f ->
        if (f.name.lowercase().contains(q) || f.originalFileName.lowercase().contains(q)) {
            results.add(SearchResult(ResultType.FILE, file = f, pathIds = pathIds, breadcrumb = breadcrumb))
        }
    }
    tree.folders.forEach { f ->
        results.addAll(flattenSearch(f, query, pathIds + f.id, pathNames + f.name))
    }
    return results
}

fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

fun maskedValue(value: String): String = "•".repeat(minOf(value.ifEmpty { "        " }.length, 24))

data class GithubAppConfigStatus(
    val appId: String?,
    val installationId: String?,
    val privateKeyFile: FileItem?,
)

private fun normalizeKey(s: String): String = s.lowercase().replace(Regex("[^a-z0-9]"), "")

/** Looks in one folder's own variables/files for an APP_ID variable, an INSTALLATION_ID variable, and a .pem file. */
fun detectGithubAppConfig(node: FolderNode): GithubAppConfigStatus {
    val appId = node.variables.find { normalizeKey(it.key) == "appid" }?.value
    val installationId = node.variables.find { normalizeKey(it.key) == "installationid" }?.value
    val pemFile = node.files.find { it.originalFileName.endsWith(".pem", ignoreCase = true) }
    return GithubAppConfigStatus(appId, installationId, pemFile)
}
