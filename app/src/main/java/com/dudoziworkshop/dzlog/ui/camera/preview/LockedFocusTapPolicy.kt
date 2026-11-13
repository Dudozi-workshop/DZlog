package com.dudoziworkshop.dzlog.ui.camera.preview

/** While locked, the first preview tap only reveals exposure controls.
 * The next preview tap, while those controls remain visible, selects a
 * different focus point without releasing AE/AF lock.
 */
internal enum class LockedFocusTapAction { SHOW_EXPOSURE, REFOCUS }

internal fun lockedFocusTapAction(
    controlsExpanded: Boolean,
    awaitingSecondTap: Boolean,
): LockedFocusTapAction =
    if (controlsExpanded && awaitingSecondTap) LockedFocusTapAction.REFOCUS
    else LockedFocusTapAction.SHOW_EXPOSURE
