package com.dudoziworkshop.dzlog.ui.navigation

import com.dudoziworkshop.dzlog.feature.log.policy.GalleryMediaAccess
import org.junit.Assert.*
import org.junit.Test

class RequiredPermissionStateTest {
    @Test fun revocationAndPartialAccessCloseGateAndFullAccessRestoresIt() {
        val granted = RequiredPermissionState(true, GalleryMediaAccess.FULL)
        assertTrue(granted.ready)
        assertFalse(granted.copy(photos = GalleryMediaAccess.NOT_GRANTED).ready)
        assertFalse(granted.copy(photos = GalleryMediaAccess.SELECTED_ONLY).ready)
        assertFalse(granted.copy(camera = false).ready)
        assertTrue(granted.copy(photos = GalleryMediaAccess.FULL).ready)
    }
}
