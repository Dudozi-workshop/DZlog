package com.dudoziworkshop.dzlog.ui.camera.presenter

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.LifecycleOwner
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.ui.camera.preview.CameraPreviewAreaArgs
import com.dudoziworkshop.dzlog.ui.camera.preview.WatermarkUiArgs
import com.dudoziworkshop.dzlog.ui.camera.settings.CameraSettingsWriter
import com.dudoziworkshop.dzlog.ui.camera.state.CameraUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun rememberCameraPreviewAreaArgs(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    ui: CameraUiState,
    appSettings: AppSettings,
    settingsWriter: CameraSettingsWriter,
    tableTemplateState: TableTemplateState,
    tableResolver: TableResolver,
    scopeNextCounter: Int?,
    phraseProgressCursor: Int,
    dateFormat: String,
    timeFormat: String,
    fnDelim: String,
    shutterButtonTopY: Float?,
    safeTopY: Float?,
    safeBottomY: Float?,
    usableVerticalMarginPx: Float,
    onOpenTableEditor: () -> Unit,
): CameraPreviewAreaArgs {
    val scope = rememberCoroutineScope()

    return remember(
        context,
        lifecycleOwner,
        ui.prefs.captureAspect,
        appSettings.saveMode,
        ui.prefs.continuousPreviewMode,
        appSettings.photoQualityMode,
        ui.prefs.counterDigits,
        dateFormat,
        timeFormat,
        fnDelim,
        scopeNextCounter,
        phraseProgressCursor,
        tableTemplateState,
        tableResolver,
        ui.capture.now,
        ui.prefs.showWmPreview,
        ui.prefs.showGrid,
        ui.prefs.zoomRatioTenths,
        ui.capture.maxZoomTenths,
        ui.focusMode,
        ui.prefs.wmTableAnchor,
        ui.prefs.wmTableWidthRatio,
        ui.prefs.wmTableHeightRatio,
        ui.prefs.wmOffsetXRatio,
        ui.prefs.wmOffsetYRatio,
        ui.prefs.wmBoundsOffsetX10000,
        ui.prefs.wmBoundsOffsetY10000,
        ui.prefs.wmBgAlpha,
        ui.prefs.wmBgStyle,
        ui.prefs.wmValueScale,
        ui.prefs.wmTextColorMode,
        ui.prefs.wmManualTextColor,
        ui.prefs.wmTextAlign,
        ui.prefs.wmGridEnabled,
        ui.prefs.wmRotationCwDeg,
        shutterButtonTopY,
        safeTopY,
        safeBottomY,
        usableVerticalMarginPx,
    ) {
        CameraPreviewAreaArgs(
            context = context,
            lifecycleOwner = lifecycleOwner,
            scope = scope,
            captureAspect = ui.prefs.captureAspect,
            saveMode = appSettings.saveMode,
            continuousPreviewMode = ui.prefs.continuousPreviewMode,
            photoQualityMode = appSettings.photoQualityMode,
            counterDigits = ui.prefs.counterDigits,
            dateFormat = dateFormat,
            timeFormat = timeFormat,
            fnDelim = fnDelim,
            scopeNextCounter = scopeNextCounter,
            phraseProgressCursor = phraseProgressCursor,
            tableTemplateState = tableTemplateState,
            tableResolver = tableResolver,
            now = ui.capture.now,
            showWmPreview = ui.prefs.showWmPreview,
            showGrid = ui.prefs.showGrid,
            zoomRatioTenths = ui.prefs.zoomRatioTenths,
            maxZoomTenths = ui.capture.maxZoomTenths,
            focusMode = ui.focusMode,
            onActualZoomTenthsChange = { ui.capture.actualZoomTenths = it },
            onRequestedZoomTenthsCommit = { next ->
                val normalized = next.coerceIn(10, ui.capture.maxZoomTenths.coerceAtLeast(10))
                ui.prefs.zoomRatioTenths = normalized
                scope.launch { settingsWriter.setZoomTenths(normalized) }
            },
            onMaxZoomTenthsChange = { ui.capture.maxZoomTenths = it.coerceAtLeast(10) },
            onPinchZoomActiveChange = { ui.isPinchZoomActive = it },
            shutterButtonTopY = shutterButtonTopY,
            safeTopY = safeTopY,
            safeBottomY = safeBottomY,
            usableVerticalMarginPx = usableVerticalMarginPx,
            onUsableVerticalRatioChange = { topRatio, bottomRatio ->
                ui.capture.usableTopRatio = topRatio
                ui.capture.usableBottomRatio = bottomRatio
            },
            onWatermarkOffsetRatioPreview = { x, y ->
                ui.prefs.wmTableAnchor = WatermarkTableAnchor.CUSTOM
                ui.prefs.wmOffsetXRatio = x
                ui.prefs.wmOffsetYRatio = y
            },
            onWatermarkOffsetRatioCommit = { x, y ->
                commitOffsetRatio(
                    scope = scope,
                    ui = ui,
                    settingsWriter = settingsWriter,
                    x = x,
                    y = y,
                )
            },
            onWatermarkBoundsOffset10000Preview = { x10000, y10000 ->
                val nx10000 = x10000.coerceIn(0, 10000)
                val ny10000 = y10000.coerceIn(0, 10000)
                ui.prefs.wmTableAnchor = WatermarkTableAnchor.CUSTOM
                ui.prefs.wmBoundsOffsetX10000 = nx10000
                ui.prefs.wmBoundsOffsetY10000 = ny10000
                ui.prefs.wmOffsetXRatio = (nx10000 / 100f).roundToInt().coerceIn(0, 100)
                ui.prefs.wmOffsetYRatio = (ny10000 / 100f).roundToInt().coerceIn(0, 100)
            },
            onWatermarkBoundsOffset10000Commit = { x10000, y10000 ->
                commitBoundsOffset10000(
                    scope = scope,
                    ui = ui,
                    settingsWriter = settingsWriter,
                    x10000 = x10000,
                    y10000 = y10000,
                )
            },
            onOpenTableEditor = onOpenTableEditor,
            watermarkUi = WatermarkUiArgs(
                anchor = ui.prefs.wmTableAnchor,
                tableWidthRatio = ui.prefs.wmTableWidthRatio,
                tableHeightRatio = ui.prefs.wmTableHeightRatio,
                offsetXRatio = ui.prefs.wmOffsetXRatio,
                offsetYRatio = ui.prefs.wmOffsetYRatio,
                boundsOffsetX10000 = ui.prefs.wmBoundsOffsetX10000,
                boundsOffsetY10000 = ui.prefs.wmBoundsOffsetY10000,
                bgAlpha = ui.prefs.wmBgAlpha,
                bgStyle = ui.prefs.wmBgStyle,
                valueScale = ui.prefs.wmValueScale,
                textColorMode = ui.prefs.wmTextColorMode,
                manualTextColor = ui.prefs.wmManualTextColor,
                textAlign = ui.prefs.wmTextAlign,
                wmGridEnabled = ui.prefs.wmGridEnabled,
                rotationCwDeg = ui.prefs.wmRotationCwDeg,
            ),
        )
    }
}

private fun commitOffsetRatio(
    scope: CoroutineScope,
    ui: CameraUiState,
    settingsWriter: CameraSettingsWriter,
    x: Int,
    y: Int,
) {
    val nx = x.coerceIn(0, 100)
    val ny = y.coerceIn(0, 100)
    ui.prefs.wmTableAnchor = WatermarkTableAnchor.CUSTOM
    ui.prefs.wmOffsetXRatio = nx
    ui.prefs.wmOffsetYRatio = ny
    scope.launch { settingsWriter.setWmCustomOffsetRatio(nx, ny) }
}

private fun commitBoundsOffset10000(
    scope: CoroutineScope,
    ui: CameraUiState,
    settingsWriter: CameraSettingsWriter,
    x10000: Int,
    y10000: Int,
) {
    val nx10000 = x10000.coerceIn(0, 10000)
    val ny10000 = y10000.coerceIn(0, 10000)
    val nx = (nx10000 / 100f).roundToInt().coerceIn(0, 100)
    val ny = (ny10000 / 100f).roundToInt().coerceIn(0, 100)
    ui.prefs.wmTableAnchor = WatermarkTableAnchor.CUSTOM
    ui.prefs.wmBoundsOffsetX10000 = nx10000
    ui.prefs.wmBoundsOffsetY10000 = ny10000
    ui.prefs.wmOffsetXRatio = nx
    ui.prefs.wmOffsetYRatio = ny
    scope.launch { settingsWriter.setWmCustomBoundsOffset10000(nx10000, ny10000) }
}
