package com.dudoziworkshop.dzlog.ui.navigation

import com.dudoziworkshop.dzlog.feature.log.policy.GalleryMediaAccess
import org.junit.Assert.*
import org.junit.Test

class RequiredPermissionStateTest {
    @Test fun onboardingPickerStartsAtPicturesButExistingFlowTargetsDzlog() {
        assertEquals("primary:Pictures", folderPickerStartDocumentId(onboarding = true))
        assertEquals("primary:Pictures/DZlog", folderPickerStartDocumentId(onboarding = false))
    }

    @Test fun revocationAndPartialAccessCloseGateAndFullAccessRestoresIt() {
        val granted = RequiredPermissionState(true, GalleryMediaAccess.FULL)
        assertTrue(granted.ready)
        assertFalse(granted.copy(photos = GalleryMediaAccess.NOT_GRANTED).ready)
        assertFalse(granted.copy(photos = GalleryMediaAccess.SELECTED_ONLY).ready)
        assertFalse(granted.copy(camera = false).ready)
        assertTrue(granted.copy(photos = GalleryMediaAccess.FULL).ready)
    }

    @Test fun setupAlwaysAdvancesFromCameraToPhotosToFolder() {
        val denied = RequiredPermissionState(false, GalleryMediaAccess.NOT_GRANTED)
        assertEquals(RequiredSetupStep.CAMERA, nextRequiredSetupStep(denied, false))
        assertEquals(RequiredSetupStep.PHOTOS, nextRequiredSetupStep(denied.copy(camera = true), false))
        assertEquals(
            RequiredSetupStep.PHOTOS,
            nextRequiredSetupStep(RequiredPermissionState(true, GalleryMediaAccess.SELECTED_ONLY), false),
        )
        assertEquals(
            RequiredSetupStep.FOLDER,
            nextRequiredSetupStep(RequiredPermissionState(true, GalleryMediaAccess.FULL), false),
        )
        assertNull(nextRequiredSetupStep(RequiredPermissionState(true, GalleryMediaAccess.FULL), true))
    }
}
