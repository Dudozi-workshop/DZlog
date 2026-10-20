package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.table.mock.TableEditorStylePanel
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Isolated migration fixture. Draft stays in memory; no template/settings writes. */
@Composable
internal fun TableStylePoc() {
    var draft by remember { mutableStateOf(TableStyleState()) }
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding()) {
        if (open) {
            DDZTopBar(title = "표 스타일", onBack = { open = false })
            TableEditorStylePanel(draft = draft, onDraftChange = { draft = it },
                modifier = Modifier.weight(1f))
        } else {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("스타일 확인 화면", style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary)
                Text("임시 설정이며 저장되지 않습니다.", style = DDZTypography.Body, color = DDZColor.TextSecondary)
                DDZButton(text = "표 스타일 열기", onClick = { open = true })
            }
        }
        // Fixture observation outside the original panel, not a new product UI element.
        Text("임시 상태: 배경 ${draft.bgStyle} / 정렬 ${draft.textAlign}",
            modifier = Modifier.padding(16.dp), style = DDZTypography.Caption, color = DDZColor.TextMuted)
    }
}
