package com.dudoziworkshop.dzlog.ui.camera.preview

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomBoundsPolicyTest {

    @Test
    fun min_zoom_never_goes_below_one() {
        val (minZoom, maxZoom) = resolveZoomBounds(minSupported = 0.6f, maxSupported = 5f)

        assertEquals(1f, minZoom, 0.0001f)
        assertEquals(2f, maxZoom, 0.0001f)
    }

    @Test
    fun min_zoom_tracks_supported_range_when_above_one() {
        val (minZoom, maxZoom) = resolveZoomBounds(minSupported = 1.2f, maxSupported = 1.8f)

        assertEquals(1.2f, minZoom, 0.0001f)
        assertEquals(1.8f, maxZoom, 0.0001f)
    }

    @Test
    fun min_zoom_is_clamped_to_max_when_camera_reports_invalid_range() {
        val (minZoom, maxZoom) = resolveZoomBounds(minSupported = 3f, maxSupported = 1.1f)

        assertEquals(1.1f, minZoom, 0.0001f)
        assertEquals(1.1f, maxZoom, 0.0001f)
    }
}
