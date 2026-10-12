package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.common.DDZTextField

@Composable
internal fun TableEditorPhraseProgressDialog(
    cell: TableEditorCellUiModel,
    set: RotatingPhraseSet,
    onApply: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val every = (cell.everyOverride ?: set.defaultEvery).coerceAtLeast(1)
    var index by remember(cell.id, set.items) {
        mutableStateOf(cell.phraseIndex.coerceIn(0, set.items.lastIndex))
    }
    var completedText by remember(cell.id, every) {
        mutableStateOf(cell.phraseCompletedShots.coerceIn(0, every - 1).toString())
    }
    val completed = completedText.toIntOrNull()?.takeIf { it in 0 until every }
    DDZAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("순환 진행 조정") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("다음 촬영 문구 · 선택하면 0장부터 시작")
                set.items.forEachIndexed { itemIndex, item ->
                    DDZButton(text = item, modifier = Modifier.fillMaxWidth(),
                        style = if (index == itemIndex) DDZButtonStyle.Primary else DDZButtonStyle.Secondary,
                        onClick = { index = itemIndex; completedText = "0" })
                }
                Text("현재 문구로 저장 완료한 장수 · 0~${every - 1}")
                DDZTextField(value = completedText,
                    onValueChange = { value ->
                        if (value.length <= 10 && value.all { it in '0'..'9' }) completedText = value
                    }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                if (completed == null) Text("0~${every - 1} 사이의 장수를 입력하세요.")
                DDZButton(text = "처음부터 시작 · ${set.items.first()}", style = DDZButtonStyle.Secondary,
                    onClick = { index = 0; completedText = "0" })
                Text("표 편집 화면에서 저장하면 반영됩니다. 자동번호는 기존 저장 규칙을 따릅니다.")
            }
        },
        dismissButton = {
            DDZDialogAction(onClick = onDismiss, style = DDZButtonStyle.Secondary) { Text("취소") }
        },
        confirmButton = {
            DDZDialogAction(enabled = completed != null, onClick = {
                completed?.let { onApply(index, it); onDismiss() }
            }) { Text("적용") }
        },
    )
}
