package com.dudoziworkshop.dzlog.ui.camera.preview

import kotlin.math.abs
import kotlin.math.hypot

/** Select the visible-table corner nearest the center of the photographed area. */
enum class ResizeHandleCorner(val right: Boolean, val bottom: Boolean) {
    TOP_LEFT(false, false), TOP_RIGHT(true, false), BOTTOM_LEFT(false, true), BOTTOM_RIGHT(true, true)
}

/** Pure Kotlin coordinates make this corner policy testable without Android graphics mocks. */
fun chooseResizeHandleCorner(
    tableLeft: Float,
    tableTop: Float,
    tableRight: Float,
    tableBottom: Float,
    photoCenterX: Float,
    photoCenterY: Float,
): ResizeHandleCorner {
    val right = abs(tableRight - photoCenterX) <= abs(tableLeft - photoCenterX)
    val bottom = abs(tableBottom - photoCenterY) <= abs(tableTop - photoCenterY)
    return when {
        right && bottom -> ResizeHandleCorner.BOTTOM_RIGHT
        right -> ResizeHandleCorner.TOP_RIGHT
        bottom -> ResizeHandleCorner.BOTTOM_LEFT
        else -> ResizeHandleCorner.TOP_LEFT
    }
}

/** The pivot and initial touch stay in photo coordinates for the entire drag. */
class CameraResizeGesture(
    private val pivotX: Float,
    private val pivotY: Float,
    downX: Float,
    downY: Float,
) {
    private fun distance(x: Float, y: Float) =
        hypot((x - pivotX).toDouble(), (y - pivotY).toDouble()).toFloat()

    private val startDistance = distance(downX, downY).coerceAtLeast(1f)

    fun scaleAt(x: Float, y: Float): Float = (distance(x, y) / startDistance).coerceIn(0.1f, 10f)
}
