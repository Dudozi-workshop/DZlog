package com.dudoziworkshop.dzlog.ui.camera.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsableVerticalBoundsTest {

    @Test
    fun uses_button_positions_and_margin_inside_content_bounds() {
        val (top, bottom) = resolveUsableVerticalBounds(
            contentTop = 100f,
            contentBottom = 900f,
            settingsButtonBottomY = 180f,
            shutterButtonTopY = 820f,
            safeTopY = null,
            safeBottomY = null,
            verticalMarginPx = 10f
        )

        assertEquals(190f, top)
        assertEquals(810f, bottom)
    }

    @Test
    fun safe_insets_override_button_bounds_when_more_restrictive() {
        val (top, bottom) = resolveUsableVerticalBounds(
            contentTop = 0f,
            contentBottom = 1000f,
            settingsButtonBottomY = 120f,
            shutterButtonTopY = 900f,
            safeTopY = 180f,
            safeBottomY = 760f,
            verticalMarginPx = 10f
        )

        assertEquals(180f, top)
        assertEquals(760f, bottom)
    }

    @Test
    fun collapses_to_minimum_height_when_constraints_overlap() {
        val (top, bottom) = resolveUsableVerticalBounds(
            contentTop = 0f,
            contentBottom = 1000f,
            settingsButtonBottomY = 700f,
            shutterButtonTopY = 680f,
            safeTopY = 710f,
            safeBottomY = 675f,
            verticalMarginPx = 10f
        )

        assertTrue(bottom > top)
        assertEquals(1f, bottom - top)
        assertTrue(top >= 0f)
        assertTrue(bottom <= 1000f)
    }
}
