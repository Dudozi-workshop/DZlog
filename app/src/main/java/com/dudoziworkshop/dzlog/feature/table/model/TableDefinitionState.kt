package com.dudoziworkshop.dzlog.feature.table.model

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

data class TableDefinitionState(
    val rows: Int,
    val cols: Int,
    val rowWeights: List<Float>?,
    val colWeights: List<Float>?,
    val cells: List<TableCellState>,
) {
    companion object {
        fun from(template: TableTemplateState): TableDefinitionState = TableDefinitionState(
            rows = template.rows,
            cols = template.cols,
            rowWeights = template.rowWeights,
            colWeights = template.colWeights,
            cells = template.cells,
        )
    }
}
