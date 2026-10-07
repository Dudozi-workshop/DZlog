package com.dudoziworkshop.dzlog.ui.table.section

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.table.BottomEditorPanel
import com.dudoziworkshop.dzlog.ui.table.CellSettingsBottomPanel
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private val BottomBarReserveHeight = 160.dp

@Composable
fun LayoutTabContent(
    uiState: LayoutTabUiState,
    actions: LayoutTabActions,
    modifier: Modifier = Modifier
) {
    val isInlineEditing = uiState.editingCellId != null

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        LaunchedEffect(uiState.bottomPanelMode, uiState.selectedCellId, uiState.templateState.cells) {
            val isTableDetailMode =
                uiState.bottomPanelMode == BottomEditorPanelMode.CELL_EDIT ||
                    uiState.bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT ||
                    uiState.bottomPanelMode == BottomEditorPanelMode.STYLE_EDIT
            if (!isTableDetailMode || uiState.selectedCellId != null) return@LaunchedEffect
            val firstCellId = uiState.templateState.cells
                .minWithOrNull(
                    compareBy<com.dudoziworkshop.dzlog.domain.model.TableCellState> { it.rowIndex }
                        .thenBy { it.colIndex }
                )
                ?.cellId
            if (firstCellId != null) {
                actions.onSelectCellId(firstCellId)
            }
        }

        val density = LocalDensity.current
        var previewBottomPx by remember { mutableFloatStateOf(0f) }
        val screenHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
        val panelTopSpacingPx = with(density) { 8.dp.toPx() }
        val panelAvailableHeightDp = with(density) {
            // 정책 변경: 고정 380dp 제한을 제거하고, 표 프리뷰 하단 기준 남은 높이를 패널 최대 높이로 사용한다.
            (screenHeightPx - previewBottomPx - panelTopSpacingPx).coerceAtLeast(0f).toDp()
        }
        val contentColumnModifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
                .let { baseModifier ->
                    if (isInlineEditing) {
                        baseModifier.imePadding()
                    } else {
                        baseModifier.padding(bottom = BottomBarReserveHeight)
                    }
                }

        Column(
            modifier = contentColumnModifier
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (uiState.bottomPanelMode == BottomEditorPanelMode.NONE) {
                    Text(
                        text = "수정할 셀을 눌러보세요",
                        style = DDZTypography.Caption,
                        color = DDZColor.TextMuted,
                    )
                    Spacer(Modifier.height(10.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .onGloballyPositioned { coordinates ->
                            previewBottomPx = coordinates.boundsInParent().bottom
                        }
                        .background(DDZColor.Card, RoundedCornerShape(14.dp))
                        .padding(6.dp)
                ) {
                    RealTableGridSection(
                        templateState = uiState.templateState,
                        displayTextProvider = { cellId ->
                            val selected = uiState.selectedCell
                            val isPanelEditingTextCell =
                                selected != null &&
                                    selected.cellId == cellId &&
                                    uiState.editingCellId == cellId &&
                                    (selected.dataType == com.dudoziworkshop.dzlog.domain.model.TableCellDataType.TEXT ||
                                        selected.dataType == com.dudoziworkshop.dzlog.domain.model.TableCellDataType.NUMBER ||
                                        selected.dataType == com.dudoziworkshop.dzlog.domain.model.TableCellDataType.COUNTER)
                            if (isPanelEditingTextCell) {
                                // 주요 정책: 표 내부 인라인 에디터를 제거했으므로, CELL_EDIT 입력 UI 상태(editingValue)를 표 렌더 텍스트에 즉시 반영한다.
                                uiState.editingValue
                            } else {
                                uiState.plan.resolvedCells.firstOrNull { it.id == cellId }?.resolvedText.orEmpty()
                            }
                        },
                        selectedCellId = uiState.selectedCellId,
                        editingCellId = uiState.editingCellId,
                        wmWidthRatio = uiState.wmWidthRatio,
                        wmBgStyle = uiState.wmBgStyle,
                        wmBgAlpha = uiState.wmBgAlpha,
                        wmValueScale = uiState.wmValueScale,
                        wmTextColorMode = uiState.wmTextColorMode,
                        wmManualTextColor = uiState.wmManualTextColor,
                        wmTextAlign = uiState.wmTextAlign,
                        wmGridEnabled = uiState.wmGridEnabled,
                        isStructureMode = uiState.bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT,
                        structureSelectedCellIds = uiState.structureSelectedCellIds,
                        onSelectCell = { id ->
                            // 정책 변경: FILENAME_EDIT + 셀 선택 대기 상태에서는 표 셀 탭을
                            // CELL_EDIT 진입이 아니라 "파일명 슬롯 셀 연결"로 우선 처리한다.
                            if (uiState.bottomPanelMode == BottomEditorPanelMode.FILENAME_EDIT &&
                                uiState.isFileNameCellPickMode &&
                                id != null
                            ) {
                                actions.onSelectCellId(id)
                                actions.onBindSelectedSlotToCell(id)
                                return@RealTableGridSection
                            }
                            if (uiState.bottomPanelMode == BottomEditorPanelMode.PATH_EDIT &&
                                uiState.isPathCellPickMode &&
                                id != null
                            ) {
                                actions.onSelectCellId(id)
                                actions.onBindSelectedPathSlotToCell(id)
                                return@RealTableGridSection
                            }

                            if (uiState.editingCellId != null && uiState.editingCellId != id) {
                                if (!actions.onTryCommitInlineAndContinue()) return@RealTableGridSection
                            }
                            actions.onSelectCellId(id)
                            if (uiState.bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) {
                                // 구조 모드에서는 셀 편집 진입 차단, 선택만 수행
                                actions.onShowCellSettingsPanel(false)
                                return@RealTableGridSection
                            }
                            // 정책 변경: 셀 편집 진입점은 단일하게 CELL_EDIT 패널로 고정한다(더블탭 인라인 편집 제거).
                            actions.onChangeBottomPanelMode(BottomEditorPanelMode.CELL_EDIT)
                            actions.onShowCellSettingsPanel(false)
                        },
                        onDoubleClickCell = { cell ->
                            // 정책 변경: 더블탭도 단일탭과 동일하게 "셀 선택 + CELL_EDIT 패널" 흐름만 사용한다.
                            if (uiState.bottomPanelMode == BottomEditorPanelMode.FILENAME_EDIT && uiState.isFileNameCellPickMode) {
                                actions.onSelectCellId(cell.cellId)
                                actions.onBindSelectedSlotToCell(cell.cellId)
                                return@RealTableGridSection
                            }
                            if (uiState.bottomPanelMode == BottomEditorPanelMode.PATH_EDIT && uiState.isPathCellPickMode) {
                                actions.onSelectCellId(cell.cellId)
                                actions.onBindSelectedPathSlotToCell(cell.cellId)
                                return@RealTableGridSection
                            }
                            if (uiState.editingCellId != null && uiState.editingCellId != cell.cellId) {
                                if (!actions.onTryCommitInlineAndContinue()) return@RealTableGridSection
                            }
                            actions.onSelectCellId(cell.cellId)
                            if (uiState.bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) {
                                actions.onShowCellSettingsPanel(false)
                                return@RealTableGridSection
                            }
                            actions.onChangeBottomPanelMode(BottomEditorPanelMode.CELL_EDIT)
                            actions.onShowCellSettingsPanel(false)
                        },
                        onSelectRange = actions.onSelectStructureRange,
                        onCommitRowWeights = actions.onCommitRowWeights,
                        onCommitColumnWeights = actions.onCommitColumnWeights,
                    )
                }

                // 정책 보강: 안내 문구 제거 후에도 하단 패널 가림을 피하기 위해 여유 공간만 유지한다.
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(DDZColor.Background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val isLayoutActive = uiState.bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT
            val isStyleActive = uiState.bottomPanelMode == BottomEditorPanelMode.STYLE_EDIT
            val isSaveRulesActive =
                uiState.bottomPanelMode == BottomEditorPanelMode.FILENAME_EDIT ||
                    uiState.bottomPanelMode == BottomEditorPanelMode.PATH_EDIT

            CompactBottomNavButton(
                modifier = Modifier.weight(1f),
                label = "레이아웃",
                selected = isLayoutActive,
                onClick = {
                    if (isLayoutActive) actions.onCloseBottomPanel()
                    else actions.onChangeBottomPanelMode(BottomEditorPanelMode.STRUCTURE_EDIT)
                },
            )
            CompactBottomNavButton(
                modifier = Modifier.weight(1f),
                label = "스타일",
                selected = isStyleActive,
                onClick = {
                    if (isStyleActive) actions.onCloseBottomPanel()
                    else actions.onChangeBottomPanelMode(BottomEditorPanelMode.STYLE_EDIT)
                },
            )
            CompactBottomNavButton(
                modifier = Modifier.weight(1f),
                label = "저장 규칙",
                selected = isSaveRulesActive,
                onClick = {
                    if (isSaveRulesActive) actions.onCloseBottomPanel()
                    else actions.onChangeBottomPanelMode(BottomEditorPanelMode.FILENAME_EDIT)
                },
            )
        }

        if (uiState.bottomPanelMode != BottomEditorPanelMode.NONE) {
            BottomEditorPanel(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .heightIn(max = (panelAvailableHeightDp - 72.dp).coerceAtLeast(0.dp))
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 72.dp),
                panelMode = uiState.bottomPanelMode,
            rows = uiState.templateState.rows,
            cols = uiState.templateState.cols,
            isSaving = uiState.isSavingTemplate,
            selectedCell = uiState.selectedCell,
            editingCellId = uiState.editingCellId,
            editingValue = uiState.editingValue,
            templateState = uiState.templateState,
            resolvedByCellId = uiState.resolvedByCellId,
            resolvedCells = uiState.plan.resolvedCells,
            hasGroup1 = uiState.hasGroup1,
            hasGroup2 = uiState.hasGroup2,
            previewNow = uiState.previewNow,
            previewCounterDigits = uiState.previewCounterDigits,
            scopeNextCounter = uiState.scopeNextCounter,
            phraseProgressCursor = uiState.phraseProgressCursor,
            dateFormat = uiState.dateFormat,
            timeFormat = uiState.timeFormat,
            phraseSets = uiState.phraseSets,
            autoNextCounterValue = uiState.autoNextCounterValue,
            fileNameSlotItems = uiState.fileNameSlotItems,
            selectedFileNameSlot = uiState.currentlySelectedFileNameSlot,
            onToggleFileNameForSelected = actions.onToggleFileNameForSelected,
            onReorderFileNameSlots = actions.onReorderFileNameSlots,
            onPathGroupActionForSelected = actions.onPathGroupActionForSelected,
            onSetDataTypeForSelected = actions.onSetDataTypeForSelected,
            onSetCounterScopeModeForSelected = actions.onSetCounterScopeModeForSelected,
            onResetCounterSeedForSelected = actions.onResetCounterSeedForSelected,
            onOpenFormatDialog = actions.onOpenFormatDialog,
            onOpenRotatingTemplateDialogForSelected = actions.onOpenRotatingTemplateDialogForSelected,
            onStartInlineEditing = actions.onStartInlineEditing,
            onEditingValueChange = actions.onEditingValueChange,
            onSelectFileNameSlot = actions.onSelectFileNameSlot,
            onFillEmptyFileNameSlot = actions.onFillEmptyFileNameSlot,
            onMoveSelectedFileNameSlotLeft = actions.onMoveSelectedFileNameSlotLeft,
            onMoveSelectedFileNameSlotRight = actions.onMoveSelectedFileNameSlotRight,
            onDeleteSelectedFileNameSlot = actions.onDeleteSelectedFileNameSlot,
            isFileNameCellPickMode = uiState.isFileNameCellPickMode,
            manualInputDraft = uiState.manualInputDraft,
            showManualInputEditor = uiState.showManualInputEditor,
            onStartFileNameCellPick = actions.onStartFileNameCellPick,
            onStartManualInputEditor = actions.onStartManualInputEditor,
            onManualInputDraftChange = actions.onManualInputDraftChange,
            onApplyManualInput = actions.onApplyManualInput,
            pathSlotItems = uiState.pathSlotItems,
            selectedPathSlot = uiState.currentlySelectedPathSlot,
            isPathCellPickMode = uiState.isPathCellPickMode,
            showPathManualInputEditor = uiState.showPathManualInputEditor,
            pathManualInputDraft = uiState.pathManualInputDraft,
            onSelectPathSlot = actions.onSelectPathSlot,
            onFillEmptyPathSlot = actions.onFillEmptyPathSlot,
            onMoveSelectedPathSlotLeft = actions.onMoveSelectedPathSlotLeft,
            onMoveSelectedPathSlotRight = actions.onMoveSelectedPathSlotRight,
            onDeleteSelectedPathSlot = actions.onDeleteSelectedPathSlot,
            onStartPathCellPick = actions.onStartPathCellPick,
            onStartPathManualInputEditor = actions.onStartPathManualInputEditor,
            onPathManualInputDraftChange = actions.onPathManualInputDraftChange,
            onApplyPathManualInput = actions.onApplyPathManualInput,
            onClosePanel = actions.onCloseBottomPanel,
            onAddRow = actions.onAddRow,
            onAddCol = actions.onAddCol,
            onMergeSelection = actions.onMergeSelection,
            onDeleteSelection = actions.onDeleteSelection,
            onUndo = actions.onUndo,
            isUndoAvailable = uiState.isUndoAvailable,
            onReset = actions.onReset,
            onSave = actions.onSave,
            onSetBgStyle = actions.onSetBgStyle,
            onSetBgAlpha = actions.onSetBgAlpha,
            onSetGridEnabled = actions.onSetGridEnabled,
            onSetTextColorMode = actions.onSetTextColorMode,
            onSetManualTextColor = actions.onSetManualTextColor,
            onSetValueScale = actions.onSetValueScale,
            onSetTextAlign = actions.onSetTextAlign,
            onSetWmWidthRatio = actions.onSetWmWidthRatio,
            wmBgStyle = uiState.wmBgStyle,
            wmBgAlpha = uiState.wmBgAlpha,
            wmGridEnabled = uiState.wmGridEnabled,
            wmTextColorMode = uiState.wmTextColorMode,
            wmManualTextColor = uiState.wmManualTextColor,
            wmValueScale = uiState.wmValueScale,
            wmTextAlign = uiState.wmTextAlign,
                wmWidthRatio = uiState.wmWidthRatio,
                structureSelectedCount = uiState.structureSelectedCellIds.size,
                isMergedSelection = uiState.structureSelectedCellIds.singleOrNull()?.let { selectedId ->
                    uiState.templateState.cells.firstOrNull { it.cellId == selectedId }?.let { cell ->
                        cell.rowSpan > 1 || cell.colSpan > 1
                    }
                } == true,
                onOpenCellMode = { actions.onChangeBottomPanelMode(BottomEditorPanelMode.CELL_EDIT) },
                onOpenStructureMode = { actions.onChangeBottomPanelMode(BottomEditorPanelMode.STRUCTURE_EDIT) },
                onOpenStyleMode = { actions.onChangeBottomPanelMode(BottomEditorPanelMode.STYLE_EDIT) },
                onOpenFileNameMode = { actions.onChangeBottomPanelMode(BottomEditorPanelMode.FILENAME_EDIT) },
                onOpenPathMode = { actions.onChangeBottomPanelMode(BottomEditorPanelMode.PATH_EDIT) },
            )
        }

        if (uiState.showCellSettingsPanel && uiState.selectedCell != null && uiState.editingCellId == null && uiState.bottomPanelMode != BottomEditorPanelMode.CELL_EDIT) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DDZColor.PrimaryDark.copy(alpha = 0.4f))
                    .clickable { actions.onDismissSettingsPanel() }
            )

            CellSettingsBottomPanel(
                modifier = Modifier.align(Alignment.BottomCenter),
                cell = uiState.selectedCell,
                hasGroup1 = uiState.hasGroup1,
                hasGroup2 = uiState.hasGroup2,
                templateState = uiState.templateState,
                baseResolvedByCellId = uiState.resolvedByCellId,
                onToggleFileNameForCell = actions.onToggleFileNameForSelected,
                onReorderFileNameSlots = actions.onReorderFileNameSlots,
                onPathGroupAction = actions.onPathGroupActionForSelected,
                onSetDataType = actions.onSetDataTypeForSelected,
                onSetCounterScopeMode = actions.onSetCounterScopeModeForSelected,
                onResetCounterSeed = actions.onResetCounterSeedForSelected,
                autoNextCounterValue = uiState.autoNextCounterValue,
                onOpenRotatingTemplateDialog = {
                    uiState.selectedCell?.let { actions.onOpenRotatingTemplateDialogForSelected(it.cellId) }
                },
                                onOpenFormatDialog = actions.onOpenFormatDialog,
                previewNow = uiState.previewNow,
                previewCounterDigits = uiState.previewCounterDigits,
                scopeNextCounter = uiState.scopeNextCounter,
                phraseProgressCursor = uiState.phraseProgressCursor,
                dateFormat = uiState.dateFormat,
                timeFormat = uiState.timeFormat,
                phraseSets = uiState.phraseSets
            )
        }
    }
}

@Composable
private fun CompactBottomNavButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                color = if (selected) DDZColor.Primary.copy(alpha = 0.12f) else DDZColor.Card,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = DDZTypography.ButtonText,
            color = if (selected) DDZColor.Primary else DDZColor.TextPrimary,
        )
    }
}
