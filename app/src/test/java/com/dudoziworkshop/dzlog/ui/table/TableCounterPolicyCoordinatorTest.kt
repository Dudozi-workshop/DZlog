package com.dudoziworkshop.dzlog.ui.table

import com.dudoziworkshop.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterPolicyCoordinator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableCounterPolicyCoordinatorTest {

    @Test
    fun same_stream_keeps_manual_override_flag() {
        val result = TableCounterPolicyCoordinator.resolveSeedForScope(
            input = TableCounterPolicyCoordinator.CounterSeedSyncInput(
                currentScopeSnapshot = CounterScopeSnapshot(
                    relativePathKey = "A",
                    prefix = "B",
                    scopeKey = "A|B",
                    includePathInScope = true,
                    includeFilenameInScope = true,
                ),
                isManualMode = true,
                hasCounterCell = true,
                currentSeed = 7,
                streamNext = 20,
                previousScopeSnapshot = CounterScopeSnapshot(
                    relativePathKey = "A",
                    prefix = "B",
                    scopeKey = "A|B",
                    includePathInScope = true,
                    includeFilenameInScope = true,
                ),
                preserveManualCounterSeed = true,
                manualSeedOverride = 11,
            )
        )

        assertFalse(result.shouldClearManualOverride)
    }

    @Test
    fun new_stream_clears_manual_override_flag() {
        val result = TableCounterPolicyCoordinator.resolveSeedForScope(
            input = TableCounterPolicyCoordinator.CounterSeedSyncInput(
                currentScopeSnapshot = CounterScopeSnapshot(
                    relativePathKey = "C",
                    prefix = "D",
                    scopeKey = "C|D",
                    includePathInScope = true,
                    includeFilenameInScope = true,
                ),
                isManualMode = true,
                hasCounterCell = true,
                currentSeed = 7,
                streamNext = 20,
                previousScopeSnapshot = CounterScopeSnapshot(
                    relativePathKey = "A",
                    prefix = "B",
                    scopeKey = "A|B",
                    includePathInScope = true,
                    includeFilenameInScope = true,
                ),
                preserveManualCounterSeed = true,
                manualSeedOverride = 11,
            )
        )

        assertTrue(result.shouldClearManualOverride)
    }

    @Test
    fun forced_new_scope_clears_manual_override_even_when_scope_key_is_same() {
        val sameSnapshot = CounterScopeSnapshot(
            relativePathKey = "A",
            prefix = "B",
            scopeKey = "A|B",
            includePathInScope = true,
            includeFilenameInScope = true,
        )
        val result = TableCounterPolicyCoordinator.resolveSeedForScope(
            input = TableCounterPolicyCoordinator.CounterSeedSyncInput(
                currentScopeSnapshot = sameSnapshot,
                isManualMode = true,
                hasCounterCell = true,
                currentSeed = 7,
                streamNext = 20,
                previousScopeSnapshot = sameSnapshot,
                preserveManualCounterSeed = true,
                manualSeedOverride = 11,
                forceTreatAsNewScope = true,
            )
        )

        assertTrue(result.shouldClearManualOverride)
    }
}
