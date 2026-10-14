package com.dudoziworkshop.dzlog.feature.table.policy

import java.util.concurrent.TimeUnit

/**
 * Business policy only. Purchase ownership must be authenticated by the Play/Billing adapter
 * before constructing [ProPurchaseEvidence.Verified]. Never create Verified from a local toggle.
 *
 * This policy does NOT persist evidence, query Play, acknowledge purchases or control capture.
 */
sealed interface ProPurchaseEvidence {
    /** Last successfully authenticated purchase; revalidation updates [lastVerifiedAtMillis]. */
    data class Verified(
        val lastVerifiedAtMillis: Long,
        /** Most recent trusted wall clock observed since verification (rollback defense). */
        val latestObservedAtMillis: Long = lastVerifiedAtMillis,
    ) : ProPurchaseEvidence

    /** Temporary store/network failure with no trusted prior verification. */
    data object Unknown : ProPurchaseEvidence
    data object NotOwned : ProPurchaseEvidence
    data object Pending : ProPurchaseEvidence
    data object Revoked : ProPurchaseEvidence
}

enum class ProAccessReason {
    VERIFIED_WITHIN_WINDOW,
    EXPIRED,
    CLOCK_ROLLBACK,
    UNVERIFIED,
    NOT_OWNED,
    PENDING,
    REVOKED,
}

data class ProAccessDecision(
    val tier: SaveRuleTier,
    val reason: ProAccessReason,
    /** A verified user's remaining grace interval, else zero. */
    val remainingGraceMillis: Long = 0L,
) {
    val isPro: Boolean get() = tier == SaveRuleTier.VERIFIED_PRO
}

object ProEntitlementAccessPolicy {
    const val OFFLINE_GRACE_DAYS: Long = 30
    val OFFLINE_GRACE_MILLIS: Long = TimeUnit.DAYS.toMillis(OFFLINE_GRACE_DAYS)

    /**
     * Offline fallback until the last verified purchase exceeds 30 days.
     * A known refund or revocation must be represented as Revoked, not Unknown.
     */
    fun evaluate(evidence: ProPurchaseEvidence, nowMillis: Long): ProAccessDecision =
        when (evidence) {
            is ProPurchaseEvidence.Verified -> {
                val invalidTime = evidence.lastVerifiedAtMillis <= 0L ||
                    nowMillis < evidence.lastVerifiedAtMillis ||
                    nowMillis < evidence.latestObservedAtMillis ||
                    evidence.latestObservedAtMillis < evidence.lastVerifiedAtMillis
                if (invalidTime) {
                    ProAccessDecision(SaveRuleTier.FREE, ProAccessReason.CLOCK_ROLLBACK)
                } else {
                    val age = nowMillis - evidence.lastVerifiedAtMillis
                    if (age <= OFFLINE_GRACE_MILLIS) {
                        ProAccessDecision(
                            SaveRuleTier.VERIFIED_PRO,
                            ProAccessReason.VERIFIED_WITHIN_WINDOW,
                            OFFLINE_GRACE_MILLIS - age,
                        )
                    } else {
                        ProAccessDecision(SaveRuleTier.FREE, ProAccessReason.EXPIRED)
                    }
                }
            }
            ProPurchaseEvidence.Unknown ->
                ProAccessDecision(SaveRuleTier.FREE, ProAccessReason.UNVERIFIED)
            ProPurchaseEvidence.NotOwned ->
                ProAccessDecision(SaveRuleTier.FREE, ProAccessReason.NOT_OWNED)
            ProPurchaseEvidence.Pending ->
                ProAccessDecision(SaveRuleTier.FREE, ProAccessReason.PENDING)
            ProPurchaseEvidence.Revoked ->
                ProAccessDecision(SaveRuleTier.FREE, ProAccessReason.REVOKED)
        }
}
