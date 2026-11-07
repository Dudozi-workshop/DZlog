package com.dudoziworkshop.dzlog.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
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

    @Test
    fun resolved_painter_skips_merged_internal_vertical_grid_line() {
        val bitmap = Bitmap.createBitmap(240, 120, Bitmap.Config.ARGB_8888)
        drawWatermarkTableOnCanvasWithResolvedGeometry(
            canvas = Canvas(bitmap),
            tableRect = RectF(0f, 0f, 240f, 120f),
            rowEdges = listOf(0f, 60f, 120f),
            colEdges = listOf(0f, 120f, 240f),
            cells = List(4) { WatermarkCell("") },
            rows = 2, cols = 2,
            bgAlpha = 0, valueScale = 100,
            bgStyle = 2,
            drawGrid = true,
            mergedRootCells = listOf(RenderRootCell("merged", 0, 0, 1, 2)),
        )
        assertEquals(0, Color.alpha(bitmap.getPixel(120, 25)))
        assertTrue(Color.alpha(bitmap.getPixel(120, 90)) > 0)
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
