package com.dudoziworkshop.dzlog.ui.table.debug

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellState

private fun String?.orDash(): String = if (this.isNullOrBlank()) "-" else this

private fun shortCellId(cellId: String): String = cellId.take(6)

private fun summarizeCellValue(cell: TableCellState): String {
    val raw = cell.rawText.takeIf { it.isNotBlank() }
    if (raw != null) return raw

    return when (val typed = cell.typedValue) {
        is CellValue.Text -> typed.text.ifBlank { "blank" }
        is CellValue.Number -> typed.text.ifBlank { "blank" }
        is CellValue.CounterSeed -> "counter:${typed.start}"
        CellValue.Auto -> "auto"
    }
}

fun summarizeAxisCells(cells: List<TableCellState>): String {
    if (cells.isEmpty()) return "-"
    return cells.joinToString(separator = "\n") { cell ->
        val badge = when (cell.groupLevel) {
            GroupLevel.NONE -> "-"
            else -> cell.groupLevel.name
        }
        "[${cell.rowIndex},${cell.colIndex}] id=${shortCellId(cell.cellId)} value=${summarizeCellValue(cell)} badge=$badge type=${cell.dataType}"
    }
}

fun buildTableEditorDebugOverlayState(source: TableEditorDebugOverlaySource): TableEditorDebugOverlayState {
    val restoreInfo = source.latestRestoreDebugInfo
    val deletionInfo = source.latestDeletionDebugInfo
    val selectedRowCol = source.selectedCell?.let { "row=${it.rowIndex}, col=${it.colIndex}" } ?: "-"
    val selectionSummary = source.selectionRange?.let {
        "rows=${it.minRow}..${it.maxRow}, cols=${it.minCol}..${it.maxCol}"
    } ?: "-"

    return TableEditorDebugOverlayState(
        lastAction = source.lastAction,
        currentMode = source.currentMode,
        bottomPanelMode = source.bottomPanelMode.name,
        rows = source.rows,
        cols = source.cols,
        selectedCellId = source.selectedCellId.orDash(),
        selectedRowCol = selectedRowCol,
        selectionSummary = selectionSummary,
        deletedSnapshotSummary = summarizeAxisCells(
            restoreInfo?.deletedSnapshotCells ?: deletionInfo?.deletedCells.orEmpty()
        ),
        restoredPayloadSummary = summarizeAxisCells(restoreInfo?.deletedSnapshotCells.orEmpty()),
        reindexedPayloadSummary = summarizeAxisCells(restoreInfo?.reindexedRestoredCells.orEmpty()),
        mergedAxisSummary = summarizeAxisCells(restoreInfo?.mergedAxisCells.orEmpty()),
        sanitizedAxisSummary = summarizeAxisCells(restoreInfo?.sanitizedAxisCells.orEmpty()),
        restoreAxis = restoreInfo?.axis?.name ?: deletionInfo?.axis?.name ?: "-",
        restoreTargetIndex = restoreInfo?.targetIndex?.toString()
            ?: deletionInfo?.deletedRange?.firstOrNull()?.toString()
            ?: "-",
        hasSlotSnapshot = restoreInfo?.hasSlotSnapshot ?: deletionInfo?.hasSlotSnapshot ?: false,
        fileNameSlotsDirty = source.fileNameSlotsDirty,
        pathSlotsDirty = source.pathSlotsDirty,
        undoSummary = source.undoSummary,
        deletedStacksSummary = "rows=${source.deletedRowsStackSize}, cols=${source.deletedColsStackSize}",
    )
}
