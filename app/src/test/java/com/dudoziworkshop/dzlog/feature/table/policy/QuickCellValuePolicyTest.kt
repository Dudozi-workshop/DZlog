package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.ui.common.input.decimalInputOrPrevious
import org.junit.Assert.*
import org.junit.Test

class QuickCellValuePolicyTest {
    @Test fun signedDecimalsPreserveEditingStatesAndPrecision() {
        listOf("", "-", ".", "-.", "0.", "-2.50", "4.00", "000.08").forEach {
            assertEquals(it, decimalInputOrPrevious(it, "4.39"))
        }
        listOf("--2", "2-", "1.2.3", "abc", "1e2").forEach {
            assertEquals("4.39", decimalInputOrPrevious(it, "4.39"))
        }
        assertEquals("-2.5", decimalInputOrPrevious("−2.5", ""))
    }

    @Test fun liveChangesPreserveOtherCellsAndTemplateRules() {
        val text = TableCellState(0, 0, rawText = "old", cellId = "text")
        val number = TableCellState(0, 1, dataType = TableCellDataType.NUMBER, cellId = "number")
        val counter = TableCellState(0, 2, dataType = TableCellDataType.COUNTER, typedValue = CellValue.CounterSeed(5), cellId = "counter")
        val template = TableTemplateState(1, 3, listOf(text, number, counter), colWeights = listOf(1f, 2f, 1f), fileNameSlotDrafts = listOf(TableEditorSlotDraft("CELL", "셀", "text")))
        val changed = updateQuickCellValues(template, mapOf("text" to "line1\nline2", "number" to "-4.00", "counter" to "999"))
        assertEquals(CellValue.Text("line1\nline2"), changed.cells[0].typedValue)
        assertEquals(CellValue.Number("-4.00"), changed.cells[1].typedValue)
        assertEquals(counter, changed.cells[2])
        assertEquals(template.colWeights, changed.colWeights)
        assertEquals(template.fileNameSlotDrafts, changed.fileNameSlotDrafts)
        assertEquals("old", template.cells[0].rawText)
        val editedNumber = updateQuickCellValues(changed, mapOf("number" to "-")).cells[1]
        assertEquals("-", editedNumber.numberDraft)
        assertEquals("", editedNumber.rawText)
    }

    @Test fun legacyLockedTextAndNumberRemainEditable() {
        val heading = TableCellState(0, 0, cellId = "heading", rawText = "title", quickEditable = false)
        val number = TableCellState(0, 1, cellId = "number", dataType = TableCellDataType.NUMBER,
            rawText = "7", quickEditable = false)
        val counter = TableCellState(0, 2, cellId = "counter", dataType = TableCellDataType.COUNTER,
            typedValue = CellValue.CounterSeed(8), quickEditable = false)
        val template = TableTemplateState(1, 3, listOf(heading, number, counter))
        assertEquals(listOf("heading", "number"), quickEditableCells(template).map { it.cellId })
        val changed = updateQuickCellValues(template, mapOf("heading" to "new title", "number" to "8.00", "counter" to "999"))
        assertEquals(CellValue.Text("new title"), changed.cells[0].typedValue)
        assertEquals(CellValue.Number("8.00"), changed.cells[1].typedValue)
        assertEquals(counter, changed.cells[2])
    }

    @Test fun navigationUsesRootCellsAndWrapsInBothDirections() {
        val text = TableCellState(0, 0, cellId = "text", colSpan = 2)
        val covered = TableCellState(0, 1, cellId = "covered")
        val date = TableCellState(0, 2, cellId = "date", dataType = TableCellDataType.DATE)
        val number = TableCellState(1, 0, cellId = "number", dataType = TableCellDataType.NUMBER)
        val counter = TableCellState(1, 1, cellId = "counter", dataType = TableCellDataType.COUNTER)
        val time = TableCellState(1, 2, cellId = "time", dataType = TableCellDataType.TIME)
        val template = TableTemplateState(2, 3, listOf(number, covered, time, counter, date, text))
        val cells = quickNavigableCells(template)
        assertEquals(listOf("text", "date", "number", "counter", "time"), cells.map { it.cellId })
        assertEquals("date", adjacentQuickCellId(cells, "text", 1))
        assertEquals("text", adjacentQuickCellId(cells, "date", -1))
        assertEquals("text", adjacentQuickCellId(cells, "time", 1))
        assertEquals("time", adjacentQuickCellId(cells, "text", -1))
        assertEquals("counter", adjacentQuickCellId(cells, "number", 1))
        assertEquals("time", adjacentQuickCellId(cells, "counter", 1))
        assertEquals("number", adjacentQuickCellId(cells, "counter", -1))
        assertNull(adjacentQuickCellId(cells, "covered", 1))
        assertNull(adjacentQuickCellId(emptyList(), null, 1))
        assertNull(adjacentQuickCellId(cells, "text", 0))
        assertNull(adjacentQuickCellId(listOf(text), "text", 1))
    }

    @Test fun mergedCoveredCellsAreNotListedAndPositionShowsRange() {
        val root = TableCellState(0, 0, cellId = "root", colSpan = 2, kind = TableCellKind.INPUT)
        val covered = TableCellState(0, 1, cellId = "covered", kind = TableCellKind.INPUT)
        val base = TableCellState(1, 0, cellId = "base")
        val template = TableTemplateState(2, 2, listOf(base, covered, root))
        assertEquals(listOf(root, base), quickEditableCells(template))
        assertEquals("1행 1~2열", quickCellPosition(root))
        assertEquals(listOf(base), quickEditableCells(template.copy(cells = listOf(base))))
    }
}
