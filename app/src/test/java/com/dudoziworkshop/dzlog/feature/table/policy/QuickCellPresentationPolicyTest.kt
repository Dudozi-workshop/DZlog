package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class QuickCellPresentationPolicyTest {
    @Test fun linksUseCellIdsAndDeduplicateReferencesIncludingAutomaticCells() {
        val cells = listOf(TableCellState(0, 0, cellId = "date", dataType = TableCellDataType.DATE),
            TableCellState(0, 1, cellId = "other", rawText = "20261011"))
        val ref = TableEditorSlotDraft("CELL", "셀", cellId = "date")
        val template = TableTemplateState(1, 2, cells, fileNameSlotDrafts = listOf(ref, ref,
            TableEditorSlotDraft("MANUAL", "직접 입력", manualText = "20261011")), pathSlotDrafts = listOf(ref))
        assertEquals(QuickCellLinks(true, true), quickCellLinks(template, "date"))
        assertEquals(2, quickCellLinks(template, "date").count)
        assertEquals(QuickCellLinks(false, false), quickCellLinks(template, "other"))
        val edited = updateQuickCellValues(template, mapOf("other" to "다른 값"))
        assertEquals(quickCellLinks(template, "date"), quickCellLinks(edited, "date"))
    }

    @Test fun longPreviewMovesToSelectedContributionAndPreservesClippedRanges() {
        val text = "앞부분\n다음부분\n오징어\n끝부분\n마지막"
        val starts = listOf(0, 4, 9, 13, 17)
        val start = text.indexOf("오징어")
        val result = quickNamingWindow(text, listOf(start until start + 3), starts)
        assertEquals("…오징어\n끝부분…", result.text)
        assertEquals(listOf(1 until 4), result.highlights)
        assertEquals("앞부분\n다음부분…", quickNamingWindow(text, emptyList(), starts).text)
    }

    @Test fun shortPreviewAndRepeatedVisibleReferencesStayIntact() {
        val text = "오징어_오징어"
        val ranges = listOf(0 until 3, 4 until 7)
        val result = quickNamingWindow(text, ranges, listOf(0))
        assertEquals(text, result.text)
        assertEquals(ranges, result.highlights)
        assertEquals("", quickNamingWindow("", emptyList(), listOf(0)).text)
    }

    @Test fun lastLineContributionRemainsVisibleWithPreviousContext() {
        val text = "a\nb\nc\nd"
        val result = quickNamingWindow(text, listOf(6..6), listOf(0, 2, 4, 6))
        assertEquals("…c\nd", result.text)
        assertEquals(listOf(3..3), result.highlights)
    }

    @Test fun denseMarkersHideBeforeTheyTouchValuesAndLockCanFitAlone() {
        assertTrue(quickMarkersFit(60f, 24f, true, 2, 12f, 4f))
        assertFalse(quickMarkersFit(40f, 24f, true, 2, 12f, 4f))
        assertTrue(quickMarkersFit(40f, 24f, true, 0, 12f, 4f))
        assertFalse(quickMarkersFit(100f, 15f, false, 2, 12f, 4f))
        assertFalse(quickMarkersFit(100f, 24f, false, 0, 12f, 4f))
    }
    @Test fun singleLinePreviewTracksLateSelectedReferenceWithoutChangingFullText() {
        val text = "prefix\nselected\ntail"
        val result = quickNamingWindow(text, listOf(7 until 15), listOf(0, 7, 16), visibleLines = 1)
        assertEquals("…selected…", result.text)
        assertEquals(listOf(1 until 9), result.highlights)
        assertEquals("prefix…", quickNamingWindow(text, emptyList(), listOf(0, 7, 16), 1).text)
    }

    @Test fun editingColumnsKeepProportionsAndMinimumTapWidthOnNarrowScreens() {
        val sizes = quickEditingSizes(3, listOf(1f, 2f, 1f), 100f, 72f)
        assertEquals(listOf(72f, 144f, 72f), sizes)
        assertEquals(listOf(100f, 100f), quickEditingSizes(2, listOf(Float.NaN, -1f), 200f, 72f))
    }

    @Test fun mergedRowsGrowForWrappedTextAndShrinkFromFreshBaseAfterDeletion() {
        val roots = listOf(TableCellState(0, 0, cellId = "merged", rowSpan = 2))
        val base = listOf(56f, 56f)
        assertEquals(listOf(100f, 100f), quickEditingRows(base, roots, listOf(200f)))
        assertEquals(base, quickEditingRows(base, roots, listOf(30f)))
        assertEquals(listOf(56f, 56f), base)
    }
}
