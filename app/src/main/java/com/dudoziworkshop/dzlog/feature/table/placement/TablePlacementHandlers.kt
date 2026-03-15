package com.dudoziworkshop.dzlog.feature.table.placement

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_X_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_Y_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_OFFSET_X
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_OFFSET_Y
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.policy.TableWatermarkAction
import com.dudoziworkshop.dzlog.feature.table.policy.applyTableWatermarkAction
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

suspend fun loadTablePlacementState(context: Context): TablePlacementState {
    val prefs = context.dataStore.data.first()
    val boundsOffsetX10000 = (prefs[KEY_WM_BOUNDS_OFFSET_X_10000] ?: 0).coerceIn(0, 10000)
    val boundsOffsetY10000 = (prefs[KEY_WM_BOUNDS_OFFSET_Y_10000] ?: 0).coerceIn(0, 10000)
    val ratioOffsetX = (boundsOffsetX10000 / 100f).roundToInt().coerceIn(0, 100)
    val ratioOffsetY = (boundsOffsetY10000 / 100f).roundToInt().coerceIn(0, 100)
    return TablePlacementState(
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
        captureAspect = CaptureAspect.from(prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v)
    )
}

suspend fun applyCaptureAspectChange(
    context: Context,
    aspect: CaptureAspect,
    currentState: TablePlacementState
): TablePlacementState {
    context.dataStore.edit { prefs ->
        prefs[KEY_CAPTURE_ASPECT] = aspect.v
    }
    return currentState.copy(captureAspect = aspect)
}

suspend fun applyAnchorOffsetDragChange(
    context: Context,
    offsetXRatio: Int,
    offsetYRatio: Int,
    currentState: TablePlacementState
): TablePlacementState {
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
    currentState: TablePlacementState
): TablePlacementState {
    val patch = applyTableWatermarkAction(context, TableWatermarkAction.WidthRatioChanged(width))
    return currentState.copy(wmWidthRatio = patch.widthRatio ?: currentState.wmWidthRatio)
}

suspend fun applyHeightRatioChange(
    context: Context,
    height: Int,
    currentState: TablePlacementState
): TablePlacementState {
    val patch = applyTableWatermarkAction(context, TableWatermarkAction.HeightRatioChanged(height))
    return currentState.copy(wmHeightRatio = patch.heightRatio ?: currentState.wmHeightRatio)
}
