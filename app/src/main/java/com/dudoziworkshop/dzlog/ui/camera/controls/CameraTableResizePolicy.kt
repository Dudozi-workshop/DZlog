package com.dudoziworkshop.dzlog.ui.camera.controls

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
    val w = base.widthRatio.coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO).toFloat()
    val h = base.heightRatio.coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO).toFloat()
    val rotated = base.rotationCwDeg % 180 != 0
    val photoHeightPercent = 100f / base.photoAspect.coerceAtLeast(0.01f)
    // Fit the rotated table inside both sides of the actual photo.
    val maxScale = minOf(
        TableLayoutCalculator.CAMERA_MAX_RATIO / w,
        TableLayoutCalculator.CAMERA_MAX_RATIO / h,
        100f / (if (rotated) h else w),
        photoHeightPercent / (if (rotated) w else h),
    )
    val minScale = maxOf(10f / w, 10f / h).coerceAtMost(maxScale)
    return minScale..maxScale
}

/** 100% means the largest proportional table fitting inside the photo. */
internal fun cameraTableFitPercent(base: CameraTableResizeBaseline, widthRatio: Int): Int =
    ((widthRatio.toFloat() / base.widthRatio.coerceAtLeast(1)) /
        cameraTableScaleRange(base).endInclusive * 100f).roundToInt().coerceIn(0, 100)

internal fun resizeCameraTableKeepingCenter(
    base: CameraTableResizeBaseline,
    scale: Float,
): CameraTableResizeResult {
    val range = cameraTableScaleRange(base)
    val appliedScale = scale.coerceIn(range.start, range.endInclusive)
    val oldW = base.widthRatio.coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO) / 100f
    val oldH = base.heightRatio.coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO) / 100f
    val photoHeightInWidths = 1f / base.photoAspect.coerceAtLeast(0.01f)
    val rotated = (base.rotationCwDeg % 180) != 0
    val maxW = minOf(TableLayoutCalculator.CAMERA_MAX_RATIO,
        if (rotated) (photoHeightInWidths * 100f).toInt() else 100)
    val maxH = minOf(TableLayoutCalculator.CAMERA_MAX_RATIO,
        if (rotated) 100 else (photoHeightInWidths * 100f).toInt())
    val newW = (base.widthRatio * appliedScale).roundToInt().coerceIn(10, maxW)
    val newH = (base.heightRatio * appliedScale).roundToInt().coerceIn(10, maxH)

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
