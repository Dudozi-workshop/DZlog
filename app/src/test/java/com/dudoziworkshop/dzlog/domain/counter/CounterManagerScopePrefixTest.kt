package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TimeFormatOptions
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    fun `computeCounterPrefix includes date and time only when scope values are provided`() {
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
            scopeOptions = CounterScopeOptions(dateScopeValues = listOf("2026-03-01")),
        )

        val allEnabledPrefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text, date, time),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, date.id, time.id),
            includeFilenameInScope = true,
            scopeOptions = CounterScopeOptions(dateScopeValues = listOf("2026-03-01"), timeScopeValues = listOf("12:34")),
        )

        assertEquals("N600", defaultPrefix)
        assertEquals("N600_d_2026-03-01", dateOnlyPrefix)
        assertEquals("N600_d_2026-03-01_t_12_34", allEnabledPrefix)
    }


    @Test
    fun `time scope token is HHmm when seconds option is disabled`() {
        val now = fixedDate("2026-03-01 12:34:56")
        val resolver = TableResolver()
        val timeCell = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            dataType = TableCellDataType.TIME,
            typedValue = CellValue.Auto,
            timeFormatOptions = TimeFormatOptions(includeSeconds = false),
        )

        val plan = resolver.plan(
            cells = listOf(timeCell),
            captureNow = now,
            config = TableResolver.Config(
                counterDigits = 0,
                dateFormat = "yyyy-MM-dd",
                timeFormat = "HH:mm",
                locale = Locale.US,
            )
        )

        val resolvedTime = plan.resolvedCells.single()
        assertEquals("1234", resolvedTime.scopeToken)
    }

    @Test
    fun `time scope token is HHmmss when seconds option is enabled`() {
        val now = fixedDate("2026-03-01 12:34:56")
        val resolver = TableResolver()
        val timeCell = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            dataType = TableCellDataType.TIME,
            typedValue = CellValue.Auto,
            timeFormatOptions = TimeFormatOptions(includeSeconds = true),
        )

        val plan = resolver.plan(
            cells = listOf(timeCell),
            captureNow = now,
            config = TableResolver.Config(
                counterDigits = 0,
                dateFormat = "yyyy-MM-dd",
                timeFormat = "HH:mm:ss",
                locale = Locale.US,
            )
        )

        val resolvedTime = plan.resolvedCells.single()
        assertEquals("123456", resolvedTime.scopeToken)
    }

    @Test
    fun `computeCounterPrefix excludes time when time scope values are not provided even with scope token`() {
        val text = resolvedCell(0, TableCellDataType.TEXT, "N600")
        val time = resolvedCell(1, TableCellDataType.TIME, "12:34", scopeToken = "1234")

        val prefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text, time),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, time.id),
            includeFilenameInScope = true,
            scopeOptions = CounterScopeOptions(dateScopeValues = listOf("2026-03-01")),
        )

        assertEquals("N600", prefix)
    }


    @Test
    fun `computeCounterPrefix includes time when time scope values are provided`() {
        val text = resolvedCell(0, TableCellDataType.TEXT, "N600")
        val time = resolvedCell(1, TableCellDataType.TIME, "12:34", scopeToken = "1234")

        val prefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text, time),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, time.id),
            includeFilenameInScope = true,
            scopeOptions = CounterScopeOptions(timeScopeValues = listOf("12:34")),
        )

        assertEquals("N600_t_12_34", prefix)
    }

    @Test
    fun `date scope token is yyyyMMdd`() {
        val now = fixedDate("2026-03-01 12:34:56")
        val resolver = TableResolver()
        val dateCell = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            dataType = TableCellDataType.DATE,
            typedValue = CellValue.Auto,
            formatPattern = "yyyy.MM.dd",
        )

        val plan = resolver.plan(
            cells = listOf(dateCell),
            captureNow = now,
            config = TableResolver.Config(
                counterDigits = 0,
                dateFormat = "yyyy-MM-dd",
                timeFormat = "HH:mm",
                locale = Locale.US,
            )
        )

        val resolvedDate = plan.resolvedCells.single()
        assertEquals("20260301", resolvedDate.scopeToken)
    }




    @Test
    fun `computeCounterPrefix includes phrase scope values in prefix`() {
        val text = resolvedCell(0, TableCellDataType.TEXT, "N600")

        val prefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, null, null),
            includeFilenameInScope = true,
            scopeOptions = CounterScopeOptions(phraseScopeValues = listOf("p_setA_1")),
        )

        assertEquals("N600_p_setA_1", prefix)
    }


    @Test
    fun `computeCounterPrefix keeps phrase scope separation when filename scope is off`() {
        val text = resolvedCell(0, TableCellDataType.TEXT, "N600")

        val prefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, null, null),
            includeFilenameInScope = false,
            scopeOptions = CounterScopeOptions(phraseScopeValues = listOf("p_setA_1")),
        )

        assertEquals("name=off_p_setA_1", prefix)
    }

    @Test
    fun `computeCounterPrefix normalizes phrase scope values without marker`() {
        val text = resolvedCell(0, TableCellDataType.TEXT, "N600")

        val prefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, null, null),
            includeFilenameInScope = true,
            scopeOptions = CounterScopeOptions(phraseScopeValues = listOf("set A/1")),
        )

        assertEquals("N600_p_set_A_1", prefix)
    }

    @Test
    fun `computeCounterStreamRelativePathKey separates g2 enabled but empty from g1 only`() {
        val base = "Pictures/DZlog/A/"

        val g1Only = CounterManager.computeCounterStreamRelativePathKey(
            baseRelativePath = base,
            hasG2Group = false,
            group2Value = "",
        )
        val g2EnabledEmpty = CounterManager.computeCounterStreamRelativePathKey(
            baseRelativePath = base,
            hasG2Group = true,
            group2Value = "",
        )

        assertEquals("Pictures/DZlog/A/", g1Only)
        assertEquals("Pictures/DZlog/A/|g2=enabled_empty", g2EnabledEmpty)
    }

    @Test
    fun `computeCounterStreamRelativePathKey keeps base path when g2 has value`() {
        val key = CounterManager.computeCounterStreamRelativePathKey(
            baseRelativePath = "Pictures/DZlog/A/B/",
            hasG2Group = true,
            group2Value = "B",
        )

        assertEquals("Pictures/DZlog/A/B/", key)
    }
    private fun resolvedCell(
        col: Int,
        type: TableCellDataType,
        text: String,
        scopeToken: String? = null,
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
            isEmpty = false,
            scopeToken = scopeToken,
        )
    }

    private fun fixedDate(value: String): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.parse(value) ?: error("invalid date: $value")
    }
}
