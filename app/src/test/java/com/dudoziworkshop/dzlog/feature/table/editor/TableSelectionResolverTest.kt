package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellKind
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableSelectionResolverTest {

    private fun cells(rows: Int, cols: Int): List<TableCellState> =
        (0 until rows).flatMap { r ->
            (0 until cols).map { c ->
                TableCellState(
                    cellId = "r${r}c${c}",
                    rowIndex = r,
                    colIndex = c,
                    kind = TableCellKind.INPUT,
                    dataType = TableCellDataType.TEXT,
                    rawText = "",
                    typedValue = CellValue.Text(""),
                    groupLevel = GroupLevel.NONE,
                )
            }
        }

    @Test
    fun tap_additive_expands_to_continuous_rectangle() {
        val data = cells(3, 3)
        val first = TableSelectionResolver.selectByTap(data, TableSelectionResult(emptySet(), null, null), "r1c1", additive = false)
        val second = TableSelectionResolver.selectByTap(data, first, "r2c2", additive = true)

        assertEquals(setOf("r1c1", "r1c2", "r2c1", "r2c2"), second.selectedCellIds)
        assertTrue(TableSelectionResolver.isContinuousRectangle(data, second.selectedCellIds))
    }

    @Test
    fun drag_selects_rectangular_range() {
        val data = cells(4, 4)
        val result = TableSelectionResolver.selectByDrag(data, "r1c1", "r3c2")

        assertEquals(6, result.selectedCellIds.size)
        assertTrue(result.selectedCellIds.contains("r2c2"))
    }
}
