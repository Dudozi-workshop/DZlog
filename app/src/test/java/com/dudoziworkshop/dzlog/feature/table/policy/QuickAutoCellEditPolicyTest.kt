package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class QuickAutoCellEditPolicyTest {
    private val text = TableCellState(0, 0, cellId = "text")
    private val date = TableCellState(0, 1, cellId = "date", dataType = TableCellDataType.DATE)
    private val time = TableCellState(0, 2, cellId = "time", dataType = TableCellDataType.TIME)
    private val phrase = TableCellState(1, 0, cellId = "phrase", dataType = TableCellDataType.ROTATING_TEXT,
        phraseSetId = "old", phraseIndex = 2, phraseCompletedShots = 1)
    private val counter = TableCellState(1, 1, cellId = "counter", dataType = TableCellDataType.COUNTER)
    private val hidden = TableCellState(1, 2, cellId = "hidden", quickEditable = false)
    private val template = TableTemplateState(2, 3, listOf(phrase, counter, time, text, hidden, date),
        phraseSets = listOf(
            RotatingPhraseSet("old", "Existing", listOf("A", "B", "C")),
            RotatingPhraseSet("new", "New", listOf("X", "Y"))
        ))
    @Test fun `only eligible visible cells are selectable`() {
        assertEquals(listOf("text", "date", "time", "phrase"), quickSelectableCells(template).map { it.cellId })
    }
    @Test fun `date changes only supported format and leaves all other cells untouched`() {
        val changed = updateQuickDateFormat(template, "date", "yyMMdd")
        assertEquals("yyMMdd", changed.cells.first { it.cellId == "date" }.formatPattern)
        assertEquals(phrase, changed.cells.first { it.cellId == "phrase" })
        assertSame(template, updateQuickDateFormat(template, "date", "yyyy.MM.dd"))
        assertSame(template, updateQuickDateFormat(template, "time", "yyMMdd"))
    }
    @Test fun `switching phrase set resets only chosen phrase cell`() {
        val changed = selectQuickPhraseSet(template, "phrase", "new")
        val selected = changed.cells.first { it.cellId == "phrase" }
        assertEquals("new", selected.phraseSetId)
        assertEquals(0, selected.phraseIndex)
        assertEquals(0, selected.phraseCompletedShots)
        assertNull(selected.everyOverride)
        assertEquals(date, changed.cells.first { it.cellId == "date" })
        assertSame(template, selectQuickPhraseSet(template, "phrase", "old"))
        assertSame(template, selectQuickPhraseSet(template, "phrase", "missing"))
        assertSame(template, selectQuickPhraseSet(template, "date", "new"))
    }
}
