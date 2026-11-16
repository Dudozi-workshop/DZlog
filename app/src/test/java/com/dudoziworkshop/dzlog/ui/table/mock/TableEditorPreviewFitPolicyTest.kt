package com.dudoziworkshop.dzlog.ui.table.mock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableEditorPreviewFitPolicyTest {
    @Test
    fun keyboard_and_expanded_input_keep_the_unobscured_scale_until_release() {
        val closed = editorPreviewFitScale(260f, 420f, 300f, 500f)
        listOf(360f, 260f, 180f, 100f, 260f, 500f).forEach { height ->
            val fit = editorPreviewFitScale(260f, 420f, 300f, height)
            assertEquals(closed, editorPreviewInputScale(fit, closed, true), 0.0001f)
        }
        val changedViewport = editorPreviewFitScale(260f, 420f, 300f, 360f)
        assertEquals(changedViewport, editorPreviewInputScale(changedViewport, closed, false), 0.0001f)
        assertEquals(changedViewport, editorPreviewInputScale(changedViewport, null, true), 0.0001f)
    }

    @Test
    fun selected_cell_scroll_centres_merged_cells_and_clamps_at_both_ends() {
        assertEquals(0f, editorPreviewSelectionScroll(36f, 91f, 180f, 420f), 0.0001f)
        assertEquals(240f, editorPreviewSelectionScroll(366f, 420f, 180f, 420f), 0.0001f)
        assertEquals(130f, editorPreviewSelectionScroll(165f, 275f, 180f, 420f), 0.0001f)
        assertEquals(0f, editorPreviewSelectionScroll(20f, 40f, 500f, 420f), 0.0001f)
    }

    @Test
    fun selected_and_next_visible_does_not_scroll_unnecessarily() {
        assertEquals(
            20f,
            editorPreviewCurrentNextScroll(35f, 55f, 70f, 90f, 120f, 400f, 20f),
            0.001f,
        )
        // Two columns within the same row also keep their shared vertical position.
        assertEquals(
            30f,
            editorPreviewCurrentNextScroll(52f, 78f, 52f, 78f, 120f, 400f, 30f),
            0.001f,
        )
    }

    @Test
    fun next_cell_outside_view_moves_only_as_far_as_needed() {
        assertEquals(
            48f,
            editorPreviewCurrentNextScroll(70f, 90f, 130f, 160f, 120f, 400f, 20f),
            0.001f,
        )
        // Scrolling upward keeps the selected and next cell visible.
        assertEquals(
            22f,
            editorPreviewCurrentNextScroll(30f, 40f, 50f, 60f, 120f, 400f, 90f),
            0.001f,
        )
    }

    @Test
    fun oversized_next_cell_preserves_selection_and_peeks_when_possible() {
        // Whole range cannot fit; the selected cell must remain visible.
        assertEquals(
            15f,
            editorPreviewCurrentNextScroll(30f, 50f, 95f, 160f, 100f, 300f, 0f),
            0.001f,
        )
        // A very tall selected cell prioritizes its beginning rather than forcing zoom.
        assertEquals(
            22f,
            editorPreviewCurrentNextScroll(30f, 180f, 190f, 220f, 100f, 320f, 100f),
            0.001f,
        )
    }

    @Test
    fun last_cell_and_short_content_clamp_to_available_scroll() {
        assertEquals(
            180f,
            editorPreviewCurrentNextScroll(280f, 300f, null, null, 120f, 300f, 60f),
            0.001f,
        )
        assertEquals(
            0f,
            editorPreviewCurrentNextScroll(20f, 40f, 50f, 70f, 500f, 300f, 0f),
            0.001f,
        )
    }

    @Test
    fun same_visible_selection_stays_put_when_keyboard_or_tab_changes() {
        assertEquals(false, editorPreviewShouldReveal(
            true, "row15", "row15", 150f, 180f, 120f, 100f, false,
        ))
        // A transition out of the scrollable input mode must not force a reset.
        assertEquals(false, editorPreviewShouldReveal(
            false, "row15", "row15", 150f, 180f, 120f, 100f, false,
        ))
        assertEquals(false, editorPreviewShouldReveal(
            true, null, "row15", 150f, 180f, 120f, 100f, false,
        ))
    }

    @Test
    fun new_selection_reveals_itself_but_manual_scroll_wins_until_selection_changes() {
        // A newly selected cell may reveal itself and its next editable cell.
        assertEquals(true, editorPreviewShouldReveal(
            true, "row16", "row15", 150f, 180f, 120f, 100f, false,
        ))
        // Selected row 15 is outside the viewport and needs a minimal correction.
        assertEquals(true, editorPreviewShouldReveal(
            true, "row15", "row15", 80f, 105f, 120f, 140f, false,
        ))
        // User scrolled to row 18 manually: do not pull the view back to row 15.
        assertEquals(false, editorPreviewShouldReveal(
            true, "row15", "row15", 80f, 105f, 120f, 140f, true,
        ))
        assertEquals(false, editorPreviewShouldReveal(
            true, "row16", "row15", 150f, 180f, 120f, 100f, true,
        ))
    }

    @Test
    fun clipping_and_margin_are_respected_after_viewport_resize() {
        assertEquals(false, editorPreviewShouldReveal(
            true, "cell", "cell", 130f, 190f, 120f, 100f, false,
        ))
        assertEquals(true, editorPreviewShouldReveal(
            true, "cell", "cell", 130f, 190f, 90f, 100f, false,
        ))
        assertEquals(true, editorPreviewShouldReveal(
            true, "cell", "cell", 107f, 130f, 120f, 100f, false,
        ))
    }

    @Test
    fun shrinking_keeps_gesture_zoom_then_refits_the_same_aspect_on_release() {
        val initial = editorPreviewFitScale(80f, 20f, 400f, 300f)
        assertEquals(5f, initial, 0.0001f)
        val finalFit = editorPreviewFitScale(40f, 20f, 400f, 300f)
        val duringDrag = editorPreviewDragScale(finalFit, initial)
        assertEquals(initial, duringDrag, 0.0001f)
        assertEquals(200f, 40f * duringDrag, 0.0001f)
        val released = editorPreviewDragScale(finalFit, null)
        assertEquals(400f, 40f * released, 0.0001f)
        assertEquals(200f, 20f * released, 0.0001f)
        assertEquals(2f, (40f * released) / (20f * released), 0.0001f)
    }

    @Test
    fun growth_stays_inside_the_viewport_and_tall_shapes_fill_height() {
        val growingFit = editorPreviewFitScale(120f, 20f, 400f, 300f)
        val growthScale = editorPreviewDragScale(growingFit, 5f)
        assertTrue(120f * growthScale <= 400.001f)
        assertTrue(20f * growthScale <= 300.001f)
        val tallFit = editorPreviewFitScale(20f, 80f, 400f, 300f)
        assertEquals(300f, 80f * tallFit, 0.0001f)
        assertEquals(75f, 20f * tallFit, 0.0001f)
    }
}
