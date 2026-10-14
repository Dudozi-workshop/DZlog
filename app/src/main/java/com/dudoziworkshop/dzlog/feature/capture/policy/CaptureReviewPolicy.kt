package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode

/** The save pipeline is shared; only post-save image review varies by mode. */
internal object CaptureReviewPolicy {
    fun expectsSavedPreview(mode: ContinuousPreviewMode): Boolean =
        mode != ContinuousPreviewMode.OFF

    /** Only the exact reserved shot may open the review overlay on commit. */
    fun shouldOpenSavedPreview(pendingId: String?, committedId: String): Boolean =
        pendingId != null && pendingId == committedId
}
