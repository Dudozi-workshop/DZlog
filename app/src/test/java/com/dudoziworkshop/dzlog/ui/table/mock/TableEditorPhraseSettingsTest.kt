package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class TableEditorPhraseSettingsTest {
    private val set = RotatingPhraseSet("set", "처리구", listOf("T1", "T2", "T3", "T4"), 12)
    private val cell = TableCellState(0, 0, cellId = "cell", dataType = TableCellDataType.ROTATING_TEXT,
        phraseSetId = set.id, phraseIndex = 2, phraseCompletedShots = 5)
    private val template = TableTemplateState(1, 1, listOf(cell), phraseSets = listOf(set))

    @Test fun period_change_keeps_item_and_restarts_its_completed_count() {
        val next = applyMockPhraseEvery(template, cell.cellId, 6).cells.single()
        assertEquals(2, next.phraseIndex)
        assertEquals(0, next.phraseCompletedShots)
        assertEquals(6, next.everyOverride)
    }

    @Test fun selecting_same_set_and_renaming_preserve_progress() {
        assertEquals(template, applyMockPhraseSet(template, cell.cellId, set.id))
        val renamed = updateMockPhraseSet(template, set.id) { it.copy(name = "이름 변경") }
        assertEquals(cell, renamed.cells.single())
    }

    @Test fun different_set_and_changed_item_order_start_at_first_item() {
        val other = set.copy(id = "other")
        val changed = applyMockPhraseSet(template.copy(phraseSets = listOf(set, other)), cell.cellId, other.id)
        assertEquals(0, changed.cells.single().phraseIndex)
        assertEquals(0, changed.cells.single().phraseCompletedShots)
        val reordered = updateMockPhraseSet(template, set.id) { it.copy(items = it.items.reversed()) }
        assertEquals(0, reordered.cells.single().phraseIndex)
        assertEquals(0, reordered.cells.single().phraseCompletedShots)
        assertEquals(cell, template.cells.single())
    }
}
