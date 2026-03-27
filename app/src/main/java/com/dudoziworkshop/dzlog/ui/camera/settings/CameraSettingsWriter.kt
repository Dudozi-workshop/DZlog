package com.dudoziworkshop.dzlog.ui.camera.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAMERA_GRID_ON
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAMERA_FLASH_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAMERA_ZOOM_TENTHS
import com.dudoziworkshop.dzlog.data.preferences.KEY_ASSIST_SHUTTER_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_ASSIST_SHUTTER_X_RATIO
import com.dudoziworkshop.dzlog.data.preferences.KEY_ASSIST_SHUTTER_Y_RATIO
import com.dudoziworkshop.dzlog.data.preferences.KEY_CONTINUOUS_PREVIEW_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_SAVE_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_SHOW_WM_PREVIEW
import com.dudoziworkshop.dzlog.data.preferences.KEY_VOLUME_KEY_ACTION
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_X_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_Y_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_OFFSET_X
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_OFFSET_Y
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_ROTATION_CW_90
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.preferences.persistCaptureAspect
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.VolumeKeyAction
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFlashMode

internal class CameraSettingsWriter(
    private val context: Context,
) {
    suspend fun setCaptureAspect(aspect: CaptureAspect) {
        persistCaptureAspect(context, aspect)
    }

    suspend fun setSaveMode(mode: SaveMode) {
        context.dataStore.edit { it[KEY_SAVE_MODE] = mode.v }
    }

    suspend fun setContinuousPreviewMode(mode: ContinuousPreviewMode) {
        context.dataStore.edit { it[KEY_CONTINUOUS_PREVIEW_MODE] = mode.v }
    }

    suspend fun setShowGrid(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CAMERA_GRID_ON] = enabled }
    }

    suspend fun setShowWmPreview(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_WM_PREVIEW] = if (enabled) 1 else 0 }
    }

    suspend fun setVolumeKeyAction(action: VolumeKeyAction) {
        context.dataStore.edit { it[KEY_VOLUME_KEY_ACTION] = action.v }
    }

    suspend fun setZoomTenths(zoomTenths: Int) {
        context.dataStore.edit { it[KEY_CAMERA_ZOOM_TENTHS] = zoomTenths }
    }

    suspend fun setFlashMode(mode: CameraFlashMode) {
        context.dataStore.edit { it[KEY_CAMERA_FLASH_MODE] = mode.toPrefValue() }
    }

    suspend fun setAssistShutterEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ASSIST_SHUTTER_ENABLED] = enabled }
    }

    suspend fun setAssistShutterPositionRatio(xRatio: Float, yRatio: Float) {
        context.dataStore.edit {
            it[KEY_ASSIST_SHUTTER_X_RATIO] = xRatio
            it[KEY_ASSIST_SHUTTER_Y_RATIO] = yRatio
        }
    }

    suspend fun setWmRotationCwDeg(rotationCwDeg: Int) {
        context.dataStore.edit { it[KEY_WM_ROTATION_CW_90] = if (rotationCwDeg == 90) 90 else 0 }
    }

    suspend fun setWmCustomOffsetRatio(x: Int, y: Int) {
        val nx = x.coerceIn(0, 100)
        val ny = y.coerceIn(0, 100)
        context.dataStore.edit {
            it[KEY_WM_TABLE_ANCHOR] = 4
            it[KEY_WM_OFFSET_X] = nx
            it[KEY_WM_OFFSET_Y] = ny
        }
    }

    suspend fun setWmCustomBoundsOffset10000(x10000: Int, y10000: Int) {
        val nx10000 = x10000.coerceIn(0, 10000)
        val ny10000 = y10000.coerceIn(0, 10000)
        val nx = (nx10000 / 100f).toInt().coerceIn(0, 100)
        val ny = (ny10000 / 100f).toInt().coerceIn(0, 100)
        context.dataStore.edit {
            it[KEY_WM_TABLE_ANCHOR] = 4
            it[KEY_WM_BOUNDS_OFFSET_X_10000] = nx10000
            it[KEY_WM_BOUNDS_OFFSET_Y_10000] = ny10000
            it[KEY_WM_OFFSET_X] = nx
            it[KEY_WM_OFFSET_Y] = ny
        }
    }
}

private fun CameraFlashMode.toPrefValue(): Int = when (this) {
    CameraFlashMode.OFF -> 0
    CameraFlashMode.AUTO -> 1
    CameraFlashMode.ON -> 2
}
