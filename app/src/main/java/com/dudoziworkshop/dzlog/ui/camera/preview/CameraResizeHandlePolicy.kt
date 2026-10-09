package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/** Select the visible-table corner nearest the center of the photographed area. */
internal enum class ResizeHandleCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

internal fun chooseResizeHandleCorner(table: RectF, photo: RectF): ResizeHandleCorner =
    chooseResizeHandleCorner(
        tableLeft = table.left,
        tableTop = table.top,
        tableRight = table.right,
        tableBottom = table.bottom,
        photoCenterX = photo.centerX(),
        photoCenterY = photo.centerY(),
    )

/** Pure Kotlin coordinates make this corner policy testable without Android graphics mocks. */
internal fun chooseResizeHandleCorner(
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

internal fun handleCornerPoint(table: RectF, corner: ResizeHandleCorner): Offset = when (corner) {
    ResizeHandleCorner.TOP_LEFT -> Offset(table.left, table.top)
    ResizeHandleCorner.TOP_RIGHT -> Offset(table.right, table.top)
    ResizeHandleCorner.BOTTOM_LEFT -> Offset(table.left, table.bottom)
    ResizeHandleCorner.BOTTOM_RIGHT -> Offset(table.right, table.bottom)
}

internal fun oppositeHandleCornerPoint(table: RectF, corner: ResizeHandleCorner): Offset = when (corner) {
    ResizeHandleCorner.TOP_LEFT -> Offset(table.right, table.bottom)
    ResizeHandleCorner.TOP_RIGHT -> Offset(table.left, table.bottom)
    ResizeHandleCorner.BOTTOM_LEFT -> Offset(table.right, table.top)
    ResizeHandleCorner.BOTTOM_RIGHT -> Offset(table.left, table.top)
}
