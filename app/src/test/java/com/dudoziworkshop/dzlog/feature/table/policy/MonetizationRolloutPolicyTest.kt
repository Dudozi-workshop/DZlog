package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MonetizationRolloutPolicyTest {
    private val base = requireNotNull(OnboardingStarterCatalog.buildState("basic"))
    private fun manual(text: String) = TableEditorSlotDraft(
        kind = "MANUAL", label = "직접입력", manualText = text
    )
    private val fiveSlotTemplate = base.copy(
        fileNameSlotDrafts = base.fileNameSlotDrafts + listOf(manual("A"), manual("B")),
        pathSlotDrafts = (1..5).map { manual("segment$it") },
    )

    @Test
    fun `default release gives everyone full access without billing`() {
        assertEquals(MonetizationRolloutMode.ALL_UNLOCKED, MonetizationRolloutConfig.mode)
        assertFalse(MonetizationRolloutConfig.purchasesEnabled)
        assertFalse(MonetizationRolloutConfig.showPaywall)
        val result = MonetizationRolloutPolicy.evaluate(
            template = fiveSlotTemplate,
            currentSavedTemplateCount = 50,
            action = TemplateCollectionAction.DUPLICATE,
            verifiedTier = SaveRuleTier.FREE,
            freeSavedTemplateLimit = 3,
            proSavedTemplateLimit = 5,
        )
        assertTrue(result.canCaptureWithTemplate)
        assertTrue(result.canAddSavedTemplates)
        assertEquals(5, result.slotEvaluation.fileNameLimit)
        assertEquals(5, result.slotEvaluation.pathLimit)
        assertEquals(null, result.quotaEvaluation.maxSavedCount)
        assertFalse(result.purchaseUIVisible)
        assertEquals(SaveRuleTier.VERIFIED_PRO, result.effectiveTier)
    }

    @Test
    fun `paid release restricts five slot non owner without deleting template`() {
        val original = fiveSlotTemplate.toString()
        val outcome = MonetizationRolloutPolicy.evaluate(
            fiveSlotTemplate, 7, TemplateCollectionAction.CREATE,
            mode = MonetizationRolloutMode.PAID_RELEASE,
            verifiedTier = null, freeSavedTemplateLimit = 3,
            proSavedTemplateLimit = 5,
        )
        assertFalse(outcome.canCaptureWithTemplate)
        assertFalse(outcome.canAddSavedTemplates)
        assertTrue(outcome.purchaseUIVisible)
        assertEquals(SaveRuleTier.FREE, outcome.effectiveTier)
        assertEquals(original, fiveSlotTemplate.toString())
    }

    @Test
    fun `paid release grants five slots only to verified owners`() {
        val result = MonetizationRolloutPolicy.evaluate(
            fiveSlotTemplate, 3, TemplateCollectionAction.IMPORT,
            mode = MonetizationRolloutMode.PAID_RELEASE,
            verifiedTier = SaveRuleTier.VERIFIED_PRO,
            freeSavedTemplateLimit = 3,
            proSavedTemplateLimit = 5,
            requestedNewCount = 2
        )
        assertTrue(result.canCaptureWithTemplate)
        assertTrue(result.canAddSavedTemplates)
        assertEquals(SaveRuleTier.VERIFIED_PRO, result.effectiveTier)
    }

    @Test
    fun `eleven starters still work for free when monetization begins`() {
        OnboardingStarterCatalog.definitions.forEach { def ->
            val t = requireNotNull(OnboardingStarterCatalog.buildState(def.id))
            val outcome = MonetizationRolloutPolicy.evaluate(
                t, 1, TemplateCollectionAction.VIEW,
                mode = MonetizationRolloutMode.PAID_RELEASE,
                verifiedTier = SaveRuleTier.FREE,
                freeSavedTemplateLimit = 3,
                proSavedTemplateLimit = 5,
            )
            assertTrue(def.id, outcome.canCaptureWithTemplate)
        }
    }
}
