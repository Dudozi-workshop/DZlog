@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

/** Optional app haptic after AVFoundation accepts the shutter request. */
internal class IosCaptureFeedback {
    private val impact = UIImpactFeedbackGenerator(
        style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium,
    )

    fun prepare(enabled: Boolean) {
        if (enabled) impact.prepare()
    }

    fun play(enabled: Boolean) {
        if (enabled) impact.impactOccurred()
    }
}
