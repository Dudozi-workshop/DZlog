package com.dudoziworkshop.dzlog.watermark

import com.dudoziworkshop.dzlog.feature.table.render.RenderRootCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MergedTableCellDrawPlanTest {
    @Test
    fun covered_positions_are_hidden_and_anchor_spans_the_entire_merge() {
        val plan = createMergedTableCellDrawPlan(
            rows = 2,
            cols = 3,
            rootCells = listOf(RenderRootCell("root", 0, 0, 2, 2)),
        )
        assertFalse(plan.isCovered(0))
        assertTrue(plan.isCovered(1))
        assertTrue(plan.isCovered(3))
        assertTrue(plan.isCovered(4))
        assertFalse(plan.isCovered(2))
        assertFalse(plan.isCovered(5))
        assertEquals(MergedTableCellSpan(2, 2), plan.spanAt(0))
        assertEquals(null, plan.spanAt(1))
    }

    @Test
    fun merged_cells_have_no_inner_grid_strokes_but_neighbor_cells_retain_them() {
        val plan = createMergedTableCellDrawPlan(
            rows = 2, cols = 2,
            rootCells = listOf(RenderRootCell("top", 0, 0, 1, 2)),
        )
        val strokes = mergedTableGridStrokes(
            plan,
            rowEdges = listOf(0f, 60f, 120f),
            colEdges = listOf(0f, 120f, 240f),
        )
        assertFalse(strokes.contains(TableGridStroke(120f, 0f, 120f, 60f)))
        assertTrue(strokes.contains(TableGridStroke(120f, 60f, 120f, 120f)))
        assertTrue(strokes.contains(TableGridStroke(0f, 60f, 120f, 60f)))
        assertTrue(strokes.contains(TableGridStroke(120f, 60f, 240f, 60f)))
        assertTrue(strokes.contains(TableGridStroke(0f, 0f, 0f, 120f)))
    }

    /**
     * Robolectric's transparent bitmap raster assertion does not reliably draw the
     * antialiased grid strokes. Verify the exact stroke plan here; verify pixels
     * in an instrumented Android rendering test/device smoke test instead.
     */
    @Test
    fun vertically_merged_root_suppresses_internal_row_line_only_within_its_span() {
        val plan = createMergedTableCellDrawPlan(
            rows = 3, cols = 2,
            rootCells = listOf(RenderRootCell("left", 0, 0, 2, 1)),
        )
        val strokes = mergedTableGridStrokes(
            plan,
            rowEdges = listOf(0f, 40f, 80f, 120f),
            colEdges = listOf(0f, 100f, 200f),
        )
        assertFalse(strokes.contains(TableGridStroke(0f, 40f, 100f, 40f)))
        assertTrue(strokes.contains(TableGridStroke(100f, 40f, 200f, 40f)))
        assertTrue(strokes.contains(TableGridStroke(0f, 80f, 100f, 80f)))
        assertTrue(strokes.contains(TableGridStroke(100f, 80f, 200f, 80f)))
        assertTrue(strokes.contains(TableGridStroke(100f, 0f, 100f, 40f)))
        assertTrue(strokes.contains(TableGridStroke(100f, 40f, 100f, 80f)))
    }

    @Test
    fun unmerged_grid_keeps_all_internal_strokes() {
        val plan = createMergedTableCellDrawPlan(2, 2, emptyList())
        val strokes = mergedTableGridStrokes(
            plan,
            rowEdges = listOf(0f, 60f, 120f),
            colEdges = listOf(0f, 120f, 240f),
        )
        assertTrue(strokes.contains(TableGridStroke(120f, 0f, 120f, 60f)))
        assertTrue(strokes.contains(TableGridStroke(120f, 60f, 120f, 120f)))
        assertTrue(strokes.contains(TableGridStroke(0f, 60f, 120f, 60f)))
        assertTrue(strokes.contains(TableGridStroke(120f, 60f, 240f, 60f)))
    }

    @Test
    fun out_of_range_merge_is_ignored_without_hiding_any_cell() {
        val plan = createMergedTableCellDrawPlan(
            rows = 1, cols = 2,
            rootCells = listOf(RenderRootCell("invalid", 3, 2, 2, 2)),
        )
        assertFalse(plan.isCovered(0))
        assertFalse(plan.isCovered(1))
        assertEquals(null, plan.spanAt(0))
    }
}
