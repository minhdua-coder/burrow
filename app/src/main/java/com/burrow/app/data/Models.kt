package com.burrow.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Variable(
    val id: String,
    val key: String,
    val value: String,
    val icon: String = "key",
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
    id = "root", name = "Burrow", color = "sand",
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
                        Variable(id = "v1", key = "SSH VPS", value = "ssh root@160.22.161.35 -p 22", icon = "terminal"),
                        Variable(id = "v2", key = "DB Connection String", value = "postgresql://kaspersky_user:S3cr3tPass@160.22.161.35:5432/kaspersky_db", icon = "database"),
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
    val breadcrumb = if (pathNames.isNotEmpty()) pathNames.joinToString(" / ") else "Burrow"

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
