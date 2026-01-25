package com.example.dzlog.domain.table

import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState

fun TableTemplateState.applyPatch(patch: TablePatch): TableTemplateState {
    if (patch.updatesByCellId.isEmpty()) return this
    val updated = cells.map { cell ->
        val newValue = patch.updatesByCellId[cell.cellId]
        if (newValue == null) cell else cell.copy(valueText = newValue)
    }
    return copy(cells = updated)
}
