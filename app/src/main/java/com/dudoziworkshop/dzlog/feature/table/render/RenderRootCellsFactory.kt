package com.dudoziworkshop.dzlog.feature.table.render

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions

fun buildRenderRootCells(templateCells: List<TableCellState>): List<RenderRootCell> =
    TableStructureRangeActions.rootCells(templateCells).map {
        RenderRootCell(
            cellId = it.cellId,
            rowIndex = it.rowIndex,
            colIndex = it.colIndex,
            rowSpan = it.rowSpan,
            colSpan = it.colSpan,
        )
    }
