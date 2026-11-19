package com.dudoziworkshop.dzlog.ui.camera.controls

import java.util.Locale

/** CameraX uses integer compensation steps; do not confuse them with EV units. */
internal fun cameraExposureIndex(
    requested: Int,
    supported: Boolean,
    minimum: Int,
    maximum: Int,
): Int = if (!supported || minimum >= maximum) 0
    else requested.coerceIn(minimum, maximum)

internal fun cameraExposureLabel(index: Int, evPerStep: Float): String {
    val ev = if (evPerStep.isFinite() && evPerStep > 0f) index * evPerStep else 0f
    return String.format(Locale.US, "%+.1f EV", ev)
}
