package com.dudoziworkshop.dzlog.ui.camera.preview

// Shared focus-ring geometry. Touch targets are deliberately much larger
// than the visually rendered lock and sun icons.
internal const val FOCUS_RING_RADIUS_DP = 22
internal const val FOCUS_RING_COMPACT_RADIUS_DP = 20
internal const val FOCUS_LOCK_TOUCH_DP = 36
internal const val FOCUS_EV_WIDTH_DP = 116
internal const val FOCUS_EV_TOUCH_HEIGHT_DP = 36
internal const val FOCUS_EV_TRACK_INSET_DP = 8

internal data class FocusControlsVerticalPlacement(
    val lockTop: Float,
    val exposureTop: Float,
    val lockAtBottom: Boolean,
    val exposureAbove: Boolean,
)

internal fun visibleFocusCenter(value: Float, extent: Float, radius: Float): Float =
    if (extent <= radius * 2f) extent / 2f
    else value.coerceIn(radius, extent - radius)

/**
 * Place the lock ON the circular outline (12 o'clock, or 6 o'clock near
 * the top). The sun slider stays below the ring except at the bottom edge,
 * where it moves above. Its hit region must not collide with the lock's.
 */
internal fun placeFocusControlsVertically(
    focusY: Float,
    viewHeight: Float,
    ringRadius: Float,
    lockHeight: Float,
    exposureHeight: Float,
    gap: Float,
): FocusControlsVerticalPlacement {
    val centerY = visibleFocusCenter(focusY, viewHeight, ringRadius)
    val lockAtBottom = centerY - ringRadius - lockHeight / 2f < 0f
    val lockCenterY = centerY + if (lockAtBottom) ringRadius else -ringRadius
    val lockTop = (lockCenterY - lockHeight / 2f)
        .coerceIn(0f, (viewHeight - lockHeight).coerceAtLeast(0f))

    val below = centerY + ringRadius + gap + if (lockAtBottom) lockHeight / 2f else 0f
    val above = centerY - ringRadius - gap - exposureHeight -
        if (lockAtBottom) 0f else lockHeight / 2f
    val exposureAbove = below + exposureHeight > viewHeight && above >= 0f
    val candidate = if (exposureAbove) above else below
    val exposureTop = candidate.coerceIn(0f, (viewHeight - exposureHeight).coerceAtLeast(0f))

    return FocusControlsVerticalPlacement(
        lockTop = lockTop,
        exposureTop = exposureTop,
        lockAtBottom = lockAtBottom,
        exposureAbove = exposureAbove,
    )
}
