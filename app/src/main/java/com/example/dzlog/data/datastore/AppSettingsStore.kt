package com.example.dzlog.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import com.example.dzlog.data.preferences.KEY_BLANK_WARNING_ENABLED
import com.example.dzlog.data.preferences.KEY_CONTINUOUS_PREVIEW_MODE
import com.example.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.example.dzlog.data.preferences.KEY_COUNTER_SUFFIX_ENABLED
import com.example.dzlog.data.preferences.KEY_HAPTIC_ENABLED
import com.example.dzlog.data.preferences.KEY_RESET_COUNTER_ON_PATH_CHANGE
import com.example.dzlog.data.preferences.KEY_SAVE_MODE
import com.example.dzlog.data.preferences.KEY_TOAST_ENABLED
import com.example.dzlog.data.preferences.KEY_USED_COUNTER_VALUES_JSON
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.SaveMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val saveMode: SaveMode,
    val continuousPreviewMode: ContinuousPreviewMode,
    val counterPadding: Int,
    val counterSuffixEnabled: Boolean,
    val resetCounterOnPathChange: Boolean,
    val toastEnabled: Boolean,
    val hapticEnabled: Boolean,
    val blankWarningEnabled: Boolean,
    val usedCounterValuesJson: String?
)

object AppSettingsStore {

    fun flow(context: Context): Flow<AppSettings> =
        context.dataStore.data.map { prefs ->
            AppSettings(
                saveMode = SaveMode.from(prefs[KEY_SAVE_MODE] ?: SaveMode.BOTH.v),
                continuousPreviewMode = ContinuousPreviewMode.from(prefs[KEY_CONTINUOUS_PREVIEW_MODE] ?: ContinuousPreviewMode.OFF.v),
                counterPadding = prefs[KEY_COUNTER_DIGITS] ?: 0,
                counterSuffixEnabled = prefs[KEY_COUNTER_SUFFIX_ENABLED] ?: true,
                resetCounterOnPathChange = prefs[KEY_RESET_COUNTER_ON_PATH_CHANGE] ?: true,
                toastEnabled = prefs[KEY_TOAST_ENABLED] ?: true,
                hapticEnabled = prefs[KEY_HAPTIC_ENABLED] ?: true,
                blankWarningEnabled = prefs[KEY_BLANK_WARNING_ENABLED] ?: true,
                usedCounterValuesJson = prefs[KEY_USED_COUNTER_VALUES_JSON]
            )
        }

    suspend fun setSaveMode(context: Context, mode: SaveMode) {
        context.dataStore.edit { it[KEY_SAVE_MODE] = mode.v }
    }

    suspend fun setContinuousPreviewMode(context: Context, mode: ContinuousPreviewMode) {
        context.dataStore.edit { it[KEY_CONTINUOUS_PREVIEW_MODE] = mode.v }
    }

    suspend fun setCounterPadding(context: Context, digits: Int) {
        context.dataStore.edit { it[KEY_COUNTER_DIGITS] = digits }
    }

    suspend fun setCounterSuffixEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_COUNTER_SUFFIX_ENABLED] = enabled }
    }

    suspend fun setResetCounterOnPathChange(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_RESET_COUNTER_ON_PATH_CHANGE] = enabled }
    }

    suspend fun setToastEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_TOAST_ENABLED] = enabled }
    }

    suspend fun setHapticEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC_ENABLED] = enabled }
    }

    suspend fun setBlankWarningEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_BLANK_WARNING_ENABLED] = enabled }
    }

    suspend fun setUsedCounterValuesJson(context: Context, json: String) {
        context.dataStore.edit { it[KEY_USED_COUNTER_VALUES_JSON] = json }
    }
}
