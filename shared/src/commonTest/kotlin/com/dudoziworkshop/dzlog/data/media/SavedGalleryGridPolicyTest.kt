package com.dudoziworkshop.dzlog.data.media

import kotlin.test.assertEquals
import kotlin.test.Test

class SavedGalleryGridPolicyTest {
    @Test
    fun defaultsAndLimitsMatchAndroid() {
        assertEquals(3, SavedGalleryGridPolicy.DEFAULT_COLUMNS)
        assertEquals(2, SavedGalleryGridPolicy.normalize(0))
        assertEquals(2, SavedGalleryGridPolicy.normalize(2))
        assertEquals(3, SavedGalleryGridPolicy.normalize(3))
        assertEquals(5, SavedGalleryGridPolicy.normalize(5))
        assertEquals(5, SavedGalleryGridPolicy.normalize(200))
    }

    @Test
    fun smallFingerMovementDoesNotChangeColumnCount() {
        assertEquals(3, SavedGalleryGridPolicy.columnsForCumulativePinch(3, 1.05f))
        assertEquals(3, SavedGalleryGridPolicy.columnsForCumulativePinch(3, 0.96f))
    }

    @Test
    fun spreadingFingersDecreasesColumns() {
        assertEquals(2, SavedGalleryGridPolicy.columnsForCumulativePinch(3, 1.20f))
        assertEquals(2, SavedGalleryGridPolicy.columnsForCumulativePinch(4, 1.41f))
    }

    @Test
    fun closingFingersIncreasesColumns() {
        assertEquals(4, SavedGalleryGridPolicy.columnsForCumulativePinch(3, 0.83f))
        assertEquals(5, SavedGalleryGridPolicy.columnsForCumulativePinch(3, 0.70f))
    }

    @Test
    fun pinchCannotExceedTwoToFiveColumns() {
        assertEquals(2, SavedGalleryGridPolicy.columnsForCumulativePinch(2, 100f))
        assertEquals(5, SavedGalleryGridPolicy.columnsForCumulativePinch(5, 0.001f))
        assertEquals(2, SavedGalleryGridPolicy.columnsForCumulativePinch(4, 100f))
        assertEquals(5, SavedGalleryGridPolicy.columnsForCumulativePinch(3, 0.001f))
    }

    @Test
    fun invalidScaleDoesNotChangeColumns() {
        assertEquals(3, SavedGalleryGridPolicy.columnsForCumulativePinch(3, 0f))
        assertEquals(3, SavedGalleryGridPolicy.columnsForCumulativePinch(3, Float.NaN))
        assertEquals(3, SavedGalleryGridPolicy.columnsForCumulativePinch(3, Float.POSITIVE_INFINITY))
        assertEquals(5, SavedGalleryGridPolicy.columnsForCumulativePinch(99, -1f))
    }

    @Test
    fun sameScaleAlwaysUsesGestureStartNotIntermediateColumnCount() {
        val start = 3
        assertEquals(4, SavedGalleryGridPolicy.columnsForCumulativePinch(start, 0.84f))
        assertEquals(4, SavedGalleryGridPolicy.columnsForCumulativePinch(start, 0.84f))
        assertEquals(3, SavedGalleryGridPolicy.columnsForCumulativePinch(start, 1f))
    }
}
