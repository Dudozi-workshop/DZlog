package com.example.dzlog.ui.camera.preview

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.zIndex
import com.example.dzlog.domain.model.CaptureRequest
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas

/**
 * [WatermarkPreviewOverlay]
 * - 목적: 촬영 화면에서 워터마크 미리보기만 렌더링함
 * - 입력: request, previewContentRect, enabled
 * - 제외: 저장/촬영/CameraX 제어 로직 금지(표시 전용)
 */
@Composable
fun WatermarkPreviewOverlay(
    enabled: Boolean,
    request: CaptureRequest,
    previewContentRect: RectF?
) {
    if (!enabled || previewContentRect == null) return

    val cells = request.watermarkCells

    Canvas(
        modifier = Modifier
            .zIndex(1f)
    ) {
        drawIntoCanvas { canvas ->
            drawWatermarkTableOnCanvas(
                canvas = canvas.nativeCanvas,
                bounds = previewContentRect,
                cells = cells,
                rows = request.tableTemplate.rows,
                cols = request.tableTemplate.cols,
                showLabel = request.watermark.showLabel,
                anchor = request.watermark.anchor,
                offsetXRatio = request.watermark.offsetXRatio,
                offsetYRatio = request.watermark.offsetYRatio,
                tableHeightRatio = request.watermark.tableHeightRatio,
                tableWidthRatio = request.watermark.tableWidthRatio,
                bgAlpha = request.watermark.tableBgAlpha,
                bgStyle = request.watermark.bgStyle,
                labelScale = request.watermark.labelScale,
                valueScale = request.watermark.valueScale
            )
        }
    }
}
