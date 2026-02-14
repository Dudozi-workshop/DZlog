package com.example.dzlog.domain.counter

import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.table.ResolvedCell
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterManagerScopePrefixTest {

    @Test
    fun `computeCounterPrefix ignores date and time cells`() {
        val text = resolvedCell(
            col = 0,
            type = TableCellDataType.TEXT,
            text = "SITE-A"
        )
        val date = resolvedCell(
            col = 1,
            type = TableCellDataType.DATE,
            text = "2026-02-13"
        )
        val time = resolvedCell(
            col = 2,
            type = TableCellDataType.TIME,
            text = "12:34:56"
        )

        val prefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text, date, time),
            fnDelim = "_"
        )

        assertEquals("SITE-A", prefix)
    }

    @Test
    fun `computeCounterPrefix uses delimiter from settings`() {
        val a = resolvedCell(0, TableCellDataType.TEXT, "A")
        val b = resolvedCell(1, TableCellDataType.TEXT, "B")

        val prefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(a, b),
            fnDelim = "-"
        )

        assertEquals("A-B", prefix)
    }

    private fun resolvedCell(
        col: Int,
        type: TableCellDataType,
        text: String
    ): ResolvedCell {
        val row = 0
        val raw = TableCellState(
            rowIndex = row,
            colIndex = col,
            dataType = type,
            fileNameInclude = true,
            groupLevel = GroupLevel.NONE,
            rawText = text
        )

        return ResolvedCell(
            id = "$row-$col",
            type = type,
            raw = raw,
            resolvedText = text,
            isEmpty = false
        )
    }
}
