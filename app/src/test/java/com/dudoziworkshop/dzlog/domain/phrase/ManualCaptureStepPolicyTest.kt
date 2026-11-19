package com.dudoziworkshop.dzlog.domain.phrase

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ManualCaptureStepPolicyTest {
    private val a = TableCellState(
        rowIndex = 0, colIndex = 0, cellId = "a",
        dataType = TableCellDataType.ROTATING_TEXT,
        phraseSetId = "setA", phraseIndex = 0, phraseCompletedShots = 2,
    )
    private val b = TableCellState(
        rowIndex = 0, colIndex = 1, cellId = "b",
        dataType = TableCellDataType.ROTATING_TEXT,
        phraseSetId = "setB", phraseIndex = 0, phraseCompletedShots = 1,
    )
    private val template = TableTemplateState(
        rows = 1, cols = 2,
        cells = listOf(a, b),
        phraseSets = listOf(
            RotatingPhraseSet("setA", "A", listOf("A1", "A2"), defaultEvery = 3),
            RotatingPhraseSet("setB", "B", listOf("B1", "B2"), defaultEvery = 2),
        ),
    )

    @Test fun nextAdvancesAllRootsWithIndependentPeriods() {
        val next = ManualCaptureStepPolicy.offsetTemplate(template, 1)
        assertEquals(1, next.cells[0].phraseIndex)
        assertEquals(0, next.cells[0].phraseCompletedShots)
        assertEquals(1, next.cells[1].phraseIndex)
        assertEquals(0, next.cells[1].phraseCompletedShots)
        assertEquals(2, template.cells[0].phraseCompletedShots)
    }

    @Test fun previousReversesCellsAcrossPhraseBoundaries() {
        val next = ManualCaptureStepPolicy.offsetTemplate(template, 1)
        val back = ManualCaptureStepPolicy.offsetTemplate(next, -1)
        assertEquals(template, back)
    }

    @Test fun previousFromZeroWrapsToLastInterval() {
        val source = template.copy(
            cells = listOf(a.copy(phraseIndex = 0, phraseCompletedShots = 0)),
            phraseSets = template.phraseSets.take(1).map {
                it.copy(itemEveryOverrides = listOf(2, 5))
            },
        )
        val back = ManualCaptureStepPolicy.offsetTemplate(source, -1)
        assertEquals(1, back.cells[0].phraseIndex)
        assertEquals(4, back.cells[0].phraseCompletedShots)
        assertEquals(source, ManualCaptureStepPolicy.offsetTemplate(back, 1))
    }

    @Test fun coveredMergedCellNeverAdvancesIndependently() {
        val merged = template.copy(cells = listOf(a.copy(colSpan = 2), b))
        val next = ManualCaptureStepPolicy.offsetTemplate(merged, 1)
        assertEquals(1, next.cells[0].phraseIndex)
        assertEquals(0, next.cells[0].phraseCompletedShots)
        assertEquals(b, next.cells[1])
    }

    @Test fun largeStepsWrapUsingIndividualPeriods() {
        val advanced = ManualCaptureStepPolicy.offsetTemplate(template, 101)
        val equivalent = ManualCaptureStepPolicy.offsetTemplate(template, 101 % 6)
        assertEquals(equivalent, advanced)
    }

    @Test fun noAdjustmentRetainsObjectAndOtherCellTypesUnchanged() {
        assertSame(template, ManualCaptureStepPolicy.offsetTemplate(template, 0))
        val text = TableCellState(rowIndex = 0, colIndex = 2, cellId = "plain")
        val expanded = template.copy(cols = 3, cells = template.cells + text)
        assertEquals(text, ManualCaptureStepPolicy.offsetTemplate(expanded, 5).cells[2])
    }

    @Test fun countStepBackCannotGoBelowOne() {
        assertEquals(1, ManualCaptureStepPolicy.adjustNextCounter(2, -30))
        assertEquals(8, ManualCaptureStepPolicy.adjustNextCounter(5, 3))
        assertEquals(Int.MAX_VALUE, ManualCaptureStepPolicy.adjustNextCounter(Int.MAX_VALUE, 100))
        assertEquals(false, ManualCaptureStepPolicy.canStepBack(1, 0))
        assertEquals(true, ManualCaptureStepPolicy.canStepBack(3, -1))
    }
}
