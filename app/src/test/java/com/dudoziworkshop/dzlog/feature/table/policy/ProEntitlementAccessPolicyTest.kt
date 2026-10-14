package com.dudoziworkshop.dzlog.feature.table.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ProEntitlementAccessPolicyTest {
    private val verifiedAt = 1_791_000_000_000L
    private fun days(n: Long) = TimeUnit.DAYS.toMillis(n)

    @Test
    fun `verified Pro remains active until inclusive day thirty only`() {
        val evidence = ProPurchaseEvidence.Verified(verifiedAt)
        for (day in listOf(0L, 1L, 29L, 30L)) {
            val outcome = ProEntitlementAccessPolicy.evaluate(evidence, verifiedAt + days(day))
            assertTrue("day $day unexpectedly restricted", outcome.isPro)
            assertEquals(ProAccessReason.VERIFIED_WITHIN_WINDOW, outcome.reason)
        }
        val expired = ProEntitlementAccessPolicy.evaluate(evidence, verifiedAt + days(31))
        assertFalse(expired.isPro)
        assertEquals(ProAccessReason.EXPIRED, expired.reason)
    }

    @Test
    fun `expiry applies immediately after day thirty boundary`() {
        val evidence = ProPurchaseEvidence.Verified(verifiedAt)
        assertTrue(ProEntitlementAccessPolicy.evaluate(
            evidence, verifiedAt + days(30)
        ).isPro)
        assertFalse(ProEntitlementAccessPolicy.evaluate(
            evidence, verifiedAt + days(30) + 1
        ).isPro)
    }

    @Test
    fun `unverified pending non ownership and revoked never grant Pro`() {
        val cases = listOf(
            ProPurchaseEvidence.Unknown to ProAccessReason.UNVERIFIED,
            ProPurchaseEvidence.NotOwned to ProAccessReason.NOT_OWNED,
            ProPurchaseEvidence.Pending to ProAccessReason.PENDING,
            ProPurchaseEvidence.Revoked to ProAccessReason.REVOKED,
        )
        cases.forEach { (evidence, reason) ->
            val decision = ProEntitlementAccessPolicy.evaluate(evidence, verifiedAt)
            assertEquals(SaveRuleTier.FREE, decision.tier)
            assertEquals(reason, decision.reason)
            assertEquals(0L, decision.remainingGraceMillis)
        }
    }

    @Test
    fun `clock rollback never extends entitlement by resetting wall time`() {
        val evidence = ProPurchaseEvidence.Verified(
            lastVerifiedAtMillis = verifiedAt,
            latestObservedAtMillis = verifiedAt + days(4),
        )
        val outcome = ProEntitlementAccessPolicy.evaluate(
            evidence, verifiedAt + days(1)
        )
        assertEquals(SaveRuleTier.FREE, outcome.tier)
        assertEquals(ProAccessReason.CLOCK_ROLLBACK, outcome.reason)
        assertFalse(ProEntitlementAccessPolicy.evaluate(
            ProPurchaseEvidence.Verified(lastVerifiedAtMillis = verifiedAt + days(3)),
            verifiedAt,
        ).isPro)
        assertFalse(ProEntitlementAccessPolicy.evaluate(
            ProPurchaseEvidence.Verified(lastVerifiedAtMillis = 0),
            verifiedAt,
        ).isPro)
    }

    @Test
    fun `slot rules only unlock with verified or valid cached Pro decision`() {
        val base = requireNotNull(
            com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog.buildState("work")
        )
        val extra = com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft(
            kind = "MANUAL", label = "직접입력", manualText = "extra",
        )
        val expanded = base.copy(fileNameSlotDrafts = base.fileNameSlotDrafts + extra)
        val valid = ProEntitlementAccessPolicy.evaluate(
            ProPurchaseEvidence.Verified(verifiedAt), verifiedAt + days(29)
        )
        val expired = ProEntitlementAccessPolicy.evaluate(
            ProPurchaseEvidence.Verified(verifiedAt), verifiedAt + days(31)
        )
        assertTrue(SaveRuleSlotAccessPolicy.evaluate(expanded, valid.tier).allowed)
        assertFalse(SaveRuleSlotAccessPolicy.evaluate(expanded, expired.tier).allowed)
        assertEquals(4, expanded.fileNameSlotDrafts.size)
    }
}
