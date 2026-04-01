package com.dudoziworkshop.dzlog.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderPlacement
import com.dudoziworkshop.dzlog.feature.table.render.buildRenderRootCells
import com.dudoziworkshop.dzlog.feature.table.render.buildRenderedTableSceneFromPlacement
import com.dudoziworkshop.dzlog.feature.table.render.computeResolvedRenderLayout

class WatermarkRendererImpl : WatermarkRenderer {
    override fun renderTable(
        originalBmp: Bitmap,
        cells: List<WatermarkCell>,
        templateCells: List<TableCellState>,
        rows: Int,
        cols: Int,
        anchor: WatermarkTableAnchor,
        offsetXRatio: Int,
        offsetYRatio: Int,
        boundsOffsetX10000: Int,
        boundsOffsetY10000: Int,
        tableHeightRatio: Int,
        tableWidthRatio: Int,
        bgAlpha: Int,
        valueScale: Int,
        textColorMode: Int,
        manualTextColor: Int,
        textAlign: Int,
        rowWeights: List<Float>?,
        colWeights: List<Float>?,
        bgStyle: Int,
        drawGrid: Boolean,
        rotationCwDeg: Int
    ): Bitmap {
        val out = originalBmp.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val imageBounds = RectF(0f, 0f, out.width.toFloat(), out.height.toFloat())

        val placement = if (anchor == WatermarkTableAnchor.CUSTOM) {
            val baseW = imageBounds.width()
            val rawW = baseW * (tableWidthRatio.coerceIn(10, 100) / 100f)
            val rawH = baseW * (tableHeightRatio.coerceIn(10, 100) / 100f)
            val (boundsW, boundsH) = computeBoundsSize(rawW, rawH, rotationCwDeg)
            val boundsMaxX = (imageBounds.width() - boundsW).coerceAtLeast(0f)
            val boundsMaxY = (imageBounds.height() - boundsH).coerceAtLeast(0f)
            val boundsLeftPx = boundsMaxX * (boundsOffsetX10000.coerceIn(0, 10000) / 10000f)
            val boundsTopPx = boundsMaxY * (boundsOffsetY10000.coerceIn(0, 10000) / 10000f)
            val boundsRect = boundsRectFromOffset(
                captureRect = imageBounds,
                boundsW = boundsW,
                boundsH = boundsH,
                boundsLeftPx = boundsLeftPx,
                boundsTopPx = boundsTopPx
            )
            val rawRect = rawRectFromBounds(boundsRect, rawW, rawH)
            TableRenderPlacement(
                anchor = anchor,
                offsetXRatio = offsetXRatio,
                offsetYRatio = offsetYRatio,
                tableWidthRatio = tableWidthRatio,
                tableHeightRatio = tableHeightRatio,
                rotationCwDeg = rotationCwDeg,
                overrideOffsetLeftPx = rawRect.left - imageBounds.left,
                overrideOffsetTopPx = rawRect.top - imageBounds.top,
            )
        } else {
            TableRenderPlacement(
                anchor = anchor,
                offsetXRatio = offsetXRatio,
                offsetYRatio = offsetYRatio,
                tableWidthRatio = tableWidthRatio,
                tableHeightRatio = tableHeightRatio,
                rotationCwDeg = rotationCwDeg,
            )
        }

        val resolvedLayout = computeResolvedRenderLayout(
            cells = cells,
            rows = rows,
            cols = cols,
            valueScale = valueScale,
            baseScaleRatio = tableWidthRatio,
            rootCells = buildRenderRootCells(templateCells),
        )
        val scene = buildRenderedTableSceneFromPlacement(
            bounds = imageBounds,
            placement = placement,
            rows = rows,
            cols = cols,
            rowWeights = resolvedLayout.finalRowWeights,
            colWeights = resolvedLayout.finalColWeights,
        )
        drawWatermarkTableOnCanvasWithResolvedGeometry(
            canvas = canvas,
            tableRect = scene.tableRect,
            rowEdges = scene.geometry.grid.rowEdges,
            colEdges = scene.geometry.grid.colEdges,
            cells = cells,
            rows = rows,
            cols = cols,
            bgAlpha = bgAlpha,
            valueScale = valueScale,
            textColorMode = textColorMode,
            manualTextColor = manualTextColor,
            textAlign = textAlign,
            bgStyle = bgStyle,
            drawGrid = drawGrid,
            rotationCwDeg = rotationCwDeg,
        )
        return out
    }
}
