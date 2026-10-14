package com.dudoziworkshop.dzlog.feature.table.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedTemplateQuotaPolicyTest {
    @Test
    fun `when no size has been approved no creation restriction is activated`() {
        val decision = SavedTemplateQuotaPolicy.evaluate(
            currentCount = 25,
            action = TemplateCollectionAction.IMPORT,
            maxSavedCount = null,
            requestedNewCount = 11,
        )
        assertTrue(decision.allowed)
        assertEquals(null, decision.maxSavedCount)
    }

    @Test
    fun `proposed quota would prevent only new additions not existing data use`() {
        val hypotheticalLimit = 3 // Example in test, NOT configured as product policy
        for (action in listOf(
            TemplateCollectionAction.CREATE,
            TemplateCollectionAction.DUPLICATE,
            TemplateCollectionAction.IMPORT,
        )) {
            assertFalse(SavedTemplateQuotaPolicy.evaluate(
                currentCount = 5, action = action, maxSavedCount = hypotheticalLimit
            ).allowed)
        }
        for (action in listOf(
            TemplateCollectionAction.VIEW,
            TemplateCollectionAction.EDIT,
            TemplateCollectionAction.RENAME,
            TemplateCollectionAction.DELETE,
            TemplateCollectionAction.EXPORT,
        )) {
            assertTrue(SavedTemplateQuotaPolicy.evaluate(
                currentCount = 5, action = action, maxSavedCount = hypotheticalLimit
            ).allowed)
        }
        assertEquals(2, SavedTemplateQuotaPolicy.evaluate(
            currentCount = 5,
            action = TemplateCollectionAction.VIEW,
            maxSavedCount = hypotheticalLimit,
        ).excessExistingCount)
    }

    @Test
    fun `importing multiple actual new entries cannot partially exceed quota`() {
        val decision = SavedTemplateQuotaPolicy.evaluate(
            currentCount = 2,
            action = TemplateCollectionAction.IMPORT,
            maxSavedCount = 3,
            requestedNewCount = 2,
        )
        assertFalse(decision.allowed)
        assertEquals(2, decision.requestedNewCount)
        val allowed = SavedTemplateQuotaPolicy.evaluate(
            currentCount = 2,
            action = TemplateCollectionAction.IMPORT,
            maxSavedCount = 3,
            requestedNewCount = 1,
        )
        assertTrue(allowed.allowed)
    }

    @Test
    fun `fully deduplicated import remains allowed over a future quota`() {
        val result = SavedTemplateQuotaPolicy.evaluate(
            currentCount = 9,
            action = TemplateCollectionAction.IMPORT,
            maxSavedCount = 3,
            requestedNewCount = 0,
        )
        assertTrue(result.allowed)
        assertEquals(6, result.excessExistingCount)
    }

    @Test
    fun `retained starter catalog does not count as saved templates`() {
        // Bundled eleven definitions never count toward the user's persisted collection.
        val decision = SavedTemplateQuotaPolicy.evaluate(
            currentCount = 1,
            action = TemplateCollectionAction.CREATE,
            maxSavedCount = 3,
            requestedNewCount = 1,
        )
        assertTrue(decision.allowed)
    }
}
