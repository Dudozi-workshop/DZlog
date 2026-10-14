package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureReviewPolicyTest {
    @Test fun offDoesNotRequestPreview() {
        assertFalse(CaptureReviewPolicy.expectsSavedPreview(ContinuousPreviewMode.OFF))
    }

    @Test fun shortAndHoldRequireCommittedImageReview() {
        assertTrue(CaptureReviewPolicy.expectsSavedPreview(ContinuousPreviewMode.SHORT))
        assertTrue(CaptureReviewPolicy.expectsSavedPreview(ContinuousPreviewMode.HOLD))
    }

    @Test fun onlyMatchingCommittedReceiptMayOpenPreview() {
        assertFalse(CaptureReviewPolicy.shouldOpenSavedPreview(null, "capture-a"))
        assertFalse(CaptureReviewPolicy.shouldOpenSavedPreview("capture-a", "capture-b"))
        assertTrue(CaptureReviewPolicy.shouldOpenSavedPreview("capture-a", "capture-a"))
    }
}
