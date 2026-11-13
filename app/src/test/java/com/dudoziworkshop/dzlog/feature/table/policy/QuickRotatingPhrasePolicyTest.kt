package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgressPolicy
import com.dudoziworkshop.dzlog.domain.phrase.PhraseResolver
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuickRotatingPhrasePolicyTest {
    private val first = TableCellState(
        0, 0, cellId = "phrase-a", dataType = TableCellDataType.ROTATING_TEXT,
        phraseSetId = "first", everyOverride = 3, phraseIndex = 1,
        phraseCompletedShots = 1, quickEditable = false,
    )
    private val other = TableCellState(
        0, 1, cellId = "phrase-b", dataType = TableCellDataType.ROTATING_TEXT,
        phraseSetId = "first", phraseIndex = 2, phraseCompletedShots = 0,
    )
    private val counter = TableCellState(
        0, 2, cellId = "counter", dataType = TableCellDataType.COUNTER,
        typedValue = CellValue.CounterSeed(12),
    )
    private val template = TableTemplateState(
        1, 3, listOf(first, other, counter),
        phraseSets = listOf(
            RotatingPhraseSet("first", "생육 조사", listOf("초기", "중간", "최종"), defaultEvery = 2),
            RotatingPhraseSet("second", "처리구", listOf("대조구", "처리구"), defaultEvery = 4),
        ),
        fileNameSlotDrafts = List(FILE_NAME_SLOT_COUNT) {
            if (it == 0) TableEditorSlotDraft("CELL", "문구", "phrase-a") else null
        },
        pathSlotDrafts = List(PATH_SLOT_UI_MAX_COUNT) {
            if (it == 0) TableEditorSlotDraft("CELL", "폴더", "phrase-a") else null
        },
    )

    @Test fun `selecting phrase chooses next capture and resets only its completed count`() {
        val selected = selectQuickPhraseItem(template, "phrase-a", 2)
        assertEquals(2, selected.cells[0].phraseIndex)
        assertEquals(0, selected.cells[0].phraseCompletedShots)
        assertEquals(3, selected.cells[0].everyOverride)
        assertEquals("최종", PhraseResolver.resolveSelectedTextByCellId(
            selected.cells, selected.phraseSets, 1)["phrase-a"])
        assertEquals(other, selected.cells[1])
        assertEquals(counter, selected.cells[2])
        assertEquals(template.fileNameSlotDrafts, selected.fileNameSlotDrafts)
        assertEquals(template.pathSlotDrafts, selected.pathSlotDrafts)
        assertSame(template, selectQuickPhraseItem(template, "phrase-a", -1))
        assertSame(template, selectQuickPhraseItem(template, "phrase-a", 3))
        assertSame(template, selectQuickPhraseItem(template, "counter", 0))
    }

    @Test fun `reselecting active phrase deliberately clears completed count once`() {
        val selected = selectQuickPhraseItem(template, "phrase-a", 1)
        assertEquals(1, selected.cells[0].phraseIndex)
        assertEquals(0, selected.cells[0].phraseCompletedShots)
        assertSame(selected, selectQuickPhraseItem(selected, "phrase-a", 1))
    }

    @Test fun `changing cycle preserves valid completed count and per cell override`() {
        val changed = updateQuickPhraseEvery(template, "phrase-a", 4)
        assertEquals(1, changed.cells[0].phraseIndex)
        assertEquals(1, changed.cells[0].phraseCompletedShots)
        assertEquals(4, changed.cells[0].everyOverride)
        assertEquals(other, changed.cells[1])
        assertEquals(template.phraseSets, changed.phraseSets)
        assertEquals(1, CellPhraseProgressPolicy.current(changed.cells[0], changed.phraseSets[0]).completedShots)
        val fixedAtDefaultValue = updateQuickPhraseEvery(changed, "phrase-a", 2)
        assertEquals(2, fixedAtDefaultValue.cells[0].everyOverride)
        val defaulted = followQuickPhraseSetPeriods(fixedAtDefaultValue, "phrase-a")
        assertNull(defaulted.cells[0].everyOverride)
        assertEquals(1, defaulted.cells[0].phraseCompletedShots)
        assertEquals(other, defaulted.cells[1])
        assertSame(template, updateQuickPhraseEvery(template, "phrase-a", 3))
        assertSame(template, updateQuickPhraseEvery(template, "phrase-a", 0))
        assertSame(template, updateQuickPhraseEvery(template, "counter", 3))
    }

    @Test fun `editing completed shots stays within cycle and advances only after successful capture`() {
        val adjusted = updateQuickPhraseCompleted(template, "phrase-a", 2)
        assertEquals(1, adjusted.cells[0].phraseIndex)
        assertEquals(2, adjusted.cells[0].phraseCompletedShots)
        assertEquals(other, adjusted.cells[1])
        assertEquals(counter, adjusted.cells[2])
        val afterCapture = CellPhraseProgressPolicy.afterSuccessfulCapture(adjusted.cells[0], adjusted.phraseSets[0])
        assertEquals(2, afterCapture.index)
        assertEquals(0, afterCapture.completedShots)
        assertSame(template, updateQuickPhraseCompleted(template, "phrase-a", -1))
        assertSame(template, updateQuickPhraseCompleted(template, "phrase-a", 3))
        assertSame(template, updateQuickPhraseCompleted(template, "phrase-a", 1))
        assertSame(template, updateQuickPhraseCompleted(template, "counter", 0))
    }

    @Test fun `switching sets starts first phrase uses new set default and preserves others`() {
        val changed = selectQuickPhraseSet(template, "phrase-a", "second")
        assertEquals("second", changed.cells[0].phraseSetId)
        assertEquals(0, changed.cells[0].phraseIndex)
        assertEquals(0, changed.cells[0].phraseCompletedShots)
        assertNull(changed.cells[0].everyOverride)
        assertEquals(other, changed.cells[1])
        assertEquals(counter, changed.cells[2])
        assertEquals("대조구", PhraseResolver.resolveSelectedTextByCellId(
            changed.cells, changed.phraseSets, 1)["phrase-a"])
    }

    @Test fun `quick progress survives table persistence without changing referenced slots`() {
        val changed = updateQuickPhraseCompleted(
            updateQuickPhraseEvery(selectQuickPhraseItem(template, "phrase-a", 0), "phrase-a", 5),
            "phrase-a", 3)
        val restored = requireNotNull(tableTemplateStateFromJson(changed.toJsonString()))
        assertEquals(changed.cells, restored.cells)
        assertEquals(changed.phraseSets, restored.phraseSets)
        assertEquals(changed.fileNameSlotDrafts, restored.fileNameSlotDrafts)
        assertEquals(changed.pathSlotDrafts, restored.pathSlotDrafts)
    }

    @Test fun `switching between modes preserves cell boundary and inherited phrase rules`() {
        val inheritedSet = template.phraseSets[0].copy(
            itemEveryOverrides = listOf(null, 5, 3),
        )
        val inheritedTemplate = template.copy(phraseSets = listOf(inheritedSet, template.phraseSets[1]),
            cells = listOf(first.copy(everyOverride = null, phraseCompletedShots = 1), other, counter))
        val inherited = inheritedTemplate.cells[0]
        assertEquals(5, inheritedSet.everyForItem(inherited.phraseIndex))
        val fixed = updateQuickPhraseEvery(inheritedTemplate, inherited.cellId, 5)
        assertEquals(5, fixed.cells[0].everyOverride)
        assertEquals(1, fixed.cells[0].phraseCompletedShots)
        assertEquals(1, CellPhraseProgressPolicy.current(fixed.cells[0], inheritedSet).completedShots)
        val anotherPhrase = selectQuickPhraseItem(fixed, inherited.cellId, 2)
        assertEquals(5, anotherPhrase.cells[0].everyOverride)
        val back = followQuickPhraseSetPeriods(anotherPhrase, inherited.cellId)
        assertNull(back.cells[0].everyOverride)
        assertEquals(3, inheritedSet.everyForItem(back.cells[0].phraseIndex))
        assertEquals(other, back.cells[1])
        assertEquals(counter, back.cells[2])
        assertSame(back, followQuickPhraseSetPeriods(back, inherited.cellId))
    }

    @Test fun `empty and missing phrase sets cannot be edited by quick controls`() {
        val unavailable = template.copy(phraseSets = listOf(
            RotatingPhraseSet("first", "빈 세트", emptyList())))
        assertSame(unavailable, selectQuickPhraseItem(unavailable, "phrase-a", 0))
        assertSame(unavailable, updateQuickPhraseEvery(unavailable, "phrase-a", 4))
        assertSame(unavailable, updateQuickPhraseCompleted(unavailable, "phrase-a", 0))
    }
}
