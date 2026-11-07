package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPreviewCellBadgePolicyTest {
    private fun plan(
        width: Float,
        links: QuickCellLinks = QuickCellLinks(true, true),
        editable: Boolean = false,
    ) = quickPreviewCellBadgePlan(
        widthPx = width,
        idealSizePx = 12f,
        minimumSizePx = 8f,
        insetPx = 4f,
        links = links,
        editable = editable,
    )

    @Test fun roomy_cell_displays_all_type_naming_and_lock_markers() {
        val badges = plan(width = 100f)
        assertTrue(badges.type)
        assertTrue(badges.fileName)
        assertTrue(badges.path)
        assertTrue(badges.locked)
        assertFalse(badges.compact)
        assertEquals(12f, badges.sizePx, 0f)
    }

    @Test fun narrow_cell_keeps_all_markers_and_switches_to_compact_layout() {
        val badges = plan(width = 37f)
        assertTrue(badges.type)
        assertTrue(badges.fileName)
        assertTrue(badges.path)
        assertTrue(badges.locked)
        assertTrue(badges.compact)
    }

    @Test fun extremely_narrow_cell_never_hides_information() {
        val badges = plan(width = 1f)
        assertTrue(badges.type)
        assertTrue(badges.fileName)
        assertTrue(badges.path)
        assertTrue(badges.locked)
        assertTrue(badges.compact)
    }

    @Test fun zero_width_still_keeps_metadata_for_overflow_strip() {
        val badges = plan(width = 0f, editable = true)
        assertTrue(badges.type)
        assertTrue(badges.fileName)
        assertTrue(badges.path)
        assertFalse(badges.locked)
        assertTrue(badges.compact)
    }

    @Test fun cell_type_labels_cover_all_six_types() {
        assertEquals("텍스트", quickCellTypeLabel(TableCellDataType.TEXT))
        assertEquals("숫자", quickCellTypeLabel(TableCellDataType.NUMBER))
        assertEquals("날짜", quickCellTypeLabel(TableCellDataType.DATE))
        assertEquals("시간", quickCellTypeLabel(TableCellDataType.TIME))
        assertEquals("자동번호", quickCellTypeLabel(TableCellDataType.COUNTER))
        assertEquals("순환문구", quickCellTypeLabel(TableCellDataType.ROTATING_TEXT))
    }

    @Test fun unlinked_editable_cells_show_type_only() {
        val badges = plan(width = 100f, links = QuickCellLinks(false, false), editable = true)
        assertTrue(badges.type)
        assertFalse(badges.fileName)
        assertFalse(badges.path)
        assertFalse(badges.locked)
        assertFalse(badges.compact)
    }
}
