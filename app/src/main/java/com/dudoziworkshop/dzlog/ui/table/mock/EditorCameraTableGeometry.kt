package com.dudoziworkshop.dzlog.ui.table.mock

import android.graphics.RectF
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.feature.table.render.buildCameraTableSceneFromPlacement
import com.dudoziworkshop.dzlog.feature.table.render.buildDesignPreviewPlacement
import com.dudoziworkshop.dzlog.feature.table.render.computeRatioOnlyTableShape

/**
 * Camera and editor use the same content-driven proportions.
 * Editor viewport fitting changes only the global scale, not row/column ratios.
 */
internal data class EditorCameraTableGeometry(
    val widthToHeight: Float,
    val rowWeights: List<Float>,
    val colWeights: List<Float>,
)

internal fun computeEditorCameraTableGeometry(
    cells: List<WatermarkCell>,
    templateCells: List<TableCellState>,
    rows: Int,
    cols: Int,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    valueScale: Int,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    captureAspect: CaptureAspect,
): EditorCameraTableGeometry {
    val referenceWidth = 1000f
    val shape = computeRatioOnlyTableShape(
        tableWidthRatio = tableWidthRatio,
        tableHeightRatio = tableHeightRatio,
        maxWidthRatio = tableWidthRatio,
        maxHeightRatio = tableHeightRatio,
        hardMaxRatio = TableLayoutCalculator.CAMERA_MAX_RATIO,
    )
    val reference = buildCameraTableSceneFromPlacement(
        bounds = RectF(0f, 0f, referenceWidth, referenceWidth / captureAspect.ratioF),
        placement = buildDesignPreviewPlacement(shape.tableWidthRatio, shape.tableHeightRatio),
        cells = cells,
        templateCells = templateCells,
        rows = rows,
        cols = cols,
        valueScale = valueScale,
        baseScaleRatio = shape.tableWidthRatio,
        rowWeights = rowWeights,
        colWeights = colWeights,
    )
    val rect = reference.scene.tableRect
    return EditorCameraTableGeometry(
        widthToHeight = rect.width().coerceAtLeast(1f) / rect.height().coerceAtLeast(1f),
        rowWeights = reference.resolvedLayout.finalRowWeights,
        colWeights = reference.resolvedLayout.finalColWeights,
    )
}
