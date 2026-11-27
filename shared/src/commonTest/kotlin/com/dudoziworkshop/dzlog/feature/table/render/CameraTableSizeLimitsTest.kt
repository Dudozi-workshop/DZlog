package com.dudoziworkshop.dzlog.feature.table.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraTableSizeLimitsTest {
    @Test
    fun longAxisUsesActualPhotoAspectAndSwapsOnRotation() {
        for ((width, height) in listOf(9 to 16, 3 to 4, 1 to 1, 4 to 3, 7 to 11)) {
            val photoHeightPercent = 100 * height / width
            val normal = CameraTableSizeLimits(width.toFloat() / height, 0)
            val turned = CameraTableSizeLimits(width.toFloat() / height, 90)
            assertEquals(100, normal.maxWidthRatio)
            assertEquals(photoHeightPercent, normal.maxHeightRatio)
            assertEquals(photoHeightPercent, turned.maxWidthRatio)
            assertEquals(100, turned.maxHeightRatio)
        }
    }

    @Test
    fun fittedTableFitsAfterChangingAspectAndCanBeLoadedAgain() {
        val tall = CameraTableSizeLimits(9f / 16, 90)
        assertEquals(TableShapeRatios(177, 89), tall.fit(80, 40, 10f))
        val square = CameraTableSizeLimits(1f, 90)
        val result = square.fit(177, 89)
        assertEquals(TableShapeRatios(100, 50), result)
        assertEquals(result, square.fit(result.tableWidthRatio, result.tableHeightRatio))
        assertTrue(tall.fit(80, 40, 0.01f).tableHeightRatio >= 10)
    }
}
