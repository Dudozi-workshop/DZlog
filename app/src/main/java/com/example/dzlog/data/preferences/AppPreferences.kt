package com.example.dzlog.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dzlog.domain.model.CaptureAspect

val Context.dataStore by preferencesDataStore(name = "dzlog_prefs")

val KEY_COUNTER_DIGITS = intPreferencesKey("counter_digits")
val KEY_CAPTURE_ASPECT = intPreferencesKey("capture_aspect")
val KEY_SAVE_MODE = intPreferencesKey("save_mode")
val KEY_ORIENTATION_MODE = intPreferencesKey("orientation_mode")
val KEY_SHOW_WM_PREVIEW = intPreferencesKey("show_wm_preview")
val KEY_WM_TABLE_ANCHOR = intPreferencesKey("wm_table_anchor")
val KEY_WM_TABLE_WIDTH = intPreferencesKey("wm_table_width_ratio")
val KEY_WM_TABLE_HEIGHT = intPreferencesKey("wm_table_height_ratio")
val KEY_WM_OFFSET_X = intPreferencesKey("wm_offset_x_ratio")
val KEY_WM_OFFSET_Y = intPreferencesKey("wm_offset_y_ratio")
val KEY_WM_BG_ALPHA = intPreferencesKey("wm_bg_alpha")
val KEY_WM_LABEL_SCALE = intPreferencesKey("wm_label_scale")
val KEY_WM_VALUE_SCALE = intPreferencesKey("wm_value_scale")
val KEY_TABLE_TEMPLATE_JSON = stringPreferencesKey("table_template_json")
val KEY_USED_COUNTER_VALUES_JSON = stringPreferencesKey("used_counter_values_json")

enum class OrientationMode(val v: Int) {
    PORTRAIT_LOCK(0),
    AUTO_ROTATE(1);

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.v == v } ?: PORTRAIT_LOCK
    }
}

suspend fun persistCaptureAspect(context: Context, aspect: CaptureAspect) {
    runCatching {
        context.dataStore.edit { prefs ->
            prefs[KEY_CAPTURE_ASPECT] = aspect.v
        }
    }
}
