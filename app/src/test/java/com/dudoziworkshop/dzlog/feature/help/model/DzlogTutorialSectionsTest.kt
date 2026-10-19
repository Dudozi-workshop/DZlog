package com.dudoziworkshop.dzlog.feature.help.model

import org.junit.Assert.*
import org.junit.Test

class DzlogTutorialSectionsTest {
    @Test fun allApprovedAreasSessionsAndDetailedScenesAreReachable() {
        assertEquals(listOf("home", "camera", "gallery", "table-detail", "saving", "settings"),
            DzlogTutorialSections.areas.map { it.id })
        assertEquals(24, DzlogTutorialSections.areas.sumOf { it.sessions.size })
        DzlogTutorialSections.areas.forEach { area ->
            assertEquals(area.sessions.size, area.sessions.map { it.id }.toSet().size)
            area.sessions.forEach { session ->
                val pages = session.guidePages()
                assertFalse(pages.isEmpty())
                assertEquals(pages.size, pages.map { it.id }.toSet().size)
                pages.forEach { assertTrue(it.title.isNotBlank() && it.instruction.isNotBlank()) }
            }
        }
    }
    @Test fun cameraHasFourGroupsWithSettingsInScreenOrder() {
        val camera = DzlogTutorialSections.area("camera")!!
        assertEquals(listOf("settings", "tools", "capture", "table"), camera.sessions.map { it.id })
        assertEquals(listOf("filename", "ratio", "quality", "controls"),
            camera.sessions.first().guidePages().map { it.id })
        assertEquals(listOf(4, 8, 5, 9), camera.sessions.map { it.guidePages().size })
        assertEquals(26, camera.sessions.sumOf { it.guidePages().size })
        val home = DzlogTutorialSections.area("home")!!
        // Home introduces destinations without opening their separate tutorials.
        assertEquals(listOf("main", "templates", "manage", "more", "counter", "start",
            "recent", "album", "table-settings", "save-info", "settings"),
            home.sessions.flatMap { it.guidePages() }.map { it.id })
    }
    @Test fun resumeUsesStableSceneIdsAndFallsBackAfterContentChanges() {
        val pages = DzlogTutorialSections.area("saving")!!.sessions.first { it.id == "counter" }.guidePages()
        val sync = pages.first { it.id == "counter/sync" }
        assertEquals(sync, pages.reversed()[tutorialPageIndex(pages.reversed(), sync.id)])
        assertEquals(0, tutorialPageIndex(pages, "removed-page"))
        assertEquals(0, tutorialPageIndex(pages, ""))
    }
}
