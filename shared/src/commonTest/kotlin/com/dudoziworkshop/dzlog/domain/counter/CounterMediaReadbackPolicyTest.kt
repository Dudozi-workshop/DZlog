package com.dudoziworkshop.dzlog.domain.counter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CounterMediaReadbackPolicyTest {
    @Test
    fun readsMatchingPhysicalPathAndNeverRollsCurrentCounterBack() {
        val records = listOf(
            CounterMediaRecord("Pictures/DZlog/A/", "plot_0007.jpg"),
            CounterMediaRecord("Pictures/DZlog/A", "plot_0012.jpeg"),
            CounterMediaRecord("Pictures/DZlog/A/original/", "plot_0099.jpg"),
            CounterMediaRecord("Pictures/DZlog/B/", "plot_0100.jpg"),
            CounterMediaRecord("Pictures/DZlog/A/", "other_0050.jpg"),
        )

        assertEquals(13, CounterMediaReadbackPolicy.resolveStableNext(
            records, "Pictures/DZlog/A/", "plot", "_", 3))
        assertEquals(20, CounterMediaReadbackPolicy.resolveStableNext(
            records, "Pictures/DZlog/A/|g2=enabled_empty", "plot", "_", 20))
    }

    @Test
    fun wildcardPathAndPrefixFollowSharedScopeRules() {
        val records = listOf(
            CounterMediaRecord("Pictures/DZlog/A/", "alpha_3.jpg"),
            CounterMediaRecord("Pictures/DZlog/B/", "beta_21.jpg"),
            CounterMediaRecord("Pictures/DZlog/B/", "beta_bad.jpg"),
        )
        assertEquals(setOf(3, 21), CounterMediaReadbackPolicy.usedCounters(records, "*", "*", "_"))
        assertEquals(22, CounterMediaReadbackPolicy.resolveStableNext(records, "*", "*", "_", 1))
    }

    @Test
    fun rotatingPrefixChoicesAndPaddingChangesAreReadSafely() {
        val encoded = CounterScanPrefixes.encode(listOf(listOf("T1"), listOf("처리 A", "처리 B")))
        val records = listOf(
            CounterMediaRecord("Pictures/DZlog/A/", "T1_처리 A_007.jpg"),
            CounterMediaRecord("Pictures/DZlog/A/", "T1_처리 B_0012.jpg"),
            CounterMediaRecord("Pictures/DZlog/A/", "T1_처리 C_099.jpg"),
        )
        assertEquals(setOf(7, 12), CounterMediaReadbackPolicy.usedCounters(
            records, "Pictures/DZlog/A/", encoded, "_"))
        assertNull(CounterMediaReadbackPolicy.parseCounter("T1_처리 A_bad.jpg", encoded, "_"))
    }

    @Test
    fun exhaustedSavedCounterFailsInsteadOfWrapping() {
        assertFailsWith<IllegalStateException> {
            CounterMediaReadbackPolicy.resolveStableNext(
                listOf(CounterMediaRecord("Pictures/DZlog/", "DZlog_${Int.MAX_VALUE}.jpg")),
                "Pictures/DZlog/", "DZlog", "_", 1,
            )
        }
    }
}
