package com.dudoziworkshop.dzlog.ui.camera.controls

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomPresetSelectionTest {
    @Test fun onlySupportedStopsAppear() {
        assertEquals(listOf(10, 20, 30), rememberZoomPresets(30))
        assertEquals(listOf(10, 20, 40, 100), rememberZoomPresets(100))
    }

    @Test fun minimumAndNonstandardMaximumHaveNoDuplicates() {
        assertEquals(listOf(10), rememberZoomPresets(10))
        assertEquals(listOf(10, 20, 40, 65), rememberZoomPresets(65))
    }
}
