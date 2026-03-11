package com.dudoziworkshop.dzlog.ui.table.counter

import org.junit.Assert.assertEquals
import org.junit.Test

class TableCounterSyncStabilityTest {

    @Test
    fun same_scope_prevents_counter_rollback() {
        val stable = stabilizeTableStreamNext(
            streamNext = 2,
            currentScopeNext = 3,
            isNewScope = false,
        )

        assertEquals(3, stable)
    }

    @Test
    fun new_scope_uses_new_scope_seed() {
        val stable = stabilizeTableStreamNext(
            streamNext = 1,
            currentScopeNext = 3,
            isNewScope = true,
        )

        assertEquals(1, stable)
    }
}
