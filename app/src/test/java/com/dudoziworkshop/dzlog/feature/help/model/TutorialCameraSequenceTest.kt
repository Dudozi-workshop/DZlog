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
        assertEquals("assist", cameraGuideSavedPage("tools", "table"))
        assertEquals("quick", cameraGuideSavedPage("table", "quick-date"))
        assertEquals("size", cameraGuideSavedPage("table", "move"))
        assertEquals("quality", cameraGuideSavedPage("settings", "capture-confirmation"))
        assertEquals("filename", cameraGuideSavedPage("settings", "counter"))
        assertEquals("unknown", cameraGuideSavedPage("table", "unknown"))
    }

    @Test fun cardMovesOnlyAtTheTwoApprovedBoundaries() {
        val pages = DzlogTutorialSections.area("camera")!!.sessions.flatMap { it.guidePages() }
        val positions = pages.map { cameraGuideAtBottom(it.id) }
        assertEquals(2, positions.zipWithNext().count { (a, b) -> a != b })
        assertTrue(positions.first())
        assertTrue(positions.takeLast(4).all { it })
        assertTrue(positions.drop(1).dropLast(4).none { it })
    }
}
