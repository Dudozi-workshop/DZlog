package com.dudoziworkshop.dzlog.feature.help.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.ui.camera.settings.CameraSettingsWriter
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFlashMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TutorialCameraReadOnlyTest {
    @Test fun everyCameraPreferenceWriterIsInertDuringGuide() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val normal = CameraSettingsWriter(context)
        normal.setZoomTenths(17)
        normal.setShowGrid(true)
        val before = context.dataStore.data.first().asMap()
        val guide = CameraSettingsWriter(context, readOnly = true)
        guide.setCaptureAspect(CaptureAspect.R1_1)
        guide.setSaveMode(SaveMode.BOTH)
        guide.setContinuousPreviewMode(ContinuousPreviewMode.OFF)
        guide.setShowGrid(false)
        guide.setShowWmPreview(false)
        guide.setVolumeKeyAction(VolumeKeyAction.CAPTURE)
        guide.setZoomTenths(10)
        guide.setFlashMode(CameraFlashMode.ON)
        guide.setAssistShutterEnabled(true)
        guide.setAssistShutterPositionRatio(0.2f, 0.3f)
        guide.setWmTableSizeAndPosition(30, 30, 4000, 5000)
        guide.setWmTableLocked(true)
        guide.setWmRotationCwDeg(90)
        guide.setWmCustomOffsetRatio(20, 30)
        guide.setWmCustomBoundsOffset10000(2000, 3000)
        assertEquals(before, context.dataStore.data.first().asMap())
    }
}
