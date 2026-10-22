package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice

/** A required platform shutter may not be muted or substituted by a custom effect. */
internal enum class CaptureSoundOutput { OFF, SYSTEM, CUSTOM, SYSTEM_AND_CUSTOM }

/**
 * A required system click does not cancel the user's explicitly selected effect.
 * If a region requires the platform sound, both sounds play on the same capture
 * event. Muting the optional sound still keeps the mandatory click.
 */
internal fun captureSoundOutput(
    enabled: Boolean,
    selected: CaptureSoundChoice,
    platformShutterRequired: Boolean = false,
): CaptureSoundOutput = when {
    platformShutterRequired && enabled && selected.available -> CaptureSoundOutput.SYSTEM_AND_CUSTOM
    platformShutterRequired -> CaptureSoundOutput.SYSTEM
    !enabled -> CaptureSoundOutput.OFF
    selected.available -> CaptureSoundOutput.CUSTOM
    else -> CaptureSoundOutput.OFF
}
