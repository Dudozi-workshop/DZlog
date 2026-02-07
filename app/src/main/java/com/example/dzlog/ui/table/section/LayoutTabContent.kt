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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.table.ResolvePlan
import com.example.dzlog.ui.common.DDZSectionHeader
import com.example.dzlog.ui.table.BottomFixedActionBar
import com.example.dzlog.ui.table.CellSettingsBottomPanel
import com.example.dzlog.ui.table.CompactPathHeader
import com.example.dzlog.ui.table.PathGroupAction
import com.example.dzlog.ui.theme.DDZColor

@Composable
fun LayoutTabContent(
    savePathPreview: String,
    filenamePreview: String,
    templateState: TableTemplateState,
    plan: ResolvePlan,
    selectedCellId: String?,
    editingCellId: String?,
    editingValue: String,
    inlineFocusRequester: FocusRequester,
    showCellSettingsPanel: Boolean,
    selectedCell: TableCellState?,
    hasGroup1: Boolean,
    hasGroup2: Boolean,
    isSavingTemplate: Boolean,
    onSelectCellId: (String?) -> Unit,
    onShowCellSettingsPanel: (Boolean) -> Unit,
    onStartInlineEditing: (cellId: String, initialText: String) -> Unit,
    onOpenFormatDialog: (cellId: String, type: TableCellDataType) -> Unit,
    onEditingValueChange: (String) -> Unit,
    onCommitInline: () -> Unit,
    onInlineFocusLostCommit: () -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: () -> Unit,
    onAddCol: () -> Unit,
    onRemoveCol: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onDismissSettingsPanel: () -> Unit,
    onSetFileNameIncludeForSelected: (Boolean) -> Unit,
    onPathGroupActionForSelected: (PathGroupAction) -> Unit,
    onSetDataTypeForSelected: (TableCellDataType) -> Unit,
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
                    savePath = savePathPreview,
                    fileName = filenamePreview
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
                        templateState = templateState,
                        displayTextProvider = { cellId ->
                            plan.resolvedCells.firstOrNull { it.id == cellId }?.resolvedText.orEmpty()
                        },
                        selectedCellId = selectedCellId,
                        editingCellId = editingCellId,
                        onSelectCell = { id ->
                            if (editingCellId != null && editingCellId != id) {
                                onInlineFocusLostCommit()
                                if (editingCellId != null) return@TableGridSection
                            }
                            onSelectCellId(id)
                            onShowCellSettingsPanel(true)
                        },
                        onDoubleClickCell = { cell ->
                            if (editingCellId != null && editingCellId != cell.cellId) {
                                onInlineFocusLostCommit()
                                if (editingCellId != null) return@TableGridSection
                                onSelectCellId(cell.cellId)
                                onShowCellSettingsPanel(true)
                                return@TableGridSection
                            }

                            onSelectCellId(cell.cellId)
                            val canInline =
                                (cell.dataType == TableCellDataType.TEXT ||
                                    cell.dataType == TableCellDataType.NUMBER ||
                                    cell.dataType == TableCellDataType.COUNTER)

                            if (canInline) {
                                onShowCellSettingsPanel(false)
                                onStartInlineEditing(cell.cellId, cell.toEditableText())
                            } else {
                                if (cell.dataType == TableCellDataType.DATE || cell.dataType == TableCellDataType.TIME) {
                                    onOpenFormatDialog(cell.cellId, cell.dataType)
                                } else {
                                    onShowCellSettingsPanel(true)
                                }
                            }
                        },
                        editingValue = editingValue,
                        onEditingValueChange = onEditingValueChange,
                        onCommitInline = onCommitInline,
                        inlineFocusRequester = inlineFocusRequester,
                        onInlineFocusLostCommit = onInlineFocusLostCommit
                    )
                }

                Spacer(Modifier.height(10.dp))
            }

            BottomFixedActionBar(
                rows = templateState.rows,
                cols = templateState.cols,
                isSaving = isSavingTemplate,
                onAddRow = onAddRow,
                onRemoveRow = onRemoveRow,
                onAddCol = onAddCol,
                onRemoveCol = onRemoveCol,
                onReset = onReset,
                onSave = onSave
            )
        }

        if (showCellSettingsPanel && selectedCell != null && editingCellId == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DDZColor.PrimaryDark.copy(alpha = 0.4f))
                    .clickable { onDismissSettingsPanel() }
            )

            CellSettingsBottomPanel(
                modifier = Modifier.align(Alignment.BottomCenter),
                cell = selectedCell,
                hasGroup1 = hasGroup1,
                hasGroup2 = hasGroup2,
                onSetFileNameInclude = onSetFileNameIncludeForSelected,
                onPathGroupAction = onPathGroupActionForSelected,
                onSetDataType = onSetDataTypeForSelected
            )
        }
    }
}
