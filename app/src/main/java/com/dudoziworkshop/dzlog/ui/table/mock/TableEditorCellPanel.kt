package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZSettingRow
import com.dudoziworkshop.dzlog.ui.common.DDZTextField
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseSetEditDialog
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TableEditorCellUiModelEditor(
    cell: TableEditorCellUiModel,
    phraseSets: List<com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet>,
    onValueChange: (String) -> Unit,
    onTypeChange: (TableEditorCellType) -> Unit,
    onDatePatternChange: (String) -> Unit,
    onApplyTimePolicy: () -> Unit,
    onPhraseSetChange: (String?) -> Unit,
    onPhraseEveryChange: (Int) -> Unit,
    onCreatePhraseSet: (String) -> Unit,
    onUpdatePhraseSet: (String, (com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet) -> com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet) -> Unit,
    onDeletePhraseSet: (String) -> Unit,
    onClose: () -> Unit,
    compactInput: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var showTypePicker by remember(cell.id) { mutableStateOf(false) }
    var showDatePicker by remember(cell.id) { mutableStateOf(false) }
    var showPhrasePicker by remember(cell.id) { mutableStateOf(false) }
    var editingPhraseSetId by remember(cell.id) { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalTextStyle provides DDZTypography.Body) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(DDZColor.Surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("선택한 셀", style = DDZTypography.SettingLabel)
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "셀 편집 닫기",
                        tint = DDZColor.TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            when (cell.type) {
                TableEditorCellType.TEXT,
                TableEditorCellType.NUMBER -> {
                    DDZTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = cell.value,
                        onValueChange = onValueChange,
                        label = "값",
                    )
                }

                TableEditorCellType.COUNTER -> {
                    val currentCounter = cell.value.toIntOrNull()?.coerceAtLeast(0) ?: 1
                    Text("자동번호", fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DDZButton(
                            text = "−",
                            style = DDZButtonStyle.Secondary,
                            minHeight = 40.dp,
                            onClick = { onValueChange((currentCounter - 1).coerceAtLeast(0).toString()) },
                        )
                        DDZTextField(
                            modifier = Modifier.weight(1f),
                            value = currentCounter.toString(),
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                if (digits.isNotBlank()) {
                                    onValueChange(digits)
                                }
                            },
                            label = "시작 번호",
                        )
                        DDZButton(
                            text = "+",
                            style = DDZButtonStyle.Secondary,
                            minHeight = 40.dp,
                            onClick = { onValueChange((currentCounter + 1).toString()) },
                        )
                    }
                }

                TableEditorCellType.DATE,
                TableEditorCellType.TIME,
                TableEditorCellType.ROTATING_TEXT -> {
                    Text(
                        text = (cell.previewValue ?: cell.value).ifBlank {
                            if (cell.type == TableEditorCellType.ROTATING_TEXT) "문구 세트를 선택하세요" else ""
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // The selected value stays above the scrollable type-specific settings.
            if (!compactInput) Column(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (cell.type) {
                    TableEditorCellType.COUNTER -> Text(
                        "촬영 성공 후 다음 번호로 증가합니다.", color = DDZColor.TextMuted,
                        style = DDZTypography.Caption,
                    )
                    TableEditorCellType.DATE -> DDZSettingRow(
                        label = "날짜 형식",
                        value = tableEditorDateFormatLabel(cell.formatPattern),
                        onClick = { showDatePicker = true },
                    )
                    TableEditorCellType.TIME -> DDZSettingRow(
                        label = "시간 형식",
                        value = "HHmm · 분 단위 고정",
                        onClick = onApplyTimePolicy,
                    )
                    TableEditorCellType.ROTATING_TEXT -> {
                        val selectedSet = phraseSets.firstOrNull { it.id == cell.phraseSetId }
                        DDZSettingRow(
                            label = "문구 세트",
                            value = selectedSet?.name ?: "선택 안 함",
                            onClick = { showPhrasePicker = true },
                        )
                        if (selectedSet != null) {
                            val everyValue = (cell.everyOverride ?: selectedSet.defaultEvery).coerceAtLeast(1)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("변경 주기", modifier = Modifier.weight(1f), color = DDZColor.TextMuted)
                                DDZButton(text = "−", style = DDZButtonStyle.Secondary, minHeight = 40.dp,
                                    onClick = { onPhraseEveryChange((everyValue - 1).coerceAtLeast(1)) })
                                Text(everyValue.toString() + "장")
                                DDZButton(text = "+", style = DDZButtonStyle.Secondary, minHeight = 40.dp,
                                    onClick = { onPhraseEveryChange(everyValue + 1) })
                            }
                        }
                    }
                    else -> Unit
                }

                DDZSettingRow(
                    label = "셀 종류",
                    value = cell.type.label,
                    onClick = { showTypePicker = true },
                )
            }
        }

    }

    if (showTypePicker) {
        TableEditorCellTypeSheet(
            selectedType = cell.type,
            onSelect = { type ->
                onTypeChange(type)
                showTypePicker = false
            },
            onDismiss = { showTypePicker = false },
        )
    }

    if (showDatePicker) {
        TableEditorDateFormatSheet(
            selectedPattern = cell.formatPattern,
            onDismiss = { showDatePicker = false },
            onSelect = { pattern ->
                onDatePatternChange(pattern)
                showDatePicker = false
            },
        )
    }

    if (showPhrasePicker) {
        TableEditorPhraseSetSheet(
            cellId = cell.id,
            phraseSets = phraseSets,
            selectedSetId = cell.phraseSetId,
            onSelect = { id ->
                onPhraseSetChange(id)
                showPhrasePicker = false
            },
            onEdit = { id ->
                editingPhraseSetId = id
                showPhrasePicker = false
            },
            onCreate = onCreatePhraseSet,
            onDismiss = { showPhrasePicker = false },
        )
    }

    editingPhraseSetId
        ?.let { id -> phraseSets.firstOrNull { it.id == id } }
        ?.let { editingSet ->
            RotatingPhraseSetEditDialog(
                phraseSet = editingSet,
                onClose = { editingPhraseSetId = null },
                onUpdateSet = { transform ->
                    onUpdatePhraseSet(editingSet.id, transform)
                },
                onDeleteSet = { phraseSetId ->
                    onDeletePhraseSet(phraseSetId)
                    editingPhraseSetId = null
                },
            )
        }

}

