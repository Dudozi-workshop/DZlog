package com.dudoziworkshop.dzlog.ui.table.rotating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseCsvCodec
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

internal class PhraseCsvFiles(
    val open: () -> Unit,
    val export: (name: String, csv: String) -> Unit,
)

@Composable
internal expect fun rememberPhraseCsvFiles(
    onRead: (String) -> Unit,
    onFinished: (String?) -> Unit,
): PhraseCsvFiles

@Composable
internal fun RotatingPhraseCsvActions(
    phraseSet: RotatingPhraseSet,
    onReplaceItems: (List<String>, List<Int?>) -> Unit,
    showHeading: Boolean = true,
) {
    val latestSet by rememberUpdatedState(phraseSet)
    val latestReplace by rememberUpdatedState(onReplaceItems)
    var pendingItems by remember(phraseSet.id) { mutableStateOf<RotatingPhraseCsvCodec.ImportResult.Success?>(null) }
    var working by remember(phraseSet.id) { mutableStateOf(false) }
    var message by remember(phraseSet.id) { mutableStateOf<String?>(null) }
    val files = rememberPhraseCsvFiles(
        onRead = { csv ->
            working = false
            when (val result = RotatingPhraseCsvCodec.decode(csv)) {
                is RotatingPhraseCsvCodec.ImportResult.Success -> pendingItems = result
                is RotatingPhraseCsvCodec.ImportResult.Failure -> message = result.message
            }
        },
        onFinished = { working = false; message = it },
    )

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showHeading) Text("CSV 일괄 관리", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DDZButton(
                text = "CSV 내보내기",
                onClick = {
                    val safeName = latestSet.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        .trim().take(40).ifBlank { "순환문구" }
                    message = null
                    working = true
                    files.export(safeName + ".csv", RotatingPhraseCsvCodec.encode(latestSet.items, latestSet.itemEveryOverrides))
                },
                modifier = Modifier.weight(1f),
                enabled = !working,
                style = DDZButtonStyle.Secondary,
                horizontalPadding = 6.dp,
            )
            DDZButton(
                text = "CSV 불러오기",
                onClick = { message = null; working = true; files.open() },
                modifier = Modifier.weight(1f),
                enabled = !working,
                style = DDZButtonStyle.Secondary,
                horizontalPadding = 6.dp,
            )
        }
        message?.let { Text(it, style = DDZTypography.Caption) }
        Text("현재 세트 내보내기 · CSV 열: order, phrase, every (빈칸=기본)",
            style = DDZTypography.Caption, color = DDZColor.TextSecondary)
    }

    val preview = pendingItems
    if (preview != null) {
        DDZAlertDialog(
            onDismissRequest = { pendingItems = null },
            title = { Text("문구 목록 교체") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("현재 " + phraseSet.items.size + "개 → 가져온 " + preview.items.size + "개")
                    preview.items.take(3).forEachIndexed { index, item ->
                        val every = preview.itemEveryOverrides.getOrNull(index)
                        Text((index + 1).toString() + ". " + item + " · " +
                            (every?.let { it.toString() + "장" } ?: "기본"),
                            style = DDZTypography.Secondary, color = DDZColor.TextPrimary)
                    }
                    if (preview.items.size > 3) {
                        Text("외 " + (preview.items.size - 3) + "개",
                            style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                    }
                    Text("이 세트를 사용하는 다른 셀에도 변경이 적용되며, 문구 진행 상태가 초기화될 수 있습니다. 표 편집에서 저장해야 확정됩니다.",
                        style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                }
            },
            confirmButton = {
                DDZDialogAction(onClick = {
                    latestReplace(preview.items, preview.itemEveryOverrides)
                    pendingItems = null
                }) { Text("목록 교체") }
            },
            dismissButton = {
                DDZDialogAction(onClick = { pendingItems = null },
                    style = DDZButtonStyle.Secondary) { Text("취소") }
            },
        )
    }
}
