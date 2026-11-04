package com.dudoziworkshop.dzlog.poc

import kotlin.math.roundToInt

/** Photo-space bounds shared by the raster renderer and native gesture handles. */
internal data class IosTableBounds(
    val x: Float, val y: Float, val width: Float, val height: Float,
    val rawX: Float, val rawY: Float,
)

internal fun iosCaptureTableBounds(w: Int, h: Int, p: IosCaptureTablePlacement): IosTableBounds {
    require(w > 0 && h > 0)
    require(p.rotationCwDeg == 0 || (p.rotationCwDeg == 90 && p.anchor == IosTableAnchor.CUSTOM))
    val rawW = w * p.widthRatio / 100f
    val rawH = w * p.heightRatio / 100f
    val rotated = p.rotationCwDeg == 90
    val bw = if (rotated) rawH else rawW
    val bh = if (rotated) rawW else rawH
    val freeX = w - bw
    val freeY = h - bh
    require(freeX >= 0 && freeY >= 0) { "표가 사진 영역을 초과합니다." }
    val x = when (p.anchor) {
        IosTableAnchor.TOP_LEFT, IosTableAnchor.BOTTOM_LEFT -> 0f
        IosTableAnchor.TOP_RIGHT, IosTableAnchor.BOTTOM_RIGHT -> freeX
        IosTableAnchor.CUSTOM -> freeX * p.offsetXPercent / 100f
    }
    val y = when (p.anchor) {
        IosTableAnchor.TOP_LEFT, IosTableAnchor.TOP_RIGHT -> 0f
        IosTableAnchor.BOTTOM_LEFT, IosTableAnchor.BOTTOM_RIGHT -> freeY
        IosTableAnchor.CUSTOM -> freeY * p.offsetYPercent / 100f
    }
    return IosTableBounds(x, y, bw, bh, x + (bw - rawW) / 2f, y + (bh - rawH) / 2f)
}

private fun atPhotoPosition(p: IosCaptureTablePlacement, w: Int, h: Int, x: Float, y: Float): IosCaptureTablePlacement {
    val custom = p.copy(anchor = IosTableAnchor.CUSTOM, offsetXPercent = 0, offsetYPercent = 0)
    val box = iosCaptureTableBounds(w, h, custom)
    fun percent(position: Float, free: Float): Int =
        if (free <= 0f) 0 else (position.coerceIn(0f, free) / free * 100).roundToInt().coerceIn(0, 100)
    return custom.copy(offsetXPercent = percent(x, w - box.width), offsetYPercent = percent(y, h - box.height))
}

internal fun moveIosCaptureTable(p: IosCaptureTablePlacement, w: Int, h: Int, dx: Float, dy: Float): IosCaptureTablePlacement {
    require(dx.isFinite() && dy.isFinite())
    val box = iosCaptureTableBounds(w, h, p)
    return atPhotoPosition(p, w, h, box.x + dx, box.y + dy)
}

/** Uniform scaling around the displayed top-left, including CUSTOM 90-degree tables. */
internal fun resizeIosCaptureTable(p: IosCaptureTablePlacement, w: Int, h: Int, dx: Float, dy: Float): IosCaptureTablePlacement {
    require(dx.isFinite() && dy.isFinite())
    val box = iosCaptureTableBounds(w, h, p)
    val minScale = maxOf(10f / p.widthRatio, 10f / p.heightRatio)
    val maxScale = minOf(100f / p.widthRatio, 100f / p.heightRatio,
        (w - box.x) / box.width, (h - box.y) / box.height)
    val scale = (1f + (dx + dy) / (box.width + box.height)).coerceIn(minScale, maxOf(minScale, maxScale))
    val sized = p.copy(widthRatio = (p.widthRatio * scale).roundToInt().coerceIn(10, 100),
        heightRatio = (p.heightRatio * scale).roundToInt().coerceIn(10, 100))
    // Rounding can move a flush edge by less than one percent; clamp its position too.
    return runCatching { atPhotoPosition(sized, w, h, box.x, box.y) }.getOrDefault(p)
}
