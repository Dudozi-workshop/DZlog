package com.dudoziworkshop.dzlog.domain.preview

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TimeFormatOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewTickTest {

    @Test
    fun `template tick uses SECOND when any time cell includes seconds`() {
        val cells = listOf(
            TableCellState(rowIndex = 0, colIndex = 0, dataType = TableCellDataType.TIME, timeFormatOptions = TimeFormatOptions(includeSeconds = true))
        )

        val result = decideTickUnitFromTemplate(cells)
        assertEquals(TickUnit.SECOND, result)
    }

    @Test
    fun `template tick uses MINUTE when time cell exists without seconds`() {
        val cells = listOf(
            TableCellState(rowIndex = 0, colIndex = 0, dataType = TableCellDataType.TIME, timeFormatOptions = TimeFormatOptions(includeSeconds = false))
        )

        val result = decideTickUnitFromTemplate(cells)
        assertEquals(TickUnit.MINUTE, result)
    }

    @Test
    fun `template tick uses DAY when only date cell exists`() {
        val cells = listOf(
            TableCellState(rowIndex = 0, colIndex = 0, dataType = TableCellDataType.DATE)
        )

        val result = decideTickUnitFromTemplate(cells)
        assertEquals(TickUnit.DAY, result)
    }

    @Test
    fun `template tick defaults to MINUTE when no date or time cells`() {
        val cells = listOf(
            TableCellState(rowIndex = 0, colIndex = 0, dataType = TableCellDataType.TEXT)
        )

        val result = decideTickUnitFromTemplate(cells)
        assertEquals(TickUnit.MINUTE, result)
    }

    @Test
    fun `compute next delay uses next boundary`() {
        val result = computeNextDelayMillis(unit = TickUnit.MINUTE, nowMillis = 65_000L)
        assertEquals(55_000L, result)
    }
}
