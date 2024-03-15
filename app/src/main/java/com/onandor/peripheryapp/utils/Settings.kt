package com.onandor.peripheryapp.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

object BtSettingKeys {
    val KEYBOARD_LOCALE = intPreferencesKey("keyboard_locale")
    val MOUSE_POLLING_RATE = longPreferencesKey("mouse_polling_rate")
    val SEND_VOLUME_INPUT = booleanPreferencesKey("send_volume_input")
    val EXTENDED_KEYBOARD_SHOWN = booleanPreferencesKey("extended_keyboard_enabled")
}

class Settings @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    fun <T> observe(key: Preferences.Key<T>, emptyValue: T): Flow<T> {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs -> prefs[key] ?: emptyValue }
    }

    suspend fun <T> get(key: Preferences.Key<T>, emptyValue: T): T {
        return observe(key, emptyValue).first()
    }

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    suspend fun <T> remove(key: Preferences.Key<T>) {
        dataStore.edit { prefs -> prefs.remove(key) }
    }
}