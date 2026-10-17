package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.phrase.PhraseResolver
import com.dudoziworkshop.dzlog.feature.table.policy.selectQuickPhraseSet
import com.dudoziworkshop.dzlog.feature.table.policy.updateQuickDateFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Regression for changing linked values after a one-time rule-composition copy. */
class Dzq009015CombinedRegressionTest {
    @Test
    fun copiedDateCellStaysLinkedWhenQuickEditChangesTheFormat() {
        val date = TableCellState(0, 0, cellId = "date", dataType = TableCellDataType.DATE)
        val original = TableTemplateState(
            rows = 1,
            cols = 1,
            cells = listOf(date),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "date"),
                null, null, null, null
            )
        )
        val draft = mockSaveRulesDraftFromTemplate(original, true, false)
        val allowedCells = mockCellsFromTemplate(original)
            .mapNotNull { linkedRuleItemFromCell(it)?.cellId }.toSet()
        val copied = copySaveRuleComposition(
            draft, SaveRuleCopyDirection.FILE_NAME_TO_PATH, allowedCells
        )
        val saved = applyMockSaveRulesDraft(original, copied)
        val modified = updateQuickDateFormat(saved, "date", "yyMMdd")

        assertEquals("date", modified.fileNameSlotDrafts.first()?.cellId)
        assertEquals("date", modified.pathSlotDrafts.first()?.cellId)
        assertEquals("yyMMdd", modified.cells.single().formatPattern)
        assertEquals(5, modified.pathSlotDrafts.size)
        assertEquals("", original.cells.single().formatPattern)
    }

    @Test
    fun changingPhraseSetRetainsBothRuleReferencesAndOtherCellValues() {
        val phrase = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            cellId = "phrase",
            dataType = TableCellDataType.ROTATING_TEXT,
            phraseSetId = "old",
            phraseIndex = 1,
            phraseCompletedShots = 2
        )
        val other = TableCellState(0, 1, cellId = "other", rawText = "Original")
        val original = TableTemplateState(
            rows = 1, cols = 2,
            cells = listOf(phrase, other),
            phraseSets = listOf(
                RotatingPhraseSet("old", "Old", listOf("A", "B")),
                RotatingPhraseSet("new", "New", listOf("Draper", "Duke"))
            ),
            pathSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "phrase"),
                null, null, null, null
            )
        )
        val draft = mockSaveRulesDraftFromTemplate(original, false, false)
        val allowedCells = mockCellsFromTemplate(original)
            .mapNotNull { linkedRuleItemFromCell(it)?.cellId }.toSet()
        val copied = copySaveRuleComposition(
            draft, SaveRuleCopyDirection.PATH_TO_FILE_NAME, allowedCells
        )
        val saved = applyMockSaveRulesDraft(original, copied)
        val modified = selectQuickPhraseSet(saved, "phrase", "new")
        val selected = modified.cells.first { it.cellId == "phrase" }
        val resolved = PhraseResolver.resolveSelectedTextByCellId(
            modified.cells, modified.phraseSets, 1
        )

        assertEquals("phrase", modified.fileNameSlotDrafts.first()?.cellId)
        assertEquals("phrase", modified.pathSlotDrafts.first()?.cellId)
        assertEquals("new", selected.phraseSetId)
        assertEquals(0, selected.phraseIndex)
        assertEquals(0, selected.phraseCompletedShots)
        assertEquals(other, modified.cells.first { it.cellId == "other" })
        assertEquals("Draper", resolved["phrase"])
        assertSame(original, selectQuickPhraseSet(original, "phrase", "old"))
    }
}
