package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.table.resolveTableRootCells

internal data class IosCaptureCellRect(val row: Int, val col: Int, val rows: Int, val cols: Int)

/** Same visible-root rule as the editor; malformed/crossing spans fail before saving. */
internal fun iosCaptureCellRects(template: TableTemplateState): List<IosCaptureCellRect> {
    val rows = template.rows
    val cols = template.cols
    require(rows > 0 && cols > 0 && rows.toLong() * cols <= 10000) { "표 크기가 올바르지 않습니다." }
    require(template.cells.map { it.cellId }.distinct().size == template.cells.size) { "중복된 표 셀 ID가 있습니다." }
    require(template.cells.map { it.rowIndex to it.colIndex }.distinct().size == template.cells.size) {
        "중복된 표 셀 좌표가 있습니다."
    }
    template.cells.forEach {
        require(it.rowIndex in 0 until rows && it.colIndex in 0 until cols &&
            it.rowSpan > 0 && it.rowSpan <= rows - it.rowIndex &&
            it.colSpan > 0 && it.colSpan <= cols - it.colIndex) { "병합 영역이 표 범위를 벗어납니다." }
    }
    val occupied = BooleanArray(rows * cols)
    val rects = resolveTableRootCells(template.cells).map { cell ->
        for (r in cell.rowIndex until cell.rowIndex + cell.rowSpan) {
            for (c in cell.colIndex until cell.colIndex + cell.colSpan) {
                val index = r * cols + c
                require(!occupied[index]) { "병합 영역이 서로 겹칩니다." }
                occupied[index] = true
            }
        }
        IosCaptureCellRect(cell.rowIndex, cell.colIndex, cell.rowSpan, cell.colSpan)
    }
    require(occupied.all { it }) { "표 셀 좌표가 누락됐습니다." }
    return rects
}
