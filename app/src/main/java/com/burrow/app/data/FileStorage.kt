package com.burrow.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/** Item file bytes live in internal storage (private to the app), keyed by the FileItem's id. */
object FileStorage {

    private fun dir(context: Context): File = File(context.filesDir, "burrow_files").apply { mkdirs() }

    private fun storagePath(context: Context, id: String): File = File(dir(context), id)

    fun save(context: Context, sourceUri: Uri, id: String): Boolean = runCatching {
        context.contentResolver.openInputStream(sourceUri)!!.use { input ->
            storagePath(context, id).outputStream().use { output -> input.copyTo(output) }
        }
    }.isSuccess

    fun exportTo(context: Context, id: String, destUri: Uri): Boolean = runCatching {
        storagePath(context, id).inputStream().use { input ->
            context.contentResolver.openOutputStream(destUri)!!.use { output -> input.copyTo(output) }
        }
    }.isSuccess

    fun delete(context: Context, id: String) {
        storagePath(context, id).delete()
    }

    fun exists(context: Context, id: String): Boolean = storagePath(context, id).exists()

    fun readText(context: Context, id: String): String? = runCatching {
        storagePath(context, id).readText()
    }.getOrNull()

    fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    }.getOrNull()

    fun querySize(context: Context, uri: Uri): Long = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
        } ?: 0L
    }.getOrDefault(0L)
}
