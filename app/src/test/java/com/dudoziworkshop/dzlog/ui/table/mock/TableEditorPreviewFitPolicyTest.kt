package com.dudoziworkshop.dzlog.ui.table.mock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableEditorPreviewFitPolicyTest {
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
