package com.example.dzlog.ui.table.watermark

import android.content.Context
import com.example.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.example.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.example.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.example.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.example.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.example.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.example.dzlog.data.preferences.KEY_WM_TEXT_ALIGN
import com.example.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MANUAL
import com.example.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MODE
import com.example.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.WatermarkManualTextColor
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.model.WatermarkTextAlign
import com.example.dzlog.domain.model.WatermarkTextColorMode
import com.example.dzlog.feature.table.policy.TableWatermarkAction
import com.example.dzlog.feature.table.policy.applyTableWatermarkAction
import kotlinx.coroutines.flow.first

suspend fun loadTableWatermarkUiState(context: Context): TableWatermarkUiState {
    val prefs = context.dataStore.data.first()
    return TableWatermarkUiState(
        wmAnchor = when (prefs[KEY_WM_TABLE_ANCHOR] ?: 3) {
            0 -> WatermarkTableAnchor.TOP_LEFT
            1 -> WatermarkTableAnchor.TOP_RIGHT
            2 -> WatermarkTableAnchor.BOTTOM_LEFT
            else -> WatermarkTableAnchor.BOTTOM_RIGHT
        },
        wmWidthRatio = (prefs[KEY_WM_TABLE_WIDTH] ?: 40).coerceIn(40, 100),
        wmHeightRatio = (prefs[KEY_WM_TABLE_HEIGHT] ?: 20).coerceIn(10, 35),
        wmBgStyle = (prefs[KEY_WM_TABLE_BG_STYLE] ?: 0).coerceIn(0, 2),
        wmBgAlpha = (prefs[KEY_WM_BG_ALPHA] ?: 80).coerceIn(0, 255),
        wmValueScale = (prefs[KEY_WM_VALUE_SCALE] ?: 100).coerceIn(60, 160),
        wmTextColorMode = (prefs[KEY_WM_TEXT_COLOR_MODE] ?: WatermarkTextColorMode.AUTO).coerceIn(
            WatermarkTextColorMode.AUTO,
            WatermarkTextColorMode.MANUAL
        ),
        wmManualTextColor = (prefs[KEY_WM_TEXT_COLOR_MANUAL] ?: WatermarkManualTextColor.BLACK).coerceIn(
            WatermarkManualTextColor.WHITE,
            WatermarkManualTextColor.BLACK
        ),
        wmTextAlign = (prefs[KEY_WM_TEXT_ALIGN] ?: WatermarkTextAlign.LEFT).coerceIn(
            WatermarkTextAlign.LEFT,
            WatermarkTextAlign.RIGHT
        ),
        captureAspect = CaptureAspect.from(prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v)
    )
}

suspend fun applyWidthRatioChange(
    context: Context,
    width: Int,
    currentState: TableWatermarkUiState
): TableWatermarkUiState {
    val patch = applyTableWatermarkAction(context, TableWatermarkAction.WidthRatioChanged(width))
    return currentState.copy(wmWidthRatio = patch.widthRatio ?: currentState.wmWidthRatio)
}

suspend fun applyHeightRatioChange(
    context: Context,
    height: Int,
    currentState: TableWatermarkUiState
): TableWatermarkUiState {
    val patch = applyTableWatermarkAction(context, TableWatermarkAction.HeightRatioChanged(height))
    return currentState.copy(wmHeightRatio = patch.heightRatio ?: currentState.wmHeightRatio)
}

suspend fun applyBgStyleChange(
    context: Context,
    bgStyle: Int,
    currentState: TableWatermarkUiState
): TableWatermarkUiState {
    val patch = applyTableWatermarkAction(context, TableWatermarkAction.BgStyleChanged(bgStyle))
    return currentState.copy(wmBgStyle = patch.bgStyle ?: currentState.wmBgStyle)
}

suspend fun applyBgAlphaChange(
    context: Context,
    alpha: Int,
    currentState: TableWatermarkUiState
): TableWatermarkUiState {
    val patch = applyTableWatermarkAction(context, TableWatermarkAction.BgAlphaChanged(alpha))
    return currentState.copy(wmBgAlpha = patch.bgAlpha ?: currentState.wmBgAlpha)
}

suspend fun applyValueScaleChange(
    context: Context,
    scale: Int,
    currentState: TableWatermarkUiState
): TableWatermarkUiState {
    val patch = applyTableWatermarkAction(context, TableWatermarkAction.ValueScaleChanged(scale))
    return currentState.copy(wmValueScale = patch.valueScale ?: currentState.wmValueScale)
}

suspend fun applyTextColorModeChange(
    context: Context,
    mode: Int,
    currentState: TableWatermarkUiState
): TableWatermarkUiState {
    val patch = applyTableWatermarkAction(context, TableWatermarkAction.TextColorModeChanged(mode))
    return currentState.copy(wmTextColorMode = patch.textColorMode ?: currentState.wmTextColorMode)
}

suspend fun applyManualTextColorChange(
    context: Context,
    color: Int,
    currentState: TableWatermarkUiState
): TableWatermarkUiState {
    val patch = applyTableWatermarkAction(context, TableWatermarkAction.ManualTextColorChanged(color))
    return currentState.copy(wmManualTextColor = patch.manualTextColor ?: currentState.wmManualTextColor)
}

suspend fun applyTextAlignChange(
    context: Context,
    align: Int,
    currentState: TableWatermarkUiState
): TableWatermarkUiState {
    val patch = applyTableWatermarkAction(context, TableWatermarkAction.TextAlignChanged(align))
    return currentState.copy(wmTextAlign = patch.textAlign ?: currentState.wmTextAlign)
}
