package com.dudoziworkshop.dzlog.domain.counter
import org.junit.Assert.assertEquals
import org.junit.Test
class CounterScopePathPolicyTest {
    @Test fun pathSlotsDetermineCounterPath() {
        val result = buildCounterScope(emptyList(), emptyList(), 1, false,
            relativePathOverride = "Pictures/DZlog/pathSlotA/pathSlotB/")
        assertEquals("Pictures/DZlog/pathSlotA/pathSlotB/", result.relativePathKey)
    }
}
