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

    @Test fun `editing selected phrase preserves completed shots and other phrase periods`() {
        val result = applyQuickPhrasePeriodEdit(template, QuickPhrasePeriodEdit("c1", false, 4))!!
        assertEquals(4, result.phraseSets.single().everyForItem(0))
        assertEquals(2, result.phraseSets.single().everyForItem(1))
        assertEquals(5, result.phraseSets.single().defaultEvery)
        assertEquals(listOf(3, 1, 1), result.cells.map { it.phraseCompletedShots })
        assertEquals(c2, result.cells[1])
    }

    @Test fun `individual camera quick period edits only selected phrase and never set default`() {
        val individual = applyQuickPhrasePeriodEdit(
            template, QuickPhrasePeriodEdit("c1", false, 6),
        )!!
        assertEquals(5, individual.phraseSets.single().defaultEvery)
        assertEquals(listOf(6, 2), individual.phraseSets.single().itemEveryOverrides)
        assertTrue(individual.cells.all { it.everyOverride == null })
        assertEquals(6, individual.phraseSets.single().everyForItem(0))
        assertEquals(2, individual.phraseSets.single().everyForItem(1))
        assertEquals(template.cells, individual.cells)
    }

    @Test fun `B individual change does not alter A C or overall default`() {
        val abc = RotatingPhraseSet("abc", "ABC", listOf("A", "B", "C"), defaultEvery = 5)
        val current = c1.copy(phraseSetId = "abc", phraseIndex = 1, phraseCompletedShots = 2)
        val other = c2.copy(phraseSetId = "abc", phraseIndex = 0)
        val working = TableTemplateState(1, 2, cells = listOf(current, other), phraseSets = listOf(abc))

        val edited = applyQuickPhrasePeriodEdit(working, QuickPhrasePeriodEdit("c1", false, 3))!!
        assertEquals(5, edited.phraseSets.single().defaultEvery)
        assertEquals(listOf(5, 3, 5), edited.phraseSets.single().items.indices.map {
            edited.phraseSets.single().everyForItem(it)
        })
        assertEquals(listOf(null, 3, null), edited.phraseSets.single().itemEveryOverrides)
        assertEquals(working.cells, edited.cells)

        val reset = applyQuickPhrasePeriodEdit(edited, QuickPhrasePeriodEdit("c1", true, 5))!!
        assertEquals(listOf(5, 5, 5), reset.phraseSets.single().items.indices.map {
            reset.phraseSets.single().everyForItem(it)
        })
        assertEquals(5, reset.phraseSets.single().defaultEvery)
        assertNull(reset.phraseSets.single().itemEveryOverrides[1])
    }

    @Test fun `individual edit keeps old cell wide periods for other phrases`() {
        val abc = RotatingPhraseSet("abc", "ABC", listOf("A", "B", "C"), defaultEvery = 5)
        val legacy = c1.copy(phraseSetId = "abc", phraseIndex = 1,
            everyOverride = 2, phraseCompletedShots = 1)
        val working = TableTemplateState(1, 1, cells = listOf(legacy), phraseSets = listOf(abc))
        val edited = applyQuickPhrasePeriodEdit(working, QuickPhrasePeriodEdit("c1", false, 3))!!
        assertEquals(2, edited.cells.single().everyOverride)
        val changedSet = edited.phraseSets.single()
        assertEquals(5, changedSet.defaultEvery)
        assertEquals(2, com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgressPolicy.everyFor(
            legacy, changedSet, 0))
        assertEquals(3, com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgressPolicy.everyFor(
            legacy, changedSet, 1))
        assertEquals(2, com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgressPolicy.everyFor(
            legacy, changedSet, 2))
    }

    @Test fun `shortening one phrase requires explicit resolution for other cells on that phrase`() {
        val first = c1.copy(phraseIndex = 1, phraseCompletedShots = 1)
        val second = c2.copy(phraseIndex = 1, phraseCompletedShots = 3)
        val working = template.copy(
            cells = listOf(first, second),
            phraseSets = listOf(set.copy(itemEveryOverrides = listOf(null, 5))),
        )
        val edit = QuickPhrasePeriodEdit("c1", false, 2)
        val proposal = previewQuickPhrasePeriodEdit(working, edit)!!
        assertEquals(listOf("c2"), quickPhrasePeriodConflicts(working, proposal).map { it.cellId })
        assertNull(applyQuickPhrasePeriodEdit(working, edit))
        val applied = applyQuickPhrasePeriodEdit(
            working, edit, mapOf("c2" to CellPhraseProgress(1, 1)),
        )!!
        assertEquals(5, applied.phraseSets.single().defaultEvery)
        assertEquals(2, applied.phraseSets.single().everyForItem(1))
        assertEquals(1, applied.cells[1].phraseCompletedShots)
    }

    @Test fun `one phrase interval is shared but each cell keeps independent capture progress`() {
        val abc = RotatingPhraseSet("abc", "ABC", listOf("A", "B", "C"), defaultEvery = 5)
        val first = c1.copy(phraseSetId = "abc", phraseIndex = 1, phraseCompletedShots = 1)
        val second = c2.copy(phraseSetId = "abc", phraseIndex = 1, phraseCompletedShots = 2)
        val working = TableTemplateState(1, 2, cells = listOf(first, second), phraseSets = listOf(abc))

        val edited = applyQuickPhrasePeriodEdit(working, QuickPhrasePeriodEdit("c1", false, 3))!!
        val editedSet = edited.phraseSets.single()
        val progressPolicy = com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgressPolicy

        assertEquals(5, editedSet.defaultEvery)
        assertEquals(listOf(5, 3, 5), editedSet.items.indices.map(editedSet::everyForItem))
        assertEquals(working.cells, edited.cells)
        assertEquals(3, progressPolicy.everyFor(edited.cells[0], editedSet, 1))
        assertEquals(3, progressPolicy.everyFor(edited.cells[1], editedSet, 1))
        assertEquals(CellPhraseProgress(1, 2), progressPolicy.afterSuccessfulCapture(edited.cells[0], editedSet))
        assertEquals(CellPhraseProgress(2, 0), progressPolicy.afterSuccessfulCapture(edited.cells[1], editedSet))
    }

    @Test fun `only set mode changes shared default while preserving per item override`() {
        val inherited = applyQuickPhrasePeriodEdit(
            template, QuickPhrasePeriodEdit("c1", true, 7),
        )!!
        assertEquals(7, inherited.phraseSets.single().defaultEvery)
        assertEquals(2, inherited.phraseSets.single().everyForItem(1))
        assertTrue(inherited.cells.all { it.everyOverride == null })
        assertEquals(5, template.phraseSets.single().defaultEvery)
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
