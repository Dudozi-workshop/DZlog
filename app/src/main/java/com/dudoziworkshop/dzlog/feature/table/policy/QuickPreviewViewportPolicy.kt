package com.dudoziworkshop.dzlog.feature.table.policy

/** Whole-table fit preserves the actual output ratio, even for tall/very wide tables. */
internal fun quickPreviewFit(width: Float, height: Float, aspect: Float): Pair<Float, Float> {
    val safeAspect = aspect.takeIf { it.isFinite() && it > 0f } ?: 1f
    val fittedWidth = minOf(width.coerceAtLeast(1f), height.coerceAtLeast(1f) * safeAspect)
    return fittedWidth to fittedWidth / safeAspect
}

/** Thumb start/length in a track; no cue when content fits or scroll range is unmeasured. */
internal fun quickScrollThumb(track: Float, maxScroll: Int, value: Int, minimum: Float): Pair<Float, Float>? {
    if (maxScroll <= 0 || maxScroll == Int.MAX_VALUE || track <= 0f) return null
    val length = (track * track / (track + maxScroll)).coerceIn(minOf(minimum, track), track)
    val start = (track - length) * value.coerceIn(0, maxScroll) / maxScroll
    return start to length
}
