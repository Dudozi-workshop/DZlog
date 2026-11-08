package com.dudoziworkshop.dzlog.ui.table.rotating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/**
 * Used by an individual phrase's "N장" button and by multi-selection.
 * null period means follow the set default. The number field opens the
 * Android numeric keyboard only when touched (not on dialog creation).
 */
@Composable
internal fun RotatingPhrasePeriodDialog(
    initialEvery: Int?,
    defaultEvery: Int,
    targetLabel: String,
    selectedCount: Int,
    onApply: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var inherit by remember(targetLabel, selectedCount, initialEvery) {
        mutableStateOf(initialEvery == null)
    }
    var text by remember(targetLabel, selectedCount, initialEvery) {
        mutableStateOf(TextFieldValue((initialEvery ?: defaultEvery).coerceIn(1, 999999).toString()))
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val numeric = text.text.toIntOrNull()?.takeIf { it in 1..999999 }
    val effective = if (inherit) defaultEvery else numeric

    DDZAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedCount > 1) "변경 주기 일괄 설정" else "변경 주기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (selectedCount > 1) "선택한 ${selectedCount}개 문구" else targetLabel,
                    style = DDZTypography.Secondary,
                    color = DDZColor.TextSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DDZButton(
                        text = "−",
                        modifier = Modifier.width(48.dp),
                        minHeight = 44.dp,
                        horizontalPadding = 6.dp,
                        style = DDZButtonStyle.Secondary,
                        enabled = !inherit && numeric != null && numeric > 1,
                        onClick = { numeric?.let { text = TextFieldValue((it - 1).toString()) } },
                    )
                    OutlinedTextField(
                        value = if (inherit) TextFieldValue(defaultEvery.toString()) else text,
                        onValueChange = { next ->
                            if (next.text.length <= 6 && next.text.all(Char::isDigit)) text = next
                        },
                        modifier = Modifier.weight(1f).onFocusChanged { focus ->
                            if (focus.isFocused) text = text.copy(
                                selection = TextRange(0, text.text.length),
                            )
                        },
                        enabled = !inherit,
                        textStyle = DDZTypography.CardTitle.copy(textAlign = TextAlign.Center),
                        suffix = { Text("장") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        isError = !inherit && numeric == null,
                    )
                    DDZButton(
                        text = "+",
                        modifier = Modifier.width(48.dp),
                        minHeight = 44.dp,
                        horizontalPadding = 6.dp,
                        style = DDZButtonStyle.Secondary,
                        enabled = !inherit && numeric != null && numeric < 999999,
                        onClick = { numeric?.let { text = TextFieldValue((it + 1).toString()) } },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(
                        value = inherit,
                        role = Role.Checkbox,
                        onValueChange = { inherit = it },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = inherit, onCheckedChange = null)
                    Text(
                        "기본값 사용 (${defaultEvery}장)",
                        style = DDZTypography.Body,
                        color = DDZColor.TextPrimary,
                    )
                }
                if (!inherit && numeric == null) {
                    Text("1~999999 사이의 장수를 입력해 주세요.",
                        style = DDZTypography.Caption, color = DDZColor.Destructive)
                }
            }
        },
        confirmButton = {
            DDZDialogAction(
                enabled = inherit || numeric != null,
                onClick = {
                    if (inherit || effective != null) {
                        onApply(if (inherit) null else numeric)
                    }
                },
            ) { Text("적용") }
        },
        dismissButton = {
            DDZDialogAction(
                style = DDZButtonStyle.Secondary,
                onClick = onDismiss,
            ) { Text("취소") }
        },
    )
}
