package com.dudoziworkshop.dzlog.ui.camera.controls

import com.dudoziworkshop.dzlog.feature.table.render.CameraTableSizeLimits
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import kotlin.math.roundToInt

/** Stored ratios remain photo-width percentages for backward compatibility. */
internal data class CameraTableResizeBaseline(
    val widthRatio: Int,
    val heightRatio: Int,
    val rotationCwDeg: Int,
    val anchor: WatermarkTableAnchor,
    val x10000: Int,
    val y10000: Int,
    val photoAspect: Float,
)

internal data class CameraTableResizeResult(
    val widthRatio: Int,
    val heightRatio: Int,
    val x10000: Int,
    val y10000: Int,
)

internal fun cameraTableScaleRange(base: CameraTableResizeBaseline): ClosedFloatingPointRange<Float> {
    return CameraTableSizeLimits(base.photoAspect, base.rotationCwDeg).scaleRange(base.widthRatio, base.heightRatio)
}

/** 100% means the largest proportional table fitting inside the photo. */
internal fun cameraTableFitPercent(base: CameraTableResizeBaseline, widthRatio: Int): Int =
    ((widthRatio.toFloat() / base.widthRatio.coerceAtLeast(1)) /
        cameraTableScaleRange(base).endInclusive * 100f).roundToInt().coerceIn(0, 100)

internal fun resizeCameraTableKeepingCenter(
    base: CameraTableResizeBaseline,
    scale: Float,
): CameraTableResizeResult {
    val (newW, newH) = CameraTableSizeLimits(base.photoAspect, base.rotationCwDeg)
        .fit(base.widthRatio, base.heightRatio, scale)
    val oldW = base.widthRatio.coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO) / 100f
    val oldH = base.heightRatio.coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO) / 100f
    val photoHeightInWidths = 1f / base.photoAspect.coerceAtLeast(0.01f)
    val rotated = (base.rotationCwDeg % 180) != 0
    val oldBoundsW = if (rotated) oldH else oldW
    val oldBoundsH = if (rotated) oldW else oldH
    val newBoundsW = (if (rotated) newH else newW) / 100f
    val newBoundsH = (if (rotated) newW else newH) / 100f

    val baseX = when (base.anchor) {
        WatermarkTableAnchor.TOP_LEFT, WatermarkTableAnchor.BOTTOM_LEFT -> 0f
        WatermarkTableAnchor.TOP_RIGHT, WatermarkTableAnchor.BOTTOM_RIGHT -> 1f
        WatermarkTableAnchor.CUSTOM -> base.x10000.coerceIn(0, 10000) / 10000f
    }
    val baseY = when (base.anchor) {
        WatermarkTableAnchor.TOP_LEFT, WatermarkTableAnchor.TOP_RIGHT -> 0f
        WatermarkTableAnchor.BOTTOM_LEFT, WatermarkTableAnchor.BOTTOM_RIGHT -> 1f
        WatermarkTableAnchor.CUSTOM -> base.y10000.coerceIn(0, 10000) / 10000f
    }

    val oldCenterX = oldBoundsW / 2f + (1f - oldBoundsW).coerceAtLeast(0f) * baseX
    val oldCenterY = oldBoundsH / 2f + (photoHeightInWidths - oldBoundsH).coerceAtLeast(0f) * baseY
    val maxX = (1f - newBoundsW).coerceAtLeast(0f)
    val maxY = (photoHeightInWidths - newBoundsH).coerceAtLeast(0f)

    val nextX = if (maxX <= 0f) 0f else (oldCenterX - newBoundsW / 2f).div(maxX).coerceIn(0f, 1f)
    val nextY = if (maxY <= 0f) 0f else (oldCenterY - newBoundsH / 2f).div(maxY).coerceIn(0f, 1f)
    return CameraTableResizeResult(
        widthRatio = newW,
        heightRatio = newH,
        x10000 = (nextX * 10000f).roundToInt(),
        y10000 = (nextY * 10000f).roundToInt(),
    )
}

/**
 * Normalize size when rotating or changing photo aspect without moving the
 * table's relative center. Rotation is purely a placement operation.
 */
internal fun fitCameraTableForPhotoChange(
    base: CameraTableResizeBaseline,
    newRotationCwDeg: Int = base.rotationCwDeg,
    newPhotoAspect: Float = base.photoAspect,
): CameraTableResizeResult {
    val oldPhotoHeight = 1f / base.photoAspect.coerceAtLeast(0.01f)
    val newPhotoHeight = 1f / newPhotoAspect.coerceAtLeast(0.01f)
    val oldRotated = base.rotationCwDeg % 180 != 0
    val oldWidth = (if (oldRotated) base.heightRatio else base.widthRatio) / 100f
    val oldHeight = (if (oldRotated) base.widthRatio else base.heightRatio) / 100f
    val normalizedX = when (base.anchor) {
        WatermarkTableAnchor.TOP_LEFT, WatermarkTableAnchor.BOTTOM_LEFT -> 0f
        WatermarkTableAnchor.TOP_RIGHT, WatermarkTableAnchor.BOTTOM_RIGHT -> 1f
        WatermarkTableAnchor.CUSTOM -> base.x10000.coerceIn(0, 10000) / 10000f
    }
    val normalizedY = when (base.anchor) {
        WatermarkTableAnchor.TOP_LEFT, WatermarkTableAnchor.TOP_RIGHT -> 0f
        WatermarkTableAnchor.BOTTOM_LEFT, WatermarkTableAnchor.BOTTOM_RIGHT -> 1f
        WatermarkTableAnchor.CUSTOM -> base.y10000.coerceIn(0, 10000) / 10000f
    }
    val centerX = oldWidth / 2f + (1f - oldWidth).coerceAtLeast(0f) * normalizedX
    val centerYFraction = (oldHeight / 2f +
        (oldPhotoHeight - oldHeight).coerceAtLeast(0f) * normalizedY) / oldPhotoHeight

    val nextBase = base.copy(rotationCwDeg = newRotationCwDeg, photoAspect = newPhotoAspect)
    val resized = resizeCameraTableKeepingCenter(nextBase, 1f)
    val rotated = newRotationCwDeg % 180 != 0
    val nextWidth = (if (rotated) resized.heightRatio else resized.widthRatio) / 100f
    val nextHeight = (if (rotated) resized.widthRatio else resized.heightRatio) / 100f
    val slackX = (1f - nextWidth).coerceAtLeast(0f)
    val slackY = (newPhotoHeight - nextHeight).coerceAtLeast(0f)
    val x = if (slackX <= 0f) 0f else ((centerX - nextWidth / 2f) / slackX).coerceIn(0f, 1f)
    val y = if (slackY <= 0f) 0f else ((centerYFraction * newPhotoHeight - nextHeight / 2f) / slackY)
        .coerceIn(0f, 1f)
    return resized.copy(x10000 = (x * 10000f).roundToInt(),
        y10000 = (y * 10000f).roundToInt())
}
