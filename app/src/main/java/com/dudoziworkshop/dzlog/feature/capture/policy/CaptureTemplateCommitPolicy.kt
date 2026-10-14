package com.dudoziworkshop.dzlog.feature.capture.policy

/** Bound to the saved template ID at shutter time, never the later UI selection. */
internal object CaptureTemplateCommitPolicy {
    fun canPersist(expectedTemplateId: String?, activeTemplateId: String?): Boolean =
        !expectedTemplateId.isNullOrBlank() && expectedTemplateId == activeTemplateId

    /** Null identity belongs to the pre-ID journal schema and needs cautious recovery. */
    fun matchesReceipt(receiptTemplateId: String?, activeTemplateId: String?): Boolean =
        receiptTemplateId == null || canPersist(receiptTemplateId, activeTemplateId)
}
