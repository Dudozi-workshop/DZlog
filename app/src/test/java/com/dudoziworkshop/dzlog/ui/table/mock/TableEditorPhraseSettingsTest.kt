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
    @Test fun csv_replacement_updates_shared_set_and_resets_related_progress_only() {
        val companion = cell.copy(cellId = "second", colIndex = 1, phraseIndex = 1, phraseCompletedShots = 2)
        val unrelatedSet = RotatingPhraseSet("other", "다른 세트", listOf("X", "Y"))
        val unrelated = cell.copy(cellId = "unrelated", colIndex = 2, phraseSetId = "other")
        val counter = TableCellState(0, 3, cellId = "counter", dataType = TableCellDataType.COUNTER,
            typedValue = CellValue.CounterSeed(11))
        val shared = TableTemplateState(1, 4, listOf(cell, companion, unrelated, counter),
            phraseSets = listOf(set, unrelatedSet))
        val csv = com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseCsvCodec.decode(
            "order,phrase\n2,최종 조사\n1,초기 조사\n"
        ) as com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseCsvCodec.ImportResult.Success
        val updated = updateMockPhraseSet(shared, set.id) { it.copy(items = csv.items) }
        assertEquals(listOf("초기 조사", "최종 조사"), updated.phraseSets[0].items)
        assertEquals(set.counterProgressMode, updated.phraseSets[0].counterProgressMode)
        assertEquals(set.defaultEvery, updated.phraseSets[0].defaultEvery)
        assertEquals(unrelatedSet, updated.phraseSets[1])
        assertEquals(0, updated.cells[0].phraseIndex)
        assertEquals(0, updated.cells[0].phraseCompletedShots)
        assertEquals(0, updated.cells[1].phraseIndex)
        assertEquals(0, updated.cells[1].phraseCompletedShots)
        assertEquals(unrelated, updated.cells[2])
        assertEquals(counter, updated.cells[3])
    }

}
