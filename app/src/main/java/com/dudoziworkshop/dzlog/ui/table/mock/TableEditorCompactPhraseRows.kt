package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZTextField
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun TableEditorCompactSettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
            .clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = DDZTypography.SettingLabel, modifier = Modifier.weight(1f),
            color = DDZColor.TextPrimary, maxLines = 1)
        Text(value, style = DDZTypography.Secondary, modifier = Modifier.weight(1.3f),
            color = DDZColor.TextSecondary, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
        Icon(Icons.Filled.ChevronRight, contentDescription = null,
            modifier = Modifier.size(18.dp), tint = DDZColor.TextSecondary)
    }
}

@Composable
internal fun TableEditorPhrasePeriodRow(
    every: Int,
    overridden: Boolean,
    onEveryChange: (Int) -> Unit,
    onFollowSet: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var input by remember(every, editing) { mutableStateOf(every.toString()) }
    val parsed = input.toIntOrNull()?.takeIf { it in 1..RotatingPhraseSet.MAX_EVERY }

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("변경 주기", style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
            Text(
                if (overridden) "셀 개별 설정" else "세트 적용",
                style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                modifier = Modifier.clickable {
                    if (overridden) onFollowSet() else onEveryChange(every)
                },
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, DDZColor.Border),
            color = DDZColor.Surface,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onEveryChange((every - 1).coerceAtLeast(1)) },
                    enabled = every > 1, modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Filled.Remove, "주기 감소", Modifier.size(18.dp))
                }
                Surface(onClick = { editing = true }, color = DDZColor.Surface) {
                    Text(
                        "${every}장", style = DDZTypography.Secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        color = DDZColor.TextPrimary,
                    )
                }
                IconButton(
                    onClick = { onEveryChange((every + 1).coerceAtMost(RotatingPhraseSet.MAX_EVERY)) },
                    enabled = every < RotatingPhraseSet.MAX_EVERY,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Filled.Add, "주기 증가", Modifier.size(18.dp))
                }
            }
        }
    }

    if (editing) {
        DDZAlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("변경 주기 입력") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DDZTextField(
                        value = input,
                        onValueChange = { value ->
                            if (value.length <= 10 && value.all { it in '0'..'9' }) input = value
                        },
                        label = "촬영 장수 (1~${RotatingPhraseSet.MAX_EVERY})",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (parsed == null) Text(
                        "1~${RotatingPhraseSet.MAX_EVERY} 사이의 장수를 입력하세요.",
                        style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                    )
                }
            },
            dismissButton = {
                DDZDialogAction(onClick = { editing = false }, style = DDZButtonStyle.Secondary) {
                    Text("취소")
                }
            },
            confirmButton = {
                DDZDialogAction(enabled = parsed != null, onClick = {
                    parsed?.let(onEveryChange)
                    editing = false
                }) { Text("적용") }
            },
        )
    }
}
