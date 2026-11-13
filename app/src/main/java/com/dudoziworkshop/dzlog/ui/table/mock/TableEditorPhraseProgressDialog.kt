package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZTextField
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun TableEditorPhraseProgressDialog(
    cell: TableEditorCellUiModel,
    set: RotatingPhraseSet,
    onApply: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (set.items.isEmpty()) return
    var index by remember(cell.domainCellId, set.id) {
        mutableStateOf(cell.phraseIndex.coerceIn(0, set.items.lastIndex))
    }
    val every = (cell.everyOverride ?: set.everyForItem(index)).coerceAtLeast(1)
    var completedText by remember(cell.domainCellId, set.id) {
        mutableStateOf(cell.phraseCompletedShots.coerceIn(0, every - 1).toString())
    }
    var editingCount by remember(cell.domainCellId, set.id) { mutableStateOf(false) }
    val completed = completedText.toIntOrNull()?.takeIf { it in 0 until every }

    DDZBottomSheet(title = "문구 진행 상태", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("다음 촬영 문구 선택", style = DDZTypography.SettingLabel, color = DDZColor.TextSecondary)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DDZColor.Surface,
                border = BorderStroke(1.dp, DDZColor.Border),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    set.items.forEachIndexed { itemIndex, item ->
                        val selected = index == itemIndex
                        Surface(
                            onClick = {
                                index = itemIndex
                                completedText = "0"
                                editingCount = false
                            },
                            color = if (selected) DDZColor.SelectedSoft else DDZColor.Surface,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    item,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = DDZColor.TextPrimary,
                                )
                                if (selected) Icon(
                                    Icons.Filled.Check, contentDescription = "선택됨",
                                    tint = DDZColor.SelectedDark,
                                )
                            }
                        }
                        if (itemIndex != set.items.lastIndex) HorizontalDivider(
                            thickness = 1.dp, color = DDZColor.Border,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("완료 장수", modifier = Modifier.weight(1f), color = DDZColor.TextPrimary)
                if (every == 1) {
                    Text("0 / 1장", color = DDZColor.TextSecondary)
                } else {
                    DDZButton(
                        text = "−", style = DDZButtonStyle.Secondary, minHeight = 44.dp,
                        enabled = (completed ?: 0) > 0,
                        onClick = { completed?.let { completedText = (it - 1).toString() } },
                    )
                    Surface(
                        onClick = { editingCount = !editingCount },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DDZColor.Border),
                        color = DDZColor.Surface,
                    ) {
                        Text(
                            "${completedText} / ${every}장",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            color = DDZColor.TextPrimary,
                        )
                    }
                    DDZButton(
                        text = "+", style = DDZButtonStyle.Secondary, minHeight = 44.dp,
                        enabled = completed != null && completed < every - 1,
                        onClick = { completed?.let { completedText = (it + 1).toString() } },
                    )
                }
            }

            if (editingCount && every > 1) {
                DDZTextField(
                    value = completedText,
                    onValueChange = { value ->
                        if (value.length <= 10 && value.all { it in '0'..'9' }) completedText = value
                    },
                    label = "완료 장수 (0~${every - 1})",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (completed == null) Text(
                "0~${every - 1} 사이의 장수를 입력하세요.",
                color = DDZColor.TextSecondary, style = DDZTypography.Caption,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DDZButton(
                    text = "처음부터", style = DDZButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        index = 0
                        completedText = "0"
                        editingCount = false
                    },
                )
                DDZButton(
                    text = "적용", modifier = Modifier.weight(1f), enabled = completed != null,
                    onClick = {
                        completed?.let { onApply(index, it) }
                        onDismiss()
                    },
                )
            }
        }
    }
}
