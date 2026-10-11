package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlin.math.roundToInt

@Composable
internal fun TableEditorStylePanel(
    draft: TableStyleState,
    onDraftChange: (TableStyleState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderColors = SliderDefaults.colors(thumbColor = DDZColor.SelectedDark,
        activeTrackColor = DDZColor.Selected, inactiveTrackColor = DDZColor.Border)
    Column(modifier.fillMaxWidth().background(DDZColor.Surface)
        .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        CompactStyleRow("배경") {
            StyleChoices(listOf("밝게", "어둡게", "투명"), when (draft.bgStyle) { 1 -> 0; 0 -> 1; else -> 2 }) {
                onDraftChange(draft.copy(bgStyle = listOf(1, 0, 2)[it]))
            }
        }
        CompactStyleRow("글자 크기") {
            Slider(value = draft.valueScale.toFloat(), valueRange = 60f..160f,
                modifier = Modifier.weight(1f), colors = sliderColors,
                onValueChange = { onDraftChange(draft.copy(valueScale = it.roundToInt())) })
            StylePercent(draft.valueScale)
        }
        CompactStyleRow("정렬") {
            StyleChoices(listOf("왼쪽", "가운데", "오른쪽"), draft.textAlign) {
                onDraftChange(draft.copy(textAlign = it))
            }
        }
        CompactStyleRow("테두리") {
            Spacer(Modifier.weight(1f))
            Switch(checked = draft.gridEnabled, onCheckedChange = { onDraftChange(draft.copy(gridEnabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = DDZColor.Surface,
                    checkedTrackColor = DDZColor.Selected, uncheckedThumbColor = DDZColor.TextSecondary,
                    uncheckedTrackColor = DDZColor.SurfaceSoft, uncheckedBorderColor = DDZColor.Border))
        }
        CompactStyleRow("배경 투명도") {
            Slider(value = draft.bgAlpha.toFloat(), valueRange = 0f..255f,
                modifier = Modifier.weight(1f), colors = sliderColors, enabled = draft.bgStyle != 2,
                onValueChange = { onDraftChange(draft.copy(bgAlpha = it.roundToInt())) })
            StylePercent(if (draft.bgStyle == 2) 0 else (draft.bgAlpha / 255f * 100).roundToInt())
        }
        CompactStyleRow("글자 색") {
            StyleChoices(listOf("자동", "흰색", "검정"),
                if (draft.textColorMode == 0) 0 else if (draft.manualTextColor == 0) 1 else 2) {
                onDraftChange(when (it) { 0 -> draft.copy(textColorMode = 0)
                    1 -> draft.copy(textColorMode = 1, manualTextColor = 0)
                    else -> draft.copy(textColorMode = 1, manualTextColor = 1) })
            }
        }
    }
}

@Composable
private fun CompactStyleRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(80.dp), style = DDZTypography.Caption, color = DDZColor.TextPrimary)
        content()
    }
}

@Composable
private fun StyleChoices(labels: List<String>, current: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { index, label ->
            Box(Modifier.weight(1f).height(48.dp).semantics { selected = index == current }
                .clickable(role = Role.RadioButton) { onSelect(index) }, contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxWidth().height(32.dp)
                    .background(if (index == current) DDZColor.SelectedSoft else DDZColor.SurfaceSoft, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(label, style = DDZTypography.Caption, maxLines = 1,
                        color = if (index == current) DDZColor.SelectedDark else DDZColor.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun StylePercent(value: Int) {
    Text("$value%", modifier = Modifier.width(42.dp), style = DDZTypography.Caption, color = DDZColor.TextSecondary)
}
