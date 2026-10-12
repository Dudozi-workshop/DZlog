package com.dudoziworkshop.dzlog.ui.camera.preview

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomRestorePolicyTest {
    @Test fun restores_saved_zoom_using_hardware_bounds_after_reentry() {
        assertEquals(1.5f, restoredZoomRatio(15, 1f, 8f), 0.001f)
        // Previously the initial UI maximum could truncate a saved 4x request to 2x.
        assertEquals(4f, restoredZoomRatio(40, 1f, 8f), 0.001f)
    }

    @Test fun new_hardware_limits_clamp_request_without_changing_the_saved_preference() {
        assertEquals(2f, restoredZoomRatio(40, 0.6f, 2f), 0.001f)
        assertEquals(1f, restoredZoomRatio(10, 0.6f, 8f), 0.001f)
        assertEquals(1.2f, restoredZoomRatio(10, 1.2f, 8f), 0.001f)
    }
}
