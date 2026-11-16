package com.dudoziworkshop.dzlog.ui.table.mock

internal fun editorPreviewFitScale(naturalWidth: Float, naturalHeight: Float,
    availableWidth: Float, availableHeight: Float): Float =
    minOf(availableWidth.coerceAtLeast(1f) / naturalWidth.coerceAtLeast(0.0001f),
        availableHeight.coerceAtLeast(1f) / naturalHeight.coerceAtLeast(0.0001f))
        .coerceAtLeast(0.0001f)

// No automatic zoom-in during a drag. Zoom-out is allowed only to keep growing geometry visible.
internal fun editorPreviewDragScale(fitScale: Float, gestureScale: Float?): Float =
    gestureScale?.let { minOf(it, fitScale) } ?: fitScale

// The IME changes the viewport, not the table's editing magnification.
internal fun editorPreviewInputScale(fitScale: Float, unobscuredScale: Float?, locked: Boolean): Float =
    if (locked) unobscuredScale ?: fitScale else fitScale

/** Preserve manual/visible preview position across keyboard and tab transitions.
 * Reveal is needed on a newly selected cell or if the current cell is clipped.
 * A manual scroll for the same selection must never be overridden automatically.
 */
internal fun editorPreviewShouldReveal(
    isScrollable: Boolean,
    selectedId: String?,
    previouslyRevealedId: String?,
    selectedTop: Float,
    selectedBottom: Float,
    viewportHeight: Float,
    currentScroll: Float,
    manuallyScrolledSelected: Boolean,
    margin: Float = 8f,
): Boolean {
    if (!isScrollable || selectedId == null || manuallyScrolledSelected) return false
    if (selectedId != previouslyRevealedId) return true
    val viewport = viewportHeight.coerceAtLeast(1f)
    val inset = margin.coerceIn(0f, viewport / 4f)
    val top = minOf(selectedTop, selectedBottom)
    val bottom = maxOf(selectedTop, selectedBottom)
    return top < currentScroll + inset || bottom > currentScroll + viewport - inset
}

internal fun editorPreviewSelectionScroll(top: Float, bottom: Float,
    viewportHeight: Float, contentHeight: Float): Float =
    ((top + bottom - viewportHeight) / 2f)
        .coerceIn(0f, (contentHeight - viewportHeight).coerceAtLeast(0f))

/**
 * Move only enough to expose the selected cell and its next editable cell.
 * If both cells cannot fit at this scale, prioritize the selected cell and
 * reveal the start of the next cell when doing so does not clip the selection.
 *
 * All positions are in the same coordinate system (dp or px).
 */
internal fun editorPreviewCurrentNextScroll(
    selectedTop: Float,
    selectedBottom: Float,
    nextTop: Float?,
    nextBottom: Float?,
    viewportHeight: Float,
    contentHeight: Float,
    currentScroll: Float,
    margin: Float = 8f,
): Float {
    val viewport = viewportHeight.coerceAtLeast(1f)
    val limit = (contentHeight - viewport).coerceAtLeast(0f)
    val position = currentScroll.coerceIn(0f, limit)
    val inset = margin.coerceIn(0f, viewport / 4f)
    val selectedStart = minOf(selectedTop, selectedBottom)
    val selectedEnd = maxOf(selectedTop, selectedBottom)

    fun nearestVisibleScroll(start: Float, end: Float, scroll: Float): Float {
        val minimum = (end + inset - viewport).coerceIn(0f, limit)
        val maximum = (start - inset).coerceIn(0f, limit)
        return if (minimum <= maximum) scroll.coerceIn(minimum, maximum)
        else maximum // A single oversized cell: prioritize its leading edge.
    }

    if (nextTop != null && nextBottom != null) {
        val nextStart = minOf(nextTop, nextBottom)
        val nextEnd = maxOf(nextTop, nextBottom)
        val combinedStart = minOf(selectedStart, nextStart)
        val combinedEnd = maxOf(selectedEnd, nextEnd)
        if (combinedEnd - combinedStart <= viewport - 2f * inset) {
            return nearestVisibleScroll(combinedStart, combinedEnd, position)
        }

        val selectedPosition = nearestVisibleScroll(selectedStart, selectedEnd, position)
        if (nextStart >= selectedStart && selectedEnd - selectedStart <= viewport - 2f * inset) {
            // At least a glimpse of the next cell, without moving the selected
            // cell out of view. Never zoom out solely to make another cell fit.
            val peek = minOf(nextEnd - nextStart, maxOf(8f, viewport * 0.12f))
            val desired = (nextStart + peek + inset - viewport).coerceIn(0f, limit)
            val selectedMaximum = (selectedStart - inset).coerceIn(0f, limit)
            if (desired <= selectedMaximum) {
                return maxOf(selectedPosition, desired).coerceAtMost(selectedMaximum)
            }
        }
        return selectedPosition
    }

    return nearestVisibleScroll(selectedStart, selectedEnd, position)
}
