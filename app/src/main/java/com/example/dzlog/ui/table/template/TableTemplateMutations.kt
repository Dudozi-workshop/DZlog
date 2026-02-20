package com.example.dzlog.ui.table.template

import com.example.dzlog.domain.model.CellKey
import com.example.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellKind
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState

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

fun addRow(templateState: TableTemplateState): TableTemplateState {
    val newRowIndex = templateState.rows
    val newCells = (0 until templateState.cols).map { col ->
        TableCellState(
            rowIndex = newRowIndex,
            colIndex = col,
            kind = TableCellKind.INPUT,
            dataType = TableCellDataType.TEXT,
            rawText = "",
            typedValue = com.example.dzlog.domain.model.CellValue.Text(""),
            groupLevel = GroupLevel.NONE,
            label = ""
        )
    }

    // Stage 1: rowWeights는 "행 단위 높이 비율"을 위한 데이터. 아직 렌더링에는 반영하지 않는다.
    val baseRowWeights = templateState.rowWeights ?: List(templateState.rows.coerceAtLeast(1)) { 1f }
    val nextRowWeights = baseRowWeights + 1f

    return templateState.copy(
        rows = templateState.rows + 1,
        cells = templateState.cells + newCells,
        rowWeights = nextRowWeights
    )
}

fun removeRow(templateState: TableTemplateState): TableTemplateState {
    val lastRowIndex = templateState.rows - 1
    val remainingCells = templateState.cells.filterNot { it.rowIndex == lastRowIndex }
    val sanitizedFileNameSlots = sanitizeFileNameSlots(
        slots = templateState.fileNameSlots,
        remainingCells = remainingCells
    )

    val baseRowWeights = templateState.rowWeights ?: List(templateState.rows.coerceAtLeast(1)) { 1f }
    val nextRowWeights = if (baseRowWeights.isNotEmpty()) baseRowWeights.dropLast(1) else baseRowWeights

    return templateState.copy(
        rows = templateState.rows - 1,
        cells = remainingCells,
        rowWeights = nextRowWeights,
        fileNameSlots = sanitizedFileNameSlots
    )
}

fun addColumn(templateState: TableTemplateState): TableTemplateState {
    val newColIndex = templateState.cols
    val newCells = (0 until templateState.rows).map { row ->
        TableCellState(
            rowIndex = row,
            colIndex = newColIndex,
            kind = TableCellKind.INPUT,
            dataType = TableCellDataType.TEXT,
            rawText = "",
            typedValue = com.example.dzlog.domain.model.CellValue.Text(""),
            groupLevel = GroupLevel.NONE,
            label = ""
        )
    }

    // Stage 1: colWeights는 "열 단위 너비 비율"을 위한 데이터. 아직 렌더링에는 반영하지 않는다.
    val baseColWeights = templateState.colWeights ?: List(templateState.cols.coerceAtLeast(1)) { 1f }
    val nextColWeights = baseColWeights + 1f

    return templateState.copy(
        cols = templateState.cols + 1,
        cells = templateState.cells + newCells,
        colWeights = nextColWeights
    )
}

fun removeColumn(templateState: TableTemplateState): TableTemplateState {
    val lastColIndex = templateState.cols - 1
    val remainingCells = templateState.cells.filterNot { it.colIndex == lastColIndex }
    val sanitizedFileNameSlots = sanitizeFileNameSlots(
        slots = templateState.fileNameSlots,
        remainingCells = remainingCells
    )

    val baseColWeights = templateState.colWeights ?: List(templateState.cols.coerceAtLeast(1)) { 1f }
    val nextColWeights = if (baseColWeights.isNotEmpty()) baseColWeights.dropLast(1) else baseColWeights

    return templateState.copy(
        cols = templateState.cols - 1,
        cells = remainingCells,
        colWeights = nextColWeights,
        fileNameSlots = sanitizedFileNameSlots
    )
}

fun addToFileNameSlots(slots: List<CellKey?>, cellKey: CellKey): List<CellKey?> {
    val normalized = slots.normalizeFileNameSlots()
    if (normalized.contains(cellKey)) return normalized

    val firstEmptyIndex = normalized.indexOfFirst { it == null }
    if (firstEmptyIndex == -1) return normalized

    return normalized.toMutableList().apply {
        this[firstEmptyIndex] = cellKey
    }
}

fun removeFromFileNameSlots(slots: List<CellKey?>, cellKey: CellKey): List<CellKey?> {
    val removed = slots.normalizeFileNameSlots().map { key ->
        if (key == cellKey) null else key
    }
    return compressFileNameSlots(removed)
}

fun reorderFileNameSlots(slots: List<CellKey?>, fromIndex: Int, toIndex: Int): List<CellKey?> {
    val normalized = slots.normalizeFileNameSlots()
    if (fromIndex !in normalized.indices || toIndex !in normalized.indices) {
        return normalized
    }

    val mutable = normalized.toMutableList()
    val moving = mutable.removeAt(fromIndex)
    mutable.add(toIndex, moving)
    return compressFileNameSlots(mutable)
}

fun compressFileNameSlots(slots: List<CellKey?>): List<CellKey?> {
    val nonNulls = slots.filterNotNull().distinct().take(FILE_NAME_SLOT_COUNT)
    return nonNulls + List(FILE_NAME_SLOT_COUNT - nonNulls.size) { null }
}

private fun List<CellKey?>.normalizeFileNameSlots(): List<CellKey?> {
    return take(FILE_NAME_SLOT_COUNT) + List((FILE_NAME_SLOT_COUNT - size).coerceAtLeast(0)) { null }
}

private fun sanitizeFileNameSlots(
    slots: List<CellKey?>,
    remainingCells: List<TableCellState>
): List<CellKey?> {
    val remainingCellIds = remainingCells.map { it.cellId }.toSet()
    val keptOrNull = slots.normalizeFileNameSlots().map { slot ->
        if (slot != null && slot !in remainingCellIds) null else slot
    }
    return compressFileNameSlots(keptOrNull)
}
