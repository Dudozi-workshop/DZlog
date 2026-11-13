package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import org.junit.Assert.*
import org.junit.Test

class QuickPhrasePeriodEditPolicyTest {
    private val set = RotatingPhraseSet("s", "Test", listOf("A", "B"), defaultEvery = 5,
        itemEveryOverrides = listOf(null, 2))
    private val c1 = TableCellState(0, 0, cellId = "c1", dataType = TableCellDataType.ROTATING_TEXT,
        phraseSetId = "s", phraseIndex = 0, phraseCompletedShots = 3)
    private val c2 = c1.copy(colIndex = 1, cellId = "c2", phraseCompletedShots = 1)
    private val c3 = c1.copy(colIndex = 2, cellId = "c3", phraseIndex = 1, phraseCompletedShots = 1)
    private val template = TableTemplateState(1, 3, cells = listOf(c1, c2, c3), phraseSets = listOf(set))

    @Test fun `raising default leaves all completed shots untouched`() {
        val result = applyQuickPhrasePeriodEdit(template, QuickPhrasePeriodEdit("c1", true, 7))!!
        assertEquals(7, result.phraseSets.first().defaultEvery)
        assertEquals(listOf(3, 1, 1), result.cells.map { it.phraseCompletedShots })
        assertEquals(2, result.phraseSets.first().everyForItem(1))
    }

    @Test fun `reducing default requires manual resolution for each newly-invalid cell`() {
        val edit = QuickPhrasePeriodEdit("c1", true, 1)
        val candidate = previewQuickPhrasePeriodEdit(template, edit)!!
        assertEquals(setOf("c1", "c2"), quickPhrasePeriodConflicts(template, candidate).map { it.cellId }.toSet())
        assertNull(applyQuickPhrasePeriodEdit(template, edit))
        val result = applyQuickPhrasePeriodEdit(template, edit, mapOf(
            "c1" to CellPhraseProgress(1, 1),
            "c2" to CellPhraseProgress(0, 0),
        ))!!
        assertEquals(1, result.phraseSets.first().defaultEvery)
        assertEquals(1, result.cells.first().phraseIndex)
        assertEquals(1, result.cells.first().phraseCompletedShots)
        assertEquals(0, result.cells[1].phraseCompletedShots)
        assertEquals(1, result.cells[2].phraseCompletedShots)
        assertNull(applyQuickPhrasePeriodEdit(template, edit, mapOf(
            "c1" to CellPhraseProgress(0, 1), "c2" to CellPhraseProgress(0, 0))))
    }

    @Test fun `switching cell scope preserves progress when valid and does not affect peers`() {
        val result = applyQuickPhrasePeriodEdit(template, QuickPhrasePeriodEdit("c1", false, 4))!!
        assertEquals(4, result.cells.first().everyOverride)
        assertEquals(3, result.cells.first().phraseCompletedShots)
        assertEquals(c2, result.cells[1])
        assertEquals(set, result.phraseSets.first())
    }

    @Test fun `switching to inherited set requires resolution if previously larger local period`() {
        val original = template.copy(cells = listOf(c1.copy(everyOverride = 7, phraseCompletedShots = 6), c2, c3))
        val edit = QuickPhrasePeriodEdit("c1", true, 5)
        assertNull(applyQuickPhrasePeriodEdit(original, edit))
        assertNotNull(applyQuickPhrasePeriodEdit(original, edit,
            mapOf("c1" to CellPhraseProgress(0, 4))))
    }

    @Test fun `atomic progress editing honors per-phrase periods`() {
        val result = updateQuickPhraseProgress(template, "c1", 1, 1)
        assertEquals(1, result.cells.first().phraseIndex)
        assertEquals(1, result.cells.first().phraseCompletedShots)
        assertSame(template, updateQuickPhraseProgress(template, "c1", 1, 2))
    }
}
