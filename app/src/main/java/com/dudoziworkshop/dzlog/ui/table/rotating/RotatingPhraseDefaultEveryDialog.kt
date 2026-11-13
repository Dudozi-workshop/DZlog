package com.dudoziworkshop.dzlog.ui.table.rotating

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction

/** Direct numeric entry for the set-wide default cycle, preserving the compact bar. */
@Composable
internal fun RotatingPhraseDefaultEveryDialog(
    initialEvery: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit,
) {
    var draft by remember(initialEvery) {
        mutableStateOf(TextFieldValue(initialEvery.toString(), TextRange(0, initialEvery.toString().length)))
    }
    val value = draft.text.toIntOrNull()?.takeIf { it in 1..RotatingPhraseSet.MAX_EVERY }
    val requester = remember { FocusRequester() }
    DDZAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("기본 변경 주기") },
        text = {
            Column {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.text.length <= 6 && it.text.all(Char::isDigit)) draft = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(requester),
                    label = { Text("변경 주기 (장)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = value == null,
                )
                if (value == null) Text("1~999999장 사이로 입력해 주세요.")
            }
        },
        dismissButton = {
            DDZDialogAction(onClick = onDismiss, style = DDZButtonStyle.Secondary) { Text("취소") }
        },
        confirmButton = {
            DDZDialogAction(enabled = value != null, onClick = { value?.let(onApply) }) { Text("적용") }
        },
    )
    LaunchedEffect(requester) { requester.requestFocus() }
}
