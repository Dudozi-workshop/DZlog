package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
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
            fnDelim = "_",
            fileNameSlots = listOf(text.id, date.id, time.id),
            includeFilenameInScope = true,
        )

        assertEquals("SITE-A", prefix)
    }

    @Test
    fun `computeCounterPrefix uses delimiter from settings`() {
        val a = resolvedCell(0, TableCellDataType.TEXT, "A")
        val b = resolvedCell(1, TableCellDataType.TEXT, "B")

        val prefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(a, b),
            fnDelim = "-",
            fileNameSlots = listOf(a.id, b.id, null),
            includeFilenameInScope = true,
        )

        assertEquals("A-B", prefix)
    }

    @Test
    fun `computeCounterPrefix includes date and time only when scope options are enabled`() {
        val text = resolvedCell(0, TableCellDataType.TEXT, "N600")
        val date = resolvedCell(1, TableCellDataType.DATE, "2026-03-01")
        val time = resolvedCell(2, TableCellDataType.TIME, "12:34")

        val defaultPrefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text, date, time),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, date.id, time.id),
            includeFilenameInScope = true,
        )

        val dateOnlyPrefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text, date, time),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, date.id, time.id),
            includeFilenameInScope = true,
            scopeOptions = CounterScopeOptions(includeDateInCounterScope = true, includeTimeInCounterScope = false),
        )

        val allEnabledPrefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text, date, time),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, date.id, time.id),
            includeFilenameInScope = true,
            scopeOptions = CounterScopeOptions(includeDateInCounterScope = true, includeTimeInCounterScope = true),
        )

        assertEquals("N600", defaultPrefix)
        assertEquals("N600_2026-03-01", dateOnlyPrefix)
        assertEquals("N600_2026-03-01_1234", allEnabledPrefix)
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
