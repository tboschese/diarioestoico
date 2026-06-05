package com.diarioestoico.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemePreferences(private val context: Context) {

    private val keyMode = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        when (prefs[keyMode]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name  -> ThemeMode.DARK
            else                 -> ThemeMode.SYSTEM
        }
    }

    suspend fun save(mode: ThemeMode) {
        context.themeDataStore.edit { it[keyMode] = mode.name }
    }
}
