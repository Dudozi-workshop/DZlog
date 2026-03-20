package com.dudoziworkshop.dzlog.feature.table.editor.coordinator

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
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
    private fun reindexRestoredCells(
        cells: List<TableCellState>,
        currentTemplate: TableTemplateState,
        restoreAxis: StructureRestoreAxis,
    ): List<TableCellState> {
        return when (restoreAxis) {
            StructureRestoreAxis.ROW -> {
                val newRowIndex = currentTemplate.rows
                cells.map { it.copy(rowIndex = newRowIndex) }
            }

            StructureRestoreAxis.COL -> {
                val newColIndex = currentTemplate.cols
                cells.map { it.copy(colIndex = newColIndex) }
            }
        }
    }

    private fun buildNextTemplate(
        currentTemplate: TableTemplateState,
        restoredCells: List<TableCellState>,
        restoreAxis: StructureRestoreAxis,
    ): TableTemplateState {
        return when (restoreAxis) {
            StructureRestoreAxis.ROW -> {
                val baseRowWeights = currentTemplate.rowWeights
                    ?: List(currentTemplate.rows.coerceAtLeast(1)) { 1f }
                currentTemplate.copy(
                    rows = currentTemplate.rows + 1,
                    cells = currentTemplate.cells + restoredCells,
                    rowWeights = baseRowWeights + 1f,
                )
            }

            StructureRestoreAxis.COL -> {
                val baseColWeights = currentTemplate.colWeights
                    ?: List(currentTemplate.cols.coerceAtLeast(1)) { 1f }
                currentTemplate.copy(
                    cols = currentTemplate.cols + 1,
                    cells = currentTemplate.cells + restoredCells,
                    colWeights = baseColWeights + 1f,
                )
            }
        }
    }

    fun restore(input: TableEditorStructureRestoreCoordinatorInput): TableEditorStructureRestoreCoordinatorResult {
        val restoredReindexedCells = reindexRestoredCells(
            cells = input.restoredSnapshot.cells,
            currentTemplate = input.currentTemplate,
            restoreAxis = input.restoreAxis,
        )
        var nextTemplate = input.sanitizeTemplate(
            buildNextTemplate(
                currentTemplate = input.currentTemplate,
                restoredCells = restoredReindexedCells,
                restoreAxis = input.restoreAxis,
            )
        )

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
}
