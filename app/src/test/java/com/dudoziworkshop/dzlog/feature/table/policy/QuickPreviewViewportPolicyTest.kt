package com.dudoziworkshop.dzlog.feature.table.policy

import org.junit.Assert.*
import org.junit.Test

class QuickPreviewViewportPolicyTest {
    @Test fun keyboardAndExpandedInputKeepRatioAndEveryEdgeWithinViewport() {
        for (aspect in listOf(0.2f, 2.3f, 20f)) for (height in listOf(600f, 240f, 90f)) {
            val fit = quickPreviewFit(300f, height, aspect)
            assertTrue(fit.first <= 300f + 0.01f)
            assertTrue(fit.second <= height + 0.01f)
            assertEquals(aspect, fit.first / fit.second, 0.001f)
        }
    }
    @Test fun widthLimitedTableDoesNotShrinkJustBecauseKeyboardOpens() {
        assertEquals(quickPreviewFit(300f, 600f, 4f), quickPreviewFit(300f, 100f, 4f))
    }
    @Test fun thumbMarksBothEndsAndOnlyAppearsForMeasuredOverflow() {
        assertNull(quickScrollThumb(100f, 0, 0, 20f))
        assertNull(quickScrollThumb(100f, Int.MAX_VALUE, 0, 20f))
        val start = quickScrollThumb(100f, 300, 0, 20f)!!
        val end = quickScrollThumb(100f, 300, 300, 20f)!!
        assertEquals(0f, start.first, 0.001f)
        assertEquals(100f, end.first + end.second, 0.001f)
        assertEquals(start.second, end.second, 0.001f)
        assertTrue(start.second >= 20f)
    }
}
