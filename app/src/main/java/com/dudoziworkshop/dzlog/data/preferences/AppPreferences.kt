package com.dudoziworkshop.dzlog.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect

val Context.dataStore by preferencesDataStore(name = "dzlog_prefs")

val KEY_COUNTER_DIGITS = intPreferencesKey("counter_digits")
val KEY_CAPTURE_ASPECT = intPreferencesKey("capture_aspect")
val KEY_SAVE_MODE = intPreferencesKey("save_mode")
val KEY_PHOTO_QUALITY_MODE = intPreferencesKey("photo_quality_mode")
val KEY_ORIENTATION_MODE = intPreferencesKey("orientation_mode")
val KEY_SHOW_WM_PREVIEW = intPreferencesKey("show_wm_preview")
val KEY_WM_TABLE_ANCHOR = intPreferencesKey("wm_table_anchor")
val KEY_WM_TABLE_WIDTH = intPreferencesKey("wm_table_width_ratio")
val KEY_WM_TABLE_HEIGHT = intPreferencesKey("wm_table_height_ratio")
val KEY_WM_OFFSET_X = intPreferencesKey("wm_offset_x_ratio")
val KEY_WM_OFFSET_Y = intPreferencesKey("wm_offset_y_ratio")
val KEY_WM_BOUNDS_OFFSET_X_10000 = intPreferencesKey("wm_bounds_offset_x_10000")
val KEY_WM_BOUNDS_OFFSET_Y_10000 = intPreferencesKey("wm_bounds_offset_y_10000")
val KEY_WM_BG_ALPHA = intPreferencesKey("wm_bg_alpha")
val KEY_WM_VALUE_SCALE = intPreferencesKey("wm_value_scale")
val KEY_WM_TEXT_COLOR_MODE = intPreferencesKey("wm_text_color_mode")
val KEY_WM_TEXT_COLOR_MANUAL = intPreferencesKey("wm_text_color_manual")
val KEY_WM_TEXT_ALIGN = intPreferencesKey("wm_text_align")
val KEY_WM_ROTATION_CW_90 = intPreferencesKey("wm_rotation_cw_90")
val KEY_WM_GRID_ENABLED = booleanPreferencesKey("wm_grid_enabled")
val KEY_TABLE_DETAIL_GRID_ENABLED = booleanPreferencesKey("table_detail_grid_enabled")
val KEY_TABLE_TEMPLATE_JSON = stringPreferencesKey("table_template_json")
val KEY_TABLE_TEMPLATES_JSON = stringPreferencesKey("table_templates_json_v1")
val KEY_ACTIVE_TABLE_TEMPLATE_ID = stringPreferencesKey("active_table_template_id_v1")

val KEY_COUNTER_MANUAL_NEXT_OVERRIDES_V1 = stringPreferencesKey("counter_manual_next_overrides_v1")

val KEY_CONTINUOUS_PREVIEW_MODE = intPreferencesKey("continuous_preview_mode")
val KEY_CAMERA_GRID_ON = booleanPreferencesKey("camera_grid_on")
val KEY_CAMERA_ZOOM_TENTHS = intPreferencesKey("camera_zoom_tenths")
val KEY_CAMERA_FLASH_MODE = intPreferencesKey("camera_flash_mode")
val KEY_INCLUDE_PATH_IN_COUNTER_SCOPE = booleanPreferencesKey("include_path_in_counter_scope")
val KEY_INCLUDE_FILENAME_IN_COUNTER_SCOPE = booleanPreferencesKey("include_filename_in_counter_scope")
val KEY_TOAST_ENABLED = booleanPreferencesKey("toast_enabled")
val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
val KEY_CAPTURE_HAPTIC_ENABLED = booleanPreferencesKey("capture_haptic_enabled")
val KEY_CAPTURE_SOUND_ENABLED = booleanPreferencesKey("capture_sound_enabled")
val KEY_VOLUME_KEY_ACTION = intPreferencesKey("volume_key_action")
val KEY_BLANK_WARNING_ENABLED = booleanPreferencesKey("blank_warning_enabled")
val KEY_ASSIST_SHUTTER_ENABLED = booleanPreferencesKey("assist_shutter_enabled")
val KEY_ASSIST_SHUTTER_X_RATIO = floatPreferencesKey("assist_shutter_x_ratio")
val KEY_ASSIST_SHUTTER_Y_RATIO = floatPreferencesKey("assist_shutter_y_ratio")


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
