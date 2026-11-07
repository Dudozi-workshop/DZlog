package com.dudoziworkshop.dzlog.ui.table.mock

import android.graphics.RectF
import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.feature.table.render.buildCameraTableSceneFromPlacement
import com.dudoziworkshop.dzlog.feature.table.render.buildDesignPreviewPlacement
import com.dudoziworkshop.dzlog.feature.table.render.computeRatioOnlyTableShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditorCameraTableGeometryTest {
    private fun compareWithCamera(
        template: TableTemplateState,
        values: List<String>,
        aspect: CaptureAspect,
        widthRatio: Int,
        heightRatio: Int,
        valueScale: Int = 100,
    ): EditorCameraTableGeometry {
        val cells = values.map(::WatermarkCell)
        val editor = computeEditorCameraTableGeometry(
            cells = cells,
            templateCells = template.cells,
            rows = template.rows,
            cols = template.cols,
            rowWeights = template.rowWeights,
            colWeights = template.colWeights,
            valueScale = valueScale,
            tableWidthRatio = widthRatio,
            tableHeightRatio = heightRatio,
            captureAspect = aspect,
        )
        val width = 2632f
        val shape = computeRatioOnlyTableShape(
            tableWidthRatio = widthRatio,
            tableHeightRatio = heightRatio,
            maxWidthRatio = widthRatio,
            maxHeightRatio = heightRatio,
            hardMaxRatio = TableLayoutCalculator.CAMERA_MAX_RATIO,
        )
        val camera = buildCameraTableSceneFromPlacement(
            bounds = RectF(0f, 0f, width, width / aspect.ratioF),
            placement = buildDesignPreviewPlacement(shape.tableWidthRatio, shape.tableHeightRatio),
            cells = cells,
            templateCells = template.cells,
            rows = template.rows,
            cols = template.cols,
            valueScale = valueScale,
            baseScaleRatio = shape.tableWidthRatio,
            rowWeights = template.rowWeights,
            colWeights = template.colWeights,
        )
        assertEquals(
            camera.scene.tableRect.width() / camera.scene.tableRect.height(),
            editor.widthToHeight,
            0.0002f,
        )
        camera.resolvedLayout.finalColWeights.zip(editor.colWeights).forEach { (a, b) ->
            assertEquals(a, b, 0.0001f)
        }
        camera.resolvedLayout.finalRowWeights.zip(editor.rowWeights).forEach { (a, b) ->
            assertEquals(a, b, 0.0001f)
        }
        return editor
    }

    @Test
    fun regular_cells_use_camera_content_driven_proportions_in_all_capture_aspects() {
        val template = newBlankTableTemplateState(2, 3).copy(
            rowWeights = listOf(1f, 1.4f),
            colWeights = listOf(1f, 2f, 0.7f),
        )
        val values = listOf("긴 텍스트 측정", "25", "S3", "표", "2026-11-07", "")
        for (aspect in CaptureAspect.entries) {
            compareWithCamera(template, values, aspect, widthRatio = 45, heightRatio = 25, valueScale = 130)
        }
    }

    @Test
    fun merged_cells_share_camera_row_and_column_proportions() {
        val original = newBlankTableTemplateState(2, 2)
        val template = original.copy(
            cells = original.cells.mapIndexed { index, cell ->
                if (index == 0) cell.copy(colSpan = 2) else cell
            },
            rowWeights = listOf(1.2f, 1f),
            colWeights = listOf(1f, 1.8f),
        )
        val geom = compareWithCamera(
            template, listOf("하나의 병합 셀", "", "A", "B"),
            CaptureAspect.R9_16, widthRatio = 40, heightRatio = 20,
        )
        assertEquals(2, geom.colWeights.size)
        assertTrue(geom.widthToHeight > 0f)
    }

    @Test
    fun global_editor_fit_preserves_canonical_camera_aspect() {
        val template = newBlankTableTemplateState(3, 2)
        val geom = compareWithCamera(
            template, listOf("A", "B", "C", "D", "E", "F"),
            CaptureAspect.R3_4, widthRatio = 35, heightRatio = 18,
        )
        val naturalWidth = 300f
        val naturalHeight = naturalWidth / geom.widthToHeight
        val scale = editorPreviewFitScale(naturalWidth, naturalHeight, 300f, 250f)
        assertEquals(geom.widthToHeight, naturalWidth * scale / (naturalHeight * scale), 0.0001f)
    }
}
