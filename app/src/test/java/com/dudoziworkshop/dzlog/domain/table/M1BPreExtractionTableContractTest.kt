package com.dudoziworkshop.dzlog.domain.table

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** M1-B B1: freeze capture-time formatting and manual counter preview contracts. */
class M1BPreExtractionTableContractTest {
    private fun atFixedInstant() = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
        clear()
        set(2026, Calendar.OCTOBER, 19, 13, 14, 59)
    }.time

    @Test
    fun `capture time ignores stale date and time raw text`() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val plan = TableResolver().plan(
                cells = listOf(
                    TableCellState(rowIndex = 0, colIndex = 0, cellId = "d", dataType = TableCellDataType.DATE, rawText = "OLD"),
                    TableCellState(rowIndex = 0, colIndex = 1, cellId = "t", dataType = TableCellDataType.TIME, rawText = "OLD"),
                ),
                captureNow = atFixedInstant(),
                config = TableResolver.Config(counterDigits = 4, dateFormat = "yyyyMMdd", timeFormat = "HHmm", locale = Locale.US),
            )
            assertEquals("20261019", plan.resolvedCells.first { it.id == "d" }.resolvedText)
            assertEquals("1314", plan.resolvedCells.first { it.id == "t" }.resolvedText)
            assertEquals("20261019", plan.resolvedCells.first { it.id == "d" }.scopeToken)
            assertEquals("1314", plan.resolvedCells.first { it.id == "t" }.scopeToken)
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test
    fun `manual counter override controls padded preview rather than stale cell text`() {
        val counter = TableCellState(
            rowIndex = 0, colIndex = 0, cellId = "n", dataType = TableCellDataType.COUNTER, rawText = "9999",
        )
        val plan = TableResolver().plan(
            cells = listOf(counter),
            captureNow = atFixedInstant(),
            config = TableResolver.Config(counterDigits = 4, dateFormat = "yyyyMMdd", timeFormat = "HHmm", locale = Locale.US),
            counterSeedOverride = 12,
        )
        assertEquals("0012", plan.resolvedCells.single().resolvedText)
        assertEquals("13", plan.patch.cellUpdates["n"])
    }
}
