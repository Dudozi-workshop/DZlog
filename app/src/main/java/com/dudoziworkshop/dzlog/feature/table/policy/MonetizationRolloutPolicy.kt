package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

/**
 * DZlog monetization roll-out is opt-in. The published initial build grants FULL access.
 * DO NOT expose this switch through local preferences or a user-controlled toggle.
 *
 * Switching to PAID_RELEASE requires: valid Play Console product, a purchase verifier,
 * entitlement restoration, and a separately tested store release. It never deletes data.
 */
enum class MonetizationRolloutMode {
    ALL_UNLOCKED,
    PAID_RELEASE,
}

object MonetizationRolloutConfig {
    val mode: MonetizationRolloutMode = MonetizationRolloutMode.ALL_UNLOCKED
    const val PRO_PRODUCT_ID = "dzlog_pro_lifetime"
    const val PRO_PLANNED_KRW_PRICE = 5_900

    // Product decision pending: specify both values before enabling PAID_RELEASE.
    // ALL_UNLOCKED ignores these quota limits completely.
    val freeSavedTemplateLimit: Int? = null
    val proSavedTemplateLimit: Int? = null

    val purchasesEnabled: Boolean get() = mode == MonetizationRolloutMode.PAID_RELEASE
    val showPaywall: Boolean get() = purchasesEnabled
}

data class MonetizationAccessResult(
    val slotEvaluation: SaveRuleSlotEvaluation,
    val quotaEvaluation: TemplateQuotaDecision,
    val purchaseUIVisible: Boolean,
    val effectiveTier: SaveRuleTier,
) {
    val canCaptureWithTemplate: Boolean get() = slotEvaluation.allowed
    val canAddSavedTemplates: Boolean get() = quotaEvaluation.allowed
}

object MonetizationRolloutPolicy {
    /**
     * ALL_UNLOCKED overrides all future Free/Pro restrictions and hides checkout.
     * In PAID_RELEASE, the caller MUST supply a purchase-server verified tier (not a
     * local boolean). 'null' means unverified => Free.
     */
    fun evaluate(
        template: TableTemplateState,
        currentSavedTemplateCount: Int,
        action: TemplateCollectionAction,
        mode: MonetizationRolloutMode = MonetizationRolloutConfig.mode,
        verifiedTier: SaveRuleTier? = null,
        freeSavedTemplateLimit: Int? = null,
        proSavedTemplateLimit: Int? = null,
        requestedNewCount: Int = 1,
    ): MonetizationAccessResult {
        val unlocked = mode == MonetizationRolloutMode.ALL_UNLOCKED
        val effectiveTier = when {
            unlocked -> SaveRuleTier.VERIFIED_PRO
            verifiedTier == SaveRuleTier.VERIFIED_PRO -> SaveRuleTier.VERIFIED_PRO
            else -> SaveRuleTier.FREE
        }
        val configuredQuota = if (unlocked) null
            else if (effectiveTier == SaveRuleTier.VERIFIED_PRO) proSavedTemplateLimit
            else freeSavedTemplateLimit
        return MonetizationAccessResult(
            slotEvaluation = SaveRuleSlotAccessPolicy.evaluate(template, effectiveTier),
            quotaEvaluation = SavedTemplateQuotaPolicy.evaluate(
                currentCount = currentSavedTemplateCount,
                action = action,
                maxSavedCount = configuredQuota,
                requestedNewCount = requestedNewCount,
            ),
            purchaseUIVisible = !unlocked,
            effectiveTier = effectiveTier,
        )
    }
}
