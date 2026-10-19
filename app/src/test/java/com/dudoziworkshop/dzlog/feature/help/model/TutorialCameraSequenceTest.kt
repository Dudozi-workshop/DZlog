package com.dudoziworkshop.dzlog.feature.help.model

import org.junit.Assert.*
import org.junit.Test

class TutorialCameraSequenceTest {
    @Test fun sceneBoundariesClampAndRestartWithoutChangingNavigationPages() {
        assertEquals(CameraGuideFrame("shutter", 0f), cameraGuideFrame("saving", -1f))
        assertEquals(CameraGuideFrame("saving", 1f), cameraGuideFrame("saving", 2f))
        assertEquals(CameraGuideFrame("save-badge", 0f), cameraGuideFrame("saving", 1f / 3))
        assertEquals(CameraGuideFrame("shutter", 0f), cameraGuideFrame("saving", 0f))
        assertEquals(listOf("move", "resize-button", "size", "corner-size"), cameraGuideScenes("size"))
    }

    @Test fun legacySavedPositionsStayWithTheirMergedFeature() {
        assertEquals("undo", cameraGuideSavedPage("capture", "warning"))
        assertEquals("recent", cameraGuideSavedPage("capture", "result"))
        assertEquals("zoom", cameraGuideSavedPage("tools", "pinch"))
        assertEquals("table", cameraGuideSavedPage("tools", "table"))
        assertEquals("quick-date", cameraGuideSavedPage("table", "quick-date"))
        assertEquals("size", cameraGuideSavedPage("table", "move"))
        assertEquals("quality", cameraGuideSavedPage("settings", "capture-confirmation"))
        assertEquals("filename", cameraGuideSavedPage("settings", "counter"))
        assertEquals("unknown", cameraGuideSavedPage("table", "unknown"))
    }

    @Test fun cameraGuideStartsWithTopInformationAndThenTouchFocus() {
        val sessions = DzlogTutorialSections.area("camera")!!.sessions
        assertEquals(listOf("settings", "tools", "capture", "table"), sessions.map { it.id })
        assertEquals("filename", sessions.first().steps.first().id)
        assertEquals(listOf("menu", "focus"), sessions[1].steps.take(2).map { it.id })
        assertEquals(31, sessions.sumOf { it.steps.size })
        assertEquals(listOf("flash", "grid", "table", "assist", "zoom"), sessions[1].steps.drop(2).map { it.id })
        assertTrue(cameraGuideAtBottom("filename"))
        assertFalse(cameraGuideAtBottom("focus"))
        assertEquals(listOf("quick-text", "quick-number", "quick-date", "quick-phrase"), sessions.last().steps.takeLast(5).map { it.id })
        assertEquals(listOf("menu"), cameraGuideScenes("menu"))
        assertEquals(listOf("focus", "manual-focus"), cameraGuideScenes("focus"))
        assertEquals("focus", cameraGuideSavedPage("tools", "manual-focus"))
        assertEquals(listOf("flash"), cameraGuideScenes("flash"))
        assertEquals(listOf("grid"), cameraGuideScenes("grid"))
        assertEquals(listOf("table"), cameraGuideScenes("table"))
        assertEquals(listOf("quick-text", "quick-number", "quick-date", "quick-phrase"), cameraGuideScenes("quick-values"))
    }
}
