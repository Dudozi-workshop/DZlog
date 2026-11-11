package com.dudoziworkshop.dzlog.ui.camera.preview

/**
 * Positions the touch controls without clipping or overlapping the lock/EV controls.
 * Preferred layout: lock above the ring, EV below. At the top/bottom edge,
 * the controls stack together on the side with available vertical space.
 * This policy is view-independent and can be tested without Android.
 */
internal data class FocusControlsVerticalPlacement(
    val lockTop: Float,
    val exposureTop: Float,
)

internal fun placeFocusControlsVertically(
    focusY: Float,
    viewHeight: Float,
    ringRadius: Float,
    lockHeight: Float,
    exposureHeight: Float,
    gap: Float,
): FocusControlsVerticalPlacement {
    val lockAbove = focusY - ringRadius - gap - lockHeight
    val exposureBelow = focusY + ringRadius + gap
    if (lockAbove >= 0f && exposureBelow + exposureHeight <= viewHeight) {
        return FocusControlsVerticalPlacement(lockAbove, exposureBelow)
    }

    // Top edge: both controls below the ring.
    val stackedBelowLock = focusY + ringRadius + gap
    val stackedBelowExposure = stackedBelowLock + lockHeight + gap
    if (lockAbove < 0f && stackedBelowExposure + exposureHeight <= viewHeight) {
        return FocusControlsVerticalPlacement(stackedBelowLock, stackedBelowExposure)
    }

    // Bottom edge: lock above EV, both above the ring.
    val stackedAboveExposure = focusY - ringRadius - gap - exposureHeight
    val stackedAboveLock = stackedAboveExposure - gap - lockHeight
    if (exposureBelow + exposureHeight > viewHeight && stackedAboveLock >= 0f) {
        return FocusControlsVerticalPlacement(stackedAboveLock, stackedAboveExposure)
    }

    // Very short previews: keep the controls inside the bounds. The available
    // height may not permit avoiding the focus ring, but never overlap controls.
    val stackHeight = lockHeight + gap + exposureHeight
    if (viewHeight >= stackHeight) {
        val top = (focusY - stackHeight / 2f).coerceIn(0f, viewHeight - stackHeight)
        return FocusControlsVerticalPlacement(top, top + lockHeight + gap)
    }
    return FocusControlsVerticalPlacement(
        0f,
        (viewHeight - exposureHeight).coerceAtLeast(0f),
    )
}
