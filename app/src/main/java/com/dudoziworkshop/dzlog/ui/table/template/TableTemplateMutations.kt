package com.dudoziworkshop.dzlog.ui.table.template

import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellKind
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft

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
            typedValue = com.dudoziworkshop.dzlog.domain.model.CellValue.Text(""),
            groupLevel = GroupLevel.NONE
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
    val sanitizedFileNameSlotDrafts = sanitizeFileNameSlotDrafts(
        drafts = templateState.fileNameSlotDrafts,
        remainingCells = remainingCells
    )
    val sanitizedPathSlotDrafts = sanitizePathSlotDrafts(
        drafts = templateState.pathSlotDrafts,
        remainingCells = remainingCells
    )

    val baseRowWeights = templateState.rowWeights ?: List(templateState.rows.coerceAtLeast(1)) { 1f }
    val nextRowWeights = if (baseRowWeights.isNotEmpty()) baseRowWeights.dropLast(1) else baseRowWeights

    return templateState.copy(
        rows = templateState.rows - 1,
        cells = remainingCells,
        rowWeights = nextRowWeights,
        fileNameSlotDrafts = sanitizedFileNameSlotDrafts,
        pathSlotDrafts = sanitizedPathSlotDrafts
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
            typedValue = com.dudoziworkshop.dzlog.domain.model.CellValue.Text(""),
            groupLevel = GroupLevel.NONE
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
    val sanitizedFileNameSlotDrafts = sanitizeFileNameSlotDrafts(
        drafts = templateState.fileNameSlotDrafts,
        remainingCells = remainingCells
    )
    val sanitizedPathSlotDrafts = sanitizePathSlotDrafts(
        drafts = templateState.pathSlotDrafts,
        remainingCells = remainingCells
    )

    val baseColWeights = templateState.colWeights ?: List(templateState.cols.coerceAtLeast(1)) { 1f }
    val nextColWeights = if (baseColWeights.isNotEmpty()) baseColWeights.dropLast(1) else baseColWeights

    return templateState.copy(
        cols = templateState.cols - 1,
        cells = remainingCells,
        colWeights = nextColWeights,
        fileNameSlotDrafts = sanitizedFileNameSlotDrafts,
        pathSlotDrafts = sanitizedPathSlotDrafts
    )
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
    // 주요 정책: 삭제 후 슬롯은 앞쪽으로 압축해 중간 null hole을 제거한다.
    return filtered + List((slotCount - filtered.size).coerceAtLeast(0)) { null }
}
