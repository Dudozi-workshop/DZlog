package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.table.resolveTableRootCells
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@Composable
fun TemplateMiniPreview(template: TableTemplateState) {
    val rows = TableLayoutCalculator.resolveWeights(template.rowWeights, template.rows)
    val cols = TableLayoutCalculator.resolveWeights(template.colWeights, template.cols)
    val roots = resolveTableRootCells(template.cells)
    Canvas(Modifier.size(width = 48.dp, height = 36.dp)) {
        if (rows.isEmpty() || cols.isEmpty()) return@Canvas
        val rowTotal = rows.sum().coerceAtLeast(0.0001f)
        val colTotal = cols.sum().coerceAtLeast(0.0001f)
        roots.forEach { cell ->
            val r = cell.rowIndex.coerceIn(rows.indices)
            val c = cell.colIndex.coerceIn(cols.indices)
            val bottom = (r + cell.rowSpan.coerceAtLeast(1)).coerceAtMost(rows.size)
            val right = (c + cell.colSpan.coerceAtLeast(1)).coerceAtMost(cols.size)
            val origin = Offset(cols.take(c).sum() / colTotal * size.width,
                rows.take(r).sum() / rowTotal * size.height)
            val cellSize = Size(cols.subList(c, right).sum() / colTotal * size.width,
                rows.subList(r, bottom).sum() / rowTotal * size.height)
            drawRect(DDZColor.SelectedSoft, origin, cellSize)
            drawRect(DDZColor.SageBorder, origin, cellSize, style = Stroke(1.dp.toPx()))
        }
    }
}
