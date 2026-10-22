package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice

/** Do not substitute or suppress a shutter the device requires by policy. */
internal enum class CaptureSoundOutput { OFF, SYSTEM, CUSTOM }

/**
 * All optional DZlog clicks, including the bundled default, share one
 * app-controlled audio path. Only a mandatory platform click uses SYSTEM.
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
