package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.DeletedStructureSnapshot
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.StructureRestoreAxis
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureDeletionCoordinatorInput
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureRestoreCoordinator
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureRestoreCoordinatorInput
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem

data class StructureActionResult(
    val nextTemplate: TableTemplateState,
    val nextFileNameSlotsDirtySinceStructureChange: Boolean,
    val nextPathSlotsDirtySinceStructureChange: Boolean,
    val nextDeletedRowsStack: List<DeletedStructureSnapshot>,
    val nextDeletedColsStack: List<DeletedStructureSnapshot>,
    val nextStructureSelectionRange: TableSelectionRange?,
    val nextStructureSelectedCellIds: Set<String>,
    val nextSelectedCellId: String?,
    val actionLabel: String,
)

data class StructureRemoveInput(
    val axis: StructureRestoreAxis,
    val editor: StructureEditorContext,
    val sanitizeTemplate: (TableTemplateState) -> TableTemplateState,
    val applyFileNameSlots: (TableTemplateState, List<FileNameSlotUiItem?>) -> TableTemplateState,
    val applyPathSlots: (TableTemplateState, List<PathSlotUiItem?>) -> TableTemplateState,
    val removeCellRefsFromFileNameSlots: (List<FileNameSlotUiItem?>, Set<String>) -> List<FileNameSlotUiItem?>,
    val removeCellRefsFromPathSlots: (List<PathSlotUiItem?>, Set<String>) -> List<PathSlotUiItem?>,
)

data class StructureAddOrRestoreInput(
    val axis: StructureRestoreAxis,
    val editor: StructureEditorContext,
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

    fun addOrRestore(input: StructureAddOrRestoreInput): StructureActionResult {
        val restoredSnapshot = lastDeletedSnapshotForAxis(input.axis, input.editor).lastOrNull()
        return if (restoredSnapshot != null) {
            val nextStacks = consumeDeletedStackForRestore(
                axis = input.axis,
                deletedRowsStack = input.editor.deletedRowsStack,
                deletedColsStack = input.editor.deletedColsStack,
            )
            val restoreResult = TableEditorStructureRestoreCoordinator.restore(
                TableEditorStructureRestoreCoordinatorInput(
                    currentTemplate = input.editor.currentTemplate,
                    restoredSnapshot = restoredSnapshot,
                    currentFileNameSlots = input.editor.currentFileNameSlots,
                    currentPathSlots = input.editor.currentPathSlots,
                    fileNameSlotsDirtySinceStructureChange = input.editor.fileNameSlotsDirtySinceStructureChange,
                    pathSlotsDirtySinceStructureChange = input.editor.pathSlotsDirtySinceStructureChange,
                    restoreAxis = input.axis,
                    sanitizeTemplate = input.sanitizeTemplate,
                    applyFileNameSlots = input.applyFileNameSlots,
                    applyPathSlots = input.applyPathSlots,
                )
            )
            buildStructureActionResult(
                editor = input.editor,
                selectionState = resolveSelectionAfterAddOrRestore(
                    isStructureEditMode = input.editor.isStructureEditMode,
                    currentSelectionRange = input.editor.structureSelectionRange,
                    nextTemplate = restoreResult.nextTemplate,
                ),
                nextTemplate = restoreResult.nextTemplate,
                nextFileNameSlotsDirtySinceStructureChange = restoreResult.nextFileNameSlotsDirtySinceStructureChange,
                nextPathSlotsDirtySinceStructureChange = restoreResult.nextPathSlotsDirtySinceStructureChange,
                nextDeletedRowsStack = nextStacks.first,
                nextDeletedColsStack = nextStacks.second,
                actionLabel = when (input.axis) {
                    StructureRestoreAxis.ROW -> "add_row_restore"
                    StructureRestoreAxis.COL -> "add_col_restore"
                },
            )
        } else {
            val nextTemplate = input.sanitizeTemplate(
                when (input.axis) {
                    StructureRestoreAxis.ROW -> {
                        if (input.editor.isStructureEditMode) {
                            addRowBySelection(input.editor.currentTemplate, input.editor.structureSelectionRange)
                        } else {
                            addRow(input.editor.currentTemplate)
                        }
                    }

                    StructureRestoreAxis.COL -> {
                        if (input.editor.isStructureEditMode) {
                            addColumnBySelection(input.editor.currentTemplate, input.editor.structureSelectionRange)
                        } else {
                            addColumn(input.editor.currentTemplate)
                        }
                    }
                }
            )
            buildStructureActionResult(
                editor = input.editor,
                selectionState = resolveSelectionAfterAddOrRestore(
                    isStructureEditMode = input.editor.isStructureEditMode,
                    currentSelectionRange = input.editor.structureSelectionRange,
                    nextTemplate = nextTemplate,
                ),
                nextTemplate = nextTemplate,
                nextFileNameSlotsDirtySinceStructureChange = input.editor.fileNameSlotsDirtySinceStructureChange,
                nextPathSlotsDirtySinceStructureChange = input.editor.pathSlotsDirtySinceStructureChange,
                nextDeletedRowsStack = input.editor.deletedRowsStack,
                nextDeletedColsStack = input.editor.deletedColsStack,
                actionLabel = when (input.axis) {
                    StructureRestoreAxis.ROW -> "add_row_blank"
                    StructureRestoreAxis.COL -> "add_col_blank"
                },
            )
        }
    }

    fun addBlank(input: StructureAddOrRestoreInput): StructureActionResult {
        val nextTemplate = input.sanitizeTemplate(
            when (input.axis) {
                StructureRestoreAxis.ROW -> {
                    if (input.editor.isStructureEditMode) {
                        addRowBySelection(input.editor.currentTemplate, input.editor.structureSelectionRange)
                    } else {
                        addRow(input.editor.currentTemplate)
                    }
                }
                StructureRestoreAxis.COL -> {
                    if (input.editor.isStructureEditMode) {
                        addColumnBySelection(input.editor.currentTemplate, input.editor.structureSelectionRange)
                    } else {
                        addColumn(input.editor.currentTemplate)
                    }
                }
            }
        )
        return buildStructureActionResult(
            editor = input.editor,
            selectionState = resolveSelectionAfterAddOrRestore(
                isStructureEditMode = input.editor.isStructureEditMode,
                currentSelectionRange = input.editor.structureSelectionRange,
                nextTemplate = nextTemplate,
            ),
            nextTemplate = nextTemplate,
            nextFileNameSlotsDirtySinceStructureChange = input.editor.fileNameSlotsDirtySinceStructureChange,
            nextPathSlotsDirtySinceStructureChange = input.editor.pathSlotsDirtySinceStructureChange,
            nextDeletedRowsStack = emptyList(),
            nextDeletedColsStack = emptyList(),
            actionLabel = when (input.axis) {
                StructureRestoreAxis.ROW -> "add_row_blank"
                StructureRestoreAxis.COL -> "add_col_blank"
            },
        )
    }

    fun remove(input: StructureRemoveInput): StructureActionResult {
        val requestedRange = resolveDeletionRange(
            axis = input.axis,
            isStructureEditMode = input.editor.isStructureEditMode,
            structureSelectionRange = input.editor.structureSelectionRange,
            currentTemplate = input.editor.currentTemplate,
        )
        val deletionRange = when (input.axis) {
            StructureRestoreAxis.ROW -> normalizeRowRemovalRange(input.editor.currentTemplate, requestedRange)
            StructureRestoreAxis.COL -> normalizeColRemovalRange(input.editor.currentTemplate, requestedRange)
        }

        if (deletionRange == null) {
            return buildStructureActionResult(
                editor = input.editor,
                selectionState = resolveSelectionAfterRemoveAttempt(),
                nextTemplate = input.editor.currentTemplate,
                nextFileNameSlotsDirtySinceStructureChange = input.editor.fileNameSlotsDirtySinceStructureChange,
                nextPathSlotsDirtySinceStructureChange = input.editor.pathSlotsDirtySinceStructureChange,
                nextDeletedRowsStack = input.editor.deletedRowsStack,
                nextDeletedColsStack = input.editor.deletedColsStack,
                actionLabel = actionLabelForRemove(input.axis),
            )
        }

        val deletionPayload = TableEditorStructureRestoreCoordinator.buildDeletionPayload(
            TableEditorStructureDeletionCoordinatorInput(
                currentTemplate = input.editor.currentTemplate,
                deleteAxis = input.axis,
                deletedRange = deletionRange,
                currentFileNameSlots = input.editor.currentFileNameSlots,
                currentPathSlots = input.editor.currentPathSlots,
                removeCellRefsFromFileNameSlots = input.removeCellRefsFromFileNameSlots,
                removeCellRefsFromPathSlots = input.removeCellRefsFromPathSlots,
            )
        )

        val removedTemplate = when (input.axis) {
            StructureRestoreAxis.ROW -> removeRowsByRange(input.editor.currentTemplate, deletionRange)
            StructureRestoreAxis.COL -> removeColsByRange(input.editor.currentTemplate, deletionRange)
        }
        val sanitizedTemplate = input.sanitizeTemplate(removedTemplate)
        val nextTemplate = applySlotsToTemplate(
            baseTemplate = sanitizedTemplate,
            nextFileNameSlots = deletionPayload.nextFileNameSlots,
            nextPathSlots = deletionPayload.nextPathSlots,
            applyFileNameSlots = input.applyFileNameSlots,
            applyPathSlots = input.applyPathSlots,
        )
        val nextStacks = appendDeletedSnapshotAfterSuccessfulRemove(
            axis = input.axis,
            deletedSnapshot = deletionPayload.deletedSnapshot,
            deletedRowsStack = input.editor.deletedRowsStack,
            deletedColsStack = input.editor.deletedColsStack,
        )

        return buildStructureActionResult(
            editor = input.editor,
            selectionState = resolveSelectionAfterSuccessfulRemove(),
            nextTemplate = nextTemplate,
            nextFileNameSlotsDirtySinceStructureChange = deletionPayload.nextFileNameSlotsDirtySinceStructureChange,
            nextPathSlotsDirtySinceStructureChange = deletionPayload.nextPathSlotsDirtySinceStructureChange,
            nextDeletedRowsStack = nextStacks.first,
            nextDeletedColsStack = nextStacks.second,
            actionLabel = actionLabelForRemove(input.axis),
        )
    }

    private fun buildStructureActionResult(
        editor: StructureEditorContext,
        selectionState: StructureSelectionState,
        nextTemplate: TableTemplateState,
        nextFileNameSlotsDirtySinceStructureChange: Boolean,
        nextPathSlotsDirtySinceStructureChange: Boolean,
        nextDeletedRowsStack: List<DeletedStructureSnapshot>,
        nextDeletedColsStack: List<DeletedStructureSnapshot>,
        actionLabel: String,
    ): StructureActionResult {
        return StructureActionResult(
            nextTemplate = nextTemplate,
            nextFileNameSlotsDirtySinceStructureChange = nextFileNameSlotsDirtySinceStructureChange,
            nextPathSlotsDirtySinceStructureChange = nextPathSlotsDirtySinceStructureChange,
            nextDeletedRowsStack = nextDeletedRowsStack,
            nextDeletedColsStack = nextDeletedColsStack,
            nextStructureSelectionRange = selectionState.range,
            nextStructureSelectedCellIds = selectionState.selectedCellIds,
            nextSelectedCellId = resolveNextSelectedCellId(
                isStructureEditMode = editor.isStructureEditMode,
                selectedCellId = editor.selectedCellId,
                nextTemplate = nextTemplate,
            ),
            actionLabel = actionLabel,
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

    private fun resolveSelectionAfterRemoveAttempt(): StructureSelectionState {
        return clearStructureSelection()
    }

    private fun resolveSelectionAfterSuccessfulRemove(): StructureSelectionState {
        return clearStructureSelection()
    }

    private fun clearStructureSelection(): StructureSelectionState {
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

    private fun lastDeletedSnapshotForAxis(
        axis: StructureRestoreAxis,
        editor: StructureEditorContext,
    ): List<DeletedStructureSnapshot> {
        return when (axis) {
            StructureRestoreAxis.ROW -> editor.deletedRowsStack
            StructureRestoreAxis.COL -> editor.deletedColsStack
        }
    }

    private fun appendDeletedSnapshotAfterSuccessfulRemove(
        axis: StructureRestoreAxis,
        deletedSnapshot: DeletedStructureSnapshot?,
        deletedRowsStack: List<DeletedStructureSnapshot>,
        deletedColsStack: List<DeletedStructureSnapshot>,
    ): Pair<List<DeletedStructureSnapshot>, List<DeletedStructureSnapshot>> {
        return when (axis) {
            StructureRestoreAxis.ROW -> {
                val nextRowsStack = deletedSnapshot?.let { deletedRowsStack + it } ?: deletedRowsStack
                nextRowsStack to deletedColsStack
            }

            StructureRestoreAxis.COL -> {
                val nextColsStack = deletedSnapshot?.let { deletedColsStack + it } ?: deletedColsStack
                deletedRowsStack to nextColsStack
            }
        }
    }

    private fun consumeDeletedStackForRestore(
        axis: StructureRestoreAxis,
        deletedRowsStack: List<DeletedStructureSnapshot>,
        deletedColsStack: List<DeletedStructureSnapshot>,
    ): Pair<List<DeletedStructureSnapshot>, List<DeletedStructureSnapshot>> {
        return when (axis) {
            StructureRestoreAxis.ROW -> deletedRowsStack.dropLast(1) to deletedColsStack
            StructureRestoreAxis.COL -> deletedRowsStack to deletedColsStack.dropLast(1)
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
