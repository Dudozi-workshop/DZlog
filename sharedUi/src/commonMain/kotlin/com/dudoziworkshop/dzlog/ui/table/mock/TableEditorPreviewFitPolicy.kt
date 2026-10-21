package com.dudoziworkshop.dzlog.ui.table.mock

fun editorPreviewFitScale(naturalWidth: Float, naturalHeight: Float,
    availableWidth: Float, availableHeight: Float): Float =
    minOf(availableWidth.coerceAtLeast(1f) / naturalWidth.coerceAtLeast(0.0001f),
        availableHeight.coerceAtLeast(1f) / naturalHeight.coerceAtLeast(0.0001f))
        .coerceAtLeast(0.0001f)

// No automatic zoom-in during a drag. Zoom-out is allowed only to keep growing geometry visible.
fun editorPreviewDragScale(fitScale: Float, gestureScale: Float?): Float =
    gestureScale?.let { minOf(it, fitScale) } ?: fitScale

// The IME changes the viewport, not the table's editing magnification.
fun editorPreviewInputScale(fitScale: Float, unobscuredScale: Float?, locked: Boolean): Float =
    if (locked) unobscuredScale ?: fitScale else fitScale

fun editorPreviewSelectionScroll(top: Float, bottom: Float,
    viewportHeight: Float, contentHeight: Float): Float =
    ((top + bottom - viewportHeight) / 2f)
        .coerceIn(0f, (contentHeight - viewportHeight).coerceAtLeast(0f))
