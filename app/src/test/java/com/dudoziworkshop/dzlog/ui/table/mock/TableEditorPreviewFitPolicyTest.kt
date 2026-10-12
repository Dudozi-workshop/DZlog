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
