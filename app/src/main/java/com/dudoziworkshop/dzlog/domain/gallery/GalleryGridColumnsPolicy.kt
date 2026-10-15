package com.dudoziworkshop.dzlog.domain.gallery

/**
 * Shared grid-column rules for the regular gallery and camera-session quick results.
 *
 * A single gesture uses the cumulative scale measured from gesture start.
 * The caller anchors the touched photo and performs animation/gesture arbitration.
 */
object GalleryGridColumnsPolicy {
    const val DEFAULT_COLUMNS = 3
    const val MIN_COLUMNS = 2
    const val MAX_COLUMNS = 5

    // One discrete column step requires approximately an 18% pinch-scale change.
    private const val PINCH_STEP_SCALE = 1.18f

    fun normalize(columns: Int): Int = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS)

    /**
     * scale > 1: fingers spread, thumbnails grow, fewer columns.
     * scale < 1: fingers close, thumbnails shrink, more columns.
     * A continuous pinch can cross more than one step, within 2..5.
     */
    fun columnsForCumulativePinch(startColumns: Int, scale: Float): Int {
        var columns = normalize(startColumns)
        if (!scale.isFinite() || scale <= 0f) return columns

        var remaining = scale
        if (remaining >= PINCH_STEP_SCALE) {
            while (remaining >= PINCH_STEP_SCALE && columns > MIN_COLUMNS) {
                columns -= 1
                remaining /= PINCH_STEP_SCALE
            }
        } else if (remaining <= 1f / PINCH_STEP_SCALE) {
            while (remaining <= 1f / PINCH_STEP_SCALE && columns < MAX_COLUMNS) {
                columns += 1
                remaining *= PINCH_STEP_SCALE
            }
        }
        return columns
    }
}
