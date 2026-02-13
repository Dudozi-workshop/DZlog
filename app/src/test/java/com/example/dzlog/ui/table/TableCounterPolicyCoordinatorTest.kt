package com.example.dzlog.ui.table

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableCounterPolicyCoordinatorTest {

    @Test
    fun same_stream_keeps_manual_override_flag() {
        val result = TableCounterPolicyCoordinator.resolveSeedForScope(
            input = TableCounterPolicyCoordinator.CounterSeedSyncInput(
                currentScopeKey = "A|B",
                isManualMode = true,
                hasCounterCell = true,
                currentSeed = 7,
                streamNext = 20,
                previousScopeKey = "A|B",
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
                currentScopeKey = "C|D",
                isManualMode = true,
                hasCounterCell = true,
                currentSeed = 7,
                streamNext = 20,
                previousScopeKey = "A|B",
                preserveManualCounterSeed = true,
                manualSeedOverride = 11,
            )
        )

        assertTrue(result.shouldClearManualOverride)
    }
}
