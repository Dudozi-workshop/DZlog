package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.quickDateFormats
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keep the primary quick-edit viewport the same size regardless of the number
 * of available phrase sets. Options are visible only when explicitly requested.
 */
@Composable
internal fun CameraQuickAutoValueEditor(
    cell: TableCellState,
    template: TableTemplateState,
    now: Date,
    enabled: Boolean,
    onDateFormat: (String) -> Unit,
    onPhraseSet: (String) -> Unit,
) {
    var showOptions by remember(cell.cellId) { mutableStateOf(false) }
    val availableSets = template.phraseSets.filter { it.items.isNotEmpty() }

    when (cell.dataType) {
        TableCellDataType.DATE -> {
            val pattern = cell.formatPattern.takeIf { it in quickDateFormats } ?: "yyyyMMdd"
            val example = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
            DDZButton(
                text = "날짜 형식 · $example",
                modifier = Modifier.fillMaxWidth(),
                style = DDZButtonStyle.Secondary,
                enabled = enabled,
                onClick = { showOptions = true },
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
            val name = availableSets.firstOrNull { it.id == cell.phraseSetId }?.name
            DDZButton(
                text = "문구 세트 · ${name ?: "세트 선택"}",
                modifier = Modifier.fillMaxWidth(),
                style = DDZButtonStyle.Secondary,
                enabled = enabled && availableSets.isNotEmpty(),
                onClick = { showOptions = true },
            )
            if (availableSets.isEmpty()) {
                Text(
                    "표 상세설정에서 문구를 먼저 등록해 주세요.",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextSecondary,
                )
            }
        }
        else -> Unit
    }

    if (showOptions) {
        DDZBottomSheet(
            title = if (cell.dataType == TableCellDataType.DATE) "날짜 형식 선택" else "순환문구 세트 선택",
            onDismiss = { showOptions = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (cell.dataType) {
                    TableCellDataType.DATE -> {
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
                                    showOptions = false
                                },
                            )
                        }
                    }
                    TableCellDataType.ROTATING_TEXT -> {
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
                                    showOptions = false
                                },
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}
