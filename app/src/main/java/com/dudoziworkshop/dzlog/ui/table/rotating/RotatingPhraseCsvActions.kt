package com.dudoziworkshop.dzlog.ui.table.rotating

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseCsvCodec
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Storage Access Framework only: the user chooses the CSV file/destination.
 * Import never changes data until the preview's Replace action is confirmed.
 */
@Composable
internal fun RotatingPhraseCsvActions(
    phraseSet: RotatingPhraseSet,
    onReplaceItems: (List<String>, List<Int?>) -> Unit,
    showHeading: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestSet by rememberUpdatedState(phraseSet)
    val latestReplace by rememberUpdatedState(onReplaceItems)
    var pendingItems by remember(phraseSet.id) { mutableStateOf<RotatingPhraseCsvCodec.ImportResult.Success?>(null) }
    var working by remember(phraseSet.id) { mutableStateOf(false) }

    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                working = true
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val csv = RotatingPhraseCsvCodec.encode(latestSet.items, latestSet.itemEveryOverrides)
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: error("CSV 파일을 열 수 없습니다.")
                        output.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                    }
                }
                working = false
                Toast.makeText(
                    context,
                    if (result.isSuccess) "CSV 내보내기가 완료되었습니다." else "CSV 저장에 실패했습니다.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                working = true
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val input = context.contentResolver.openInputStream(uri)
                            ?: error("CSV 파일을 열 수 없습니다.")
                        val bytes = input.use { stream ->
                            // Cap reads before decoding: arbitrary user-selected files may be huge.
                            val buffer = ByteArray(1_000_001)
                            var size = 0
                            while (size < buffer.size) {
                                val read = stream.read(buffer, size, buffer.size - size)
                                if (read < 0) break
                                if (read == 0) continue
                                size += read
                            }
                            if (size > 1_000_000) error("파일 크기가 1 MB를 초과합니다.")
                            String(buffer, 0, size, Charsets.UTF_8)
                        }
                        RotatingPhraseCsvCodec.decode(bytes)
                    }.getOrElse {
                        RotatingPhraseCsvCodec.ImportResult.Failure(
                            it.message ?: "CSV 파일을 읽을 수 없습니다.",
                        )
                    }
                }
                working = false
                when (result) {
                    is RotatingPhraseCsvCodec.ImportResult.Success -> pendingItems = result
                    is RotatingPhraseCsvCodec.ImportResult.Failure ->
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showHeading) Text("CSV 일괄 관리", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DDZButton(
                text = "CSV 내보내기",
                onClick = {
                    val safeName = latestSet.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        .trim().take(40).ifBlank { "순환문구" }
                    exportPicker.launch(safeName + ".csv")
                },
                modifier = Modifier.weight(1f),
                enabled = !working,
                style = DDZButtonStyle.Secondary,
                horizontalPadding = 6.dp,
            )
            DDZButton(
                text = "CSV 불러오기",
                onClick = { importPicker.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f),
                enabled = !working,
                style = DDZButtonStyle.Secondary,
                horizontalPadding = 6.dp,
            )
        }
        Text("현재 세트 내보내기 · CSV 열: order, phrase, every (빈칸=기본)",
            style = DDZTypography.Caption, color = DDZColor.TextSecondary)
        Text("기존 2열 CSV도 불러올 수 있습니다. 세트 기본 주기·번호 방식은 유지됩니다.",
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
