package com.dudoziworkshop.dzlog.ui.navigation

import com.dudoziworkshop.dzlog.feature.log.policy.GalleryMediaAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Checks only the independent, current Android permission decision. */
class RequiredPermissionStepTest {
    @Test fun fullyGrantedPermissionsNeedNoRequest() {
        assertNull(
            nextRequiredSetupStep(
                RequiredPermissionState(camera = true, photos = GalleryMediaAccess.FULL),
                folderConnected = true,
            ),
        )
    }

    @Test fun missingCameraIsRequestedBeforeOtherPermissions() {
        assertEquals(
            RequiredSetupStep.CAMERA,
            nextRequiredSetupStep(
                RequiredPermissionState(camera = false, photos = GalleryMediaAccess.NOT_GRANTED),
                folderConnected = false,
            ),
        )
    }

    @Test fun selectedPhotosAreNotTreatedAsFullAccess() {
        assertEquals(
            RequiredSetupStep.PHOTOS,
            nextRequiredSetupStep(
                RequiredPermissionState(camera = true, photos = GalleryMediaAccess.SELECTED_ONLY),
                folderConnected = true,
            ),
        )
    }

    @Test fun folderConnectionIsOnlyRequestedAfterMediaPermissions() {
        assertEquals(
            RequiredSetupStep.FOLDER,
            nextRequiredSetupStep(
                RequiredPermissionState(camera = true, photos = GalleryMediaAccess.FULL),
                folderConnected = false,
            ),
        )
    }
}
