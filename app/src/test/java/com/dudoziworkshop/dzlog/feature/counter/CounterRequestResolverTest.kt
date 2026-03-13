package com.dudoziworkshop.dzlog.feature.counter

import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CounterRequestResolverTest {

    @Test
    fun `fromCamera watermark and both normalize to same stream inputs and effective mode`() {
        val watermark = CounterRequestResolver.fromCamera(
            saveMode = SaveMode.WATERMARK_ONLY,
            relativePathKey = "Pictures/DZlog/A/",
            prefix = "prefixA",
            scanPrefix = "scanA",
            includePathInScope = true,
            includeFilenameInScope = true,
        )
        val both = CounterRequestResolver.fromCamera(
            saveMode = SaveMode.BOTH,
            relativePathKey = "Pictures/DZlog/A/",
            prefix = "prefixA",
            scanPrefix = "scanA",
            includePathInScope = true,
            includeFilenameInScope = true,
        )

        assertEquals(watermark.relativePathKey, both.relativePathKey)
        assertEquals(watermark.prefix, both.prefix)
        assertEquals(watermark.scanPrefix, both.scanPrefix)
        assertEquals(watermark.effectiveSaveMode, both.effectiveSaveMode)
        assertEquals(SaveMode.WATERMARK_ONLY, both.effectiveSaveMode)
    }

    @Test
    fun `fromCamera original only normalizes to separate stream inputs and effective mode`() {
        val watermark = CounterRequestResolver.fromCamera(
            saveMode = SaveMode.WATERMARK_ONLY,
            relativePathKey = "Pictures/DZlog/A/",
            prefix = "prefixA",
            scanPrefix = "scanA",
            includePathInScope = true,
            includeFilenameInScope = true,
        )
        val original = CounterRequestResolver.fromCamera(
            saveMode = SaveMode.ORIGINAL_ONLY,
            relativePathKey = "Pictures/DZlog/A/",
            prefix = "prefixA",
            scanPrefix = "scanA",
            includePathInScope = true,
            includeFilenameInScope = true,
        )

        assertNotEquals(watermark.relativePathKey, original.relativePathKey)
        assertEquals("Pictures/DZlog/A/original/", original.relativePathKey)
        // 분리 축은 saveMode 문자열이 아니라 정규화된 입력 + effective mode 차이로 보장한다.
        assertEquals(watermark.prefix, original.prefix)
        assertEquals(watermark.scanPrefix, original.scanPrefix)
        assertEquals(SaveMode.WATERMARK_ONLY, watermark.effectiveSaveMode)
        assertEquals(SaveMode.ORIGINAL_ONLY, original.effectiveSaveMode)
    }

    @Test
    fun `fromTable follows same save mode normalization axis for path and effective mode`() {
        val both = CounterRequestResolver.fromTable(
            saveMode = SaveMode.BOTH,
            relativePathKey = "Pictures/DZlog/B/",
            prefix = "prefixB",
            scanPrefix = "scanB",
            includePathInScope = true,
            includeFilenameInScope = true,
            tableTemplateId = "template-1",
        )
        val original = CounterRequestResolver.fromTable(
            saveMode = SaveMode.ORIGINAL_ONLY,
            relativePathKey = "Pictures/DZlog/B/",
            prefix = "prefixB",
            scanPrefix = "scanB",
            includePathInScope = true,
            includeFilenameInScope = true,
            tableTemplateId = "template-1",
        )

        assertEquals("Pictures/DZlog/B/", both.relativePathKey)
        assertEquals("Pictures/DZlog/B/original/", original.relativePathKey)
        assertNotEquals(both.relativePathKey, original.relativePathKey)
        assertEquals(SaveMode.WATERMARK_ONLY, both.effectiveSaveMode)
        assertEquals(SaveMode.ORIGINAL_ONLY, original.effectiveSaveMode)
    }

    @Test
    fun `fromHome applies same save mode normalization contract`() {
        val scope = CounterScope(
            relativePathKey = "Pictures/DZlog/H/",
            streamPrefix = "homePrefix",
            nextCounter = 1,
            isManualMode = false,
        )

        val water = CounterRequestResolver.fromHome(
            counterScope = scope,
            saveMode = SaveMode.WATERMARK_ONLY,
            scanPrefix = "homeScan",
            includePathInScope = true,
            includeFilenameInScope = true,
        )
        val both = CounterRequestResolver.fromHome(
            counterScope = scope,
            saveMode = SaveMode.BOTH,
            scanPrefix = "homeScan",
            includePathInScope = true,
            includeFilenameInScope = true,
        )
        val original = CounterRequestResolver.fromHome(
            counterScope = scope,
            saveMode = SaveMode.ORIGINAL_ONLY,
            scanPrefix = "homeScan",
            includePathInScope = true,
            includeFilenameInScope = true,
        )

        assertEquals(water.relativePathKey, both.relativePathKey)
        assertEquals(SaveMode.WATERMARK_ONLY, water.effectiveSaveMode)
        assertEquals(SaveMode.WATERMARK_ONLY, both.effectiveSaveMode)

        assertNotEquals(water.relativePathKey, original.relativePathKey)
        assertEquals(SaveMode.ORIGINAL_ONLY, original.effectiveSaveMode)
    }

}
