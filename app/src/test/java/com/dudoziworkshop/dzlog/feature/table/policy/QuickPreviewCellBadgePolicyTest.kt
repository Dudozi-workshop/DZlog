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
    ) = quickPreviewCellBadgePlan(
        widthPx = width,
        idealSizePx = 12f,
        minimumSizePx = 8f,
        insetPx = 4f,
        links = links,
    )

    @Test fun roomy_cell_shows_type_left_and_both_links_right_on_one_line() {
        val badges = plan(width = 100f)
        assertTrue(badges.type)
        assertTrue(badges.fileName)
        assertTrue(badges.path)
        assertFalse(badges.stackLinks)
        assertEquals(12f, badges.sizePx, 0f)
    }

    @Test fun compact_cell_scales_icons_before_stacking_without_hiding_them() {
        val badges = plan(width = 30f)
        assertEquals(9f, badges.sizePx, 0f)
        assertTrue(badges.type)
        assertTrue(badges.fileName)
        assertTrue(badges.path)
        assertTrue(badges.stackLinks)
    }

    @Test fun narrow_cell_stacks_only_the_two_right_hand_links() {
        val badges = plan(width = 37f)
        assertTrue(badges.type)
        assertTrue(badges.fileName)
        assertTrue(badges.path)
        assertTrue(badges.stackLinks)
    }

    @Test fun very_narrow_cell_never_hides_type_or_naming_information() {
        val badges = plan(width = 1f)
        assertTrue(badges.type)
        assertTrue(badges.fileName)
        assertTrue(badges.path)
        assertTrue(badges.stackLinks)
    }

    @Test fun even_zero_width_retains_all_badges_for_overlay() {
        val badges = plan(width = 0f)
        assertTrue(badges.type)
        assertTrue(badges.fileName)
        assertTrue(badges.path)
        assertTrue(badges.stackLinks)
    }

    @Test fun one_right_hand_link_never_needs_two_line_stacking() {
        val badges = plan(width = 1f, links = QuickCellLinks(false, true))
        assertTrue(badges.type)
        assertFalse(badges.fileName)
        assertTrue(badges.path)
        assertFalse(badges.stackLinks)
    }

    @Test fun unlinked_cell_shows_type_only_in_any_width() {
        val badges = plan(width = 1f, links = QuickCellLinks(false, false))
        assertTrue(badges.type)
        assertFalse(badges.fileName)
        assertFalse(badges.path)
        assertFalse(badges.stackLinks)
    }

    @Test fun type_labels_still_cover_all_six_types() {
        assertEquals("텍스트", quickCellTypeLabel(TableCellDataType.TEXT))
        assertEquals("숫자", quickCellTypeLabel(TableCellDataType.NUMBER))
        assertEquals("날짜", quickCellTypeLabel(TableCellDataType.DATE))
        assertEquals("시간", quickCellTypeLabel(TableCellDataType.TIME))
        assertEquals("자동번호", quickCellTypeLabel(TableCellDataType.COUNTER))
        assertEquals("순환문구", quickCellTypeLabel(TableCellDataType.ROTATING_TEXT))
    }

    @Test fun stacking_threshold_does_not_drop_a_badge() {
        val wide = plan(width = 52f)
        val narrow = plan(width = 51f)
        assertFalse(wide.stackLinks)
        assertTrue(narrow.stackLinks)
        assertEquals(wide.fileName, narrow.fileName)
        assertEquals(wide.path, narrow.path)
    }
}
