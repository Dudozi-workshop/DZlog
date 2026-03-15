package com.dudoziworkshop.dzlog.feature.table.state

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_DETAIL_GRID_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_GRID_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_ALIGN
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MANUAL
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.model.WatermarkManualTextColor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlinx.coroutines.flow.first

suspend fun loadTableStyleState(context: Context): TableStyleState {
    val prefs = context.dataStore.data.first()
    return TableStyleState(
        bgStyle = (prefs[KEY_WM_TABLE_BG_STYLE] ?: 0).coerceIn(0, 2),
        bgAlpha = (prefs[KEY_WM_BG_ALPHA] ?: 80).coerceIn(0, 255),
        valueScale = (prefs[KEY_WM_VALUE_SCALE] ?: 100).coerceIn(60, 160),
        textColorMode = (prefs[KEY_WM_TEXT_COLOR_MODE] ?: WatermarkTextColorMode.AUTO).coerceIn(
            WatermarkTextColorMode.AUTO,
            WatermarkTextColorMode.MANUAL,
        ),
        manualTextColor = (prefs[KEY_WM_TEXT_COLOR_MANUAL] ?: WatermarkManualTextColor.BLACK).coerceIn(
            WatermarkManualTextColor.WHITE,
            WatermarkManualTextColor.BLACK,
        ),
        textAlign = (prefs[KEY_WM_TEXT_ALIGN] ?: WatermarkTextAlign.LEFT).coerceIn(
            WatermarkTextAlign.LEFT,
            WatermarkTextAlign.RIGHT,
        ),
        gridEnabled = if (prefs.contains(KEY_TABLE_DETAIL_GRID_ENABLED)) {
            prefs[KEY_TABLE_DETAIL_GRID_ENABLED] ?: true
        } else {
            prefs[KEY_WM_GRID_ENABLED] ?: true
        },
    )
}

suspend fun applyBgStyleChange(context: Context, bgStyle: Int, current: TableStyleState): TableStyleState {
    val normalized = bgStyle.coerceIn(0, 2)
    context.dataStore.edit { it[KEY_WM_TABLE_BG_STYLE] = normalized }
    return current.copy(bgStyle = normalized)
}

suspend fun applyBgAlphaChange(context: Context, alpha: Int, current: TableStyleState): TableStyleState {
    val normalized = alpha.coerceIn(0, 255)
    context.dataStore.edit { it[KEY_WM_BG_ALPHA] = normalized }
    return current.copy(bgAlpha = normalized)
}

suspend fun applyValueScaleChange(context: Context, scale: Int, current: TableStyleState): TableStyleState {
    val normalized = scale.coerceIn(60, 160)
    context.dataStore.edit { it[KEY_WM_VALUE_SCALE] = normalized }
    return current.copy(valueScale = normalized)
}

suspend fun applyTextColorModeChange(context: Context, mode: Int, current: TableStyleState): TableStyleState {
    val normalized = mode.coerceIn(WatermarkTextColorMode.AUTO, WatermarkTextColorMode.MANUAL)
    context.dataStore.edit { it[KEY_WM_TEXT_COLOR_MODE] = normalized }
    return current.copy(textColorMode = normalized)
}

suspend fun applyManualTextColorChange(context: Context, color: Int, current: TableStyleState): TableStyleState {
    val normalized = color.coerceIn(WatermarkManualTextColor.WHITE, WatermarkManualTextColor.BLACK)
    context.dataStore.edit { it[KEY_WM_TEXT_COLOR_MANUAL] = normalized }
    return current.copy(manualTextColor = normalized)
}

suspend fun applyTextAlignChange(context: Context, align: Int, current: TableStyleState): TableStyleState {
    val normalized = align.coerceIn(WatermarkTextAlign.LEFT, WatermarkTextAlign.RIGHT)
    context.dataStore.edit { it[KEY_WM_TEXT_ALIGN] = normalized }
    return current.copy(textAlign = normalized)
}

suspend fun applyGridEnabledChange(context: Context, enabled: Boolean, current: TableStyleState): TableStyleState {
    context.dataStore.edit { it[KEY_TABLE_DETAIL_GRID_ENABLED] = enabled }
    return current.copy(gridEnabled = enabled)
}


suspend fun persistTableStyleState(context: Context, style: TableStyleState) {
    context.dataStore.edit {
        it[KEY_WM_TABLE_BG_STYLE] = style.bgStyle.coerceIn(0, 2)
        it[KEY_WM_BG_ALPHA] = style.bgAlpha.coerceIn(0, 255)
        it[KEY_WM_VALUE_SCALE] = style.valueScale.coerceIn(60, 160)
        it[KEY_WM_TEXT_COLOR_MODE] = style.textColorMode.coerceIn(
            WatermarkTextColorMode.AUTO,
            WatermarkTextColorMode.MANUAL,
        )
        it[KEY_WM_TEXT_COLOR_MANUAL] = style.manualTextColor.coerceIn(
            WatermarkManualTextColor.WHITE,
            WatermarkManualTextColor.BLACK,
        )
        it[KEY_WM_TEXT_ALIGN] = style.textAlign.coerceIn(
            WatermarkTextAlign.LEFT,
            WatermarkTextAlign.RIGHT,
        )
        it[KEY_TABLE_DETAIL_GRID_ENABLED] = style.gridEnabled
    }
}
