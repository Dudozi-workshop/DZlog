package com.example.dzlog.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.dzlog.data.preferences.KEY_BLANK_WARNING_ENABLED
import com.example.dzlog.data.preferences.KEY_CAPTURE_HAPTIC_ENABLED
import com.example.dzlog.data.preferences.KEY_CONTINUOUS_PREVIEW_MODE
import com.example.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.example.dzlog.data.preferences.KEY_HAPTIC_ENABLED
import com.example.dzlog.data.preferences.KEY_INCLUDE_FILENAME_IN_COUNTER_SCOPE
import com.example.dzlog.data.preferences.KEY_INCLUDE_PATH_IN_COUNTER_SCOPE
import com.example.dzlog.data.preferences.KEY_SAVE_MODE
import com.example.dzlog.data.preferences.KEY_PHOTO_QUALITY_MODE
import com.example.dzlog.data.preferences.KEY_TOAST_ENABLED
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.domain.model.PhotoQualityMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val saveMode: SaveMode,
    val continuousPreviewMode: ContinuousPreviewMode,
    val photoQualityMode: PhotoQualityMode = PhotoQualityMode.BALANCED,
    val counterPadding: Int,
    val includePathInCounterScope: Boolean,
    val includeFilenameInCounterScope: Boolean,
    val toastEnabled: Boolean,
    val hapticEnabled: Boolean,
    val captureHapticEnabled: Boolean = true,
    val blankWarningEnabled: Boolean,
)

object AppSettingsStore {

    fun flow(context: Context): Flow<AppSettings> =
        context.dataStore.data.map { prefs ->
            AppSettings(
                saveMode = SaveMode.from(prefs[KEY_SAVE_MODE] ?: SaveMode.BOTH.v),
                continuousPreviewMode = ContinuousPreviewMode.from(prefs[KEY_CONTINUOUS_PREVIEW_MODE] ?: ContinuousPreviewMode.OFF.v),
                photoQualityMode = PhotoQualityMode.from(prefs[KEY_PHOTO_QUALITY_MODE] ?: PhotoQualityMode.BALANCED.v),
                counterPadding = prefs[KEY_COUNTER_DIGITS] ?: 0,
                includePathInCounterScope = prefs[KEY_INCLUDE_PATH_IN_COUNTER_SCOPE] ?: true,
                includeFilenameInCounterScope = prefs[KEY_INCLUDE_FILENAME_IN_COUNTER_SCOPE] ?: true,
                toastEnabled = prefs[KEY_TOAST_ENABLED] ?: true,
                hapticEnabled = prefs[KEY_HAPTIC_ENABLED] ?: true,
                captureHapticEnabled = prefs[KEY_CAPTURE_HAPTIC_ENABLED] ?: true,
                blankWarningEnabled = prefs[KEY_BLANK_WARNING_ENABLED] ?: true,
            )
        }

    suspend fun setSaveMode(context: Context, mode: SaveMode) {
        context.dataStore.edit { it[KEY_SAVE_MODE] = mode.v }
    }

    suspend fun setContinuousPreviewMode(context: Context, mode: ContinuousPreviewMode) {
        context.dataStore.edit { it[KEY_CONTINUOUS_PREVIEW_MODE] = mode.v }
    }

    suspend fun setPhotoQualityMode(context: Context, mode: PhotoQualityMode) {
        context.dataStore.edit { it[KEY_PHOTO_QUALITY_MODE] = mode.v }
    }

    suspend fun setCounterPadding(context: Context, digits: Int) {
        context.dataStore.edit { it[KEY_COUNTER_DIGITS] = digits }
    }

    suspend fun setIncludePathInCounterScope(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_INCLUDE_PATH_IN_COUNTER_SCOPE] = enabled }
    }

    suspend fun setIncludeFilenameInCounterScope(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_INCLUDE_FILENAME_IN_COUNTER_SCOPE] = enabled }
    }

    suspend fun setToastEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_TOAST_ENABLED] = enabled }
    }

    suspend fun setHapticEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC_ENABLED] = enabled }
    }

    suspend fun setCaptureHapticEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_CAPTURE_HAPTIC_ENABLED] = enabled }
    }

    suspend fun setBlankWarningEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_BLANK_WARNING_ENABLED] = enabled }
    }
}
