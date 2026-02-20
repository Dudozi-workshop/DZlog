package com.example.dzlog.ui.table.section

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.ui.common.DDZSectionHeader
import com.example.dzlog.ui.table.BottomFixedActionBar
import com.example.dzlog.ui.table.CellSettingsBottomPanel
import com.example.dzlog.ui.table.CompactPathHeader
import com.example.dzlog.ui.theme.DDZColor

@Composable
fun LayoutTabContent(
    uiState: LayoutTabUiState,
    actions: LayoutTabActions,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                CompactPathHeader(
                    savePath = uiState.savePathPreview,
                    fileName = uiState.filenamePreview,
                    counterModeLabel = uiState.counterModeLabel
                )

                Spacer(Modifier.height(10.dp))
                DDZSectionHeader(title = "GRID LAYOUT")
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .background(DDZColor.Card, RoundedCornerShape(14.dp))
                        .padding(10.dp)
                ) {
                    TableGridSection(
                        templateState = uiState.templateState,
                        displayTextProvider = { cellId ->
                            uiState.plan.resolvedCells.firstOrNull { it.id == cellId }?.resolvedText.orEmpty()
                        },
                        selectedCellId = uiState.selectedCellId,
                        editingCellId = uiState.editingCellId,
                        onSelectCell = { id ->
                            if (uiState.editingCellId != null && uiState.editingCellId != id) {
                                if (!actions.onTryCommitInlineAndContinue()) return@TableGridSection
                            }
                            actions.onSelectCellId(id)
                            actions.onShowCellSettingsPanel(true)
                        },
                        onDoubleClickCell = { cell ->
                            if (uiState.editingCellId != null && uiState.editingCellId != cell.cellId) {
                                if (!actions.onTryCommitInlineAndContinue()) return@TableGridSection
                                actions.onSelectCellId(cell.cellId)
                                actions.onShowCellSettingsPanel(true)
                                return@TableGridSection
                            }

                            actions.onSelectCellId(cell.cellId)
                            val canInline =
                                (cell.dataType == TableCellDataType.TEXT ||
                                    cell.dataType == TableCellDataType.NUMBER ||
                                    cell.dataType == TableCellDataType.COUNTER)

                            if (canInline) {
                                actions.onShowCellSettingsPanel(false)
                                actions.onStartInlineEditing(cell.cellId, cell.toEditableText())
                            } else {
                                if (cell.dataType == TableCellDataType.DATE || cell.dataType == TableCellDataType.TIME) {
                                    actions.onOpenFormatDialog(cell.cellId, cell.dataType)
                                } else if (cell.dataType == TableCellDataType.ROTATING_TEXT) {
                                    actions.onOpenRotatingTemplateDialogForSelected(cell.cellId)
                                } else {
                                    actions.onShowCellSettingsPanel(true)
                                }
                            }
                        },
                        editingValue = uiState.editingValue,
                        onEditingValueChange = actions.onEditingValueChange,
                        onCommitInline = actions.onCommitInline,
                        inlineFocusRequester = uiState.inlineFocusRequester,
                        onInlineFocusLostCommit = actions.onInlineFocusLostCommit
                    )
                }

                Spacer(Modifier.height(10.dp))
            }

            BottomFixedActionBar(
                rows = uiState.templateState.rows,
                cols = uiState.templateState.cols,
                isSaving = uiState.isSavingTemplate,
                onAddRow = actions.onAddRow,
                onRemoveRow = actions.onRemoveRow,
                onAddCol = actions.onAddCol,
                onRemoveCol = actions.onRemoveCol,
                onReset = actions.onReset,
                onSave = actions.onSave
            )
        }

        if (uiState.showCellSettingsPanel && uiState.selectedCell != null && uiState.editingCellId == null) {
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
                onToggleFileNameForCell = actions.onToggleFileNameForSelected,
                onReorderFileNameSlots = actions.onReorderFileNameSlots,
                onPathGroupAction = actions.onPathGroupActionForSelected,
                onSetDataType = actions.onSetDataTypeForSelected,
                onResetCounterSeed = actions.onResetCounterSeedForSelected,
                autoNextCounterValue = uiState.autoNextCounterValue,
                onOpenRotatingTemplateDialog = {
                    uiState.selectedCell?.let { actions.onOpenRotatingTemplateDialogForSelected(it.cellId) }
                }
            )
        }
    }
}
