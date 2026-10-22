package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice

/** A custom shutter never falls back to an unexpectedly loud system click. */
internal enum class CaptureSoundOutput { OFF, SYSTEM, CUSTOM }

internal fun captureSoundOutput(
    enabled: Boolean,
    selected: CaptureSoundChoice,
): CaptureSoundOutput = when {
    !enabled -> CaptureSoundOutput.OFF
    selected == CaptureSoundChoice.SYSTEM -> CaptureSoundOutput.SYSTEM
    else -> CaptureSoundOutput.CUSTOM
}
