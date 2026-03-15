package com.dudoziworkshop.dzlog.feature.table.placement

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_X_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_Y_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_DETAIL_GRID_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_GRID_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_OFFSET_X
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_OFFSET_Y
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_ALIGN
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MANUAL
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.WatermarkManualTextColor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode
import com.dudoziworkshop.dzlog.feature.table.model.TableWatermarkUiState
import com.dudoziworkshop.dzlog.feature.table.policy.TableWatermarkAction
import com.dudoziworkshop.dzlog.feature.table.policy.applyTableWatermarkAction
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

suspend fun loadTableWatermarkUiState(context: Context): TableWatermarkUiState {
    val prefs = context.dataStore.data.first()
    val boundsOffsetX10000 = (prefs[KEY_WM_BOUNDS_OFFSET_X_10000] ?: 0).coerceIn(0, 10000)
    val boundsOffsetY10000 = (prefs[KEY_WM_BOUNDS_OFFSET_Y_10000] ?: 0).coerceIn(0, 10000)
    val ratioOffsetX = (boundsOffsetX10000 / 100f).roundToInt().coerceIn(0, 100)
    val ratioOffsetY = (boundsOffsetY10000 / 100f).roundToInt().coerceIn(0, 100)
    return TableWatermarkUiState(
        wmAnchor = when (prefs[KEY_WM_TABLE_ANCHOR] ?: 3) {
            0 -> WatermarkTableAnchor.TOP_LEFT
            1 -> WatermarkTableAnchor.TOP_RIGHT
            2 -> WatermarkTableAnchor.BOTTOM_LEFT
            3 -> WatermarkTableAnchor.BOTTOM_RIGHT
            else -> WatermarkTableAnchor.CUSTOM
        },
        wmOffsetXRatio = ratioOffsetX,
        wmOffsetYRatio = ratioOffsetY,
        wmWidthRatio = (prefs[KEY_WM_TABLE_WIDTH] ?: 40).coerceIn(10, 100),
        wmHeightRatio = (prefs[KEY_WM_TABLE_HEIGHT] ?: 20).coerceIn(10, 100),
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
        wmGridEnabled = if (prefs.contains(KEY_TABLE_DETAIL_GRID_ENABLED)) {
            prefs[KEY_TABLE_DETAIL_GRID_ENABLED] ?: true
        } else {
            // 주요 정책: 구버전 저장값은 1회 fallback으로만 읽고, 이후부터는 표 상세설정 전용 키를 사용한다.
            prefs[KEY_WM_GRID_ENABLED] ?: true
        },
        captureAspect = CaptureAspect.from(prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v)
    )
}

suspend fun applyCaptureAspectChange(
    context: Context,
    aspect: CaptureAspect,
    currentState: TableWatermarkUiState
): TableWatermarkUiState {
    context.dataStore.edit { prefs ->
        prefs[KEY_CAPTURE_ASPECT] = aspect.v
    }
    return currentState.copy(captureAspect = aspect)
}

suspend fun applyAnchorOffsetDragChange(
    context: Context,
    offsetXRatio: Int,
    offsetYRatio: Int,
    currentState: TableWatermarkUiState
): TableWatermarkUiState {
    val nx = offsetXRatio.coerceIn(0, 100)
    val ny = offsetYRatio.coerceIn(0, 100)
    val nx10000 = (nx * 100).coerceIn(0, 10000)
    val ny10000 = (ny * 100).coerceIn(0, 10000)
    context.dataStore.edit { prefs ->
        prefs[KEY_WM_TABLE_ANCHOR] = 4
        prefs[KEY_WM_OFFSET_X] = nx
        prefs[KEY_WM_OFFSET_Y] = ny
        prefs[KEY_WM_BOUNDS_OFFSET_X_10000] = nx10000
        prefs[KEY_WM_BOUNDS_OFFSET_Y_10000] = ny10000
    }
    return currentState.copy(
        wmAnchor = WatermarkTableAnchor.CUSTOM,
        wmOffsetXRatio = nx,
        wmOffsetYRatio = ny
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


suspend fun applyGridEnabledChange(
    context: Context,
    enabled: Boolean,
    currentState: TableWatermarkUiState
): TableWatermarkUiState {
    context.dataStore.edit { prefs ->
        prefs[KEY_TABLE_DETAIL_GRID_ENABLED] = enabled
    }
    return currentState.copy(wmGridEnabled = enabled)
}
