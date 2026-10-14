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
class SaveRuleSlotAccessPolicyTest {
    private fun manual(value: String) = TableEditorSlotDraft(
        kind = "MANUAL", label = "직접입력", manualText = value,
    )

    @Test
    fun `every starter is immediately usable without Pro`() {
        OnboardingStarterCatalog.definitions.forEach { def ->
            val state = requireNotNull(OnboardingStarterCatalog.buildState(def.id))
            val result = SaveRuleSlotAccessPolicy.evaluate(state, SaveRuleTier.FREE)
            assertTrue("${def.id} exceeds Free slots", result.allowed)
            assertEquals(3, result.fileNameLimit)
            assertEquals(3, result.pathLimit)
        }
    }

    @Test
    fun `Free requires both independent rules to fit within three slots`() {
        val base = requireNotNull(OnboardingStarterCatalog.buildState("work"))
        val fourFile = base.copy(
            fileNameSlotDrafts = base.fileNameSlotDrafts + manual("extra"),
        )
        val result = SaveRuleSlotAccessPolicy.evaluate(fourFile, SaveRuleTier.FREE)
        assertFalse(result.allowed)
        assertEquals(4, result.fileNameSlotCount)
        assertEquals(1, result.excessFileNameCount)
        assertEquals(0, result.excessPathCount)
        assertTrue(SaveRuleSlotAccessPolicy.evaluate(fourFile, SaveRuleTier.VERIFIED_PRO).allowed)

        val fourPath = base.copy(pathSlotDrafts = base.pathSlotDrafts +
            listOf(manual("extra1"), manual("extra2")))
        val pathResult = SaveRuleSlotAccessPolicy.evaluate(fourPath, SaveRuleTier.FREE)
        assertFalse(pathResult.allowed)
        assertEquals(0, pathResult.excessFileNameCount)
        assertEquals(1, pathResult.excessPathCount)
        assertTrue(SaveRuleSlotAccessPolicy.evaluate(fourPath, SaveRuleTier.VERIFIED_PRO).allowed)
    }

    @Test
    fun `evaluation never mutates or truncates existing rules`() {
        val base = requireNotNull(OnboardingStarterCatalog.buildState("basic"))
        val expanded = base.copy(
            fileNameSlotDrafts = base.fileNameSlotDrafts +
                listOf(manual("four"), manual("five")),
            pathSlotDrafts = listOf(
                manual("one"), manual("two"), manual("three"),
                manual("four"), manual("five"),
            ),
        )
        val before = expanded.toString()
        assertFalse(SaveRuleSlotAccessPolicy.evaluate(expanded, SaveRuleTier.FREE).allowed)
        assertTrue(SaveRuleSlotAccessPolicy.evaluate(expanded, SaveRuleTier.VERIFIED_PRO).allowed)
        assertEquals(before, expanded.toString())
        assertEquals(5, expanded.fileNameSlotDrafts.size)
        assertEquals(5, expanded.pathSlotDrafts.size)
    }
}
