package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice

/** Route the app's optional feedback independently of the camera capture. */
internal enum class CaptureSoundOutput { OFF, SYSTEM, CUSTOM }

/**
 * Preserve the pre-DZQ-018 user-facing sound toggle:
 * OFF makes no app audio request, and an available selected effect plays once.
 * The platform requirement is independently recorded in capture diagnostics;
 * it does not prove that CameraX itself produced an audible shutter.
 */
internal fun captureSoundOutput(
    enabled: Boolean,
    selected: CaptureSoundChoice,
    platformShutterRequired: Boolean = false,
): CaptureSoundOutput = when {
    !enabled -> CaptureSoundOutput.OFF
    selected.available -> CaptureSoundOutput.CUSTOM
    platformShutterRequired -> CaptureSoundOutput.SYSTEM
    else -> CaptureSoundOutput.OFF
}
