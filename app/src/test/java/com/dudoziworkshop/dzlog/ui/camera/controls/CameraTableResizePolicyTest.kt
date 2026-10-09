package com.dudoziworkshop.dzlog.ui.camera.controls

import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraTableResizePolicyTest {
    private fun baseline(rotation: Int = 0) = CameraTableResizeBaseline(
        widthRatio = 40,
        heightRatio = 20,
        rotationCwDeg = rotation,
        anchor = WatermarkTableAnchor.CUSTOM,
        x10000 = 5000,
        y10000 = 5000,
        photoAspect = 3f / 4f,
    )

    @Test fun unchangedScaleKeepsSizeAndCenter() {
        val result = resizeCameraTableKeepingCenter(baseline(), 1f)
        assertEquals(40, result.widthRatio)
        assertEquals(20, result.heightRatio)
        assertEquals(5000, result.x10000)
        assertEquals(5000, result.y10000)
    }

    @Test fun sameScalePreservesTableAspect() {
        val result = resizeCameraTableKeepingCenter(baseline(), 1.5f)
        assertEquals(60, result.widthRatio)
        assertEquals(30, result.heightRatio)
        assertEquals(5000, result.x10000)
        assertEquals(5000, result.y10000)
    }

    @Test fun rotatedTableStaysWithinPhotoBounds() {
        val result = resizeCameraTableKeepingCenter(baseline(rotation = 90), 2f)
        assertEquals(80, result.widthRatio)
        assertEquals(40, result.heightRatio)
        assertTrue(result.x10000 in 0..10000)
        assertTrue(result.y10000 in 0..10000)
    }

    @Test fun topAlignedTableCannotMoveOutsidePhoto() {
        val initial = baseline().copy(anchor = WatermarkTableAnchor.TOP_LEFT)
        val result = resizeCameraTableKeepingCenter(initial, 2f)
        assertTrue(result.x10000 in 0..10000)
        assertTrue(result.y10000 in 0..10000)
    }
}
