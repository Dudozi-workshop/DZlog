package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.watermark.boundsRectFromOffset
import com.dudoziworkshop.dzlog.watermark.computeBoundsSize
import com.dudoziworkshop.dzlog.watermark.computeWatermarkBoundsRect
import com.dudoziworkshop.dzlog.watermark.computeWatermarkTableLayout
import com.dudoziworkshop.dzlog.watermark.rawRectFromBounds
import com.dudoziworkshop.dzlog.watermark.drawWatermarkTableOnCanvas

/**
 * [WatermarkPreviewOverlay]
 * - 목적: 촬영 화면에서 워터마크 미리보기 렌더
 */
@Composable
fun WatermarkPreviewOverlay(
    enabled: Boolean,
    request: CaptureRequest,
    previewContentRect: RectF?,
    overrideOffsetPx: Offset?,
    isArmed: Boolean,
    onBoundsRectChange: (RectF?) -> Unit,
    onRawRectChange: (RectF?) -> Unit
) {
    if (!enabled || previewContentRect == null) {
        LaunchedEffect(enabled, previewContentRect) {
            onBoundsRectChange(null)
            onRawRectChange(null)
        }
        return
    }

    val layout = computeWatermarkTableLayout(
        bounds = previewContentRect,
        anchor = request.watermark.anchor,
        offsetXRatio = request.watermark.offsetXRatio,
        offsetYRatio = request.watermark.offsetYRatio,
        tableHeightRatio = request.watermark.tableHeightRatio,
        tableWidthRatio = request.watermark.tableWidthRatio
    )

    val baseRawRect = layout.rect
    val rawW = baseRawRect.width()
    val rawH = baseRawRect.height()
    val (boundsW, boundsH) = computeBoundsSize(rawW, rawH, request.watermark.rotationCwDeg)
    val boundsRect = if (request.watermark.anchor == WatermarkTableAnchor.CUSTOM && overrideOffsetPx != null) {
        boundsRectFromOffset(
            captureRect = previewContentRect,
            boundsW = boundsW,
            boundsH = boundsH,
            boundsLeftPx = overrideOffsetPx.x,
            boundsTopPx = overrideOffsetPx.y
        )
    } else {
        computeWatermarkBoundsRect(baseRawRect, request.watermark.rotationCwDeg)
    }
    val rawRect = rawRectFromBounds(boundsRect, rawW, rawH)
    val overrideRawLeftPx = rawRect.left - previewContentRect.left
    val overrideRawTopPx = rawRect.top - previewContentRect.top

    LaunchedEffect(
        rawRect.left,
        rawRect.top,
        rawRect.right,
        rawRect.bottom,
        boundsRect.left,
        boundsRect.top,
        boundsRect.right,
        boundsRect.bottom
    ) {
        onBoundsRectChange(RectF(boundsRect))
        onRawRectChange(RectF(rawRect))
    }

    val cells = request.watermarkCells

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
    ) {
        drawIntoCanvas { canvas ->
            drawWatermarkTableOnCanvas(
                canvas = canvas.nativeCanvas,
                bounds = previewContentRect,
                cells = cells,
                rows = request.tableTemplate.rows,
                cols = request.tableTemplate.cols,
                anchor = request.watermark.anchor,
                offsetXRatio = request.watermark.offsetXRatio,
                offsetYRatio = request.watermark.offsetYRatio,
                tableHeightRatio = request.watermark.tableHeightRatio,
                tableWidthRatio = request.watermark.tableWidthRatio,
                bgAlpha = request.watermark.tableBgAlpha,
                bgStyle = request.watermark.bgStyle,
                valueScale = request.watermark.valueScale,
                textColorMode = request.watermark.textColorMode,
                manualTextColor = request.watermark.manualTextColor,
                textAlign = request.watermark.textAlign,
                drawGrid = request.watermark.gridEnabled,
                rowWeights = request.tableTemplate.rowWeights,
                colWeights = request.tableTemplate.colWeights,
                overrideOffsetLeftPx = if (request.watermark.anchor == WatermarkTableAnchor.CUSTOM) overrideRawLeftPx else null,
                overrideOffsetTopPx = if (request.watermark.anchor == WatermarkTableAnchor.CUSTOM) overrideRawTopPx else null,
                rotationCwDeg = request.watermark.rotationCwDeg
            )
        }

        if (isArmed) {
            drawRect(
                color = DDZColor.Surface.copy(alpha = 0.85f),
                topLeft = Offset(boundsRect.left, boundsRect.top),
                size = androidx.compose.ui.geometry.Size(boundsRect.width(), boundsRect.height()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
