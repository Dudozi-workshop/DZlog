package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
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
import com.dudoziworkshop.dzlog.feature.table.model.TableDefinitionState
import com.dudoziworkshop.dzlog.feature.table.model.TableEditMode
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.model.TableSelectionState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.state.TableDetailScreenState

data class TableEditorSnapshot(
    val template: TableTemplateState,
    val style: TableStyleState,
)

class TableDetailViewModel(
    initialTemplate: TableTemplateState,
) {
    private var selection = TableSelectionResult(emptySet(), null, null)
    private val undoManager = TableUndoManager<TableEditorSnapshot>()

    private var state: TableDetailScreenState = TableDetailScreenState(
        definition = TableDefinitionState.from(initialTemplate),
        style = TableStyleState(),
        placement = TablePlacementState(),
        editMode = TableEditMode.Normal,
        selection = TableSelectionState(),
    )
        private set

    val viewState: MutableState<TableDetailScreenState> = mutableStateOf(state)

    var currentTemplate: TableTemplateState = initialTemplate
        private set

    fun applyTemplateFromUi(updated: TableTemplateState, isActionCommit: Boolean = true) {
        if (updated == currentTemplate) return
        if (isActionCommit) {
            undoManager.pushSnapshotBeforeAction(currentSnapshot())
        }
        currentTemplate = updated
        syncDefinitionState()
    }

    fun canUndo(): Boolean = undoManager.canUndo()

    fun dispatch(action: TableDetailAction): TableTemplateState {
        when (action) {
            TableDetailAction.ToggleStructureMode -> {
                updateState(state.copy(
                    editMode = if (state.editMode == TableEditMode.Normal) TableEditMode.Structure else TableEditMode.Normal
                ))
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

            TableDetailAction.AddRow -> commitTemplateChange(SelectionPolicy.KEEP_AFTER_ADD) { addRowBySelection(it, selection.range) }
            TableDetailAction.RemoveRow -> commitTemplateChange(SelectionPolicy.CLEAR_AFTER_DELETE) { removeRowBySelection(it, selection.range) }
            TableDetailAction.AddColumn -> commitTemplateChange(SelectionPolicy.KEEP_AFTER_ADD) { addColumnBySelection(it, selection.range) }
            TableDetailAction.RemoveColumn -> commitTemplateChange(SelectionPolicy.CLEAR_AFTER_DELETE) { removeColumnBySelection(it, selection.range) }
            TableDetailAction.ResetRowWeights -> commitTemplateChange(SelectionPolicy.KEEP_AFTER_ADD) { resetRowWeights(it) }
            TableDetailAction.ResetColumnWeights -> commitTemplateChange(SelectionPolicy.KEEP_AFTER_ADD) { resetColumnWeights(it) }

            is TableDetailAction.CommitRowWeightsDrag -> commitTemplateChange(SelectionPolicy.KEEP_AFTER_ADD) {
                TableHandleOverlay.applyRowWeightDragEnd(it, action.weights)
            }

            is TableDetailAction.CommitColumnWeightsDrag -> commitTemplateChange(SelectionPolicy.KEEP_AFTER_ADD) {
                TableHandleOverlay.applyColumnWeightDragEnd(it, action.weights)
            }

            is TableDetailAction.SetStyleBgStyle -> commitStyleChange { it.copy(bgStyle = action.bgStyle.coerceIn(0, 2)) }
            is TableDetailAction.SetStyleBgAlpha -> commitStyleChange { it.copy(bgAlpha = action.alpha.coerceIn(0, 255)) }
            is TableDetailAction.SetStyleGridEnabled -> commitStyleChange { it.copy(gridEnabled = action.enabled) }
            is TableDetailAction.SetStyleTextColorMode -> commitStyleChange { it.copy(textColorMode = action.mode) }
            is TableDetailAction.SetStyleManualTextColor -> commitStyleChange { it.copy(manualTextColor = action.color) }
            is TableDetailAction.SetStyleValueScale -> commitStyleChange { it.copy(valueScale = action.scale.coerceIn(60, 160)) }
            is TableDetailAction.SetStyleTextAlign -> commitStyleChange { it.copy(textAlign = action.align) }

            TableDetailAction.Undo -> {
                val restored = undoManager.undo(currentSnapshot())
                currentTemplate = restored.template
                updateState(state.copy(style = restored.style))
                syncDefinitionState()
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

    private fun updateState(newState: TableDetailScreenState) {
        state = newState
        viewState.value = newState
    }

    private fun commitStyleChange(transform: (TableStyleState) -> TableStyleState) {
        val before = state.style
        val after = transform(before)
        if (before != after) {
            undoManager.pushSnapshotBeforeAction(currentSnapshot())
            updateState(state.copy(style = after))
        }
    }

    private fun syncDefinitionState() {
        updateState(state.copy(definition = TableDefinitionState.from(currentTemplate)))
    }

    private fun syncSelectionState() {
        updateState(state.copy(
            selection = TableSelectionState(
                selectedCellIds = selection.selectedCellIds,
                lastSelectedCellId = selection.lastSelectedCellId,
                minRow = selection.range?.minRow,
                maxRow = selection.range?.maxRow,
                minCol = selection.range?.minCol,
                maxCol = selection.range?.maxCol,
            )
        ))
    }

    private inline fun commitTemplateChange(
        policy: SelectionPolicy,
        transform: (TableTemplateState) -> TableTemplateState,
    ) {
        val before = currentTemplate
        val after = transform(before)
        if (after != before) {
            undoManager.pushSnapshotBeforeAction(currentSnapshot())
            currentTemplate = after
            syncDefinitionState()
            selection = when (policy) {
                SelectionPolicy.CLEAR_AFTER_DELETE -> TableSelectionResult(emptySet(), null, null)
                SelectionPolicy.KEEP_AFTER_ADD -> rebuildSelectionForCurrentTemplate(after)
            }
            syncSelectionState()
        }
    }

    private fun rebuildSelectionForCurrentTemplate(template: TableTemplateState): TableSelectionResult {
        val range = selection.range ?: return TableSelectionResult(emptySet(), null, null)
        val maxRow = (template.rows - 1).coerceAtLeast(0)
        val maxCol = (template.cols - 1).coerceAtLeast(0)
        val bounded = TableSelectionRange(
            minRow = range.minRow.coerceIn(0, maxRow),
            maxRow = range.maxRow.coerceIn(0, maxRow),
            minCol = range.minCol.coerceIn(0, maxCol),
            maxCol = range.maxCol.coerceIn(0, maxCol),
        )
        val selected = template.cells.filter { bounded.contains(it.rowIndex, it.colIndex) }.map { it.cellId }.toSet()
        return TableSelectionResult(
            selectedCellIds = selected,
            range = bounded,
            lastSelectedCellId = selection.lastSelectedCellId?.takeIf { id -> selected.contains(id) },
        )
    }

    private fun currentSnapshot(): TableEditorSnapshot = TableEditorSnapshot(
        template = currentTemplate,
        style = state.style,
    )
}

private enum class SelectionPolicy {
    KEEP_AFTER_ADD,
    CLEAR_AFTER_DELETE,
}


@Composable
fun rememberTableDetailViewModel(initialTemplate: TableTemplateState): TableDetailViewModel {
    return remember(initialTemplate) { TableDetailViewModel(initialTemplate) }
}
