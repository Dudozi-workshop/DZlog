package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class NamingSlotPreviewTest {
    private fun manual(value: String) = TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = value)
    private fun preview(file: Boolean, index: Int, drafts: List<TableEditorSlotDraft?>) =
        buildNamingSlotPreview(file, index, emptyList(), drafts, "_", 4, 12, Date(0), "yyyyMMdd", "HHmm")

    @Test fun repeatedFilenameValuesHighlightOnlySelectedSlot() {
        val p = preview(true, 2, listOf(manual("A"), null, manual("A")))
        assertEquals("A_A_0012.jpg", p.text)
        assertEquals(2, p.highlightStart)
        assertEquals(3, p.highlightEnd)
    }
    @Test fun separatorsInsideManualValueAreNotTreatedAsSlots() {
        val p = preview(true, 1, listOf(manual("A_B"), manual("A/B")))
        assertEquals("A_B_A_B_0012.jpg", p.text)
        assertEquals(4, p.highlightStart)
        assertEquals("A_B", p.text.substring(p.highlightStart, p.highlightEnd))
    }
    @Test fun pathHighlightsOnlyTargetFolderWithoutCounterSuffix() {
        val p = preview(false, 1, listOf(manual("A"), manual("A")))
        assertEquals("Pictures/DZlog/A/A/", p.text)
        assertEquals("Pictures/DZlog/A/".length, p.highlightStart)
        assertEquals(p.highlightStart + 1, p.highlightEnd)
    }
    @Test fun emptySlotHasNoHighlightAndKeepsDefaultFilename() {
        val p = preview(true, 0, listOf(null))
        assertEquals("DZlog_0012.jpg", p.text)
        assertEquals(0, p.highlightEnd)
    }
    @Test fun blankPathKeepsDefaultFolder() {
        val p = preview(false, 0, listOf(manual("...")))
        assertEquals("Pictures/DZlog/", p.text)
        assertEquals(0, p.highlightEnd)
    }
}
