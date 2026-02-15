package com.example.dzlog.domain.camera

import android.graphics.RectF

private const val MIN_ASPECT = 0.01f
private const val ANCHOR_ASPECT_9_16 = 9f / 16f

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

    val anchorRect = fitInsideRect(
        contentRect = contentRect,
        targetAspectRatio = ANCHOR_ASPECT_9_16,
        topAligned = true,
        centerY = null
    )
    val anchorY = anchorRect.centerY()

    val targetRect = fitInsideRect(
        contentRect = contentRect,
        targetAspectRatio = captureAspectRatio,
        topAligned = false,
        centerY = anchorY
    )

    return FramingResult(captureRect = targetRect, anchorY = anchorY)
}

private fun fitInsideRect(
    contentRect: RectF,
    targetAspectRatio: Float,
    topAligned: Boolean,
    centerY: Float?
): RectF {
    val safeAspect = targetAspectRatio.coerceAtLeast(MIN_ASPECT)
    val contentWidth = contentRect.width()
    val contentHeight = contentRect.height()
    val contentAspect = contentWidth / contentHeight

    val (targetWidth, targetHeight) = if (contentAspect > safeAspect) {
        val h = contentHeight
        val w = h * safeAspect
        w to h
    } else {
        val w = contentWidth
        val h = w / safeAspect
        w to h
    }

    val left = contentRect.left + (contentWidth - targetWidth) / 2f
    val right = left + targetWidth

    val top = if (topAligned) {
        contentRect.top
    } else {
        val desiredCenter = centerY ?: contentRect.centerY()
        val clampedCenter = desiredCenter.coerceIn(
            contentRect.top + targetHeight / 2f,
            contentRect.bottom - targetHeight / 2f
        )
        clampedCenter - targetHeight / 2f
    }

    val bottom = top + targetHeight
    return RectF(left, top, right, bottom)
}
