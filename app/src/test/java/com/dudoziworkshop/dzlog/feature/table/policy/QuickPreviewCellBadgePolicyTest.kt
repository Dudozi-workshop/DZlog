package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPreviewCellBadgePolicyTest {
    private fun plan(
        width: Float,
        height: Float = 40f,
        links: QuickCellLinks = QuickCellLinks(true, true),
        editable: Boolean = false,
    ) = quickPreviewCellBadgePlan(
        widthPx = width,
        heightPx = height,
        idealSizePx = 12f,
        minimumSizePx = 8f,
        insetPx = 4f,
        links = links,
        editable = editable,
    )

    @Test fun roomy_cell_displays_type_both_naming_links_and_lock() {
        val badges = plan(width = 100f)
        assertTrue(badges.type)
        assertTrue(badges.fileName)
        assertTrue(badges.path)
        assertTrue(badges.locked)
        assertEquals(12f, badges.sizePx, 0f)
    }

    @Test fun narrow_cell_keeps_type_then_naming_before_lock() {
        val badges = plan(width = 37f)
        assertTrue(badges.type)
        assertTrue(badges.fileName)
        assertFalse(badges.path)
        assertFalse(badges.locked)
    }

    @Test fun very_narrow_cell_preserves_type_instead_of_hiding_all_markers() {
        val badges = plan(width = 24f)
        assertTrue(badges.type)
        assertFalse(badges.fileName)
        assertFalse(badges.path)
    }

    @Test fun too_small_cell_hides_badges_without_generating_invalid_sizes() {
        assertFalse(plan(width = 100f, height = 14f).type)
        assertFalse(plan(width = 10f).type)
    }

    @Test fun cell_type_markers_cover_all_six_types() {
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
    }
}
