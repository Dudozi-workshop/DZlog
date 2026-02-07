package com.example.dzlog.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.example.dzlog.data.preferences.dataStore
import kotlinx.coroutines.flow.map

/**
 * 워터마크 표 배경 스타일(0=BLACK, 1=WHITE, 2=TRANSPARENT)을 DataStore에서 구독한다.
 * - 표 상세설정 변경이 Home/Settings 프리뷰에 즉시 반영되게 하기 위한 공통 훅.
 */
@Composable
fun rememberWmBgStyle(): Int {
    val context = LocalContext.current
    val flow = remember {
        context.dataStore.data.map { prefs ->
            (prefs[KEY_WM_TABLE_BG_STYLE] ?: 0).coerceIn(0, 2)
        }
    }
    val v by flow.collectAsState(initial = 0)
    return v
}

