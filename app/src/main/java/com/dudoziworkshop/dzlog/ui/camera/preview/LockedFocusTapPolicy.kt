package com.dudoziworkshop.dzlog.ui.camera.preview

/**
 * A camera-preview tap always requests AF immediately. Preserve the user's
 * EV compensation while AE is locked; only an unlocked, newly selected
 * focus point resets EV to neutral.
 */
internal fun shouldResetExposureOnPreviewTap(locked: Boolean): Boolean = !locked
