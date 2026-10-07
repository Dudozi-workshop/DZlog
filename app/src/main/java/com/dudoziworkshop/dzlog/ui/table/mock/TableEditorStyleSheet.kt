package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@Composable
internal fun TableEditorStyleSheet(
    draft: TableStyleState,
    showAdvanced: Boolean,
    onDraftChange: (TableStyleState) -> Unit,
    onAdvancedChange: (Boolean) -> Unit,
    onApply: (TableStyleState) -> Unit,
    onDismiss: () -> Unit,
) {
    DDZBottomSheet(title = "표 스타일", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("배경", color = DDZColor.TextSecondary)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("밝게", "어둡게", "투명").forEachIndexed { index, label ->
                    val selected = when (index) {
                        0 -> draft.bgStyle == 1
                        1 -> draft.bgStyle == 0
                        else -> draft.bgStyle == 2
                    }
                    SegmentedButton(
                        selected = selected,
                        onClick = {
                            onDraftChange(draft.copy(bgStyle = when (index) {
                                0 -> 1
                                1 -> 0
                                else -> 2
                            }))
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                    ) { Text(label) }
                }
            }

            Text("글자 크기  " + draft.valueScale + "%", color = DDZColor.TextSecondary)
            Slider(
                value = draft.valueScale / 100f,
                onValueChange = { value ->
                    onDraftChange(draft.copy(valueScale = (value * 100).toInt().coerceIn(60, 160)))
                },
                valueRange = 0.6f..1.6f,
            )

            Text("정렬", color = DDZColor.TextSecondary)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("왼쪽", "가운데", "오른쪽").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = draft.textAlign == index,
                        onClick = { onDraftChange(draft.copy(textAlign = index)) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                    ) { Text(label) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("테두리 표시")
                Switch(
                    checked = draft.gridEnabled,
                    onCheckedChange = { onDraftChange(draft.copy(gridEnabled = it)) },
                )
            }

            DDZButton(
                text = if (showAdvanced) "고급 설정 접기" else "더보기",
                modifier = Modifier.fillMaxWidth(),
                style = DDZButtonStyle.Secondary,
                onClick = { onAdvancedChange(!showAdvanced) },
            )

            if (showAdvanced) {
                Text(
                    "배경 투명도  " + ((draft.bgAlpha / 255f) * 100).toInt() + "%",
                    color = DDZColor.TextSecondary,
                )
                Slider(
                    value = draft.bgAlpha.toFloat(),
                    onValueChange = { value ->
                        onDraftChange(draft.copy(bgAlpha = value.toInt().coerceIn(0, 255)))
                    },
                    valueRange = 0f..255f,
                    enabled = draft.bgStyle != 2,
                )

                Text("글자 색", color = DDZColor.TextSecondary)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("자동", "흰색", "검정").forEachIndexed { index, label ->
                        val selected = when (index) {
                            0 -> draft.textColorMode == 0
                            1 -> draft.textColorMode == 1 && draft.manualTextColor == 0
                            else -> draft.textColorMode == 1 && draft.manualTextColor == 1
                        }
                        SegmentedButton(
                            selected = selected,
                            onClick = {
                                onDraftChange(when (index) {
                                    0 -> draft.copy(textColorMode = 0)
                                    1 -> draft.copy(textColorMode = 1, manualTextColor = 0)
                                    else -> draft.copy(textColorMode = 1, manualTextColor = 1)
                                })
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, 3),
                        ) { Text(label) }
                    }
                }
            }

            DDZButton(
                text = "적용",
                modifier = Modifier.fillMaxWidth(),
                onClick = { onApply(draft) },
            )
        }
    }
}
