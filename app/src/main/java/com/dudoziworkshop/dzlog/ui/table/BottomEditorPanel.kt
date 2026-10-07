package com.dudoziworkshop.dzlog.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.resolveFileNameDraftToken
import com.dudoziworkshop.dzlog.domain.naming.resolvePathDraftToken
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.ui.table.section.BottomEditorPanelMode
import com.dudoziworkshop.dzlog.ui.table.section.FileNameFormatType
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.PathFormatType
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.util.Date

private val BottomEditorTabShape = RoundedCornerShape(10.dp)
private val BottomEditorSlotShape = RoundedCornerShape(12.dp)
private val BottomEditorToolShape = RoundedCornerShape(10.dp)

@Composable
internal fun BottomEditorPanel(
    panelMode: BottomEditorPanelMode,
    rows: Int,
    cols: Int,
    isSaving: Boolean,
    selectedCell: TableCellState?,
    editingCellId: String?,
    editingValue: String,
    templateState: TableTemplateState,
    resolvedByCellId: Map<String, String>,
    resolvedCells: List<ResolvedCell>,
    hasGroup1: Boolean,
    hasGroup2: Boolean,
    previewNow: Date,
    previewCounterDigits: Int,
    scopeNextCounter: Int,
    phraseProgressCursor: Int,
    dateFormat: String,
    timeFormat: String,
    phraseSets: List<RotatingPhraseSet>,
    autoNextCounterValue: Int,
    fileNameSlotItems: List<FileNameSlotUiItem?>,
    selectedFileNameSlot: Int?,
    isFileNameCellPickMode: Boolean,
    manualInputDraft: String,
    showManualInputEditor: Boolean,
    pathSlotItems: List<PathSlotUiItem?>,
    selectedPathSlot: Int?,
    isPathCellPickMode: Boolean,
    showPathManualInputEditor: Boolean,
    pathManualInputDraft: String,
    modifier: Modifier = Modifier,
    onToggleFileNameForSelected: (cellId: String, enabled: Boolean) -> Unit,
    onReorderFileNameSlots: (fromIndex: Int, toIndex: Int) -> Unit,
    onPathGroupActionForSelected: (PathGroupAction) -> Unit,
    onSetDataTypeForSelected: (TableCellDataType) -> Unit,
    onSetCounterScopeModeForSelected: (CounterScopeMode) -> Unit,
    onResetCounterSeedForSelected: () -> Unit,
    onOpenFormatDialog: (cellId: String, type: TableCellDataType) -> Unit,
    onOpenRotatingTemplateDialogForSelected: (String) -> Unit,
    onStartInlineEditing: (cellId: String, initialText: String) -> Unit,
    onEditingValueChange: (String) -> Unit,
    onSelectFileNameSlot: (Int) -> Unit,
    onFillEmptyFileNameSlot: (Int) -> Unit,
    onMoveSelectedFileNameSlotLeft: () -> Unit,
    onMoveSelectedFileNameSlotRight: () -> Unit,
    onDeleteSelectedFileNameSlot: () -> Unit,
    onStartFileNameCellPick: () -> Unit,
    onStartManualInputEditor: () -> Unit,
    onManualInputDraftChange: (String) -> Unit,
    onApplyManualInput: () -> Unit,
    onSelectPathSlot: (Int) -> Unit,
    onFillEmptyPathSlot: (Int) -> Unit,
    onMoveSelectedPathSlotLeft: () -> Unit,
    onMoveSelectedPathSlotRight: () -> Unit,
    onDeleteSelectedPathSlot: () -> Unit,
    onStartPathCellPick: () -> Unit,
    onStartPathManualInputEditor: () -> Unit,
    onPathManualInputDraftChange: (String) -> Unit,
    onApplyPathManualInput: () -> Unit,
    onClosePanel: () -> Unit,
    onAddRow: () -> Unit,
    onAddCol: () -> Unit,
    onMergeSelection: () -> Unit,
    onDeleteSelection: () -> Unit,
    onUndo: () -> Unit,
    isUndoAvailable: Boolean,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onSetBgStyle: (Int) -> Unit,
    onSetBgAlpha: (Int) -> Unit,
    onSetGridEnabled: (Boolean) -> Unit,
    onSetTextColorMode: (Int) -> Unit,
    onSetManualTextColor: (Int) -> Unit,
    onSetValueScale: (Int) -> Unit,
    onSetTextAlign: (Int) -> Unit,
    wmBgStyle: Int,
    wmBgAlpha: Int,
    wmGridEnabled: Boolean,
    wmTextColorMode: Int,
    wmManualTextColor: Int,
    wmValueScale: Int,
    wmTextAlign: Int,
    wmWidthRatio: Int,
    onSetWmWidthRatio: (Int) -> Unit,
    structureSelectedCount: Int,
    isMergedSelection: Boolean,
    onOpenCellMode: () -> Unit,
    onOpenStructureMode: () -> Unit,
    onOpenStyleMode: () -> Unit,
    onOpenFileNameMode: () -> Unit,
    onOpenPathMode: () -> Unit,
) {
    @Composable
    fun PanelHeader(title: String) {
        Text(title, style = DDZTypography.Body, color = DDZColor.TextPrimary)
    }

    @Composable
    fun PanelTitleRow(title: String, showClose: Boolean = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = DDZTypography.Body, color = DDZColor.TextPrimary)
            if (showClose) {
                Text(
                    text = "닫기",
                    style = DDZTypography.Caption,
                    color = DDZColor.Primary,
                    modifier = Modifier
                        .clip(BottomEditorToolShape)
                        .clickable(onClick = onClosePanel)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }

    @Composable
    fun CompactIconAction(
        modifier: Modifier = Modifier,
        label: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        enabled: Boolean = true,
        selected: Boolean = false,
        compact: Boolean = false,
        onClick: () -> Unit
    ) {
        Column(
            modifier = modifier
                .clip(BottomEditorToolShape)
                .background(
                    if (selected) DDZColor.Primary.copy(alpha = 0.2f) else DDZColor.Card.copy(alpha = if (enabled) 0.55f else 0.35f),
                    BottomEditorToolShape
                )
                .clickable(enabled = enabled, onClick = onClick)
                .heightIn(min = if (compact) 36.dp else 42.dp)
                .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 5.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                tint = when {
                    !enabled -> DDZColor.TextMuted.copy(alpha = 0.5f)
                    selected -> DDZColor.PrimaryDark
                    else -> DDZColor.TextPrimary
                }
            )
            Text(
                label,
                style = DDZTypography.Caption,
                color = when {
                    !enabled -> DDZColor.TextMuted.copy(alpha = 0.6f)
                    selected -> DDZColor.PrimaryDark
                    else -> DDZColor.TextMuted
                },
                maxLines = 1
            )
        }
    }

    @Composable
    fun CompactTextAction(
        modifier: Modifier = Modifier,
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        Box(
            modifier = modifier
                .clip(BottomEditorToolShape)
                .background(if (selected) DDZColor.Primary.copy(alpha = 0.18f) else DDZColor.Card.copy(alpha = 0.5f), BottomEditorToolShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = DDZTypography.Caption,
                color = if (selected) DDZColor.PrimaryDark else DDZColor.TextMuted
            )
        }
    }

    @Composable
    fun TableScaleSliderRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                modifier = Modifier.width(24.dp),
                style = DDZTypography.Caption,
                color = DDZColor.TextMuted
            )
            Slider(
                modifier = Modifier.weight(1f),
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt().coerceIn(10, 100)) },
                valueRange = 10f..100f
            )
            Text(
                "${value}%",
                modifier = Modifier.width(38.dp),
                style = DDZTypography.Caption,
                color = DDZColor.TextMuted
            )
        }
    }


    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DDZColor.Card, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 정책 추가: 패널 내부 최상단 구분선을 고정 배치해 본문과 경계를 명확히 한다.
        HorizontalDivider(color = DDZColor.Border.copy(alpha = 0.75f), thickness = 1.dp)
        Spacer(Modifier.size(2.dp))

        when (panelMode) {
            BottomEditorPanelMode.NONE -> Unit
            BottomEditorPanelMode.STRUCTURE_EDIT -> {
                val modeScroll = rememberScrollState()
                PanelTitleRow("레이아웃", showClose = false)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(modeScroll),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Spacer(Modifier.size(1.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactIconAction(modifier = Modifier.weight(1f), label = "+ 행", icon = Icons.Filled.ViewStream, onClick = onAddRow)
                        CompactIconAction(modifier = Modifier.weight(1f), label = "+ 열", icon = Icons.Filled.ViewColumn, onClick = onAddCol)
                        CompactIconAction(
                            modifier = Modifier.weight(1f),
                            label = if (isMergedSelection) "병합 해제" else "병합",
                            icon = Icons.Filled.Refresh,
                            enabled = isMergedSelection || structureSelectedCount >= 2,
                            onClick = onMergeSelection,
                        )
                        CompactIconAction(
                            modifier = Modifier.weight(1f),
                            label = "삭제",
                            icon = Icons.Filled.Remove,
                            enabled = structureSelectedCount >= 1,
                            onClick = onDeleteSelection,
                        )
                    }

                }
            }
            BottomEditorPanelMode.STYLE_EDIT -> {
                val modeScroll = rememberScrollState()
                PanelTitleRow("표 스타일")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(modeScroll),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(Modifier.size(2.dp))
                    Text("배경 스타일", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactTextAction(modifier = Modifier.weight(1f), label = "어둡게", selected = wmBgStyle == 0, onClick = { onSetBgStyle(0) })
                        CompactTextAction(modifier = Modifier.weight(1f), label = "밝게", selected = wmBgStyle == 1, onClick = { onSetBgStyle(1) })
                        CompactTextAction(modifier = Modifier.weight(1f), label = "투명", selected = wmBgStyle == 2, onClick = { onSetBgStyle(2) })
                    }
                    if (wmBgStyle == 2) {
                        Text("투명도", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                        Slider(
                            value = wmBgAlpha.toFloat(),
                            onValueChange = { onSetBgAlpha(it.toInt().coerceIn(0, 255)) },
                            valueRange = 0f..255f,
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("그리드", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                        Switch(checked = wmGridEnabled, onCheckedChange = onSetGridEnabled)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactIconAction(modifier = Modifier.weight(1f), label = "글씨 -", icon = Icons.Filled.Remove, onClick = { onSetValueScale((wmValueScale - 10).coerceAtLeast(60)) })
                        CompactIconAction(modifier = Modifier.weight(1f), label = "글씨 +", icon = Icons.Filled.Add, onClick = { onSetValueScale((wmValueScale + 10).coerceAtMost(160)) })
                    }
                    Text("정렬", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactTextAction(modifier = Modifier.weight(1f), label = "좌", selected = wmTextAlign == 0, onClick = { onSetTextAlign(0) })
                        CompactTextAction(modifier = Modifier.weight(1f), label = "중", selected = wmTextAlign == 1, onClick = { onSetTextAlign(1) })
                        CompactTextAction(modifier = Modifier.weight(1f), label = "우", selected = wmTextAlign == 2, onClick = { onSetTextAlign(2) })
                    }
                }
            }
            BottomEditorPanelMode.CELL_EDIT -> {
                // 주요 정책: CELL_EDIT는 값 입력과 설정만 제공하고, 저장/초기화/언두는 메인 3버튼으로 통일한다.
                val cellEditBodyScrollState = rememberScrollState()

                PanelTitleRow("선택한 셀")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(cellEditBodyScrollState),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedCell == null) {
                        Text("편집할 셀을 먼저 선택하세요.", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    } else {
                        val currentValue = resolvedByCellId[selectedCell.cellId].orEmpty().ifBlank { dataTypeLabelKo(selectedCell.dataType) }
                        val editableSourceValue = selectedCell.toEditableText()
                        val topValue = if (editingCellId == selectedCell.cellId) editingValue else editableSourceValue

                        Text("현재값", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                        Spacer(Modifier.size(2.dp))
                        when (selectedCell.dataType) {
                            TableCellDataType.TEXT,
                            TableCellDataType.NUMBER -> {
                                val focusRequester = androidx.compose.runtime.remember(
                                    selectedCell.cellId,
                                    selectedCell.dataType,
                                ) { FocusRequester() }
                                val keyboardController = LocalSoftwareKeyboardController.current
                                LaunchedEffect(selectedCell.cellId, selectedCell.dataType) {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                                OutlinedTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .focusRequester(focusRequester),
                                    value = topValue,
                                    onValueChange = { next ->
                                        if (editingCellId != selectedCell.cellId) {
                                            onStartInlineEditing(selectedCell.cellId, editableSourceValue)
                                        }
                                        onEditingValueChange(next)
                                    },
                                    singleLine = true,
                                    textStyle = DDZTypography.Body.copy(color = DDZColor.TextPrimary),
                                    placeholder = {
                                        Text("값 입력", color = DDZColor.TextMuted)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = DDZColor.TextPrimary,
                                        unfocusedTextColor = DDZColor.TextPrimary,
                                        cursorColor = DDZColor.Primary,
                                        focusedBorderColor = DDZColor.Primary,
                                        unfocusedBorderColor = DDZColor.Border
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = if (selectedCell.dataType == TableCellDataType.NUMBER) KeyboardType.Decimal else KeyboardType.Text
                                    )
                                )
                            }

                            TableCellDataType.COUNTER -> {
                                val counterDisplay = when {
                                    editingCellId == selectedCell.cellId -> editingValue
                                    selectedCell.toEditableText().isNotBlank() -> selectedCell.toEditableText()
                                    else -> autoNextCounterValue.toString()
                                }
                                val parsedCounter = counterDisplay.toIntOrNull()?.coerceAtLeast(1) ?: 1

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CompactIconAction(
                                        modifier = Modifier.width(56.dp),
                                        label = "-",
                                        icon = Icons.Filled.Remove,
                                        onClick = {
                                            if (editingCellId != selectedCell.cellId) {
                                                onStartInlineEditing(selectedCell.cellId, parsedCounter.toString())
                                            }
                                            onEditingValueChange((parsedCounter - 1).coerceAtLeast(1).toString())
                                        }
                                    )

                                    OutlinedTextField(
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        value = parsedCounter.toString(),
                                        onValueChange = { raw ->
                                            if (editingCellId != selectedCell.cellId) {
                                                onStartInlineEditing(selectedCell.cellId, parsedCounter.toString())
                                            }
                                            val next = raw.filter { it.isDigit() }
                                            val normalized = if (next.isBlank()) "1" else (next.toIntOrNull() ?: 1).coerceAtLeast(1).toString()
                                            onEditingValueChange(normalized)
                                        },
                                        singleLine = true,
                                        textStyle = DDZTypography.Body.copy(color = DDZColor.TextPrimary),
                                        placeholder = {
                                            Text("값 입력", color = DDZColor.TextMuted)
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = DDZColor.TextPrimary,
                                            unfocusedTextColor = DDZColor.TextPrimary,
                                            cursorColor = DDZColor.Primary,
                                            focusedBorderColor = DDZColor.Primary,
                                            unfocusedBorderColor = DDZColor.Border
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )

                                    CompactIconAction(
                                        modifier = Modifier.width(56.dp),
                                        label = "+",
                                        icon = Icons.Filled.Add,
                                        onClick = {
                                            if (editingCellId != selectedCell.cellId) {
                                                onStartInlineEditing(selectedCell.cellId, parsedCounter.toString())
                                            }
                                            onEditingValueChange((parsedCounter + 1).toString())
                                        }
                                    )
                                }
                            }

                            TableCellDataType.DATE,
                            TableCellDataType.TIME,
                            TableCellDataType.ROTATING_TEXT -> {
                                Text(
                                    text = currentValue,
                                    style = DDZTypography.Body,
                                    color = DDZColor.TextPrimary
                                )
                            }
                        }

                        Spacer(Modifier.size(2.dp))
                        CellSettingsBottomPanel(
                            modifier = Modifier.fillMaxWidth(),
                            cell = selectedCell,
                            templateState = templateState,
                            baseResolvedByCellId = resolvedByCellId,
                            hasGroup1 = hasGroup1,
                            hasGroup2 = hasGroup2,
                            onToggleFileNameForCell = onToggleFileNameForSelected,
                            onReorderFileNameSlots = onReorderFileNameSlots,
                            onPathGroupAction = onPathGroupActionForSelected,
                            onSetDataType = onSetDataTypeForSelected,
                            onSetCounterScopeMode = onSetCounterScopeModeForSelected,
                            onResetCounterSeed = onResetCounterSeedForSelected,
                            autoNextCounterValue = autoNextCounterValue,
                            onOpenRotatingTemplateDialog = { onOpenRotatingTemplateDialogForSelected(selectedCell.cellId) },
                            onOpenFormatDialog = onOpenFormatDialog,
                            previewNow = previewNow,
                            previewCounterDigits = previewCounterDigits,
                            scopeNextCounter = scopeNextCounter,
                            phraseProgressCursor = phraseProgressCursor,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat,
                            phraseSets = phraseSets,
                            showFileNameSection = false,
                            showPathGroupSection = false,
                            compactForBottomPanel = true
                        )
                    }
                }
            }

            BottomEditorPanelMode.FILENAME_EDIT -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompactTextAction(
                        modifier = Modifier.weight(1f),
                        label = "파일명",
                        selected = true,
                        onClick = onOpenFileNameMode,
                    )
                    CompactTextAction(
                        modifier = Modifier.weight(1f),
                        label = "저장경로",
                        selected = false,
                        onClick = onOpenPathMode,
                    )
                }
                Spacer(Modifier.height(8.dp))
                val normalizedSlots = List(3) { index -> fileNameSlotItems.getOrNull(index) }
                val canMoveLeft = selectedFileNameSlot?.let {
                    normalizedSlots.getOrNull(it) != null && normalizedSlots.getOrNull(it - 1) != null
                } == true
                val canMoveRight = selectedFileNameSlot?.let {
                    normalizedSlots.getOrNull(it) != null && normalizedSlots.getOrNull(it + 1) != null
                } == true
                val canDelete = selectedFileNameSlot?.let { normalizedSlots.getOrNull(it) != null } == true

                val fileNamePanelScrollState = rememberScrollState()
                PanelHeader("파일명 구성 편집")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(fileNamePanelScrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    normalizedSlots.forEachIndexed { index, slot ->
                        val isSelected = selectedFileNameSlot == index
                        val structureLabel = fileNameStructureLabel(slot, templateState)
                        val resolvedValue = fileNameSimpleValue(slot, resolvedByCellId, resolvedCells, previewNow, dateFormat, timeFormat)
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(BottomEditorSlotShape)
                                .background(if (isSelected) DDZColor.PrimaryDark.copy(alpha = 0.20f) else DDZColor.Card.copy(alpha = 0.75f), BottomEditorSlotShape)
                                .border(1.dp, if (isSelected) DDZColor.Primary else DDZColor.Border, BottomEditorSlotShape)
                                .clickable {
                                    onSelectFileNameSlot(index)
                                    if (slot == null) onFillEmptyFileNameSlot(index)
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(structureLabel, style = DDZTypography.Caption, color = DDZColor.TextMuted, maxLines = 1)
                                Text(
                                    text = if (slot == null) "+" else resolvedValue.ifBlank { "값 없음" },
                                    style = DDZTypography.Body,
                                    color = if (slot == null) DDZColor.TextMuted else DDZColor.TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Text("선택된 요소 조정", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = onMoveSelectedFileNameSlotLeft, enabled = canMoveLeft) { Text("← 이동", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onMoveSelectedFileNameSlotRight, enabled = canMoveRight) { Text("→ 이동", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onDeleteSelectedFileNameSlot, enabled = canDelete) { Text("삭제", style = DDZTypography.ButtonText) }
                }

                val hasSelectedSlot = selectedFileNameSlot != null
                Text("요소 선택", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = onStartFileNameCellPick, enabled = hasSelectedSlot) { Text("셀", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onStartManualInputEditor, enabled = hasSelectedSlot) { Text("직접입력", style = DDZTypography.ButtonText) }
                }

                if (showManualInputEditor && hasSelectedSlot) {
                    Text("직접입력", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = manualInputDraft,
                        onValueChange = onManualInputDraftChange,
                        singleLine = true,
                        textStyle = DDZTypography.Body.copy(color = DDZColor.TextPrimary),
                        placeholder = { Text("텍스트를 입력하세요", color = DDZColor.TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DDZColor.TextPrimary,
                            unfocusedTextColor = DDZColor.TextPrimary,
                            cursorColor = DDZColor.Primary,
                            focusedBorderColor = DDZColor.Primary,
                            unfocusedBorderColor = DDZColor.Border
                        )
                    )
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onApplyManualInput, enabled = manualInputDraft.trim().isNotBlank()) { Text("적용", style = DDZTypography.ButtonText) }
                }

                if (isFileNameCellPickMode && hasSelectedSlot) {
                    Text("셀 선택 대기 중: 위 표에서 셀을 탭하면 현재 슬롯에 연결됩니다.", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                }
                }
            }

            BottomEditorPanelMode.PATH_EDIT -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompactTextAction(
                        modifier = Modifier.weight(1f),
                        label = "파일명",
                        selected = false,
                        onClick = onOpenFileNameMode,
                    )
                    CompactTextAction(
                        modifier = Modifier.weight(1f),
                        label = "저장경로",
                        selected = true,
                        onClick = onOpenPathMode,
                    )
                }
                Spacer(Modifier.height(8.dp))
                val normalizedSlots = List(3) { index -> pathSlotItems.getOrNull(index) }
                val canMoveLeft = selectedPathSlot?.let {
                    normalizedSlots.getOrNull(it) != null && normalizedSlots.getOrNull(it - 1) != null
                } == true
                val canMoveRight = selectedPathSlot?.let {
                    normalizedSlots.getOrNull(it) != null && normalizedSlots.getOrNull(it + 1) != null
                } == true
                val canDelete = selectedPathSlot?.let { normalizedSlots.getOrNull(it) != null } == true
                val hasSelectedSlot = selectedPathSlot != null

                val pathPanelScrollState = rememberScrollState()
                PanelHeader("저장경로 구성 편집")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(pathPanelScrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    normalizedSlots.forEachIndexed { index, slot ->
                        val isSelected = selectedPathSlot == index
                        val structureLabel = pathStructureLabel(slot, templateState)
                        val resolvedValue = pathSimpleValue(slot, resolvedByCellId, resolvedCells, previewNow, dateFormat, timeFormat)
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(BottomEditorSlotShape)
                                .background(if (isSelected) DDZColor.PrimaryDark.copy(alpha = 0.20f) else DDZColor.Card.copy(alpha = 0.75f), BottomEditorSlotShape)
                                .border(1.dp, if (isSelected) DDZColor.Primary else DDZColor.Border, BottomEditorSlotShape)
                                .clickable {
                                    onSelectPathSlot(index)
                                    if (slot == null) onFillEmptyPathSlot(index)
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(structureLabel, style = DDZTypography.Caption, color = DDZColor.TextMuted, maxLines = 1)
                                Text(
                                    text = if (slot == null) "+" else resolvedValue.ifBlank { "값 없음" },
                                    style = DDZTypography.Body,
                                    color = if (slot == null) DDZColor.TextMuted else DDZColor.TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Text("선택된 요소 조정", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = onMoveSelectedPathSlotLeft, enabled = canMoveLeft) { Text("← 이동", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onMoveSelectedPathSlotRight, enabled = canMoveRight) { Text("→ 이동", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onDeleteSelectedPathSlot, enabled = canDelete) { Text("삭제", style = DDZTypography.ButtonText) }
                }

                Text("요소 선택", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = onStartPathCellPick, enabled = hasSelectedSlot) { Text("셀", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onStartPathManualInputEditor, enabled = hasSelectedSlot) { Text("직접입력", style = DDZTypography.ButtonText) }
                }

                if (showPathManualInputEditor && hasSelectedSlot) {
                    Text("직접입력", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = pathManualInputDraft,
                        onValueChange = onPathManualInputDraftChange,
                        singleLine = true,
                        textStyle = DDZTypography.Body.copy(color = DDZColor.TextPrimary),
                        placeholder = { Text("경로 텍스트를 입력하세요", color = DDZColor.TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DDZColor.TextPrimary,
                            unfocusedTextColor = DDZColor.TextPrimary,
                            cursorColor = DDZColor.Primary,
                            focusedBorderColor = DDZColor.Primary,
                            unfocusedBorderColor = DDZColor.Border
                        )
                    )
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onApplyPathManualInput, enabled = pathManualInputDraft.trim().isNotBlank()) { Text("적용", style = DDZTypography.ButtonText) }
                }

                if (isPathCellPickMode && hasSelectedSlot) {
                    Text("셀 선택 대기 중: 위 표에서 셀을 탭하면 현재 경로 슬롯에 연결됩니다.", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                }
                }
            }
        }

    }
}



private fun fileNameStructureLabel(slot: FileNameSlotUiItem?, templateState: TableTemplateState): String {
    if (slot == null) return "빈 슬롯"
    return when (slot.kind) {
        com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotKind.CELL -> {
            val cell = templateState.cells.firstOrNull { it.cellId == slot.cellId }
            if (cell == null) "셀" else "셀(${cell.rowIndex + 1},${cell.colIndex + 1})"
        }
        com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotKind.MANUAL -> "직접입력"
        com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotKind.FORMAT -> when (slot.formatType) {
            FileNameFormatType.DATE -> "날짜"
            FileNameFormatType.TIME -> "시간"
            // 주요 정책: 카운터는 시스템이 파일명 마지막에 자동 부여한다(신규 선택 비노출, 레거시 읽기만).
            FileNameFormatType.COUNTER -> "시스템 카운터(자동)"
            FileNameFormatType.ROTATING_TEXT -> "순환문구"
            null -> "서식"
        }
    }
}

private fun pathStructureLabel(slot: PathSlotUiItem?, templateState: TableTemplateState): String {
    if (slot == null) return "빈 슬롯"
    return when (slot.kind) {
        com.dudoziworkshop.dzlog.ui.table.section.PathSlotKind.CELL -> {
            val cell = templateState.cells.firstOrNull { it.cellId == slot.cellId }
            if (cell == null) "셀" else "셀(${cell.rowIndex + 1},${cell.colIndex + 1})"
        }
        com.dudoziworkshop.dzlog.ui.table.section.PathSlotKind.MANUAL -> "직접입력"
        com.dudoziworkshop.dzlog.ui.table.section.PathSlotKind.FORMAT -> when (slot.formatType) {
            PathFormatType.DATE -> "날짜"
            PathFormatType.TIME -> "시간"
            PathFormatType.ROTATING_TEXT -> "순환문구"
            null -> "서식"
        }
    }
}

private fun fileNameSimpleValue(
    slot: FileNameSlotUiItem?,
    resolvedByCellId: Map<String, String>,
    resolvedCells: List<ResolvedCell>,
    previewNow: Date,
    dateFormat: String,
    timeFormat: String,
): String {
    if (slot == null) return ""
    return if (slot.kind == com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotKind.CELL) {
        resolvedByCellId[slot.cellId].orEmpty()
    } else {
        resolveFileNameDraftToken(
            draft = TableEditorSlotDraft(
                kind = slot.kind.name,
                label = slot.label,
                cellId = slot.cellId,
                manualText = slot.manualText,
                formatType = slot.formatType?.name,
            ),
            resolvedCells = resolvedCells,
            now = previewNow,
            dateFormat = dateFormat,
            timeFormat = timeFormat,
        )
    }
}

private fun pathSimpleValue(
    slot: PathSlotUiItem?,
    resolvedByCellId: Map<String, String>,
    resolvedCells: List<ResolvedCell>,
    previewNow: Date,
    dateFormat: String,
    timeFormat: String,
): String {
    if (slot == null) return ""
    return if (slot.kind == com.dudoziworkshop.dzlog.ui.table.section.PathSlotKind.CELL) {
        resolvedByCellId[slot.cellId].orEmpty()
    } else {
        resolvePathDraftToken(
            draft = TableEditorSlotDraft(
                kind = slot.kind.name,
                label = slot.label,
                cellId = slot.cellId,
                manualText = slot.manualText,
                formatType = slot.formatType?.name,
            ),
            resolvedCells = resolvedCells,
            now = previewNow,
            dateFormat = dateFormat,
            timeFormat = timeFormat,
        )
    }
}

private fun dataTypeLabelKo(dataType: TableCellDataType): String =
    when (dataType) {
        TableCellDataType.TEXT -> "텍스트"
        TableCellDataType.NUMBER -> "숫자"
        TableCellDataType.COUNTER -> "카운터"
        TableCellDataType.DATE -> "날짜"
        TableCellDataType.TIME -> "시간"
        TableCellDataType.ROTATING_TEXT -> "순환문구"
    }
