package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.feature.help.ui.tutorialTarget
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalDensity
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
        if (draft.bgStyle != 2) {
            CompactStyleRow("배경 투명도") {
                Slider(value = draft.bgAlpha.toFloat(), valueRange = 0f..255f,
                    modifier = Modifier.weight(1f), colors = sliderColors,
                    onValueChange = { onDraftChange(draft.copy(bgAlpha = it.roundToInt())) })
                StylePercent((draft.bgAlpha / 255f * 100).roundToInt())
            }
        }
        CompactStyleRow("글자 크기") {
            Slider(value = draft.valueScale.toFloat(), valueRange = 60f..160f,
                modifier = Modifier.weight(1f), colors = sliderColors,
                onValueChange = { onDraftChange(draft.copy(valueScale = it.roundToInt())) })
            StylePercent(draft.valueScale)
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = styleRowHeight(LocalDensity.current.fontScale)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier.weight(1f).tutorialTarget("table.style.정렬"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("정렬", modifier = Modifier.width(36.dp),
                    style = DDZTypography.Caption, color = DDZColor.TextPrimary)
                StyleChoices(
                    listOf("왼쪽", "가운데", "오른쪽"), draft.textAlign,
                    onSelect = { onDraftChange(draft.copy(textAlign = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                Modifier.tutorialTarget("table.style.테두리"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("테두리", style = DDZTypography.Caption, color = DDZColor.TextPrimary)
                Box(Modifier.size(48.dp).toggleable(value = draft.gridEnabled, role = Role.Switch,
                    onValueChange = { onDraftChange(draft.copy(gridEnabled = it)) }), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 32.dp, height = 18.dp)
                        .background(if (draft.gridEnabled) DDZColor.Selected else DDZColor.SurfaceSoft, CircleShape)
                        .border(1.dp, if (draft.gridEnabled) DDZColor.Selected else DDZColor.Border, CircleShape)) {
                        Box(Modifier.align(if (draft.gridEnabled) Alignment.CenterEnd else Alignment.CenterStart)
                            .padding(horizontal = 2.dp).size(14.dp)
                            .background(if (draft.gridEnabled) DDZColor.Surface else DDZColor.TextSecondary, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactStyleRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().tutorialTarget("table.style.$label").heightIn(min = styleRowHeight(LocalDensity.current.fontScale)), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(80.dp), style = DDZTypography.Caption, color = DDZColor.TextPrimary)
        content()
    }
}

@Composable
private fun StyleChoices(
    labels: List<String>,
    current: Int,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onSelect: (Int) -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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

// Four controls with opacity; selecting a fully transparent background hides the opacity row.
internal fun styleRowHeight(fontScale: Float) = (48f * fontScale.coerceAtLeast(1f)).dp
internal fun stylePanelHeight(fontScale: Float, showOpacity: Boolean) =
    styleRowHeight(fontScale) * (if (showOpacity) 4 else 3) + 16.dp
