package com.dudoziworkshop.dzlog.feature.table.placement

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_X_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_Y_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_OFFSET_X
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_OFFSET_Y
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_ROTATION_CW_90
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

suspend fun loadTablePlacementState(context: Context): TablePlacementState {
    val prefs = context.dataStore.data.first()
    val boundsOffsetX10000 = (prefs[KEY_WM_BOUNDS_OFFSET_X_10000] ?: 0).coerceIn(0, 10000)
    val boundsOffsetY10000 = (prefs[KEY_WM_BOUNDS_OFFSET_Y_10000] ?: 0).coerceIn(0, 10000)
    val ratioOffsetX = (boundsOffsetX10000 / 100f).roundToInt().coerceIn(0, 100)
    val ratioOffsetY = (boundsOffsetY10000 / 100f).roundToInt().coerceIn(0, 100)
    return TablePlacementState(
        wmOffsetXRatio = ratioOffsetX,
        wmOffsetYRatio = ratioOffsetY,
        wmWidthRatio = (prefs[KEY_WM_TABLE_WIDTH] ?: 40).coerceIn(10, 100),
        wmHeightRatio = (prefs[KEY_WM_TABLE_HEIGHT] ?: 20).coerceIn(10, 100),
        rotationCwDeg = if ((prefs[KEY_WM_ROTATION_CW_90] ?: 0) == 90) 90 else 0,
        captureAspect = CaptureAspect.from(prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v)
    )
}

suspend fun persistTablePlacementState(
    context: Context,
    placementState: TablePlacementState,
): TablePlacementState {
    val nx = placementState.wmOffsetXRatio.coerceIn(0, 100)
    val ny = placementState.wmOffsetYRatio.coerceIn(0, 100)
    val nx10000 = (nx * 100).coerceIn(0, 10000)
    val ny10000 = (ny * 100).coerceIn(0, 10000)
    context.dataStore.edit { prefs ->
        prefs[KEY_CAPTURE_ASPECT] = placementState.captureAspect.v
        prefs[KEY_WM_TABLE_ANCHOR] = 4
        prefs[KEY_WM_OFFSET_X] = nx
        prefs[KEY_WM_OFFSET_Y] = ny
        prefs[KEY_WM_BOUNDS_OFFSET_X_10000] = nx10000
        prefs[KEY_WM_BOUNDS_OFFSET_Y_10000] = ny10000
        prefs[KEY_WM_TABLE_WIDTH] = placementState.wmWidthRatio.coerceIn(10, 100)
        prefs[KEY_WM_TABLE_HEIGHT] = placementState.wmHeightRatio.coerceIn(10, 100)
        prefs[KEY_WM_ROTATION_CW_90] = if (placementState.rotationCwDeg == 90) 90 else 0
    }
    return placementState.copy(
        wmOffsetXRatio = nx,
        wmOffsetYRatio = ny,
        wmWidthRatio = placementState.wmWidthRatio.coerceIn(10, 100),
        wmHeightRatio = placementState.wmHeightRatio.coerceIn(10, 100),
        rotationCwDeg = if (placementState.rotationCwDeg == 90) 90 else 0,
    )
}
