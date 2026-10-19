package com.dudoziworkshop.dzlog.domain.model

import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgressPolicy
import com.dudoziworkshop.dzlog.domain.phrase.PhraseResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SharedModelCompatibilityTest {
    @Test
    fun newCellIdsKeepTheExistingLowercaseUuidShapeAndStoredIdsArePreserved() {
        val first = TableCellState(0, 0)
        val second = TableCellState(0, 1)
        val uuid = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
        assertTrue(uuid.matches(first.cellId))
        assertTrue(uuid.matches(second.cellId))
        assertNotEquals(first.cellId, second.cellId)
        assertEquals(first.cellId, first.copy(rawText = "Draper").cellId)
        assertEquals("existing-id", TableCellState(0, 0, cellId = "existing-id").cellId)
    }

    @Test
    fun slotDefaultsAndCellLinksKeepExistingTemplateMeaning() {
        val drafts = listOf(TableEditorSlotDraft("CELL", "품종", cellId = "old-cell"))
        val template = TableTemplateState(1, 1, listOf(TableCellState(0, 0, cellId = "old-cell")))
        assertEquals(5, template.fileNameSlotDrafts.size)
        assertEquals(5, template.pathSlotDrafts.size)
        assertEquals(listOf("old-cell", null, null, null, null), deriveFileNameCellSlotsFromDrafts(drafts))
        assertEquals(listOf("old-cell", null, null, null, null), derivePathCellSlotsFromDrafts(drafts))
        assertFailsWith<IllegalArgumentException> { template.copy(pathSlotDrafts = List(6) { null }) }
    }

    @Test
    fun twelveShotPhraseProgressWrapsAndPlanningDoesNotMutateTheCell() {
        val set = RotatingPhraseSet("set", "처리구", listOf("T1", "T2", "T3", "T4"), 12)
        var cell = TableCellState(0, 0, cellId = "phrase", dataType = TableCellDataType.ROTATING_TEXT,
            phraseSetId = set.id)
        repeat(48) { shot ->
            assertEquals(set.items[shot / 12], PhraseResolver.resolveSelectedTextByCellId(
                listOf(cell), listOf(set), 999)[cell.cellId])
            val next = CellPhraseProgressPolicy.afterSuccessfulCapture(cell, set)
            // Computing another preview or cancelling does not commit progress.
            assertEquals(next, CellPhraseProgressPolicy.afterSuccessfulCapture(cell, set))
            assertEquals(shot % 12, cell.phraseCompletedShots)
            cell = cell.copy(phraseIndex = next.index, phraseCompletedShots = next.completedShots)
        }
        assertEquals(0, cell.phraseIndex)
        assertEquals(0, cell.phraseCompletedShots)
    }
}
