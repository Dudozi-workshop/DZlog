@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.data.datastore.CaptureVibrationStrength
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

/** Optional app haptic after AVFoundation accepts the shutter request. */
internal class IosCaptureFeedback(strength: CaptureVibrationStrength) {
    private val impact = UIImpactFeedbackGenerator(
        style = when (strength) {
            CaptureVibrationStrength.LOW -> UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
            CaptureVibrationStrength.HIGH -> UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy
            else -> UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
        },
    )

    fun prepare(enabled: Boolean) {
        if (enabled) impact.prepare()
    }

    fun play(enabled: Boolean) {
        if (enabled) impact.impactOccurred()
    }
}
