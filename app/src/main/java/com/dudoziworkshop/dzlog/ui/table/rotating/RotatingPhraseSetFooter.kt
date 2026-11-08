package com.dudoziworkshop.dzlog.ui.table.rotating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingCounterProgressMode
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseSetItemPolicy
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private enum class SetFooterSheet { NUMBERING, CSV }

/** Stays visible while the phrase list alone scrolls. */
@Composable
internal fun RotatingPhraseSetFooter(
    phraseSet: RotatingPhraseSet,
    onUpdateSet: ((RotatingPhraseSet) -> RotatingPhraseSet) -> Unit,
) {
    var showCycleInfo by remember(phraseSet.id) { mutableStateOf(false) }
    var sheet by remember(phraseSet.id) { mutableStateOf<SetFooterSheet?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = DDZColor.Surface,
            border = BorderStroke(1.dp, DDZColor.Border),
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "기본 변경 주기",
                    style = DDZTypography.Body,
                    color = DDZColor.TextPrimary,
                )
                Box {
                    IconButton(onClick = { showCycleInfo = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "기본 변경 주기 설명",
                            tint = DDZColor.TextSecondary,
                        )
                    }
                    DropdownMenu(
                        expanded = showCycleInfo,
                        onDismissRequest = { showCycleInfo = false },
                    ) {
                        Text(
                            "세트의 모든 문구에 적용되는 기본 주기입니다. " +
                                "문구별 주기를 별도로 지정하면 그 문구의 주기가 우선합니다.",
                            modifier = Modifier.width(260.dp).padding(14.dp),
                            style = DDZTypography.Secondary,
                            color = DDZColor.TextPrimary,
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                DDZButton(
                    text = "−",
                    modifier = Modifier.width(40.dp),
                    style = DDZButtonStyle.Secondary,
                    minHeight = 40.dp,
                    horizontalPadding = 8.dp,
                    enabled = phraseSet.defaultEvery > 1,
                    onClick = {
                        onUpdateSet { it.copy(defaultEvery = (it.defaultEvery - 1).coerceAtLeast(1)) }
                    },
                )
                Text(
                    "${phraseSet.defaultEvery}장",
                    modifier = Modifier.padding(horizontal = 5.dp),
                    style = DDZTypography.Body,
                    color = DDZColor.TextPrimary,
                )
                DDZButton(
                    text = "+",
                    modifier = Modifier.width(40.dp),
                    style = DDZButtonStyle.Secondary,
                    minHeight = 40.dp,
                    horizontalPadding = 8.dp,
                    enabled = phraseSet.defaultEvery < RotatingPhraseSet.MAX_EVERY,
                    onClick = { onUpdateSet { it.copy(defaultEvery = it.defaultEvery + 1) } },
                )
            }
        }
        Surface(color = DDZColor.Surface, shape = RoundedCornerShape(14.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { sheet = SetFooterSheet.NUMBERING }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "번호 진행 방식",
                        modifier = Modifier.weight(1f),
                        style = DDZTypography.Body,
                        color = DDZColor.TextPrimary,
                    )
                    Text(
                        if (phraseSet.counterProgressMode == RotatingCounterProgressMode.PER_PHRASE)
                            "문구별로 따로" else "문구가 바뀌어도 계속",
                        style = DDZTypography.Caption,
                        color = DDZColor.TextSecondary,
                    )
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = DDZColor.IconMuted)
                }
                HorizontalDivider(color = DDZColor.Border)
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { sheet = SetFooterSheet.CSV }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "CSV 일괄 관리",
                        modifier = Modifier.weight(1f),
                        style = DDZTypography.Body,
                        color = DDZColor.TextPrimary,
                    )
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = DDZColor.IconMuted)
                }
            }
        }
    }

    when (sheet) {
        SetFooterSheet.NUMBERING -> {
            DDZBottomSheet(onDismiss = { sheet = null }, title = "번호 진행 방식") {
                Column(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RotatingCounterProgressMode.entries.forEach { mode ->
                        val selected = phraseSet.counterProgressMode == mode
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onUpdateSet { it.copy(counterProgressMode = mode) }
                                        sheet = null
                                    },
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = DDZColor.SelectedDark,
                                    unselectedColor = DDZColor.IconMuted,
                                ),
                            )
                            Column(Modifier.padding(start = 6.dp)) {
                                Text(
                                    if (mode == RotatingCounterProgressMode.PER_PHRASE)
                                        "문구별로 따로" else "문구가 바뀌어도 계속",
                                    style = DDZTypography.Body,
                                    color = DDZColor.TextPrimary,
                                )
                                Text(
                                    if (mode == RotatingCounterProgressMode.PER_PHRASE)
                                        "A 0001 → B 0001 → A 0002"
                                    else "A 0001 → B 0002 → A 0003",
                                    style = DDZTypography.Caption,
                                    color = DDZColor.TextSecondary,
                                )
                            }
                        }
                    }
                    Text(
                        "표 편집에서 저장하면 적용됩니다.",
                        style = DDZTypography.Caption,
                        color = DDZColor.TextSecondary,
                    )
                }
            }
        }
        SetFooterSheet.CSV -> {
            DDZBottomSheet(onDismiss = { sheet = null }, title = "CSV 일괄 관리") {
                RotatingPhraseCsvActions(
                    phraseSet = phraseSet,
                    showHeading = false,
                    onReplaceItems = { imported, periods ->
                        if (phraseSet.items != imported ||
                            RotatingPhraseSetItemPolicy.overrides(phraseSet) != periods) {
                            onUpdateSet {
                                RotatingPhraseSetItemPolicy.replaceItems(it, imported, periods)
                            }
                        }
                    },
                )
            }
        }
        null -> Unit
    }
}
