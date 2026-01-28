package com.example.dzlog.domain.table

import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableTemplateState

fun TableTemplateState.applyPatch(patch: TablePatch): TableTemplateState {
    if (patch.updatesByCellId.isEmpty()) return this
    val updated = cells.map { cell ->
        val newValue = patch.updatesByCellId[cell.cellId] ?: return@map cell
        // 현재 patch는 COUNTER 증가(저장 성공 후) 용도로만 사용한다.
        if (cell.dataType != TableCellDataType.COUNTER) return@map cell

        val next = newValue.trim().toIntOrNull()?.takeIf { it >= 0 } ?: return@map cell
        cell.copy(typedValue = CellValue.CounterSeed(next))
    }
    return copy(cells = updated)
}
