package com.dudoziworkshop.dzlog.feature.help.model

import com.dudoziworkshop.dzlog.feature.capture.policy.CapturePendingProjection

/** Binding a guide must never clear the real capture session's pending/committed cache. */
internal fun tutorialCaptureProjection(live: CapturePendingProjection, readOnly: Boolean): CapturePendingProjection =
    if (readOnly) CapturePendingProjection() else live
