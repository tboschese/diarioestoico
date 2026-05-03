package com.diarioestoico.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

class FavoritesRepository(private val context: Context) {

    private val gson = Gson()

    private val keyFavoriteEntries = stringSetPreferencesKey("favorite_entries")
    private val keySavedPhrases = stringPreferencesKey("saved_phrases")

    // ── Favorite entries ────────────────────────────��───────────────────────

    val favoriteEntryIds: Flow<Set<String>> = context.dataStore.data
        .map { it[keyFavoriteEntries] ?: emptySet() }

    suspend fun toggleFavoriteEntry(day: Int, month: Int) {
        val id = entryId(day, month)
        context.dataStore.edit { prefs ->
            val current = prefs[keyFavoriteEntries]?.toMutableSet() ?: mutableSetOf()
            if (id in current) current.remove(id) else current.add(id)
            prefs[keyFavoriteEntries] = current
        }
    }

    fun isFavoriteEntry(day: Int, month: Int, ids: Set<String>) =
        entryId(day, month) in ids

    private fun entryId(day: Int, month: Int) = "$day-$month"

    // ── Saved phrases ────────────────────────────────────────────────────────

    val savedPhrases: Flow<List<SavedPhrase>> = context.dataStore.data
        .map { prefs ->
            val json = prefs[keySavedPhrases] ?: return@map emptyList()
            val type = object : TypeToken<List<SavedPhrase>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }

    suspend fun savePhrase(phrase: SavedPhrase) {
        context.dataStore.edit { prefs ->
            val current = readPhrases(prefs).toMutableList()
            current.add(0, phrase)
            prefs[keySavedPhrases] = gson.toJson(current)
        }
    }

    suspend fun deletePhrase(phrase: SavedPhrase) {
        context.dataStore.edit { prefs ->
            val current = readPhrases(prefs).toMutableList()
            current.removeAll { it.savedAt == phrase.savedAt && it.text == phrase.text }
            prefs[keySavedPhrases] = gson.toJson(current)
        }
    }

    private fun readPhrases(prefs: Preferences): List<SavedPhrase> {
        val json = prefs[keySavedPhrases] ?: return emptyList()
        val type = object : TypeToken<List<SavedPhrase>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
