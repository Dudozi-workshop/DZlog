package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickAutoCellEditPolicyTest {
    private val template = TableTemplateState(
        rows = 1,
        cols = 4,
        cells = listOf(
            TableCellState(0, 0, cellId = "date", dataType = TableCellDataType.DATE),
            TableCellState(0, 1, cellId = "phrase", dataType = TableCellDataType.ROTATING_TEXT,
                phraseSetId = "old", phraseIndex = 2, phraseCompletedShots = 1, everyOverride = 3),
            TableCellState(0, 2, cellId = "counter", dataType = TableCellDataType.COUNTER),
            TableCellState(0, 3, cellId = "locked", quickEditable = false),
        ),
        phraseSets = listOf(
            RotatingPhraseSet("old", "기존", listOf("A")),
            RotatingPhraseSet("new", "신규", listOf("B")),
        ),
    )

    @Test
    fun shared_quick_policy_excludes_counter_and_locked_cells() {
        assertEquals(listOf("date", "phrase"), quickSelectableCells(template).map { it.cellId })
    }

    @Test
    fun date_and_phrase_updates_preserve_their_android_rules() {
        val dated = updateQuickDateFormat(template, "date", "yyMMdd")
        assertEquals("yyMMdd", dated.cells.first { it.cellId == "date" }.formatPattern)
        assertEquals(template, updateQuickDateFormat(template, "date", "yyyy-MM-dd"))

        val changed = selectQuickPhraseSet(template, "phrase", "new")
            .cells.first { it.cellId == "phrase" }
        assertEquals("new", changed.phraseSetId)
        assertEquals(null, changed.everyOverride)
        assertEquals(0, changed.phraseIndex)
        assertEquals(0, changed.phraseCompletedShots)
        assertEquals(template, selectQuickPhraseSet(template, "phrase", "missing"))
    }
}
