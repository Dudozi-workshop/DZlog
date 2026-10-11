package com.dudoziworkshop.dzlog.feature.log.policy

import kotlin.math.abs

internal data class FilmstripItemBounds(val index: Int, val offset: Int, val size: Int)

internal fun nearestFilmstripIndex(
    viewportStart: Int,
    viewportEnd: Int,
    visibleItems: List<FilmstripItemBounds>,
): Int? {
    val center = (viewportStart.toFloat() + viewportEnd.toFloat()) / 2f
    return visibleItems.minByOrNull { abs(it.offset + it.size / 2f - center) }?.index
}

internal fun isPhotoInfoSwipe(deltaX: Float, deltaY: Float, threshold: Float): Boolean =
    deltaY <= -threshold && abs(deltaY) > abs(deltaX) * 1.25f
