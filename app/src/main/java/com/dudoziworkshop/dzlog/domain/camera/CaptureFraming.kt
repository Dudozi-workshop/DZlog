package com.dudoziworkshop.dzlog.domain.camera

import android.graphics.RectF

private const val MIN_ASPECT = 0.01f
internal data class FramingResult(
    val captureRect: RectF,
    val anchorY: Float
)

internal fun computeAnchoredCaptureRect(
    contentRect: RectF,
    captureAspectRatio: Float
): FramingResult {
    if (contentRect.width() <= 0f || contentRect.height() <= 0f) {
        val empty = RectF(0f, 0f, 0f, 0f)
        return FramingResult(captureRect = empty, anchorY = 0f)
    }

    val centerY = contentRect.centerY()
    val targetRect = fitInsideRect(
        contentRect = contentRect,
        targetAspectRatio = captureAspectRatio,
        centerY = centerY
    )

    return FramingResult(captureRect = targetRect, anchorY = centerY)
}

private fun fitInsideRect(
    contentRect: RectF,
    targetAspectRatio: Float,
    centerY: Float
): RectF {
    val safeAspect = targetAspectRatio.coerceAtLeast(MIN_ASPECT)
    val contentWidth = contentRect.width()
    val contentHeight = contentRect.height()
    val contentAspect = contentWidth / contentHeight

    val (targetWidth, targetHeight) = if (contentAspect > safeAspect) {
        (contentHeight * safeAspect) to contentHeight
    } else {
        contentWidth to (contentWidth / safeAspect)
    }

    val left = contentRect.left + (contentWidth - targetWidth) / 2f
    val right = left + targetWidth

    val clampedCenter = centerY.coerceIn(
        contentRect.top + targetHeight / 2f,
        contentRect.bottom - targetHeight / 2f
    )
    val top = clampedCenter - targetHeight / 2f

    return RectF(left, top, right, top + targetHeight)
}
