package com.dudoziworkshop.dzlog.domain.phrase

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.preview.*
import com.dudoziworkshop.dzlog.domain.table.applyPatch
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class CellPhraseProgressTest {
    private val set = RotatingPhraseSet("set", "처리구", listOf("T1", "T2", "T3", "T4"), 12)
    private val cell = TableCellState(0, 0, cellId = "phrase", dataType = TableCellDataType.ROTATING_TEXT,
        phraseSetId = set.id)

    private fun template(cells: List<TableCellState> = listOf(cell)) =
        TableTemplateState(1, cells.size, cells, phraseSets = listOf(set))

    private fun capture(t: TableTemplateState): FinalCapturePreview {
        val scope = buildScope(CaptureScopeInput(t, Date(0), 0, "yyyyMMdd", "HHmm", "_", true, SaveMode.BOTH, 30))
        return buildCapturePreview(scope, FinalCapturePreviewInput(t, Date(0), 0, "yyyyMMdd", "HHmm", "_",
            true, true, SaveMode.BOTH, 1, 30))
    }

    @Test fun twelve_successful_captures_per_item_wrap_after_forty_eight() {
        var t = template()
        repeat(48) { shot ->
            val preview = capture(t)
            assertEquals(set.items[shot / 12], preview.resolvedCells.single().resolvedText)
            assertEquals(shot % 12, t.cells.single().phraseCompletedShots)
            // Planning alone, including failed or cancelled captures, leaves persisted progress unchanged.
            assertEquals(preview.tablePatch, capture(t).tablePatch)
            t = t.applyPatch(preview.tablePatch)
        }
        assertEquals(0, t.cells.single().phraseIndex)
        assertEquals(0, t.cells.single().phraseCompletedShots)
    }

    @Test fun legacy_global_cursor_cannot_change_a_new_cells_start() {
        for (cursor in listOf(1, 25, 36, 10000)) {
            assertEquals("T1", PhraseResolver.resolveSelectedTextByCellId(listOf(cell), listOf(set), cursor)[cell.cellId])
        }
    }

    @Test fun cells_have_independent_periods_and_inactive_template_does_not_advance() {
        val other = cell.copy(cellId = "other", colIndex = 1, everyOverride = 2, phraseIndex = 2, phraseCompletedShots = 1)
        val inactive = template(listOf(other))
        val active = template(listOf(cell, other))
        val next = active.applyPatch(capture(active).tablePatch)
        assertEquals(1, next.cells[0].phraseCompletedShots)
        assertEquals(0, next.cells[0].phraseIndex)
        assertEquals(3, next.cells[1].phraseIndex)
        assertEquals(0, next.cells[1].phraseCompletedShots)
        assertEquals(1, inactive.cells.single().phraseCompletedShots)
    }

    @Test fun empty_missing_and_covered_cells_do_not_advance() {
        val root = cell.copy(colSpan = 2)
        val covered = cell.copy(cellId = "covered", colIndex = 1)
        val t = template(listOf(root, covered))
        assertEquals(setOf(root.cellId), capture(t).tablePatch.phraseProgressByCellId.keys)
        assertTrue(capture(template().copy(phraseSets = emptyList())).tablePatch.phraseProgressByCellId.isEmpty())
        assertTrue(capture(template().copy(phraseSets = listOf(set.copy(items = emptyList()))))
            .tablePatch.phraseProgressByCellId.isEmpty())
    }
}
