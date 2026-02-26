package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.counter.policy.CounterSeedInput
import com.dudoziworkshop.dzlog.domain.counter.policy.decideCounterSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterSeedPolicyTest {

    @Test
    fun new_stream_resyncs_to_stream_next() {
        val input = CounterSeedInput(
            streamNext = 20,
            currentSeed = 7,
            isNewStream = true,
            hasCounterCell = true,
            preserveManualSeed = true,
            manualSeedOverride = null,
        )

        val decision = decideCounterSeed(input)

        assertEquals(20, decision.desiredSeed)
        assertTrue(decision.shouldResyncForScopeChange)
        assertTrue(decision.shouldClearPreserveManualSeed)
    }

    @Test
    fun same_stream_keeps_manual_override() {
        val input = CounterSeedInput(
            streamNext = 20,
            currentSeed = 7,
            isNewStream = false,
            hasCounterCell = true,
            preserveManualSeed = true,
            manualSeedOverride = 11,
        )

        val decision = decideCounterSeed(input)

        assertEquals(11, decision.desiredSeed)
        assertFalse(decision.shouldResyncForScopeChange)
        assertFalse(decision.shouldClearPreserveManualSeed)
    }

    @Test
    fun same_stream_still_applies_lower_bound_with_stream_next() {
        val input = CounterSeedInput(
            streamNext = 15,
            currentSeed = 5,
            isNewStream = false,
            hasCounterCell = true,
            preserveManualSeed = false,
            manualSeedOverride = null,
        )

        val decision = decideCounterSeed(input)

        assertEquals(15, decision.desiredSeed)
        assertFalse(decision.shouldResyncForScopeChange)
    }

    @Test
    fun camera_template_seed_can_override_resolved_seed() {
        val input = CounterSeedInput(
            streamNext = 12,
            currentSeed = 9,
            isNewStream = true,
            hasCounterCell = true,
            preserveManualSeed = false,
            manualSeedOverride = null,
            templateCounterSeed = 50,
        )

        val decision = decideCounterSeed(input)

        assertEquals(50, decision.desiredSeed)
        assertTrue(decision.shouldResyncForScopeChange)
    }

    @Test
    fun counter_scope_parts_support_two_toggle_matrix() {
        val pathAndFilename = buildCounterScopeParts(relativePath = "A/B", prefix = "PFX", includePathInScope = true, includeFilenameInScope = true)
        assertEquals("A/B", pathAndFilename.relativePathKey)
        assertEquals("PFX", pathAndFilename.prefix)
        assertEquals("A/B|PFX", pathAndFilename.scopeKey)

        val pathOnly = buildCounterScopeParts(relativePath = "A/B", prefix = "PFX", includePathInScope = true, includeFilenameInScope = false)
        assertEquals("A/B", pathOnly.relativePathKey)
        assertEquals("name=off", pathOnly.prefix)
        assertEquals("A/B|name=off", pathOnly.scopeKey)

        val filenameOnly = buildCounterScopeParts(relativePath = "A/B", prefix = "PFX", includePathInScope = false, includeFilenameInScope = true)
        assertEquals("path=off", filenameOnly.relativePathKey)
        assertEquals("PFX", filenameOnly.prefix)
        assertEquals("path=off|PFX", filenameOnly.scopeKey)

        val global = buildCounterScopeParts(relativePath = "A/B", prefix = "PFX", includePathInScope = false, includeFilenameInScope = false)
        assertEquals("path=off", global.relativePathKey)
        assertEquals("name=off", global.prefix)
        assertEquals("path=off|name=off", global.scopeKey)
    }
}
