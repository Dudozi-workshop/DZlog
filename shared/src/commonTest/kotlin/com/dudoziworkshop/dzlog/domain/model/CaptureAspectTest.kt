package com.dudoziworkshop.dzlog.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CaptureAspectTest {
    @Test fun centerCropKeepsRequestedPortraitRatioWithoutUpscaling() {
        assertEquals(2250 to 3000, CaptureAspect.R3_4.centerCropSize(4000, 3000))
        assertEquals(1688 to 3000, CaptureAspect.R9_16.centerCropSize(4000, 3000))
        assertEquals(3000 to 3000, CaptureAspect.R1_1.centerCropSize(4000, 3000))
        assertEquals(3000 to 4000, CaptureAspect.R3_4.centerCropSize(3000, 4000))
    }

    @Test fun unknownStoredValueUsesThreeByFourDefault() {
        assertEquals(CaptureAspect.R3_4, CaptureAspect.from(-1))
        assertEquals(CaptureAspect.R3_4, CaptureAspect.from(99))
    }
}
