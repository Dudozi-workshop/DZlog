package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.feature.table.policy.updateQuickCellValues
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class NamingCellPreviewTest {
    private fun cell(id: String) = TableEditorSlotDraft("CELL", "셀", cellId = id)
    private fun manual(text: String) = TableEditorSlotDraft("MANUAL", "직접입력", manualText = text)
    private fun resolved(id: String, text: String, type: TableCellDataType = TableCellDataType.TEXT) =
        ResolvedCell(id, type, resolvedText = text, isEmpty = text.isBlank())
    private fun preview(file: Boolean, id: String?, cells: List<ResolvedCell>, slots: List<TableEditorSlotDraft?>, counter: Int? = 12) =
        buildNamingCellPreview(file, id, cells, slots, "_", 4, counter, Date(0), "yyyyMMdd", "HHmm")

    @Test fun equalTextDoesNotHighlightManualOrOtherCellButRepeatedReferencesDo() {
        val cells = listOf(resolved("a", "오징어"), resolved("b", "오징어"))
        val p = preview(true, "b", cells, listOf(manual("오징어"), cell("a"), null, cell("b"), cell("b")))
        assertEquals("오징어_오징어_오징어_오징어_0012.jpg", p.text)
        assertEquals(listOf(8 until 11, 12 until 15), p.highlights)
    }

    @Test fun blankSlotsAndIllegalCharactersUseTheActualCaptureRules() {
        val cells = listOf(resolved("empty", "..."), resolved("selected", " A/B "))
        val slots = listOf(cell("empty"), manual("A/B"), cell("selected"))
        val file = preview(true, "selected", cells, slots)
        val path = preview(false, "selected", cells, slots)
        assertEquals("A_B_A_B_0012.jpg", file.text)
        assertEquals(listOf(4 until 7), file.highlights)
        assertEquals("Pictures/DZlog/A_B/A_B/", path.text)
        val pathStart = "Pictures/DZlog/A_B/".length
        assertEquals(listOf(pathStart until pathStart + 3), path.highlights)
        assertEquals(buildFileName(cells, slots, "_", 4, 12, Date(0), "yyyyMMdd", "HHmm"), file.text)
        assertEquals(buildSavePath(cells, slots, Date(0), "yyyyMMdd", "HHmm"), path.text)
    }

    @Test fun liveNumberEditsUpdateFilenameAndPathWithoutLosingPrecision() {
        val template = TableTemplateState(1, 1, listOf(TableCellState(0, 0,
            cellId = "n", dataType = TableCellDataType.NUMBER, rawText = "1")),
            fileNameSlotDrafts = listOf(cell("n")), pathSlotDrafts = listOf(cell("n")))
        val edited = updateQuickCellValues(template, mapOf("n" to "-4.00"))
        val cells = listOf(resolved("n", edited.cells.single().rawText, TableCellDataType.NUMBER))
        val file = preview(true, "n", cells, edited.fileNameSlotDrafts, counter = null)
        val path = preview(false, "n", cells, edited.pathSlotDrafts)
        assertEquals("-4.00.jpg", file.text)
        assertEquals(listOf(0 until 5), file.highlights)
        assertEquals("Pictures/DZlog/-4.00/", path.text)
        val pathStart = "Pictures/DZlog/".length
        assertEquals(listOf(pathStart until pathStart + 5), path.highlights)
    }

    @Test fun unrelatedAndEmptyReferencesKeepPreviewWithoutEmphasis() {
        val cells = listOf(resolved("blank", ""))
        assertTrue(preview(true, "other", cells, listOf(manual("other"))).highlights.isEmpty())
        assertTrue(preview(true, null, cells, listOf(cell("blank"))).highlights.isEmpty())
        assertTrue(preview(true, "blank", cells, listOf(cell("blank"))).highlights.isEmpty())
        assertEquals("Pictures/DZlog/", preview(false, "blank", cells, listOf(cell("blank"))).text)
        val counter = preview(true, "counter", listOf(resolved("counter", "0012", TableCellDataType.COUNTER)), listOf(cell("counter")))
        assertEquals("DZlog_0012.jpg", counter.text)
        assertTrue(counter.highlights.isEmpty())
    }
}
