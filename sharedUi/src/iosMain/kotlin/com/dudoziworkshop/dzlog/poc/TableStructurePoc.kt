package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.table.policy.TableEditorPolicy
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.table.mock.MockLayoutPanel
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Callback/availability fixture only. The Android structure engine is not duplicated here. */
@Composable
internal fun TableStructurePoc() {
    var selectedCount by remember { mutableStateOf(0) }
    var merged by remember { mutableStateOf(false) }
    var atLimit by remember { mutableStateOf(false) }
    var lastRequest by remember { mutableStateOf("없음") }
    Column(Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding()) {
        DDZTopBar(title = "구조 확인 화면")
        Text("버튼 연결 확인용 · 실제 표는 변경되지 않습니다.", Modifier.padding(16.dp),
            style = DDZTypography.Caption, color = DDZColor.TextMuted)
        MockLayoutPanel(
            modifier = Modifier.weight(1f), selectedCount = selectedCount,
            rows = if (atLimit) TableEditorPolicy.MAX_ROWS else 3,
            cols = if (atLimit) TableEditorPolicy.MAX_COLS else 3, mergedSelection = merged,
            onAddRow = { lastRequest = "행 추가" }, onAddCol = { lastRequest = "열 추가" },
            onMergeSelection = { lastRequest = "병합 전환" },
            onDeleteRows = { lastRequest = "행 삭제" }, onDeleteColumns = { lastRequest = "열 삭제" },
            onEqualizeColumns = { lastRequest = "열 너비 균등" },
            onEqualizeRows = { lastRequest = "행 높이 균등" },
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DDZButton(text = "두 셀 선택", onClick = { selectedCount = 2; merged = false; lastRequest = "없음" })
            DDZButton(text = "병합 셀 선택", onClick = { selectedCount = 1; merged = true; lastRequest = "없음" })
            DDZButton(text = if (atLimit) "기본 크기 보기" else "한계 상태 보기",
                onClick = { atLimit = !atLimit; selectedCount = 0; merged = false; lastRequest = "없음" })
            Text("마지막 요청: $lastRequest", style = DDZTypography.Caption, color = DDZColor.TextMuted)
        }
    }
}
