package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
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
    val widthFraction: Float,
    val topOffsetPx: Float,
)

internal fun calculatePreviewBoxLayout(
    parentWidthPx: Float,
    parentHeightPx: Float,
    captureAspect: CaptureAspect,
): PreviewBoxLayout {
    val safeAspect = captureAspect.ratioF.coerceAtLeast(0.01f)
    val availableWidthPx = parentWidthPx.coerceAtLeast(0f)
    val availableHeightPx = parentHeightPx.coerceAtLeast(0f)
    // Landscape and multi-window previews must fit inside BOTH dimensions.
    // Portrait keeps the original full-width geometry.
    val previewWidthPx = minOf(availableWidthPx, availableHeightPx * safeAspect)
    val widthFraction = if (availableWidthPx > 0f) {
        (previewWidthPx / availableWidthPx).coerceIn(0f, 1f)
    } else 1f
    val centerY = previewWidthPx * 8f / 9f
    val rawTopCurrentPx = when (captureAspect) {
        CaptureAspect.R9_16 -> 0f
        CaptureAspect.R3_4 -> centerY - ((previewWidthPx * 4f / 3f) / 2f)
        CaptureAspect.R1_1 -> centerY - (previewWidthPx / 2f)
    }
    val heightCurrentPx = previewWidthPx / safeAspect
    val maxTopPx = (parentHeightPx - heightCurrentPx).coerceAtLeast(0f)
    return PreviewBoxLayout(
        safeAspect = safeAspect,
        widthFraction = widthFraction,
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
        hardMaxRatio = TableLayoutCalculator.CAMERA_MAX_RATIO,
    )
    val baseW = captureRect.width()
    val rawW = baseW * (cameraPreviewShape.tableWidthRatio.coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO) / 100f)
    val rawH = baseW * (cameraPreviewShape.tableHeightRatio.coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO) / 100f)
    val (boundsW, boundsH) = computeBoundsSize(rawW, rawH, rotationCwDeg)
    val boundsMaxX = (captureRect.width() - boundsW).coerceAtLeast(0f)
    val boundsMaxY = (captureRect.height() - boundsH).coerceAtLeast(0f)
    val baseBoundsLeftPx = boundsMaxX * (previewBoundsOffsetX10000 / 10000f)
    val baseBoundsTopPx = boundsMaxY * (previewBoundsOffsetY10000 / 10000f)

    return Offset(baseBoundsLeftPx, baseBoundsTopPx)
}
