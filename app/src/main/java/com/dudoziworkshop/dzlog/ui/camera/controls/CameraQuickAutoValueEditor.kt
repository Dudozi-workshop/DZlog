package com.dudoziworkshop.dzlog.ui.camera.controls

import com.dudoziworkshop.dzlog.feature.help.ui.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgressPolicy
import com.dudoziworkshop.dzlog.feature.table.policy.quickDateFormats
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class QuickValuePicker { DATE, PHRASE_SET }

/**
 * Camera quick edit exposes only four rotating controls: set, next phrase,
 * cycle length and completed captures. Editing set contents remains in table settings.
 * Pickers open separately so the camera/table viewport stays visible by default.
 */
@Composable
internal fun CameraQuickAutoValueEditor(
    cell: TableCellState,
    template: TableTemplateState,
    now: Date,
    enabled: Boolean,
    onDateFormat: (String) -> Unit,
    onPhraseSet: (String) -> Unit,
    onPhraseProgress: (Int, Int) -> Unit,
    onPhrasePeriodChange: (Boolean, Int) -> Unit,
) {
    var picker by remember(cell.cellId) { mutableStateOf<QuickValuePicker?>(null) }
    val availableSets = template.phraseSets.filter { it.items.isNotEmpty() }
    val selectedSet = availableSets.firstOrNull { it.id == cell.phraseSetId }

    when (cell.dataType) {
        TableCellDataType.DATE -> {
            val pattern = cell.formatPattern.takeIf { it in quickDateFormats } ?: "yyyyMMdd"
            val example = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
            DDZButton(
                text = "날짜 형식 · $example",
                modifier = Modifier.fillMaxWidth(),
                style = DDZButtonStyle.Secondary,
                enabled = enabled,
                onClick = { picker = QuickValuePicker.DATE },
            )
        }
        TableCellDataType.TIME -> {
            val time = SimpleDateFormat("HHmm", Locale.getDefault()).format(now)
            Text(
                "촬영 시각으로 자동 입력돼요 · $time",
                style = DDZTypography.Secondary,
                color = DDZColor.TextSecondary,
            )
        }
        TableCellDataType.ROTATING_TEXT -> {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                QuickPhraseChoice(
                    label = "문구 세트",
                    value = selectedSet?.name ?: "세트 선택",
                    enabled = enabled && availableSets.isNotEmpty(),
                    onClick = { picker = QuickValuePicker.PHRASE_SET },
                )
                if (selectedSet == null) {
                    Text(
                        if (availableSets.isEmpty()) "표 상세설정에서 문구를 먼저 등록해 주세요."
                        else "먼저 사용할 문구 세트를 선택하세요.",
                        style = DDZTypography.Caption,
                        color = DDZColor.TextSecondary,
                    )
                } else {
                    CameraQuickPhraseProgressCard(
                        cell = cell,
                        set = selectedSet,
                        enabled = enabled,
                        onProgressChange = onPhraseProgress,
                    )
                    CameraQuickPhrasePeriodControl(
                        cell = cell,
                        set = selectedSet,
                        enabled = enabled,
                        onPeriodChange = onPhrasePeriodChange,
                    )
                }
            }
        }
        else -> Unit
    }

    val guideOptions = LocalTutorialReadOnly.current &&
        LocalTutorialLocation.current?.pageId in listOf("quick-date", "quick-phrase") &&
        LocalTutorialDemoProgress.current < 0.5f
    val activePicker = picker ?: if (guideOptions) {
        if (cell.dataType == TableCellDataType.DATE) QuickValuePicker.DATE
        else QuickValuePicker.PHRASE_SET
    } else null
    if (activePicker != null) {
        DDZBottomSheet(
            inlinePreview = guideOptions,
            title = when (activePicker) {
                QuickValuePicker.DATE -> "날짜 형식 선택"
                QuickValuePicker.PHRASE_SET -> "순환문구 세트 선택"
            },
            onDismiss = { picker = null },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (activePicker) {
                    QuickValuePicker.DATE -> {
                        val activePattern = cell.formatPattern.takeIf { it in quickDateFormats } ?: "yyyyMMdd"
                        quickDateFormats.forEach { pattern ->
                            val isSelected = pattern == activePattern
                            val example = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
                            val label = when (pattern) {
                                "yyMMdd" -> "짧은 연월일"
                                "MMdd" -> "월일"
                                else -> "연월일"
                            }
                            DDZButton(
                                text = "$example · $label",
                                modifier = Modifier.fillMaxWidth().semantics { selected = isSelected },
                                style = DDZButtonStyle.Secondary,
                                containerColorOverride = if (isSelected) DDZColor.SelectedSoft else null,
                                enabled = enabled,
                                onClick = {
                                    onDateFormat(pattern)
                                    picker = null
                                },
                            )
                        }
                    }
                    QuickValuePicker.PHRASE_SET -> {
                        availableSets.forEach { set ->
                            val isSelected = set.id == cell.phraseSetId
                            DDZButton(
                                text = set.name,
                                modifier = Modifier.fillMaxWidth().semantics { selected = isSelected },
                                style = DDZButtonStyle.Secondary,
                                containerColorOverride = if (isSelected) DDZColor.SelectedSoft else null,
                                enabled = enabled,
                                onClick = {
                                    onPhraseSet(set.id)
                                    picker = null
                                },
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
private fun QuickPhraseChoice(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DDZColor.Surface,
        border = BorderStroke(1.dp, DDZColor.Border),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, style = DDZTypography.Secondary, color = DDZColor.TextSecondary)
            Text(value, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = DDZTypography.Body, color = DDZColor.TextPrimary, textAlign = TextAlign.End)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = DDZColor.TextSecondary)
        }
    }
}

