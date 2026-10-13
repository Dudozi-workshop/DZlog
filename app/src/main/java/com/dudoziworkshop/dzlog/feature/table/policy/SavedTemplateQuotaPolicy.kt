package com.dudoziworkshop.dzlog.feature.table.policy

/**
 * Quota policy only; enforcement is intentionally NOT wired until the user approves an exact N
 * and confirms whether Pro has the same limit. Existing templates must never be deleted.
 */
enum class TemplateCollectionAction { CREATE, DUPLICATE, IMPORT, VIEW, EDIT, RENAME, DELETE, EXPORT }

data class TemplateQuotaDecision(
    val allowed: Boolean,
    val currentCount: Int,
    val requestedNewCount: Int,
    val maxSavedCount: Int?,
    val excessExistingCount: Int,
)

object SavedTemplateQuotaPolicy {
    /**
     * [maxSavedCount] == null means policy not activated (N remains undecided).
     * [requestedNewCount] is the number of genuinely new items; import may deduplicate first.
     * Existing over-limit items remain accessible and exportable with no destructive migration.
     */
    fun evaluate(
        currentCount: Int,
        action: TemplateCollectionAction,
        maxSavedCount: Int?,
        requestedNewCount: Int = 1,
    ): TemplateQuotaDecision {
        require(currentCount >= 0)
        require(requestedNewCount >= 0)
        require(maxSavedCount == null || maxSavedCount >= 1)
        val nonCreatingAction = action in setOf(
            TemplateCollectionAction.VIEW,
            TemplateCollectionAction.EDIT,
            TemplateCollectionAction.RENAME,
            TemplateCollectionAction.DELETE,
            TemplateCollectionAction.EXPORT,
        )
        val allowed = maxSavedCount == null || nonCreatingAction ||
            requestedNewCount == 0 || requestedNewCount <= maxSavedCount - currentCount
        return TemplateQuotaDecision(
            allowed = allowed,
            currentCount = currentCount,
            requestedNewCount = if (nonCreatingAction) 0 else requestedNewCount,
            maxSavedCount = maxSavedCount,
            excessExistingCount = if (maxSavedCount == null) 0
                else (currentCount - maxSavedCount).coerceAtLeast(0),
        )
    }
}
