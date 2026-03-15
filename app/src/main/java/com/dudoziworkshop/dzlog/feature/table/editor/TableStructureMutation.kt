package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellKind
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.TableEditorPolicy

fun updateCell(
    templateState: TableTemplateState,
    cellId: String,
    transform: (TableCellState) -> TableCellState
): TableTemplateState {
    return templateState.copy(
        cells = templateState.cells.map { cell ->
            if (cell.cellId == cellId) transform(cell) else cell
        }
    )
}

fun addRow(templateState: TableTemplateState): TableTemplateState =
    addRowBySelection(templateState = templateState, selectionRange = null)

fun removeRow(templateState: TableTemplateState): TableTemplateState =
    removeRowBySelection(templateState = templateState, selectionRange = null)

fun addColumn(templateState: TableTemplateState): TableTemplateState =
    addColumnBySelection(templateState = templateState, selectionRange = null)

fun removeColumn(templateState: TableTemplateState): TableTemplateState =
    removeColumnBySelection(templateState = templateState, selectionRange = null)

fun addRowBySelection(
    templateState: TableTemplateState,
    selectionRange: TableSelectionRange?,
): TableTemplateState {
    if (templateState.rows >= TableEditorPolicy.MAX_ROWS) return templateState

    val insertAt = (selectionRange?.maxRow?.plus(1) ?: templateState.rows).coerceIn(0, templateState.rows)
    val shifted = templateState.cells.map { cell ->
        if (cell.rowIndex >= insertAt) cell.copy(rowIndex = cell.rowIndex + 1) else cell
    }
    val newCells = (0 until templateState.cols).map { col ->
        TableCellState(
            rowIndex = insertAt,
            colIndex = col,
            kind = TableCellKind.INPUT,
            dataType = TableCellDataType.TEXT,
            rawText = "",
            typedValue = com.dudoziworkshop.dzlog.domain.model.CellValue.Text(""),
            groupLevel = GroupLevel.NONE
        )
    }

    val baseWeights = resolveWeightsOrOnes(templateState.rowWeights, templateState.rows)
    val nextWeights = baseWeights.toMutableList().apply { add(insertAt, 1f) }

    return templateState.copy(
        rows = templateState.rows + 1,
        cells = (shifted + newCells).sortedWith(compareBy<TableCellState>({ it.rowIndex }, { it.colIndex })),
        rowWeights = nextWeights,
    )
}

fun addColumnBySelection(
    templateState: TableTemplateState,
    selectionRange: TableSelectionRange?,
): TableTemplateState {
    if (templateState.cols >= TableEditorPolicy.MAX_COLS) return templateState

    val insertAt = (selectionRange?.maxCol?.plus(1) ?: templateState.cols).coerceIn(0, templateState.cols)
    val shifted = templateState.cells.map { cell ->
        if (cell.colIndex >= insertAt) cell.copy(colIndex = cell.colIndex + 1) else cell
    }
    val newCells = (0 until templateState.rows).map { row ->
        TableCellState(
            rowIndex = row,
            colIndex = insertAt,
            kind = TableCellKind.INPUT,
            dataType = TableCellDataType.TEXT,
            rawText = "",
            typedValue = com.dudoziworkshop.dzlog.domain.model.CellValue.Text(""),
            groupLevel = GroupLevel.NONE
        )
    }

    val baseWeights = resolveWeightsOrOnes(templateState.colWeights, templateState.cols)
    val nextWeights = baseWeights.toMutableList().apply { add(insertAt, 1f) }

    return templateState.copy(
        cols = templateState.cols + 1,
        cells = (shifted + newCells).sortedWith(compareBy<TableCellState>({ it.rowIndex }, { it.colIndex })),
        colWeights = nextWeights,
    )
}

fun removeRowBySelection(
    templateState: TableTemplateState,
    selectionRange: TableSelectionRange?,
): TableTemplateState {
    if (templateState.rows <= TableEditorPolicy.MIN_ROWS) return templateState

    val (removeStart, removeEnd) = if (selectionRange != null) {
        selectionRange.minRow.coerceAtLeast(0) to selectionRange.maxRow.coerceAtMost(templateState.rows - 1)
    } else {
        val last = templateState.rows - 1
        last to last
    }
    val removeCount = (removeEnd - removeStart + 1).coerceAtLeast(0)
    val nextRows = (templateState.rows - removeCount).coerceAtLeast(TableEditorPolicy.MIN_ROWS)
    val actualRemoveCount = templateState.rows - nextRows
    val actualRemoveEnd = removeStart + actualRemoveCount - 1

    val remainingCells = templateState.cells
        .filterNot { it.rowIndex in removeStart..actualRemoveEnd }
        .map { cell ->
            if (cell.rowIndex > actualRemoveEnd) cell.copy(rowIndex = cell.rowIndex - actualRemoveCount) else cell
        }

    return templateState.copy(
        rows = nextRows,
        cells = remainingCells.sortedWith(compareBy<TableCellState>({ it.rowIndex }, { it.colIndex })),
        rowWeights = removeRange(resolveWeightsOrOnes(templateState.rowWeights, templateState.rows), removeStart, actualRemoveEnd),
        fileNameSlotDrafts = sanitizeFileNameSlotDrafts(templateState.fileNameSlotDrafts, remainingCells),
        pathSlotDrafts = sanitizePathSlotDrafts(templateState.pathSlotDrafts, remainingCells),
    )
}

fun removeColumnBySelection(
    templateState: TableTemplateState,
    selectionRange: TableSelectionRange?,
): TableTemplateState {
    if (templateState.cols <= TableEditorPolicy.MIN_COLS) return templateState

    val (removeStart, removeEnd) = if (selectionRange != null) {
        selectionRange.minCol.coerceAtLeast(0) to selectionRange.maxCol.coerceAtMost(templateState.cols - 1)
    } else {
        val last = templateState.cols - 1
        last to last
    }
    val removeCount = (removeEnd - removeStart + 1).coerceAtLeast(0)
    val nextCols = (templateState.cols - removeCount).coerceAtLeast(TableEditorPolicy.MIN_COLS)
    val actualRemoveCount = templateState.cols - nextCols
    val actualRemoveEnd = removeStart + actualRemoveCount - 1

    val remainingCells = templateState.cells
        .filterNot { it.colIndex in removeStart..actualRemoveEnd }
        .map { cell ->
            if (cell.colIndex > actualRemoveEnd) cell.copy(colIndex = cell.colIndex - actualRemoveCount) else cell
        }

    return templateState.copy(
        cols = nextCols,
        cells = remainingCells.sortedWith(compareBy<TableCellState>({ it.rowIndex }, { it.colIndex })),
        colWeights = removeRange(resolveWeightsOrOnes(templateState.colWeights, templateState.cols), removeStart, actualRemoveEnd),
        fileNameSlotDrafts = sanitizeFileNameSlotDrafts(templateState.fileNameSlotDrafts, remainingCells),
        pathSlotDrafts = sanitizePathSlotDrafts(templateState.pathSlotDrafts, remainingCells),
    )
}

fun resetRowWeights(templateState: TableTemplateState): TableTemplateState {
    return templateState.copy(rowWeights = List(templateState.rows.coerceAtLeast(1)) { 1f })
}

fun resetColumnWeights(templateState: TableTemplateState): TableTemplateState {
    return templateState.copy(colWeights = List(templateState.cols.coerceAtLeast(1)) { 1f })
}

fun distributeRowWeightsEvenly(templateState: TableTemplateState): TableTemplateState = resetRowWeights(templateState)
fun distributeColumnWeightsEvenly(templateState: TableTemplateState): TableTemplateState = resetColumnWeights(templateState)

private fun sanitizeFileNameSlotDrafts(
    drafts: List<TableEditorSlotDraft?>,
    remainingCells: List<TableCellState>
): List<TableEditorSlotDraft?> {
    return sanitizeAndCompressSlotDrafts(
        drafts = drafts,
        slotCount = FILE_NAME_SLOT_COUNT,
        remainingCells = remainingCells
    )
}

private fun sanitizePathSlotDrafts(
    drafts: List<TableEditorSlotDraft?>,
    remainingCells: List<TableCellState>
): List<TableEditorSlotDraft?> {
    return sanitizeAndCompressSlotDrafts(
        drafts = drafts,
        slotCount = PATH_SLOT_COUNT,
        remainingCells = remainingCells
    )
}

private fun sanitizeAndCompressSlotDrafts(
    drafts: List<TableEditorSlotDraft?>,
    slotCount: Int,
    remainingCells: List<TableCellState>
): List<TableEditorSlotDraft?> {
    val remainingCellIds = remainingCells.map { it.cellId }.toSet()
    val filtered = drafts.take(slotCount).mapNotNull { draft ->
        val isCellSlot = draft?.kind.equals("CELL", ignoreCase = true)
        if (isCellSlot && draft?.cellId != null && draft.cellId !in remainingCellIds) {
            null
        } else {
            draft
        }
    }
    return filtered + List((slotCount - filtered.size).coerceAtLeast(0)) { null }
}

private fun resolveWeightsOrOnes(weights: List<Float>?, count: Int): List<Float> {
    if (count <= 0) return emptyList()
    if (weights == null || weights.size != count) return List(count) { 1f }
    return weights.map { it.coerceAtLeast(0.0001f) }
}

private fun removeRange(weights: List<Float>, start: Int, end: Int): List<Float> {
    if (weights.isEmpty()) return weights
    return weights.filterIndexed { index, _ -> index !in start..end }
}
