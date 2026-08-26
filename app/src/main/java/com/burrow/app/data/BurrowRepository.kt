package com.burrow.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "burrow")

class BurrowRepository(private val context: Context) {

    private object Keys {
        val TREE = stringPreferencesKey("burrow_tree_v1")
        val PIN = stringPreferencesKey("burrow_pin_v1")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val treeFlow: Flow<FolderNode> = context.dataStore.data.map { prefs ->
        prefs[Keys.TREE]?.let { raw ->
            runCatching { json.decodeFromString<FolderNode>(raw) }.getOrNull()
        } ?: seedTree()
    }

    val pinFlow: Flow<String> = context.dataStore.data.map { prefs -> prefs[Keys.PIN] ?: "1234" }

    suspend fun saveTree(tree: FolderNode) {
        context.dataStore.edit { it[Keys.TREE] = json.encodeToString(tree) }
    }

    suspend fun savePin(pin: String) {
        context.dataStore.edit { it[Keys.PIN] = pin }
    }
}
