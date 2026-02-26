package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import kotlinx.coroutines.flow.map

/**
 * 표 프리뷰에 필요한 “표 설정 묶음” 상태.
 *
 * 라벨은 기획 폐기(값만 표시) 전제이므로 label 관련 필드는 포함하지 않음.
 * - wmBgStyle: 0=BLACK, 1=WHITE, 2=TRANSPARENT
 * - wmBgAlpha: 0~255
 * - wmValueScale: 60~160 (기본 100)
 */
data class TablePreviewSettingsState(
    val wmBgStyle: Int,
    val wmBgAlpha: Int,
    val wmValueScale: Int,
)

/**
 * 표 프리뷰 설정을 DataStore에서 “구독”한다.
 * - 설정 변경이 Home/Settings 프리뷰에 즉시 반영되게 하기 위한 공통 훅.
 */
@Composable
fun rememberTablePreviewSettings(): TablePreviewSettingsState {
    val context = LocalContext.current
    val flow = remember {
        context.dataStore.data.map { prefs ->
            val bgStyle = (prefs[KEY_WM_TABLE_BG_STYLE] ?: 0).coerceIn(0, 2)
            val bgAlpha = (prefs[KEY_WM_BG_ALPHA] ?: 80).coerceIn(0, 255)
            val valueScale = (prefs[KEY_WM_VALUE_SCALE] ?: 100).coerceIn(60, 160)
            TablePreviewSettingsState(
                wmBgStyle = bgStyle,
                wmBgAlpha = bgAlpha,
                wmValueScale = valueScale,
            )
        }
    }

    val state by flow.collectAsState(
        initial = TablePreviewSettingsState(
            wmBgStyle = 0,
            wmBgAlpha = 80,
            wmValueScale = 100,
        )
    )
    return state
}
