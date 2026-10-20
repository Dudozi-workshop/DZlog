package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialLocation

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.OpenInFull
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZTextField
import com.dudoziworkshop.dzlog.ui.common.input.decimalInputOrPrevious
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseSetEditDialog
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout

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
    onPhraseProgressChange: (Int, Int) -> Unit,
    onCreatePhraseSet: (String) -> String?,
    onUpdatePhraseSet: (String, (com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet) -> com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet) -> Unit,
    onDeletePhraseSet: (String) -> Unit,
    onClose: () -> Unit,
    compactInput: Boolean = false,
    inputExpanded: Boolean,
    expandedInputHeight: Dp,
    onToggleInputExpanded: () -> Unit,
    onInputFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTypePicker by remember(cell.id) { mutableStateOf(false) }
    var showDatePicker by remember(cell.id) { mutableStateOf(false) }
    var showPhrasePicker by remember(cell.id) { mutableStateOf(false) }
    var showPhraseProgress by remember(cell.id) { mutableStateOf(false) }
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    when (cell.type) {
                        TableEditorCellType.TEXT,
                        TableEditorCellType.NUMBER -> {
                            val isText = cell.type == TableEditorCellType.TEXT
                            Box(Modifier.fillMaxWidth()) {
                                DDZTextField(
                                    modifier = Modifier.fillMaxWidth()
                                        .then(if (isText) Modifier.height(
                                            if (inputExpanded) expandedInputHeight else DDZLayout.QuickInput.RowHeight
                                        ) else Modifier)
                                        .onFocusChanged { if (it.isFocused) onInputFocus() },
                                    value = cell.value,
                                    onValueChange = { input ->
                                        onValueChange(if (cell.type == TableEditorCellType.NUMBER) decimalInputOrPrevious(input, cell.value) else input)
                                    },
                                    label = "값",
                                    singleLine = !isText,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = if (cell.type == TableEditorCellType.NUMBER) KeyboardType.Decimal else KeyboardType.Text,
                                    ),
                                    trailingIcon = if (cell.type == TableEditorCellType.NUMBER) {
                                        { androidx.compose.material3.TextButton(onClick = {
                                            onValueChange(if (cell.value.startsWith("-")) cell.value.drop(1) else "-${cell.value}")
                                        }) { Text("±") } }
                                    } else {
                                        // Reserve the same text inset in both modes for the overlay button.
                                        { androidx.compose.foundation.layout.Spacer(Modifier.size(DDZLayout.QuickInput.Touch)) }
                                    },
                                )
                                if (isText) IconButton(
                                    onClick = onToggleInputExpanded,
                                    modifier = Modifier.align(Alignment.BottomEnd).size(DDZLayout.QuickInput.Touch),
                                ) {
                                    Icon(if (inputExpanded) Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull,
                                        if (inputExpanded) "입력창 축소" else "입력창 확대",
                                        tint = DDZColor.TextMuted, modifier = Modifier.size(DDZLayout.QuickInput.Icon))
                                }
                            }
                        }

                        TableEditorCellType.COUNTER -> {
                            val currentCounter = cell.value.toIntOrNull()?.coerceAtLeast(0) ?: 1
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
                                    modifier = Modifier.weight(1f)
                                        .onFocusChanged { if (it.isFocused) onInputFocus() },
                                    value = currentCounter.toString(),
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        if (digits.isNotBlank()) {
                                            onValueChange(digits)
                                        }
                                    },
                                    label = "시작 번호",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, "셀 편집 닫기", tint = DDZColor.TextMuted, modifier = Modifier.size(16.dp))
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
                    TableEditorCellType.DATE -> TableEditorSettingRow(
                        label = "날짜 형식",
                        value = tableEditorDateFormatLabel(cell.formatPattern),
                        onClick = { showDatePicker = true },
                    )
                    TableEditorCellType.TIME -> TableEditorSettingRow(
                        label = "시간 형식",
                        value = "HHmm · 분 단위 고정",
                        onClick = onApplyTimePolicy,
                    )
                    TableEditorCellType.ROTATING_TEXT -> {
                        val selectedSet = phraseSets.firstOrNull { it.id == cell.phraseSetId }
                        TableEditorSettingRow(
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
                            if (selectedSet.items.isNotEmpty()) {
                                val index = cell.phraseIndex.coerceIn(0, selectedSet.items.lastIndex)
                                val completed = cell.phraseCompletedShots.coerceIn(0, everyValue - 1)
                                TableEditorSettingRow(label = "현재 문구", value = selectedSet.items[index],
                                    onClick = { showPhraseProgress = true })
                                TableEditorSettingRow(label = "저장 완료", value = "$completed / ${everyValue}장 · 조정",
                                    onClick = { showPhraseProgress = true })
                                Text("${everyValue - completed}장 더 촬영하면 ${selectedSet.items[(index + 1) % selectedSet.items.size]}",
                                    style = DDZTypography.Caption, color = DDZColor.TextMuted, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                Text("주기 변경 시 현재 문구의 완료 장수가 초기화됩니다.",
                                    style = DDZTypography.Caption, color = DDZColor.TextMuted)
                            }
                        }
                    }
                    else -> Unit
                }

                TableEditorSettingRow(
                    label = "셀 종류",
                    value = cell.type.label,
                    valueIcon = cell.type.choiceIcon(),
                    onClick = { showTypePicker = true },
                )
            }
        }

    }

    if (showPhraseProgress) {
        phraseSets.firstOrNull { it.id == cell.phraseSetId && it.items.isNotEmpty() }?.let { set ->
            TableEditorPhraseProgressDialog(cell, set, onPhraseProgressChange, { showPhraseProgress = false })
        }
    }

    if (showTypePicker || (LocalTutorialLocation.current?.areaId == "table-detail" && LocalTutorialLocation.current?.pageId == "cells/types")) {
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
            onCreate = { name ->
                onCreatePhraseSet(name)?.let { createdSetId ->
                    showPhrasePicker = false
                    editingPhraseSetId = createdSetId
                }
            },
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

