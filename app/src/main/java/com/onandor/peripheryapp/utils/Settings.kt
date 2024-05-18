package com.onandor.peripheryapp.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.onandor.peripheryapp.kbm.bluetooth.reports.KeyboardReport
import com.onandor.peripheryapp.kbm.input.KeyMapping
import com.onandor.peripheryapp.kbm.input.TouchpadController
import com.onandor.peripheryapp.webcam.video.streamers.StreamerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class SettingKey<T>(
    val preferenceKey: Preferences.Key<T>,
    val defaultValue: T
)

object BtSettingKeys {
    val KEYBOARD_LOCALE = SettingKey(
        preferenceKey = intPreferencesKey("keyboard_locale"),
        defaultValue = KeyMapping.Locales.EN_US
    )
    val MOUSE_POLLING_RATE = SettingKey(
        preferenceKey = longPreferencesKey("mouse_polling_rate"),
        defaultValue = TouchpadController.PollingRates.HIGH
    )
    val SEND_VOLUME_INPUT = SettingKey(
        preferenceKey = booleanPreferencesKey("send_volume_input"),
        defaultValue = false
    )
    val EXTENDED_KEYBOARD_SHOWN = SettingKey(
        preferenceKey = booleanPreferencesKey("extended_keyboard_enabled"),
        defaultValue = false
    )
    val KEYBOARD_REPORT_MODE = SettingKey(
        preferenceKey = intPreferencesKey("keyboard_report_mode"),
        defaultValue = KeyboardReport.ReportMode.KEY_6.value
    )
}

object WebcamSettingKeys {
    val CAMERA_ID = SettingKey(
        preferenceKey = stringPreferencesKey("webcam_camera_id"),
        defaultValue = ""
    )
    val RESOLUTION_IDX = SettingKey(
        preferenceKey = intPreferencesKey("webcam_resolution_idx"),
        defaultValue = -1
    )
    val FRAME_RATE_IDX = SettingKey(
        preferenceKey = intPreferencesKey("webcam_frame_rate_idx"),
        defaultValue = -1
    )
    val BIT_RATE = SettingKey(
        preferenceKey = intPreferencesKey("webcam_bit_rate"),
        defaultValue = -1
    )
    val STREAMER_TYPE = SettingKey(
        preferenceKey = intPreferencesKey("webcam_streamer_type"),
        defaultValue = StreamerType.CLIENT
    )
}

class Settings @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    fun <T> observe(key: SettingKey<T>): Flow<T> {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs -> prefs[key.preferenceKey] ?: key.defaultValue }
    }

    suspend fun <T> get(key: SettingKey<T>): T {
        return observe(key).first()
    }

    suspend fun <T> save(key: SettingKey<T>, value: T) {
        dataStore.edit { prefs -> prefs[key.preferenceKey] = value }
    }

    suspend fun <T> remove(key: SettingKey<T>) {
        dataStore.edit { prefs -> prefs.remove(key.preferenceKey) }
    }
}