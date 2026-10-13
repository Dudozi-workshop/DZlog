package com.dudoziworkshop.dzlog.feature.capture.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureTemplateCommitPolicyTest {
    @Test fun receiptCanUpdateOnlyItsOriginalSavedTemplateId() {
        assertTrue(CaptureTemplateCommitPolicy.canPersist("template-a", "template-a"))
        assertFalse(CaptureTemplateCommitPolicy.canPersist("template-a", "template-b"))
        assertFalse(CaptureTemplateCommitPolicy.canPersist(null, "template-a"))
        assertFalse(CaptureTemplateCommitPolicy.canPersist("template-a", null))
    }

    @Test fun legacyReceiptKeepsBackwardReadCompatibilityButNewIdsStayStrict() {
        assertTrue(CaptureTemplateCommitPolicy.matchesReceipt(null, "template-a"))
        assertTrue(CaptureTemplateCommitPolicy.matchesReceipt("template-a", "template-a"))
        assertFalse(CaptureTemplateCommitPolicy.matchesReceipt("template-a", "template-b"))
    }
}
