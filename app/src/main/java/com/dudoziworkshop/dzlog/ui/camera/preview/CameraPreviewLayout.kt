package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.feature.table.render.computeRatioOnlyTableShape
import com.dudoziworkshop.dzlog.watermark.computeBoundsSize

internal data class PreviewCaptureLayout(
    val contentRect: RectF,
    val captureRect: RectF,
)

internal fun calculatePreviewCaptureLayout(
    previewView: PreviewView,
    overlayWidth: Float,
    overlayHeight: Float,
    captureAspectRatio: Float,
): PreviewCaptureLayout {
    val contentRect = resolvePreviewContentRect(
        previewView = previewView,
        overlayWidth = overlayWidth,
        overlayHeight = overlayHeight,
    )
    val captureRect = computeCaptureAreaRect(
        contentRect = contentRect,
        captureAspectRatio = captureAspectRatio,
        usableRect = contentRect,
    )
    return PreviewCaptureLayout(
        contentRect = contentRect,
        captureRect = captureRect,
    )
}

internal data class PreviewBoxLayout(
    val safeAspect: Float,
    val topOffsetPx: Float,
)

internal fun calculatePreviewBoxLayout(
    parentWidthPx: Float,
    parentHeightPx: Float,
    captureAspect: CaptureAspect,
): PreviewBoxLayout {
    val safeAspect = captureAspect.ratioF.coerceAtLeast(0.01f)
    val previewWidthPx = parentWidthPx
    val centerY = if (previewWidthPx > 0f) previewWidthPx * 8f / 9f else 0f
    val rawTopCurrentPx = when (captureAspect) {
        CaptureAspect.R9_16 -> 0f
        CaptureAspect.R3_4 -> centerY - ((if (previewWidthPx > 0f) previewWidthPx * 4f / 3f else 0f) / 2f)
        CaptureAspect.R1_1 -> centerY - (previewWidthPx / 2f)
    }
    val heightCurrentPx = if (previewWidthPx > 0f) previewWidthPx / safeAspect else 0f
    val maxTopPx = (parentHeightPx - heightCurrentPx).coerceAtLeast(0f)
    return PreviewBoxLayout(
        safeAspect = safeAspect,
        topOffsetPx = rawTopCurrentPx.coerceIn(0f, maxTopPx),
    )
}

internal fun calculatePreviewWatermarkBaseBoundsOffsetPx(
    captureRect: RectF,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    rotationCwDeg: Int,
    previewBoundsOffsetX10000: Int,
    previewBoundsOffsetY10000: Int,
): Offset? {
    if (captureRect.width() <= 0f || captureRect.height() <= 0f) {
        return null
    }

    val cameraPreviewShape = computeRatioOnlyTableShape(
        tableWidthRatio = tableWidthRatio,
        tableHeightRatio = tableHeightRatio,
        maxWidthRatio = tableWidthRatio,
        maxHeightRatio = tableHeightRatio,
    )
    val baseW = captureRect.width()
    val rawW = baseW * (cameraPreviewShape.tableWidthRatio.coerceIn(10, 100) / 100f)
    val rawH = baseW * (cameraPreviewShape.tableHeightRatio.coerceIn(10, 100) / 100f)
    val (boundsW, boundsH) = computeBoundsSize(rawW, rawH, rotationCwDeg)
    val boundsMaxX = (captureRect.width() - boundsW).coerceAtLeast(0f)
    val boundsMaxY = (captureRect.height() - boundsH).coerceAtLeast(0f)
    val baseBoundsLeftPx = boundsMaxX * (previewBoundsOffsetX10000 / 10000f)
    val baseBoundsTopPx = boundsMaxY * (previewBoundsOffsetY10000 / 10000f)

    return Offset(baseBoundsLeftPx, baseBoundsTopPx)
}
