package com.example.dzlog.feature.table.policy

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.example.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.example.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.example.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.example.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.example.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.example.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.data.template.toJsonString
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor

suspend fun saveTableTemplate(context: Context, templateState: TableTemplateState): Result<Unit> = runCatching {
    context.dataStore.edit { prefs ->
        prefs[KEY_TABLE_TEMPLATE_JSON] = templateState.toJsonString()
    }
}

private suspend fun persistWatermarkAnchor(context: Context, anchor: WatermarkTableAnchor) {
    context.dataStore.edit { prefs ->
        prefs[KEY_WM_TABLE_ANCHOR] = when (anchor) {
            WatermarkTableAnchor.TOP_LEFT -> 0
            WatermarkTableAnchor.TOP_RIGHT -> 1
            WatermarkTableAnchor.BOTTOM_LEFT -> 2
            WatermarkTableAnchor.BOTTOM_RIGHT -> 3
            WatermarkTableAnchor.CUSTOM -> 4
        }
    }
}

private suspend fun persistWatermarkWidthRatio(context: Context, widthRatio: Int) {
    context.dataStore.edit { it[KEY_WM_TABLE_WIDTH] = widthRatio }
}

private suspend fun persistWatermarkHeightRatio(context: Context, heightRatio: Int) {
    context.dataStore.edit { it[KEY_WM_TABLE_HEIGHT] = heightRatio }
}

private suspend fun persistWatermarkBgStyle(context: Context, bgStyle: Int) {
    context.dataStore.edit { it[KEY_WM_TABLE_BG_STYLE] = bgStyle }
}

private suspend fun persistWatermarkBgAlpha(context: Context, bgAlpha: Int) {
    context.dataStore.edit { it[KEY_WM_BG_ALPHA] = bgAlpha }
}

private suspend fun persistWatermarkValueScale(context: Context, valueScale: Int) {
    context.dataStore.edit { it[KEY_WM_VALUE_SCALE] = valueScale }
}

sealed interface TableWatermarkAction {
    data class AnchorChanged(val anchor: WatermarkTableAnchor) : TableWatermarkAction
    data class WidthRatioChanged(val width: Int) : TableWatermarkAction
    data class HeightRatioChanged(val height: Int) : TableWatermarkAction
    data class BgStyleChanged(val bgStyle: Int) : TableWatermarkAction
    data class BgAlphaChanged(val alpha: Int) : TableWatermarkAction
    data class ValueScaleChanged(val scale: Int) : TableWatermarkAction
}

data class TableWatermarkStatePatch(
    val anchor: WatermarkTableAnchor? = null,
    val widthRatio: Int? = null,
    val heightRatio: Int? = null,
    val bgStyle: Int? = null,
    val bgAlpha: Int? = null,
    val valueScale: Int? = null
)

suspend fun applyTableWatermarkAction(
    context: Context,
    action: TableWatermarkAction
): TableWatermarkStatePatch {
    return when (action) {
        is TableWatermarkAction.AnchorChanged -> {
            persistWatermarkAnchor(context, action.anchor)
            TableWatermarkStatePatch(anchor = action.anchor)
        }

        is TableWatermarkAction.WidthRatioChanged -> {
            val normalized = action.width.coerceIn(40, 100)
            persistWatermarkWidthRatio(context, normalized)
            TableWatermarkStatePatch(widthRatio = normalized)
        }

        is TableWatermarkAction.HeightRatioChanged -> {
            val normalized = action.height.coerceIn(10, 35)
            persistWatermarkHeightRatio(context, normalized)
            TableWatermarkStatePatch(heightRatio = normalized)
        }

        is TableWatermarkAction.BgStyleChanged -> {
            persistWatermarkBgStyle(context, action.bgStyle)
            TableWatermarkStatePatch(bgStyle = action.bgStyle)
        }

        is TableWatermarkAction.BgAlphaChanged -> {
            val normalized = action.alpha.coerceIn(0, 255)
            persistWatermarkBgAlpha(context, normalized)
            TableWatermarkStatePatch(bgAlpha = normalized)
        }

        is TableWatermarkAction.ValueScaleChanged -> {
            val normalized = action.scale.coerceIn(60, 160)
            persistWatermarkValueScale(context, normalized)
            TableWatermarkStatePatch(valueScale = normalized)
        }
    }
}
