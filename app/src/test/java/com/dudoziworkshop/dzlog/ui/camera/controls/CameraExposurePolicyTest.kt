package com.dudoziworkshop.dzlog.ui.camera.controls

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraExposurePolicyTest {
    @Test fun negativeValuesRemainAvailableWhileAutoFocusIsIndependent() {
        assertEquals(-3, cameraExposureIndex(-3, true, -6, 6))
        assertEquals(-6, cameraExposureIndex(-20, true, -6, 6))
        assertEquals(6, cameraExposureIndex(20, true, -6, 6))
        assertEquals(0, cameraExposureIndex(-3, false, -6, 6))
        assertEquals(0, cameraExposureIndex(-3, true, 0, 0))
    }

    @Test fun labelUsesCameraEvStepNotRawIndex() {
        assertEquals("-1.0 EV", cameraExposureLabel(-3, 1f / 3f))
        assertEquals("+0.7 EV", cameraExposureLabel(2, 1f / 3f))
        assertEquals("+0.0 EV", cameraExposureLabel(0, 1f / 3f))
    }
}
