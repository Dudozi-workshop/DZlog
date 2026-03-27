package com.dudoziworkshop.dzlog.ui.camera.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.dudoziworkshop.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.dudoziworkshop.dzlog.data.counter.clampCounterDigits
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAMERA_GRID_ON
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAMERA_FLASH_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAMERA_ZOOM_TENTHS
import com.dudoziworkshop.dzlog.data.preferences.KEY_ASSIST_SHUTTER_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_ASSIST_SHUTTER_X_RATIO
import com.dudoziworkshop.dzlog.data.preferences.KEY_ASSIST_SHUTTER_Y_RATIO
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.dudoziworkshop.dzlog.data.preferences.KEY_CONTINUOUS_PREVIEW_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.dudoziworkshop.dzlog.data.preferences.KEY_PHOTO_QUALITY_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_SAVE_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_SHOW_WM_PREVIEW
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_X_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_Y_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_GRID_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_ROTATION_CW_90
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_ALIGN
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MANUAL
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.WatermarkManualTextColor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode
import com.dudoziworkshop.dzlog.ui.camera.state.CameraUiState
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFlashMode
import kotlin.math.roundToInt

@Composable
internal fun CameraPrefsEffect(
    lifecycleOwner: LifecycleOwner,
    readPrefs: suspend () -> Preferences,
    ui: CameraUiState,
) {
    LaunchedEffect(Unit) {
        loadCameraPrefsIntoUi(readPrefs(), ui)
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            loadCameraPrefsIntoUi(readPrefs(), ui)
        }
    }
}

internal fun loadCameraPrefsIntoUi(prefs: Preferences, ui: CameraUiState) {
    try {
        ui.prefs.wmTableAnchor = when (prefs[KEY_WM_TABLE_ANCHOR] ?: 3) {
            0 -> WatermarkTableAnchor.TOP_LEFT
            1 -> WatermarkTableAnchor.TOP_RIGHT
            2 -> WatermarkTableAnchor.BOTTOM_LEFT
            3 -> WatermarkTableAnchor.BOTTOM_RIGHT
            else -> WatermarkTableAnchor.CUSTOM
        }

        ui.prefs.wmTableWidthRatio = (prefs[KEY_WM_TABLE_WIDTH] ?: 40).coerceIn(10, 100)
        ui.prefs.wmTableHeightRatio = (prefs[KEY_WM_TABLE_HEIGHT] ?: 20).coerceIn(10, 100)
        ui.prefs.wmBoundsOffsetX10000 = (prefs[KEY_WM_BOUNDS_OFFSET_X_10000] ?: 0).coerceIn(0, 10000)
        ui.prefs.wmBoundsOffsetY10000 = (prefs[KEY_WM_BOUNDS_OFFSET_Y_10000] ?: 0).coerceIn(0, 10000)
        ui.prefs.wmOffsetXRatio = (ui.prefs.wmBoundsOffsetX10000 / 100f).roundToInt().coerceIn(0, 100)
        ui.prefs.wmOffsetYRatio = (ui.prefs.wmBoundsOffsetY10000 / 100f).roundToInt().coerceIn(0, 100)

        ui.prefs.wmBgAlpha = (prefs[KEY_WM_BG_ALPHA] ?: 80).coerceIn(0, 255)
        ui.prefs.wmBgStyle = (prefs[KEY_WM_TABLE_BG_STYLE] ?: 0).coerceIn(0, 2)
        ui.prefs.wmValueScale = (prefs[KEY_WM_VALUE_SCALE] ?: 100).coerceIn(60, 160)
        ui.prefs.wmTextColorMode = (prefs[KEY_WM_TEXT_COLOR_MODE] ?: WatermarkTextColorMode.AUTO).coerceIn(0, 1)
        ui.prefs.wmManualTextColor = (prefs[KEY_WM_TEXT_COLOR_MANUAL] ?: WatermarkManualTextColor.BLACK).coerceIn(0, 1)
        ui.prefs.wmTextAlign = (prefs[KEY_WM_TEXT_ALIGN] ?: WatermarkTextAlign.LEFT).coerceIn(0, 2)
        ui.prefs.wmGridEnabled = prefs[KEY_WM_GRID_ENABLED] ?: true
        ui.prefs.wmRotationCwDeg = if ((prefs[KEY_WM_ROTATION_CW_90] ?: 0) == 90) 90 else 0

        ui.prefs.captureAspect = CaptureAspect.from(
            prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v
        )

        // 표준: 0=원본, 1=워터마크, 2=원본+워터마크
        ui.prefs.saveMode = SaveMode.from(prefs[KEY_SAVE_MODE] ?: SaveMode.BOTH.v)

        ui.prefs.continuousPreviewMode = ContinuousPreviewMode.from(
            prefs[KEY_CONTINUOUS_PREVIEW_MODE] ?: ContinuousPreviewMode.OFF.v
        )
        ui.prefs.photoQualityMode = PhotoQualityMode.from(
            prefs[KEY_PHOTO_QUALITY_MODE] ?: PhotoQualityMode.BALANCED.v
        )

        ui.prefs.counterDigits = clampCounterDigits(prefs[KEY_COUNTER_DIGITS] ?: COUNTER_DIGITS_DEFAULT)
        ui.prefs.showWmPreview = (prefs[KEY_SHOW_WM_PREVIEW] ?: 1) == 1
        ui.prefs.showGrid = prefs[KEY_CAMERA_GRID_ON] ?: false
        ui.prefs.zoomRatioTenths = (prefs[KEY_CAMERA_ZOOM_TENTHS] ?: 10).coerceIn(10, 100)
        ui.prefs.flashMode = (prefs[KEY_CAMERA_FLASH_MODE] ?: 0).toCameraFlashMode()
        ui.prefs.assistShutterEnabled = prefs[KEY_ASSIST_SHUTTER_ENABLED] ?: false
        ui.prefs.assistShutterXRatio = (prefs[KEY_ASSIST_SHUTTER_X_RATIO] ?: 0.82f)
        ui.prefs.assistShutterYRatio = (prefs[KEY_ASSIST_SHUTTER_Y_RATIO] ?: 0.62f)
        ui.capture.actualZoomTenths = ui.prefs.zoomRatioTenths
        ui.capture.maxZoomTenths = maxOf(ui.capture.maxZoomTenths, 20)
    } catch (_: Exception) {
        ui.prefs.captureAspect = CaptureAspect.R3_4
        ui.prefs.saveMode = SaveMode.WATERMARK_ONLY
        ui.prefs.continuousPreviewMode = ContinuousPreviewMode.OFF
        ui.prefs.photoQualityMode = PhotoQualityMode.BALANCED
        ui.prefs.counterDigits = COUNTER_DIGITS_DEFAULT
        ui.prefs.showWmPreview = true
        ui.prefs.showGrid = false
        ui.prefs.zoomRatioTenths = 10
        ui.prefs.flashMode = CameraFlashMode.OFF
        ui.prefs.assistShutterEnabled = false
        ui.prefs.assistShutterXRatio = 0.82f
        ui.prefs.assistShutterYRatio = 0.62f
        ui.capture.actualZoomTenths = 10
        ui.capture.maxZoomTenths = 20
        ui.prefs.wmTableAnchor = WatermarkTableAnchor.BOTTOM_RIGHT
        ui.prefs.wmTableWidthRatio = 40
        ui.prefs.wmTableHeightRatio = 20
        ui.prefs.wmOffsetXRatio = 0
        ui.prefs.wmOffsetYRatio = 0
        ui.prefs.wmBoundsOffsetX10000 = 0
        ui.prefs.wmBoundsOffsetY10000 = 0
        ui.prefs.wmBgAlpha = 80
        ui.prefs.wmBgStyle = 0
        ui.prefs.wmValueScale = 100
        ui.prefs.wmTextColorMode = WatermarkTextColorMode.AUTO
        ui.prefs.wmManualTextColor = WatermarkManualTextColor.BLACK
        ui.prefs.wmTextAlign = WatermarkTextAlign.LEFT
        ui.prefs.wmGridEnabled = true
        ui.prefs.wmRotationCwDeg = 0
    }
}

private fun Int.toCameraFlashMode(): CameraFlashMode = when (this) {
    1 -> CameraFlashMode.AUTO
    2 -> CameraFlashMode.ON
    else -> CameraFlashMode.OFF
}
