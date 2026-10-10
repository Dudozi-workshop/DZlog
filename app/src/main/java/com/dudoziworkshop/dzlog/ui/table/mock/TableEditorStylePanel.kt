package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
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
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun TableEditorStylePanel(
    draft: TableStyleState,
    showAdvanced: Boolean,
    onDraftChange: (TableStyleState) -> Unit,
    onAdvancedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val choiceColors = SegmentedButtonDefaults.colors(
        activeContainerColor = DDZColor.SelectedSoft,
        activeContentColor = DDZColor.SelectedDark,
        activeBorderColor = DDZColor.SageBorder,
        inactiveContainerColor = DDZColor.Surface,
        inactiveContentColor = DDZColor.TextPrimary,
        inactiveBorderColor = DDZColor.Border,
    )
    val sliderColors = SliderDefaults.colors(
        thumbColor = DDZColor.SelectedDark,
        activeTrackColor = DDZColor.Selected,
        inactiveTrackColor = DDZColor.Border,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DDZColor.Surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("배경", style = DDZTypography.SettingLabel, color = DDZColor.TextSecondary)
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
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = choiceColors,
                ) { Text(label, style = DDZTypography.ButtonText) }
            }
        }

        StyleValueLabel("글자 크기", "${draft.valueScale}%")
        Slider(
            value = draft.valueScale / 100f,
            onValueChange = { value ->
                onDraftChange(draft.copy(valueScale = (value * 100).toInt().coerceIn(60, 160)))
            },
            valueRange = 0.6f..1.6f,
            colors = sliderColors,
        )

        Text("정렬", style = DDZTypography.SettingLabel, color = DDZColor.TextSecondary)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("왼쪽", "가운데", "오른쪽").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = draft.textAlign == index,
                    onClick = { onDraftChange(draft.copy(textAlign = index)) },
                    shape = SegmentedButtonDefaults.itemShape(index, 3),
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = choiceColors,
                ) { Text(label, style = DDZTypography.ButtonText) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("테두리 표시", style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
            Switch(
                checked = draft.gridEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DDZColor.Surface,
                    checkedTrackColor = DDZColor.Selected,
                    uncheckedThumbColor = DDZColor.TextSecondary,
                    uncheckedTrackColor = DDZColor.SurfaceSoft,
                    uncheckedBorderColor = DDZColor.Border,
                ),
                onCheckedChange = { onDraftChange(draft.copy(gridEnabled = it)) },
            )
        }

        TextButton(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            onClick = { onAdvancedChange(!showAdvanced) },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("고급 설정", style = DDZTypography.SettingLabel, color = DDZColor.TextSecondary)
                Icon(
                    imageVector = if (showAdvanced) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (showAdvanced) "고급 설정 접기" else "고급 설정 펼치기",
                    tint = DDZColor.TextSecondary,
                )
            }
        }

        if (showAdvanced) {
            StyleValueLabel("배경 투명도", "${((draft.bgAlpha / 255f) * 100).toInt()}%")
            Slider(
                value = draft.bgAlpha.toFloat(),
                onValueChange = { value ->
                    onDraftChange(draft.copy(bgAlpha = value.toInt().coerceIn(0, 255)))
                },
                valueRange = 0f..255f,
                colors = sliderColors,
                enabled = draft.bgStyle != 2,
            )

            if (draft.bgStyle == 2) {
                Text("투명 배경에서는 투명도를 조절하지 않아요.", style = DDZTypography.Secondary, color = DDZColor.TextSecondary)
            }

            Text("글자 색", style = DDZTypography.SettingLabel, color = DDZColor.TextSecondary)
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
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = choiceColors,
                    ) { Text(label, style = DDZTypography.ButtonText) }
                }
            }
        }

    }
}

@Composable
private fun StyleValueLabel(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = DDZTypography.SettingLabel, color = DDZColor.TextSecondary)
        Text(value, style = DDZTypography.Body, color = DDZColor.TextPrimary)
    }
}

