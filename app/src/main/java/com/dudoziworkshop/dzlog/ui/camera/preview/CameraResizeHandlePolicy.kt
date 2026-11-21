package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset

internal fun chooseResizeHandleCorner(table: RectF, photo: RectF): ResizeHandleCorner =
    chooseResizeHandleCorner(
        tableLeft = table.left,
        tableTop = table.top,
        tableRight = table.right,
        tableBottom = table.bottom,
        photoCenterX = photo.centerX(),
        photoCenterY = photo.centerY(),
    )

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
