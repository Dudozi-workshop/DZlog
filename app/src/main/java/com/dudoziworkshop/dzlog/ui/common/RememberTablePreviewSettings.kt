package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_DETAIL_GRID_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_ALIGN
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MANUAL
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.model.WatermarkManualTextColor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode
import kotlinx.coroutines.flow.map

/**
 * 표 프리뷰에 필요한 “표 스타일 설정 묶음” 상태.
 *
 * 홈 프리뷰 정책:
 * - 촬영 화면과 동일한 표 스타일 경로를 사용한다.
 * - 단, 배치(회전/앵커/오프셋/크기비율)는 홈 전용 고정 정책을 유지한다.
 */
data class TablePreviewSettingsState(
    val wmBgStyle: Int,
    val wmBgAlpha: Int,
    val wmValueScale: Int,
    val wmTextColorMode: Int,
    val wmManualTextColor: Int,
    val wmTextAlign: Int,
    val tableDetailGridEnabled: Boolean,
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
            val textColorMode = (prefs[KEY_WM_TEXT_COLOR_MODE] ?: WatermarkTextColorMode.AUTO)
                .coerceIn(WatermarkTextColorMode.AUTO, WatermarkTextColorMode.MANUAL)
            val manualTextColor = (prefs[KEY_WM_TEXT_COLOR_MANUAL] ?: WatermarkManualTextColor.BLACK)
                .coerceIn(WatermarkManualTextColor.WHITE, WatermarkManualTextColor.BLACK)
            val textAlign = (prefs[KEY_WM_TEXT_ALIGN] ?: WatermarkTextAlign.LEFT)
                .coerceIn(WatermarkTextAlign.LEFT, WatermarkTextAlign.RIGHT)
            // 주요 정책: 홈 표 프리뷰 grid는 표 상세설정 전용 키만 사용한다. (촬영 워터마크 grid와 분리)
            val gridEnabled = prefs[KEY_TABLE_DETAIL_GRID_ENABLED] ?: true
            TablePreviewSettingsState(
                wmBgStyle = bgStyle,
                wmBgAlpha = bgAlpha,
                wmValueScale = valueScale,
                wmTextColorMode = textColorMode,
                wmManualTextColor = manualTextColor,
                wmTextAlign = textAlign,
                tableDetailGridEnabled = gridEnabled,
            )
        }
    }

    val state by flow.collectAsState(
        initial = TablePreviewSettingsState(
            wmBgStyle = 0,
            wmBgAlpha = 80,
            wmValueScale = 100,
            wmTextColorMode = WatermarkTextColorMode.AUTO,
            wmManualTextColor = WatermarkManualTextColor.BLACK,
            wmTextAlign = WatermarkTextAlign.LEFT,
            tableDetailGridEnabled = true,
        )
    )
    return state
}
