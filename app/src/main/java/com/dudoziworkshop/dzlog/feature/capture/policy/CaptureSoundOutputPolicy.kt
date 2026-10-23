package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice

/** Exactly one requested sound for each exposure, with no double playback. */
internal enum class CaptureSoundOutput { OFF, SYSTEM, CUSTOM }

/**
 * Honor regional platform shutter requirements. Otherwise use the selected
 * DZlog sample and never add a platform click on top of it.
 */
internal fun captureSoundOutput(
    enabled: Boolean,
    selected: CaptureSoundChoice,
    platformShutterRequired: Boolean = false,
): CaptureSoundOutput = when {
    platformShutterRequired -> CaptureSoundOutput.SYSTEM
    !enabled -> CaptureSoundOutput.OFF
    selected.available -> CaptureSoundOutput.CUSTOM
    else -> CaptureSoundOutput.OFF
}
