package com.dudoziworkshop.dzlog.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.preferences.KEY_BLANK_WARNING_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAPTURE_HAPTIC_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_ASSIST_SHUTTER_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_ASSIST_SHUTTER_X_RATIO
import com.dudoziworkshop.dzlog.data.preferences.KEY_ASSIST_SHUTTER_Y_RATIO
import com.dudoziworkshop.dzlog.data.preferences.KEY_CONTINUOUS_PREVIEW_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAPTURE_SOUND_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.dudoziworkshop.dzlog.data.preferences.KEY_HAPTIC_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_VOLUME_KEY_ACTION
import com.dudoziworkshop.dzlog.data.preferences.KEY_INCLUDE_FILENAME_IN_COUNTER_SCOPE
import com.dudoziworkshop.dzlog.data.preferences.KEY_INCLUDE_PATH_IN_COUNTER_SCOPE
import com.dudoziworkshop.dzlog.data.preferences.KEY_SAVE_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_PHOTO_QUALITY_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_TOAST_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.VolumeKeyAction
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
    val captureSoundEnabled: Boolean = true,
    val volumeKeyAction: VolumeKeyAction = VolumeKeyAction.NONE,
    val blankWarningEnabled: Boolean,
    val assistShutterEnabled: Boolean = false,
    val assistShutterXRatio: Float = 0.82f,
    val assistShutterYRatio: Float = 0.62f,
) {
    companion object {
        val Default = AppSettings(
            saveMode = SaveMode.BOTH,
            continuousPreviewMode = ContinuousPreviewMode.OFF,
            photoQualityMode = PhotoQualityMode.BALANCED,
            counterPadding = 0,
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
            toastEnabled = true,
            hapticEnabled = true,
            captureHapticEnabled = true,
            captureSoundEnabled = true,
            volumeKeyAction = VolumeKeyAction.NONE,
            blankWarningEnabled = true,
            assistShutterEnabled = false,
            assistShutterXRatio = 0.82f,
            assistShutterYRatio = 0.62f,
        )
    }
}

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
                captureSoundEnabled = prefs[KEY_CAPTURE_SOUND_ENABLED] ?: true,
                volumeKeyAction = VolumeKeyAction.from(prefs[KEY_VOLUME_KEY_ACTION] ?: VolumeKeyAction.NONE.v),
                blankWarningEnabled = prefs[KEY_BLANK_WARNING_ENABLED] ?: true,
                assistShutterEnabled = prefs[KEY_ASSIST_SHUTTER_ENABLED] ?: false,
                assistShutterXRatio = prefs[KEY_ASSIST_SHUTTER_X_RATIO] ?: 0.82f,
                assistShutterYRatio = prefs[KEY_ASSIST_SHUTTER_Y_RATIO] ?: 0.62f,
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

    suspend fun setCaptureSoundEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_CAPTURE_SOUND_ENABLED] = enabled }
    }

    suspend fun setVolumeKeyAction(context: Context, action: VolumeKeyAction) {
        context.dataStore.edit { it[KEY_VOLUME_KEY_ACTION] = action.v }
    }

    suspend fun setBlankWarningEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_BLANK_WARNING_ENABLED] = enabled }
    }

    suspend fun setAssistShutterEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_ASSIST_SHUTTER_ENABLED] = enabled }
    }
}
