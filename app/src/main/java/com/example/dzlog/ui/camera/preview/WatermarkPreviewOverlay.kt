package com.example.dzlog.ui.camera.preview

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
import com.example.dzlog.domain.model.CaptureRequest
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.watermark.computeWatermarkTableLayout
import com.example.dzlog.watermark.computeWatermarkTableLayoutPx
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas

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
    onTableRectChange: (RectF?) -> Unit
) {
    if (!enabled || previewContentRect == null) {
        LaunchedEffect(enabled, previewContentRect) { onTableRectChange(null) }
        return
    }

    val layout = if (request.watermark.anchor == WatermarkTableAnchor.CUSTOM && overrideOffsetPx != null) {
        computeWatermarkTableLayoutPx(
            bounds = previewContentRect,
            anchor = request.watermark.anchor,
            offsetLeftPx = overrideOffsetPx.x,
            offsetTopPx = overrideOffsetPx.y,
            tableHeightRatio = request.watermark.tableHeightRatio,
            tableWidthRatio = request.watermark.tableWidthRatio
        )
    } else {
        computeWatermarkTableLayout(
            bounds = previewContentRect,
            anchor = request.watermark.anchor,
            offsetXRatio = request.watermark.offsetXRatio,
            offsetYRatio = request.watermark.offsetYRatio,
            tableHeightRatio = request.watermark.tableHeightRatio,
            tableWidthRatio = request.watermark.tableWidthRatio
        )
    }

    LaunchedEffect(layout.rect.left, layout.rect.top, layout.rect.right, layout.rect.bottom) {
        onTableRectChange(RectF(layout.rect))
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
                overrideOffsetLeftPx = overrideOffsetPx?.x,
                overrideOffsetTopPx = overrideOffsetPx?.y
            )
        }

        if (isArmed) {
            drawRect(
                color = DDZColor.Surface.copy(alpha = 0.85f),
                topLeft = Offset(layout.rect.left, layout.rect.top),
                size = androidx.compose.ui.geometry.Size(layout.rect.width(), layout.rect.height()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
