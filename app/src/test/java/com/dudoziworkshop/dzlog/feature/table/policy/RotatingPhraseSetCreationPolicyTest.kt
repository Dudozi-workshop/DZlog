package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgressPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RotatingPhraseSetCreationPolicyTest {
    @Test
    fun `new set is immediately selected and starts from first phrase`() {
        val cell = TableCellState(
            rowIndex = 0, colIndex = 0, cellId = "cell",
            dataType = TableCellDataType.ROTATING_TEXT, phraseSetId = "old",
            everyOverride = 12, phraseIndex = 2, phraseCompletedShots = 7,
        )
        val template = TableTemplateState(
            rows = 1, cols = 1, cells = listOf(cell),
            phraseSets = listOf(RotatingPhraseSet("old", "Old", listOf("A", "B", "C"), 12)),
        )
        val result = requireNotNull(
            RotatingPhraseSetCreationPolicy.createAndSelect(template, "cell", " New ", "new")
        )

        assertEquals("new", result.selectedSetId)
        assertEquals("New", result.template.phraseSets.last().name)
        assertEquals(1, result.template.phraseSets.last().defaultEvery)
        val selected = result.template.cells.single()
        assertEquals("new", selected.phraseSetId)
        assertNull(selected.everyOverride)
        assertEquals(0, selected.phraseIndex)
        assertEquals(0, selected.phraseCompletedShots)
        assertEquals("old", template.cells.single().phraseSetId)
    }

    @Test
    fun `user-configured repeat count remains configurable for newly selected set`() {
        val cell = TableCellState(rowIndex = 0, colIndex = 0, cellId = "cell",
            dataType = TableCellDataType.ROTATING_TEXT)
        val template = TableTemplateState(rows = 1, cols = 1, cells = listOf(cell))
        val result = requireNotNull(
            RotatingPhraseSetCreationPolicy.createAndSelect(template, "cell", "Sample", "new")
        )
        val set = result.template.phraseSets.single().copy(items = listOf("A", "B"))
        val configured = result.template.cells.single().copy(everyOverride = 5)
        var current = configured
        repeat(4) {
            val progress = CellPhraseProgressPolicy.afterSuccessfulCapture(current, set)
            current = current.copy(phraseIndex = progress.index, phraseCompletedShots = progress.completedShots)
        }
        assertEquals(0, current.phraseIndex)
        val afterFifth = CellPhraseProgressPolicy.afterSuccessfulCapture(current, set)
        assertEquals(1, afterFifth.index)
        assertEquals(0, afterFifth.completedShots)
    }

    @Test
    fun `invalid target and duplicate id never alter template`() {
        val cell = TableCellState(rowIndex = 0, colIndex = 0, cellId = "text",
            dataType = TableCellDataType.TEXT)
        val template = TableTemplateState(rows = 1, cols = 1, cells = listOf(cell),
            phraseSets = listOf(RotatingPhraseSet("exists", "Old", listOf("A"))))
        assertNull(RotatingPhraseSetCreationPolicy.createAndSelect(template, "text", "New", "id"))
        assertNull(RotatingPhraseSetCreationPolicy.createAndSelect(template, "missing", "New", "id"))
        assertNull(RotatingPhraseSetCreationPolicy.createAndSelect(template, "text", " ", "id"))
        assertEquals("exists", template.phraseSets.single().id)
    }
}
