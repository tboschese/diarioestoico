package com.diarioestoico.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notifDataStore by preferencesDataStore(name = "notification_prefs")

data class NotificationSettings(
    val enabled: Boolean = false,
    val hour: Int = 8,
    val minute: Int = 0
)

class NotificationPreferences(private val context: Context) {

    private val keyEnabled = booleanPreferencesKey("notif_enabled")
    private val keyHour    = intPreferencesKey("notif_hour")
    private val keyMinute  = intPreferencesKey("notif_minute")

    val settings: Flow<NotificationSettings> = context.notifDataStore.data.map { prefs ->
        NotificationSettings(
            enabled = prefs[keyEnabled] ?: false,
            hour    = prefs[keyHour]    ?: 8,
            minute  = prefs[keyMinute]  ?: 0
        )
    }

    suspend fun save(enabled: Boolean, hour: Int, minute: Int) {
        context.notifDataStore.edit { prefs ->
            prefs[keyEnabled] = enabled
            prefs[keyHour]    = hour
            prefs[keyMinute]  = minute
        }
    }
}
