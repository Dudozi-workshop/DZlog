package com.dudoziworkshop.dzlog.ui.table.detail

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableHandleOverlay
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionResolver
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionResult
import com.dudoziworkshop.dzlog.feature.table.editor.TableUndoManager
import com.dudoziworkshop.dzlog.feature.table.editor.addColumnBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.addRowBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.removeColumnBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.removeRowBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.resetColumnWeights
import com.dudoziworkshop.dzlog.feature.table.editor.resetRowWeights
import com.dudoziworkshop.dzlog.feature.table.model.TableEditMode
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.model.TableSelectionState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.state.TableDetailScreenState

class TableDetailViewModel(
    initialTemplate: TableTemplateState,
) {
    private var selection = TableSelectionResult(emptySet(), null, null)
    private val undoManager = TableUndoManager()

    var state: TableDetailScreenState = TableDetailScreenState(
        definition = com.dudoziworkshop.dzlog.feature.table.model.TableDefinitionState.from(initialTemplate),
        style = TableStyleState(),
        placement = TablePlacementState(),
        editMode = TableEditMode.Normal,
        selection = TableSelectionState(),
    )
        private set

    var currentTemplate: TableTemplateState = initialTemplate
        private set

    fun canUndo(): Boolean = undoManager.canUndo()

    fun dispatch(action: TableDetailAction): TableTemplateState {
        when (action) {
            TableDetailAction.ToggleStructureMode -> {
                state = state.copy(
                    editMode = if (state.editMode == TableEditMode.Normal) TableEditMode.Structure else TableEditMode.Normal
                )
            }

            is TableDetailAction.SelectSingleCell -> {
                if (state.editMode == TableEditMode.Structure) {
                    selection = TableSelectionResolver.selectByTap(
                        cells = currentTemplate.cells,
                        current = selection,
                        tappedCellId = action.cellId,
                        additive = action.additive,
                    )
                    syncSelectionState()
                }
            }

            is TableDetailAction.SelectRange -> {
                if (state.editMode == TableEditMode.Structure) {
                    selection = TableSelectionResolver.selectByDrag(
                        cells = currentTemplate.cells,
                        startCellId = action.startCellId,
                        endCellId = action.endCellId,
                    )
                    syncSelectionState()
                }
            }

            TableDetailAction.AddRow -> commitTemplateChange { addRowBySelection(it, selection.range) }
            TableDetailAction.RemoveRow -> commitTemplateChange { removeRowBySelection(it, selection.range) }
            TableDetailAction.AddColumn -> commitTemplateChange { addColumnBySelection(it, selection.range) }
            TableDetailAction.RemoveColumn -> commitTemplateChange { removeColumnBySelection(it, selection.range) }
            TableDetailAction.ResetRowWeights -> commitTemplateChange { resetRowWeights(it) }
            TableDetailAction.ResetColumnWeights -> commitTemplateChange { resetColumnWeights(it) }

            is TableDetailAction.CommitRowWeightsDrag -> commitTemplateChange {
                TableHandleOverlay.applyRowWeightDragEnd(it, action.weights)
            }

            is TableDetailAction.CommitColumnWeightsDrag -> commitTemplateChange {
                TableHandleOverlay.applyColumnWeightDragEnd(it, action.weights)
            }

            TableDetailAction.Undo -> {
                currentTemplate = undoManager.undo(currentTemplate)
                selection = TableSelectionResult(emptySet(), null, null)
                syncSelectionState()
            }

            TableDetailAction.Save -> {
                undoManager.clear()
            }

            is TableDetailAction.InjectSelectionRangeForTest -> {
                selection = TableSelectionResult(emptySet(), action.range, null)
                syncSelectionState()
            }
        }

        return currentTemplate
    }

    private fun syncSelectionState() {
        state = state.copy(
            selection = TableSelectionState(
                selectedCellIds = selection.selectedCellIds,
                lastSelectedCellId = selection.lastSelectedCellId,
                minRow = selection.range?.minRow,
                maxRow = selection.range?.maxRow,
                minCol = selection.range?.minCol,
                maxCol = selection.range?.maxCol,
            )
        )
    }

    private inline fun commitTemplateChange(transform: (TableTemplateState) -> TableTemplateState) {
        val before = currentTemplate
        val after = transform(before)
        if (after != before) {
            undoManager.pushSnapshotBeforeAction(before)
            currentTemplate = after
            val maxRow = (after.rows - 1).coerceAtLeast(0)
            val maxCol = (after.cols - 1).coerceAtLeast(0)
            selection = TableSelectionResult(
                selectedCellIds = emptySet(),
                range = selection.range?.let {
                    TableSelectionRange(
                        minRow = it.minRow.coerceIn(0, maxRow),
                        maxRow = it.maxRow.coerceIn(0, maxRow),
                        minCol = it.minCol.coerceIn(0, maxCol),
                        maxCol = it.maxCol.coerceIn(0, maxCol),
                    )
                },
                lastSelectedCellId = null,
            )
            syncSelectionState()
        }
    }
}
