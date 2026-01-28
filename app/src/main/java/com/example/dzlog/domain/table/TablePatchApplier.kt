package com.example.dzlog.domain.table

import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.TableCellDataType

fun TableTemplateState.applyPatch(patch: TablePatch): TableTemplateState {
    if (patch.updatesByCellId.isEmpty()) return this
    val updated = cells.map { cell ->
        val newValue = patch.updatesByCellId[cell.cellId]
        if (newValue == null) {
            cell
        } else {
            if (cell.dataType == TableCellDataType.COUNTER) {
                val v = newValue.trim().toIntOrNull()?.takeIf { it >= 0 } ?: 1
                cell.copy(rawText = v.toString(), typedValue = CellValue.Counter(v))
            } else {
                // Patch는 현재 COUNTER에만 사용한다. 다른 타입은 무시한다.
                cell
            }
        }
    }
    return copy(cells = updated)
}
