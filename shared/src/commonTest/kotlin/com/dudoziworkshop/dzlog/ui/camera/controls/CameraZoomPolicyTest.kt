package com.dudoziworkshop.dzlog.ui.camera.controls

import kotlin.test.Test
import kotlin.test.assertEquals

class CameraZoomPolicyTest {
    @Test fun presetsFollowDeviceMaximumWithoutDuplicatingIt() {
        assertEquals(listOf(10, 20, 37), cameraZoomPresets(37))
        assertEquals(listOf(10, 20, 40, 100), cameraZoomPresets(100))
        assertEquals(listOf(10), cameraZoomPresets(7))
    }

    @Test fun neighborsUseTheSamePresetContractOnBothPlatforms() {
        assertEquals(20 to 40, cameraZoomNeighbors(27, 80))
        assertEquals(null to 20, cameraZoomNeighbors(10, 80))
        assertEquals(40 to 80, cameraZoomNeighbors(60, 80))
    }

    @Test fun tickWeightsAndLabelsArePlatformNeutral() {
        assertEquals(ZoomTickWeight.MAJOR, cameraZoomTickWeight(20))
        assertEquals(ZoomTickWeight.HALF, cameraZoomTickWeight(25))
        assertEquals(ZoomTickWeight.MINOR, cameraZoomTickWeight(27))
        assertEquals("2", cameraZoomTickLabel(20))
        assertEquals(null, cameraZoomTickLabel(25))
    }
}
