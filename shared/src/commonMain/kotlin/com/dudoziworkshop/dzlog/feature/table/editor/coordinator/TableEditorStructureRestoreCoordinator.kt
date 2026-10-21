package com.dudoziworkshop.dzlog.feature.table.editor.coordinator

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.addColumn
import com.dudoziworkshop.dzlog.feature.table.editor.addRow
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.StructureSlotSnapshot
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorStructureSlotHandlers
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem

data class DeletedStructureSnapshot(
    val cells: List<TableCellState>,
    val slotSnapshot: StructureSlotSnapshot? = null,
)

enum class StructureRestoreAxis {
    ROW,
    COL,
}

data class TableEditorStructureDeletionCoordinatorInput(
    val currentTemplate: TableTemplateState,
    val deleteAxis: StructureRestoreAxis,
    val deletedRange: IntRange?,
    val currentFileNameSlots: List<FileNameSlotUiItem?>,
    val currentPathSlots: List<PathSlotUiItem?>,
    val removeCellRefsFromFileNameSlots: (List<FileNameSlotUiItem?>, Set<String>) -> List<FileNameSlotUiItem?>,
    val removeCellRefsFromPathSlots: (List<PathSlotUiItem?>, Set<String>) -> List<PathSlotUiItem?>,
)

data class TableEditorStructureDeletionCoordinatorResult(
    val deletedSnapshot: DeletedStructureSnapshot?,
    val nextFileNameSlots: List<FileNameSlotUiItem?>,
    val nextPathSlots: List<PathSlotUiItem?>,
    val nextFileNameSlotsDirtySinceStructureChange: Boolean,
    val nextPathSlotsDirtySinceStructureChange: Boolean,
)

data class TableEditorStructureRestoreCoordinatorInput(
    val currentTemplate: TableTemplateState,
    val restoredSnapshot: DeletedStructureSnapshot,
    val currentFileNameSlots: List<FileNameSlotUiItem?>,
    val currentPathSlots: List<PathSlotUiItem?>,
    val fileNameSlotsDirtySinceStructureChange: Boolean,
    val pathSlotsDirtySinceStructureChange: Boolean,
    val restoreAxis: StructureRestoreAxis,
    val sanitizeTemplate: (TableTemplateState) -> TableTemplateState,
    val applyFileNameSlots: (TableTemplateState, List<FileNameSlotUiItem?>) -> TableTemplateState,
    val applyPathSlots: (TableTemplateState, List<PathSlotUiItem?>) -> TableTemplateState,
)

data class TableEditorStructureRestoreCoordinatorResult(
    val nextTemplate: TableTemplateState,
    val nextFileNameSlotsDirtySinceStructureChange: Boolean,
    val nextPathSlotsDirtySinceStructureChange: Boolean,
)

object TableEditorStructureRestoreCoordinator {
    private fun collectDeletedCells(
        currentTemplate: TableTemplateState,
        deleteAxis: StructureRestoreAxis,
        deletedRange: IntRange?,
    ): List<TableCellState> {
        val deletedCells = when (deleteAxis) {
            StructureRestoreAxis.ROW -> currentTemplate.cells
                .filter { cell -> deletedRange?.contains(cell.rowIndex) == true }
                .sortedWith(compareBy({ it.rowIndex }, { it.colIndex }))

            StructureRestoreAxis.COL -> currentTemplate.cells
                .filter { cell -> deletedRange?.contains(cell.colIndex) == true }
                .sortedWith(compareBy({ it.colIndex }, { it.rowIndex }))
        }
        return deletedCells
    }

    fun buildDeletionPayload(
        input: TableEditorStructureDeletionCoordinatorInput,
    ): TableEditorStructureDeletionCoordinatorResult {
        val deletedCells = collectDeletedCells(
            currentTemplate = input.currentTemplate,
            deleteAxis = input.deleteAxis,
            deletedRange = input.deletedRange,
        )
        val deletedCellIds = deletedCells.map { it.cellId }.toSet()
        val slotDeletionResult = TableEditorStructureSlotHandlers.handleStructureDeletion(
            deletedCellIds = deletedCellIds,
            currentFileNameSlots = input.currentFileNameSlots,
            currentPathSlots = input.currentPathSlots,
            removeCellRefsFromFileNameSlots = input.removeCellRefsFromFileNameSlots,
            removeCellRefsFromPathSlots = input.removeCellRefsFromPathSlots,
        )

        return TableEditorStructureDeletionCoordinatorResult(
            deletedSnapshot = deletedCells.takeIf { it.isNotEmpty() }?.let { cells ->
                DeletedStructureSnapshot(
                    cells = cells,
                    slotSnapshot = slotDeletionResult.snapshotToStore,
                )
            },
            nextFileNameSlots = slotDeletionResult.nextFileNameSlots,
            nextPathSlots = slotDeletionResult.nextPathSlots,
            nextFileNameSlotsDirtySinceStructureChange = slotDeletionResult.nextFileNameSlotsDirtySinceStructureChange,
            nextPathSlotsDirtySinceStructureChange = slotDeletionResult.nextPathSlotsDirtySinceStructureChange,
        )
    }

    private fun extractRestoredCellPayload(
        restoredSnapshot: DeletedStructureSnapshot,
    ): List<TableCellState> {
        return restoredSnapshot.cells
    }

    private fun reindexRestoredCells(
        restoredCells: List<TableCellState>,
        currentTemplate: TableTemplateState,
        restoreAxis: StructureRestoreAxis,
    ): List<TableCellState> {
        return when (restoreAxis) {
            StructureRestoreAxis.ROW -> {
                val newRowIndex = currentTemplate.rows
                restoredCells.map { it.copy(rowIndex = newRowIndex) }
            }

            StructureRestoreAxis.COL -> {
                val newColIndex = currentTemplate.cols
                restoredCells.map { it.copy(colIndex = newColIndex) }
            }
        }
    }

    private fun buildTemplateWithInsertedAxis(
        currentTemplate: TableTemplateState,
        restoreAxis: StructureRestoreAxis,
    ): TableTemplateState {
        return when (restoreAxis) {
            StructureRestoreAxis.ROW -> addRow(currentTemplate)
            StructureRestoreAxis.COL -> addColumn(currentTemplate)
        }
    }

    private fun validateRestoredCellsShape(
        restoredCells: List<TableCellState>,
        insertedTemplate: TableTemplateState,
        restoreAxis: StructureRestoreAxis,
    ) {
        when (restoreAxis) {
            StructureRestoreAxis.ROW -> check(restoredCells.size == insertedTemplate.cols) {
                "Restored row cell count must match inserted template cols"
            }

            StructureRestoreAxis.COL -> check(restoredCells.size == insertedTemplate.rows) {
                "Restored col cell count must match inserted template rows"
            }
        }
    }

    private fun mergeRestoredCellsIntoInsertedAxis(
        insertedTemplate: TableTemplateState,
        restoredCells: List<TableCellState>,
        restoreAxis: StructureRestoreAxis,
    ): TableTemplateState {
        validateRestoredCellsShape(
            restoredCells = restoredCells,
            insertedTemplate = insertedTemplate,
            restoreAxis = restoreAxis,
        )

        val restoredCellByPosition = when (restoreAxis) {
            StructureRestoreAxis.ROW -> restoredCells.associateBy { it.colIndex }
            StructureRestoreAxis.COL -> restoredCells.associateBy { it.rowIndex }
        }

        val insertedAxisIndex = when (restoreAxis) {
            StructureRestoreAxis.ROW -> insertedTemplate.rows - 1
            StructureRestoreAxis.COL -> insertedTemplate.cols - 1
        }

        val mergedCells = insertedTemplate.cells.map { currentCell ->
            val isInsertedAxisCell = when (restoreAxis) {
                StructureRestoreAxis.ROW -> currentCell.rowIndex == insertedAxisIndex
                StructureRestoreAxis.COL -> currentCell.colIndex == insertedAxisIndex
            }
            if (!isInsertedAxisCell) {
                return@map currentCell
            }

            val positionKey = when (restoreAxis) {
                StructureRestoreAxis.ROW -> currentCell.colIndex
                StructureRestoreAxis.COL -> currentCell.rowIndex
            }
            val restoredCell = checkNotNull(restoredCellByPosition[positionKey]) {
                "Missing restored cell for inserted axis position"
            }
            restoredCell.copy(cellId = currentCell.cellId)
        }

        return insertedTemplate.copy(cells = mergedCells)
    }

    private fun applyRestoredSlotSnapshotIfNeeded(
        sanitizedTemplate: TableTemplateState,
        input: TableEditorStructureRestoreCoordinatorInput,
    ): TableEditorStructureRestoreCoordinatorResult {
        var nextTemplate = sanitizedTemplate
        val slotRestoreResult = input.restoredSnapshot.slotSnapshot?.let { slotSnapshot ->
            TableEditorStructureSlotHandlers.handleStructureRestore(
                restoredSnapshot = slotSnapshot,
                currentFileNameSlots = input.currentFileNameSlots,
                currentPathSlots = input.currentPathSlots,
                fileNameSlotsDirtySinceStructureChange = input.fileNameSlotsDirtySinceStructureChange,
                pathSlotsDirtySinceStructureChange = input.pathSlotsDirtySinceStructureChange,
            )
        }

        if (slotRestoreResult?.restoredFileNameSlotsSnapshot == true) {
            nextTemplate = input.applyFileNameSlots(nextTemplate, slotRestoreResult.nextFileNameSlots)
        }
        if (slotRestoreResult?.restoredPathSlotsSnapshot == true) {
            nextTemplate = input.applyPathSlots(nextTemplate, slotRestoreResult.nextPathSlots)
        }

        return TableEditorStructureRestoreCoordinatorResult(
            nextTemplate = nextTemplate,
            nextFileNameSlotsDirtySinceStructureChange = slotRestoreResult?.nextFileNameSlotsDirtySinceStructureChange
                ?: input.fileNameSlotsDirtySinceStructureChange,
            nextPathSlotsDirtySinceStructureChange = slotRestoreResult?.nextPathSlotsDirtySinceStructureChange
                ?: input.pathSlotsDirtySinceStructureChange,
        )
    }

    fun restore(input: TableEditorStructureRestoreCoordinatorInput): TableEditorStructureRestoreCoordinatorResult {
        val restoredCellPayload = extractRestoredCellPayload(input.restoredSnapshot)
        val restoredReindexedCells = reindexRestoredCells(
            restoredCells = restoredCellPayload,
            currentTemplate = input.currentTemplate,
            restoreAxis = input.restoreAxis,
        )
        val insertedTemplate = buildTemplateWithInsertedAxis(
            currentTemplate = input.currentTemplate,
            restoreAxis = input.restoreAxis,
        )
        val templateWithRestoredCells = mergeRestoredCellsIntoInsertedAxis(
            insertedTemplate = insertedTemplate,
            restoredCells = restoredReindexedCells,
            restoreAxis = input.restoreAxis,
        )
        val sanitizedTemplate = input.sanitizeTemplate(templateWithRestoredCells)

        return applyRestoredSlotSnapshotIfNeeded(
            sanitizedTemplate = sanitizedTemplate,
            input = input,
        )
    }
}
