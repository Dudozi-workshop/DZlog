package com.dudoziworkshop.dzlog.ui.table.rotating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Edit the phrase and its optional period as a single atomic change. */
@Composable
internal fun RotatingPhraseItemEditDialog(
    initialText: String,
    initialEvery: Int?,
    defaultEvery: Int,
    isEdit: Boolean,
    onSave: (String, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var phraseText by remember(initialText, initialEvery) { mutableStateOf(initialText) }
    var inherit by remember(initialText, initialEvery) { mutableStateOf(initialEvery == null) }
    var periodText by remember(initialText, initialEvery) {
        mutableStateOf((initialEvery ?: defaultEvery).coerceAtLeast(1).toString())
    }
    val interval = periodText.toIntOrNull()?.takeIf { it in 1..999999 }
    val canSave = phraseText.isNotBlank() && phraseText.trim().length <= 500 &&
        (inherit || interval != null)

    DDZAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "문구 수정" else "문구 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = phraseText,
                    onValueChange = { if (it.length <= 500) phraseText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("문구") },
                )
                Text("변경 주기", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DDZButton(
                        text = "기본값 사용",
                        modifier = Modifier.weight(1f),
                        style = DDZButtonStyle.Secondary,
                        containerColorOverride = if (inherit) DDZColor.SelectedSoft else null,
                        horizontalPadding = 4.dp,
                        onClick = { inherit = true },
                    )
                    DDZButton(
                        text = "개별 지정",
                        modifier = Modifier.weight(1f),
                        style = DDZButtonStyle.Secondary,
                        containerColorOverride = if (!inherit) DDZColor.SelectedSoft else null,
                        horizontalPadding = 4.dp,
                        onClick = { inherit = false },
                    )
                }
                if (inherit) {
                    Text("이 세트의 기본 주기 " + defaultEvery + "장마다 적용",
                        style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                } else {
                    OutlinedTextField(
                        value = periodText,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) periodText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("몇 장마다 다음 문구로 변경할까요?") },
                        suffix = { Text("장") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = interval == null,
                    )
                }
            }
        },
        confirmButton = {
            DDZDialogAction(
                style = DDZButtonStyle.Primary,
                enabled = canSave,
                onClick = { if (canSave) onSave(phraseText.trim(), if (inherit) null else interval) },
            ) { Text("적용", style = DDZTypography.ButtonText) }
        },
        dismissButton = {
            DDZDialogAction(style = DDZButtonStyle.Secondary, onClick = onDismiss) {
                Text("취소", style = DDZTypography.ButtonText)
            }
        },
    )
}
