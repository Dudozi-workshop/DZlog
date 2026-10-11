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
        assertEquals("-", updateQuickCellValues(changed, mapOf("number" to "-")).cells[1].rawText)
    }

    @Test fun mergedCoveredCellsAreNotListedAndPositionShowsRange() {
        val root = TableCellState(0, 0, cellId = "root", colSpan = 2, kind = TableCellKind.INPUT)
        val covered = TableCellState(0, 1, cellId = "covered", kind = TableCellKind.INPUT)
        val base = TableCellState(1, 0, cellId = "base")
        val template = TableTemplateState(2, 2, listOf(base, covered, root))
        assertEquals(listOf(root), quickEditableCells(template))
        assertEquals("1행 1~2열", quickCellPosition(root))
        assertEquals(listOf(base), quickEditableCells(template.copy(cells = listOf(base))))
    }
}
