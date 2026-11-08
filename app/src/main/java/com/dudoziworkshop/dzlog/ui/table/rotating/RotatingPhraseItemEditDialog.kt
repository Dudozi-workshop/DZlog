package com.dudoziworkshop.dzlog.ui.table.rotating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/**
 * Name-only input. Adding phrases always inherits the set period.
 * With continuous add, save clears the field while keeping dialog/keyboard.
 */
@Composable
internal fun RotatingPhraseItemEditDialog(
    initialText: String,
    isEdit: Boolean,
    continuousAdd: Boolean,
    canAdd: Boolean,
    onContinuousAddChange: (Boolean) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var phraseText by remember(initialText, isEdit) { mutableStateOf(initialText) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val valid = phraseText.isNotBlank() && phraseText.trim().length <= 500 && (isEdit || canAdd)

    DDZAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "문구명 수정" else "문구 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phraseText,
                    onValueChange = { if (it.length <= 500) phraseText = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    singleLine = true,
                    label = { Text("문구") },
                )
                if (!isEdit) {
                    if (!canAdd) Text("문구는 최대 1,000개까지 추가할 수 있습니다.")
                    Row(
                        modifier = Modifier.fillMaxWidth().toggleable(
                            value = continuousAdd,
                            role = Role.Checkbox,
                            onValueChange = onContinuousAddChange,
                        ),
                    ) {
                        Checkbox(checked = continuousAdd, onCheckedChange = null)
                        Text("연속 추가하기", style = DDZTypography.Body)
                    }
                }
            }
        },
        confirmButton = {
            DDZDialogAction(
                enabled = valid,
                onClick = {
                    if (valid) {
                        onSave(phraseText.trim())
                        if (isEdit || !continuousAdd) {
                            onDismiss()
                        } else {
                            phraseText = ""
                            focus.requestFocus()
                            keyboard?.show()
                        }
                    }
                },
            ) { Text(if (isEdit) "적용" else "추가") }
        },
        dismissButton = {
            DDZDialogAction(
                style = DDZButtonStyle.Secondary,
                onClick = onDismiss,
            ) { Text(if (isEdit) "취소" else "닫기") }
        },
    )
}
