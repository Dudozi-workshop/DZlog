package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.DeletedStructureSnapshot
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.StructureRestoreAxis
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureDeletionCoordinatorInput
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureDeletionDebugInfo
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureRestoreCoordinator
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureRestoreCoordinatorInput
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureRestoreDebugInfo
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem

data class StructureActionResult(
    val nextTemplate: TableTemplateState,
    val nextFileNameSlotsDirtySinceStructureChange: Boolean,
    val nextPathSlotsDirtySinceStructureChange: Boolean,
    val nextStructureSelectionRange: TableSelectionRange?,
    val nextStructureSelectedCellIds: Set<String>,
    val nextSelectedCellId: String?,
    val actionLabel: String,
    val deletionDebugInfo: TableEditorStructureDeletionDebugInfo?,
    val restoreDebugInfo: TableEditorStructureRestoreDebugInfo?,
    val deletedSnapshotToPush: DeletedStructureSnapshot?,
    val deletedSnapshotAxis: StructureRestoreAxis?,
    val consumedDeletedStackAxis: StructureRestoreAxis?,
)

data class TableEditorStructureRemoveActionInput(
    val axis: StructureRestoreAxis,
    val isStructureEditMode: Boolean,
    val structureSelectionRange: TableSelectionRange?,
    val selectedCellId: String?,
    val currentTemplate: TableTemplateState,
    val currentFileNameSlots: List<FileNameSlotUiItem?>,
    val currentPathSlots: List<PathSlotUiItem?>,
    val fileNameSlotsDirtySinceStructureChange: Boolean,
    val pathSlotsDirtySinceStructureChange: Boolean,
    val sanitizeTemplate: (TableTemplateState) -> TableTemplateState,
    val applyFileNameSlots: (TableTemplateState, List<FileNameSlotUiItem?>) -> TableTemplateState,
    val applyPathSlots: (TableTemplateState, List<PathSlotUiItem?>) -> TableTemplateState,
    val removeCellRefsFromFileNameSlots: (List<FileNameSlotUiItem?>, Set<String>) -> List<FileNameSlotUiItem?>,
    val removeCellRefsFromPathSlots: (List<PathSlotUiItem?>, Set<String>) -> List<PathSlotUiItem?>,
)

data class TableEditorStructureAddOrRestoreActionInput(
    val axis: StructureRestoreAxis,
    val isStructureEditMode: Boolean,
    val structureSelectionRange: TableSelectionRange?,
    val selectedCellId: String?,
    val currentTemplate: TableTemplateState,
    val deletedRowsStack: List<DeletedStructureSnapshot>,
    val deletedColsStack: List<DeletedStructureSnapshot>,
    val currentFileNameSlots: List<FileNameSlotUiItem?>,
    val currentPathSlots: List<PathSlotUiItem?>,
    val fileNameSlotsDirtySinceStructureChange: Boolean,
    val pathSlotsDirtySinceStructureChange: Boolean,
    val sanitizeTemplate: (TableTemplateState) -> TableTemplateState,
    val applyFileNameSlots: (TableTemplateState, List<FileNameSlotUiItem?>) -> TableTemplateState,
    val applyPathSlots: (TableTemplateState, List<PathSlotUiItem?>) -> TableTemplateState,
)

object TableEditorStructureActions {

    fun resolveDeletionRange(
        axis: StructureRestoreAxis,
        isStructureEditMode: Boolean,
        structureSelectionRange: TableSelectionRange?,
        currentTemplate: TableTemplateState,
    ): IntRange {
        if (isStructureEditMode && structureSelectionRange != null) {
            return when (axis) {
                StructureRestoreAxis.ROW -> structureSelectionRange.minRow..structureSelectionRange.maxRow
                StructureRestoreAxis.COL -> structureSelectionRange.minCol..structureSelectionRange.maxCol
            }
        }

        return when (axis) {
            StructureRestoreAxis.ROW -> lastAxisIndex(currentTemplate.rows)..lastAxisIndex(currentTemplate.rows)
            StructureRestoreAxis.COL -> lastAxisIndex(currentTemplate.cols)..lastAxisIndex(currentTemplate.cols)
        }
    }

    fun addOrRestore(input: TableEditorStructureAddOrRestoreActionInput): StructureActionResult {
        val restoredSnapshot = stackForAxis(input.axis, input.deletedRowsStack, input.deletedColsStack).lastOrNull()
        return if (restoredSnapshot != null) {
            val restoreResult = TableEditorStructureRestoreCoordinator.restore(
                TableEditorStructureRestoreCoordinatorInput(
                    currentTemplate = input.currentTemplate,
                    restoredSnapshot = restoredSnapshot,
                    currentFileNameSlots = input.currentFileNameSlots,
                    currentPathSlots = input.currentPathSlots,
                    fileNameSlotsDirtySinceStructureChange = input.fileNameSlotsDirtySinceStructureChange,
                    pathSlotsDirtySinceStructureChange = input.pathSlotsDirtySinceStructureChange,
                    restoreAxis = input.axis,
                    sanitizeTemplate = input.sanitizeTemplate,
                    applyFileNameSlots = input.applyFileNameSlots,
                    applyPathSlots = input.applyPathSlots,
                )
            )
            buildActionResult(
                selectionState = resolveSelectionAfterAddOrRestore(
                    isStructureEditMode = input.isStructureEditMode,
                    currentSelectionRange = input.structureSelectionRange,
                    nextTemplate = restoreResult.nextTemplate,
                ),
                isStructureEditMode = input.isStructureEditMode,
                selectedCellId = input.selectedCellId,
                nextTemplate = restoreResult.nextTemplate,
                nextFileNameSlotsDirtySinceStructureChange = restoreResult.nextFileNameSlotsDirtySinceStructureChange,
                nextPathSlotsDirtySinceStructureChange = restoreResult.nextPathSlotsDirtySinceStructureChange,
                actionLabel = when (input.axis) {
                    StructureRestoreAxis.ROW -> "add_row_restore"
                    StructureRestoreAxis.COL -> "add_col_restore"
                },
                deletionDebugInfo = null,
                restoreDebugInfo = restoreResult.debugInfo,
                deletedSnapshotToPush = null,
                deletedSnapshotAxis = null,
                consumedDeletedStackAxis = input.axis,
            )
        } else {
            val nextTemplate = input.sanitizeTemplate(
                when (input.axis) {
                    StructureRestoreAxis.ROW -> {
                        if (input.isStructureEditMode) {
                            addRowBySelection(input.currentTemplate, input.structureSelectionRange)
                        } else {
                            addRow(input.currentTemplate)
                        }
                    }

                    StructureRestoreAxis.COL -> {
                        if (input.isStructureEditMode) {
                            addColumnBySelection(input.currentTemplate, input.structureSelectionRange)
                        } else {
                            addColumn(input.currentTemplate)
                        }
                    }
                }
            )
            buildActionResult(
                selectionState = resolveSelectionAfterAddOrRestore(
                    isStructureEditMode = input.isStructureEditMode,
                    currentSelectionRange = input.structureSelectionRange,
                    nextTemplate = nextTemplate,
                ),
                isStructureEditMode = input.isStructureEditMode,
                selectedCellId = input.selectedCellId,
                nextTemplate = nextTemplate,
                nextFileNameSlotsDirtySinceStructureChange = input.fileNameSlotsDirtySinceStructureChange,
                nextPathSlotsDirtySinceStructureChange = input.pathSlotsDirtySinceStructureChange,
                actionLabel = when (input.axis) {
                    StructureRestoreAxis.ROW -> "add_row_blank"
                    StructureRestoreAxis.COL -> "add_col_blank"
                },
                deletionDebugInfo = null,
                restoreDebugInfo = null,
                deletedSnapshotToPush = null,
                deletedSnapshotAxis = null,
                consumedDeletedStackAxis = null,
            )
        }
    }

    fun remove(input: TableEditorStructureRemoveActionInput): StructureActionResult {
        val requestedRange = resolveDeletionRange(
            axis = input.axis,
            isStructureEditMode = input.isStructureEditMode,
            structureSelectionRange = input.structureSelectionRange,
            currentTemplate = input.currentTemplate,
        )
        val deletionRange = when (input.axis) {
            StructureRestoreAxis.ROW -> normalizeRowRemovalRange(input.currentTemplate, requestedRange)
            StructureRestoreAxis.COL -> normalizeColRemovalRange(input.currentTemplate, requestedRange)
        }

        if (deletionRange == null) {
            return buildActionResult(
                selectionState = resolveSelectionAfterRemove(),
                isStructureEditMode = input.isStructureEditMode,
                selectedCellId = input.selectedCellId,
                nextTemplate = input.currentTemplate,
                nextFileNameSlotsDirtySinceStructureChange = input.fileNameSlotsDirtySinceStructureChange,
                nextPathSlotsDirtySinceStructureChange = input.pathSlotsDirtySinceStructureChange,
                actionLabel = actionLabelForRemove(input.axis),
                deletionDebugInfo = TableEditorStructureDeletionDebugInfo(
                    axis = input.axis,
                    deletedRange = requestedRange,
                    deletedCells = emptyList(),
                    hasSlotSnapshot = false,
                ),
                restoreDebugInfo = null,
                deletedSnapshotToPush = null,
                deletedSnapshotAxis = null,
                consumedDeletedStackAxis = null,
            )
        }

        val deletionPayload = TableEditorStructureRestoreCoordinator.buildDeletionPayload(
            TableEditorStructureDeletionCoordinatorInput(
                currentTemplate = input.currentTemplate,
                deleteAxis = input.axis,
                deletedRange = deletionRange,
                currentFileNameSlots = input.currentFileNameSlots,
                currentPathSlots = input.currentPathSlots,
                removeCellRefsFromFileNameSlots = input.removeCellRefsFromFileNameSlots,
                removeCellRefsFromPathSlots = input.removeCellRefsFromPathSlots,
            )
        )

        val removedTemplate = when (input.axis) {
            StructureRestoreAxis.ROW -> removeRowsByRange(input.currentTemplate, deletionRange)
            StructureRestoreAxis.COL -> removeColsByRange(input.currentTemplate, deletionRange)
        }
        val sanitizedTemplate = input.sanitizeTemplate(removedTemplate)
        val nextTemplate = applySlotsToTemplate(
            baseTemplate = sanitizedTemplate,
            nextFileNameSlots = deletionPayload.nextFileNameSlots,
            nextPathSlots = deletionPayload.nextPathSlots,
            applyFileNameSlots = input.applyFileNameSlots,
            applyPathSlots = input.applyPathSlots,
        )

        return buildActionResult(
            selectionState = resolveSelectionAfterRemove(),
            isStructureEditMode = input.isStructureEditMode,
            selectedCellId = input.selectedCellId,
            nextTemplate = nextTemplate,
            nextFileNameSlotsDirtySinceStructureChange = deletionPayload.nextFileNameSlotsDirtySinceStructureChange,
            nextPathSlotsDirtySinceStructureChange = deletionPayload.nextPathSlotsDirtySinceStructureChange,
            actionLabel = actionLabelForRemove(input.axis),
            deletionDebugInfo = deletionPayload.debugInfo,
            restoreDebugInfo = null,
            deletedSnapshotToPush = deletionPayload.deletedSnapshot,
            deletedSnapshotAxis = input.axis,
            consumedDeletedStackAxis = null,
        )
    }

    private fun buildActionResult(
        selectionState: StructureSelectionState,
        isStructureEditMode: Boolean,
        selectedCellId: String?,
        nextTemplate: TableTemplateState,
        nextFileNameSlotsDirtySinceStructureChange: Boolean,
        nextPathSlotsDirtySinceStructureChange: Boolean,
        actionLabel: String,
        deletionDebugInfo: TableEditorStructureDeletionDebugInfo?,
        restoreDebugInfo: TableEditorStructureRestoreDebugInfo?,
        deletedSnapshotToPush: DeletedStructureSnapshot?,
        deletedSnapshotAxis: StructureRestoreAxis?,
        consumedDeletedStackAxis: StructureRestoreAxis?,
    ): StructureActionResult {
        return StructureActionResult(
            nextTemplate = nextTemplate,
            nextFileNameSlotsDirtySinceStructureChange = nextFileNameSlotsDirtySinceStructureChange,
            nextPathSlotsDirtySinceStructureChange = nextPathSlotsDirtySinceStructureChange,
            nextStructureSelectionRange = selectionState.range,
            nextStructureSelectedCellIds = selectionState.selectedCellIds,
            nextSelectedCellId = resolveNextSelectedCellId(
                isStructureEditMode = isStructureEditMode,
                selectedCellId = selectedCellId,
                nextTemplate = nextTemplate,
            ),
            actionLabel = actionLabel,
            deletionDebugInfo = deletionDebugInfo,
            restoreDebugInfo = restoreDebugInfo,
            deletedSnapshotToPush = deletedSnapshotToPush,
            deletedSnapshotAxis = deletedSnapshotAxis,
            consumedDeletedStackAxis = consumedDeletedStackAxis,
        )
    }

    private fun resolveSelectionAfterAddOrRestore(
        isStructureEditMode: Boolean,
        currentSelectionRange: TableSelectionRange?,
        nextTemplate: TableTemplateState,
    ): StructureSelectionState {
        if (!isStructureEditMode || currentSelectionRange == null) {
            return StructureSelectionState(range = null, selectedCellIds = emptySet())
        }

        val maxRow = (nextTemplate.rows - 1).coerceAtLeast(0)
        val maxCol = (nextTemplate.cols - 1).coerceAtLeast(0)
        val boundedRange = TableSelectionRange(
            minRow = currentSelectionRange.minRow.coerceIn(0, maxRow),
            maxRow = currentSelectionRange.maxRow.coerceIn(0, maxRow),
            minCol = currentSelectionRange.minCol.coerceIn(0, maxCol),
            maxCol = currentSelectionRange.maxCol.coerceIn(0, maxCol),
        )
        val selectedCellIds = nextTemplate.cells
            .filter { boundedRange.contains(it.rowIndex, it.colIndex) }
            .map { it.cellId }
            .toSet()
        return StructureSelectionState(range = boundedRange, selectedCellIds = selectedCellIds)
    }

    private fun resolveSelectionAfterRemove(): StructureSelectionState {
        return StructureSelectionState(
            range = null,
            selectedCellIds = emptySet(),
        )
    }

    private fun resolveNextSelectedCellId(
        isStructureEditMode: Boolean,
        selectedCellId: String?,
        nextTemplate: TableTemplateState,
    ): String? {
        if (isStructureEditMode) return null
        if (selectedCellId == null) return null
        return selectedCellId.takeIf { cellId -> nextTemplate.cells.any { it.cellId == cellId } }
    }

    private fun applySlotsToTemplate(
        baseTemplate: TableTemplateState,
        nextFileNameSlots: List<FileNameSlotUiItem?>,
        nextPathSlots: List<PathSlotUiItem?>,
        applyFileNameSlots: (TableTemplateState, List<FileNameSlotUiItem?>) -> TableTemplateState,
        applyPathSlots: (TableTemplateState, List<PathSlotUiItem?>) -> TableTemplateState,
    ): TableTemplateState {
        var nextTemplate = applyFileNameSlots(baseTemplate, nextFileNameSlots)
        nextTemplate = applyPathSlots(nextTemplate, nextPathSlots)
        return nextTemplate
    }

    private fun stackForAxis(
        axis: StructureRestoreAxis,
        deletedRowsStack: List<DeletedStructureSnapshot>,
        deletedColsStack: List<DeletedStructureSnapshot>,
    ): List<DeletedStructureSnapshot> {
        return when (axis) {
            StructureRestoreAxis.ROW -> deletedRowsStack
            StructureRestoreAxis.COL -> deletedColsStack
        }
    }

    private fun actionLabelForRemove(axis: StructureRestoreAxis): String {
        return when (axis) {
            StructureRestoreAxis.ROW -> "remove_row"
            StructureRestoreAxis.COL -> "remove_col"
        }
    }

    private fun lastAxisIndex(axisSize: Int): Int = (axisSize - 1).coerceAtLeast(0)
}

private data class StructureSelectionState(
    val range: TableSelectionRange?,
    val selectedCellIds: Set<String>,
)
