package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellKind
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.TableEditorPolicy
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator

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

fun addColumn(templateState: TableTemplateState): TableTemplateState =
    addColumnBySelection(templateState = templateState, selectionRange = null)

fun removeColumn(templateState: TableTemplateState): TableTemplateState =
    removeColsByRange(
        templateState = templateState,
        range = lastIndexRange(templateState.cols),
    )

fun addRowBySelection(
    templateState: TableTemplateState,
    selectionRange: TableSelectionRange?,
): TableTemplateState {
    if (hasMergedCells(templateState)) return templateState
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

    val baseWeights = TableLayoutCalculator.resolveWeights(templateState.rowWeights, templateState.rows)
    val nextWeights = baseWeights.toMutableList().apply { add(insertAt, 1f) }

    return templateState.copy(
        rows = templateState.rows + 1,
        cells = (shifted + newCells).sortedWith(compareBy({ it.rowIndex }, { it.colIndex })),
        rowWeights = nextWeights,
    )
}

fun addColumnBySelection(
    templateState: TableTemplateState,
    selectionRange: TableSelectionRange?,
): TableTemplateState {
    if (hasMergedCells(templateState)) return templateState
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

    val baseWeights = TableLayoutCalculator.resolveWeights(templateState.colWeights, templateState.cols)
    val nextWeights = baseWeights.toMutableList().apply { add(insertAt, 1f) }

    return templateState.copy(
        cols = templateState.cols + 1,
        cells = (shifted + newCells).sortedWith(compareBy({ it.rowIndex }, { it.colIndex })),
        colWeights = nextWeights,
    )
}

fun removeRowBySelection(
    templateState: TableTemplateState,
    selectionRange: TableSelectionRange?,
): TableTemplateState {
    return removeRowsByRange(
        templateState = templateState,
        range = selectionRange?.let { it.minRow..it.maxRow } ?: lastIndexRange(templateState.rows),
    )
}

fun removeColumnBySelection(
    templateState: TableTemplateState,
    selectionRange: TableSelectionRange?,
): TableTemplateState {
    return removeColsByRange(
        templateState = templateState,
        range = selectionRange?.let { it.minCol..it.maxCol } ?: lastIndexRange(templateState.cols),
    )
}

fun removeRowsByRange(
    templateState: TableTemplateState,
    range: IntRange,
): TableTemplateState {
    if (hasMergedCells(templateState)) return templateState
    val normalizedRange = normalizeRowRemovalRange(templateState, range) ?: return templateState

    val remainingCells = templateState.cells
        .filterNot { it.rowIndex in normalizedRange }
        .map { cell ->
            if (cell.rowIndex > normalizedRange.last) {
                cell.copy(rowIndex = cell.rowIndex - normalizedRange.count())
            } else {
                cell
            }
        }

    return templateState.copy(
        rows = templateState.rows - normalizedRange.count(),
        cells = remainingCells.sortedWith(compareBy({ it.rowIndex }, { it.colIndex })),
        rowWeights = removeRange(
            TableLayoutCalculator.resolveWeights(templateState.rowWeights, templateState.rows),
            normalizedRange.first,
            normalizedRange.last,
        ),
        fileNameSlotDrafts = sanitizeFileNameSlotDrafts(templateState.fileNameSlotDrafts, remainingCells),
        pathSlotDrafts = sanitizePathSlotDrafts(templateState.pathSlotDrafts, remainingCells),
    )
}

fun removeColsByRange(
    templateState: TableTemplateState,
    range: IntRange,
): TableTemplateState {
    if (hasMergedCells(templateState)) return templateState
    val normalizedRange = normalizeColRemovalRange(templateState, range) ?: return templateState

    val remainingCells = templateState.cells
        .filterNot { it.colIndex in normalizedRange }
        .map { cell ->
            if (cell.colIndex > normalizedRange.last) {
                cell.copy(colIndex = cell.colIndex - normalizedRange.count())
            } else {
                cell
            }
        }

    return templateState.copy(
        cols = templateState.cols - normalizedRange.count(),
        cells = remainingCells.sortedWith(compareBy({ it.rowIndex }, { it.colIndex })),
        colWeights = removeRange(
            TableLayoutCalculator.resolveWeights(templateState.colWeights, templateState.cols),
            normalizedRange.first,
            normalizedRange.last,
        ),
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

private fun removeRange(weights: List<Float>, start: Int, end: Int): List<Float> {
    if (weights.isEmpty()) return weights
    return weights.filterIndexed { index, _ -> index !in start..end }
}

private fun hasMergedCells(templateState: TableTemplateState): Boolean {
    return templateState.cells.any { it.rowSpan > 1 || it.colSpan > 1 }
}

fun normalizeRowRemovalRange(
    templateState: TableTemplateState,
    range: IntRange,
): IntRange? {
    return normalizeRemovalRange(
        requestedRange = range,
        axisSize = templateState.rows,
        minSize = TableEditorPolicy.MIN_ROWS,
    )
}

fun normalizeColRemovalRange(
    templateState: TableTemplateState,
    range: IntRange,
): IntRange? {
    return normalizeRemovalRange(
        requestedRange = range,
        axisSize = templateState.cols,
        minSize = TableEditorPolicy.MIN_COLS,
    )
}

private fun normalizeRemovalRange(
    requestedRange: IntRange,
    axisSize: Int,
    minSize: Int,
): IntRange? {
    if (axisSize <= minSize) return null
    val boundedStart = requestedRange.first.coerceIn(0, axisSize - 1)
    val boundedEnd = requestedRange.last.coerceIn(boundedStart, axisSize - 1)
    val removableCount = (axisSize - minSize).coerceAtLeast(0)
    val requestedCount = boundedEnd - boundedStart + 1
    val actualCount = requestedCount.coerceAtMost(removableCount)
    if (actualCount == 0) return null
    return boundedStart..(boundedStart + actualCount - 1)
}

private fun lastIndexRange(axisSize: Int): IntRange {
    val lastIndex = (axisSize - 1).coerceAtLeast(0)
    return lastIndex..lastIndex
}
