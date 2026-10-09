package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.model.SaveMode
import org.junit.Assert.assertEquals
import org.junit.Test

class CapturePhysicalSavePathsTest {
    @Test
    fun originalOnlyUsesOriginalFolder() {
        assertEquals(
            listOf("Pictures/DZlog/T1/original/"),
            capturePhysicalSavePaths("Pictures/DZlog/T1/", SaveMode.ORIGINAL_ONLY),
        )
    }

    @Test
    fun watermarkOnlyUsesBaseFolder() {
        assertEquals(
            listOf("Pictures/DZlog/T1/"),
            capturePhysicalSavePaths("Pictures/DZlog/T1/", SaveMode.WATERMARK_ONLY),
        )
    }

    @Test
    fun bothListsBothPhysicalSaveFolders() {
        assertEquals(
            listOf("Pictures/DZlog/T1/", "Pictures/DZlog/T1/original/"),
            capturePhysicalSavePaths("Pictures/DZlog/T1/", SaveMode.BOTH),
        )
    }
}
