package com.dudoziworkshop.dzlog.ui.table.mock

internal fun editorPreviewFitScale(naturalWidth: Float, naturalHeight: Float,
    availableWidth: Float, availableHeight: Float): Float =
    minOf(availableWidth.coerceAtLeast(1f) / naturalWidth.coerceAtLeast(0.0001f),
        availableHeight.coerceAtLeast(1f) / naturalHeight.coerceAtLeast(0.0001f))
        .coerceAtLeast(0.0001f)

// No automatic zoom-in during a drag. Zoom-out is allowed only to keep growing geometry visible.
internal fun editorPreviewDragScale(fitScale: Float, gestureScale: Float?): Float =
    gestureScale?.let { minOf(it, fitScale) } ?: fitScale
