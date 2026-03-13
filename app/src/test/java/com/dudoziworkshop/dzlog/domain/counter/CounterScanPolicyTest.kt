package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.data.counter.CounterScanTarget
import com.dudoziworkshop.dzlog.data.counter.toCounterScanTarget
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.naming.buildCounterPath
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
        val paths = CounterManager.buildScanPaths(
            baseRel = "Pictures/DZlog/",
            target = CounterScanTarget.BOTH
        )

        assertEquals(listOf("Pictures/DZlog/", "Pictures/DZlog/original/"), paths)
    }

    @Test
    fun `original target normalizes duplicate slash`() {
        val paths = CounterManager.buildScanPaths(
            baseRel = "Pictures//DZlog//",
            target = CounterScanTarget.ORIGINAL_ONLY
        )

        assertEquals(listOf("Pictures/DZlog/original/"), paths)
    }


    @Test
    fun `counter stream path shares water stream for BOTH`() {
        val water = buildCounterPath("Pictures/DZlog/", SaveMode.WATERMARK_ONLY)
        val both = buildCounterPath("Pictures/DZlog/", SaveMode.BOTH)

        assertEquals("Pictures/DZlog/", water)
        assertEquals(water, both)
    }

    @Test
    fun `counter stream path separates ORIGINAL_ONLY to original folder`() {
        val originalOnly = buildCounterPath("Pictures/DZlog/", SaveMode.ORIGINAL_ONLY)

        assertEquals("Pictures/DZlog/original/", originalOnly)
    }

    @Test
    fun `empty counters start from one`() {
        val next = CounterManager.nextFromUsed(emptySet())

        assertEquals(1, next)
    }

    @Test
    fun `next counter uses max plus one when counters are sequential`() {
        val next = CounterManager.nextFromUsed(setOf(1, 2, 3))

        assertEquals(4, next)
    }

    @Test
    fun `next counter uses max plus one when counters contain holes`() {
        val next = CounterManager.nextFromUsed(setOf(1, 2, 6, 8, 11, 12))

        assertEquals(13, next)
    }

    @Test
    fun `deleting last file lowers next to remaining max plus one`() {
        val next = CounterManager.nextFromUsed(setOf(1, 2))

        assertEquals(3, next)
    }
}
