package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import kotlinx.coroutines.flow.map

internal data class EditorCameraTablePrefs(
    val widthRatio: Int = 40,
    val heightRatio: Int = 20,
    val captureAspect: CaptureAspect = CaptureAspect.R3_4,
)

/** Read camera placement dimensions without modifying any persisted preferences. */
@Composable
internal fun rememberEditorCameraTablePrefs(): EditorCameraTablePrefs {
    val context = LocalContext.current
    val flow = remember(context) {
        context.dataStore.data.map { prefs ->
            EditorCameraTablePrefs(
                widthRatio = (prefs[KEY_WM_TABLE_WIDTH] ?: 40)
                    .coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO),
                heightRatio = (prefs[KEY_WM_TABLE_HEIGHT] ?: 20)
                    .coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO),
                captureAspect = CaptureAspect.from(prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v),
            )
        }
    }
    val state by flow.collectAsState(initial = EditorCameraTablePrefs())
    return state
}
