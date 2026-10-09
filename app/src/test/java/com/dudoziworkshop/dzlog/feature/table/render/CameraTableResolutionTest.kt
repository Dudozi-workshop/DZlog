package com.dudoziworkshop.dzlog.feature.table.render

import android.graphics.RectF
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraTableResolutionTest {
    private val cells = listOf("새우", "S1", "DZlog", "B3", "", "", "", "")
        .map(::WatermarkCell)
    private val template = List(8) { TableCellState(rowIndex = it / 4, colIndex = it % 4) }

    private fun scene(bounds: RectF, anchor: WatermarkTableAnchor, rotation: Int = 0) =
        buildCameraTableSceneFromPlacement(
            bounds = bounds,
            placement = TableRenderPlacement(
                anchor = anchor,
                offsetXRatio = 3,
                offsetYRatio = 4,
                tableWidthRatio = 35,
                tableHeightRatio = 15,
                rotationCwDeg = rotation,
                overrideOffsetLeftPx = if (anchor == WatermarkTableAnchor.CUSTOM) bounds.width() * 0.1f else null,
                overrideOffsetTopPx = if (anchor == WatermarkTableAnchor.CUSTOM) bounds.height() * 0.2f else null,
            ),
            cells = cells,
            templateCells = template,
            rows = 2,
            cols = 4,
            valueScale = 100,
            baseScaleRatio = 35,
            rowWeights = listOf(1f, 1.5f),
            colWeights = listOf(1f, 1f, 2f, 1f),
        )

    private fun assertSameRelativeRect(a: RectF, aBounds: RectF, b: RectF, bBounds: RectF) {
        assertEquals((a.left - aBounds.left) / aBounds.width(), (b.left - bBounds.left) / bBounds.width(), 0.0001f)
        assertEquals((a.top - aBounds.top) / aBounds.height(), (b.top - bBounds.top) / bBounds.height(), 0.0001f)
        assertEquals(a.width() / aBounds.width(), b.width() / bBounds.width(), 0.0001f)
        assertEquals(a.height() / aBounds.height(), b.height() / bBounds.height(), 0.0001f)
    }

    @Test
    fun preview_and_high_resolution_photo_keep_table_size_position_and_cell_edges() {
        val previewBounds = RectF(0f, 280f, 658f, 1157f)
        val photoBounds = RectF(0f, 0f, 2632f, 3508f)
        for (anchor in WatermarkTableAnchor.entries) {
            val preview = scene(previewBounds, anchor)
            val photo = scene(photoBounds, anchor)
            assertSameRelativeRect(preview.scene.tableRect, previewBounds, photo.scene.tableRect, photoBounds)
            preview.scene.baseCellRects.zip(photo.scene.baseCellRects).forEach { (a, b) ->
                assertSameRelativeRect(RectF(a.left, a.top, a.right, a.bottom), previewBounds,
                    RectF(b.left, b.top, b.right, b.bottom), photoBounds)
            }
            assertTrue("Photo must scale beyond intrinsic pixel dimensions", photo.resolvedLayout.fitScale > 1f)
        }
    }

    @Test
    fun rotated_custom_placement_scales_with_capture_area_instead_of_screen_origin() {
        val previewBounds = RectF(20f, 300f, 678f, 1177f)
        val photoBounds = RectF(0f, 0f, 2632f, 3508f)
        for (rotation in listOf(0, 90, 180, 270)) {
            assertSameRelativeRect(
                scene(previewBounds, WatermarkTableAnchor.CUSTOM, rotation).scene.tableRect, previewBounds,
                scene(photoBounds, WatermarkTableAnchor.CUSTOM, rotation).scene.tableRect, photoBounds,
            )
        }
    }

    @Test
    fun compact_preview_and_photo_preserve_identical_table_proportions() {
        val smallBounds = RectF(0f, 0f, 240f, 320f)
        val largeBounds = RectF(0f, 0f, 3600f, 4800f)
        val small = scene(smallBounds, WatermarkTableAnchor.TOP_LEFT)
        val large = scene(largeBounds, WatermarkTableAnchor.TOP_LEFT)
        assertSameRelativeRect(small.scene.tableRect, smallBounds, large.scene.tableRect, largeBounds)
        small.resolvedLayout.finalRowWeights.zip(large.resolvedLayout.finalRowWeights).forEach { (a, b) ->
            assertEquals(a, b, 0.0001f)
        }
        small.resolvedLayout.finalColWeights.zip(large.resolvedLayout.finalColWeights).forEach { (a, b) ->
            assertEquals(a, b, 0.0001f)
        }
    }
}
