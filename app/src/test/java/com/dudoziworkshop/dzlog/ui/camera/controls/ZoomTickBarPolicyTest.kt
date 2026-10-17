package com.dudoziworkshop.dzlog.ui.camera.controls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZoomTickBarPolicyTest {
    @Test fun flatScaleUsesThreeTickLengths() {
        assertEquals(ZoomTickWeight.MAJOR, zoomTickWeight(10))
        assertEquals(ZoomTickWeight.MAJOR, zoomTickWeight(20))
        assertEquals(ZoomTickWeight.HALF, zoomTickWeight(15))
        assertEquals(ZoomTickWeight.MINOR, zoomTickWeight(14))
    }

    @Test fun labelsAppearOnlyAtWholeZoomMultipliers() {
        assertEquals("1", zoomTickLabel(10))
        assertEquals("2", zoomTickLabel(20))
        assertNull(zoomTickLabel(15))
        assertNull(zoomTickLabel(9))
    }
}
