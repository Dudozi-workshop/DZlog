package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.data.counter.CounterScanTarget
import com.dudoziworkshop.dzlog.data.counter.toCounterScanTarget
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterScanPolicyTest {

    @Test
    fun `save mode maps to scan target by ssot rule`() {
        assertEquals(CounterScanTarget.ORIGINAL_ONLY, SaveMode.ORIGINAL_ONLY.toCounterScanTarget())
        assertEquals(CounterScanTarget.WATER_ONLY, SaveMode.WATERMARK_ONLY.toCounterScanTarget())
        assertEquals(CounterScanTarget.BOTH, SaveMode.BOTH.toCounterScanTarget())
    }

    @Test
    fun `both target returns water and original paths`() {
        val paths = CounterManager.computeScanRelativePaths(
            baseRel = "Pictures/DZlog/",
            target = CounterScanTarget.BOTH
        )

        assertEquals(listOf("Pictures/DZlog/", "Pictures/DZlog/original/"), paths)
    }

    @Test
    fun `original target normalizes duplicate slash`() {
        val paths = CounterManager.computeScanRelativePaths(
            baseRel = "Pictures//DZlog//",
            target = CounterScanTarget.ORIGINAL_ONLY
        )

        assertEquals(listOf("Pictures/DZlog/original/"), paths)
    }
}
