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
        assertEquals(listOf("filename", "ratio", "quality", "capture-confirmation", "feedback", "controls"),
            camera.sessions.first().guidePages().map { it.id })
        assertEquals(listOf(6, 7, 5, 12), camera.sessions.map { it.guidePages().size })
        assertEquals(30, camera.sessions.sumOf { it.guidePages().size })
        val home = DzlogTutorialSections.area("home")!!
        // Home introduces destinations without opening their separate tutorials.
        assertEquals(listOf("main", "templates", "manage", "more", "counter", "start",
            "recent", "album", "table-settings", "save-info", "settings"),
            home.sessions.flatMap { it.guidePages() }.map { it.id })
    }
    @Test fun settingsUsesSharedV2FourSessionsAndTenStepsIncludingIntro() {
        val settings = DzlogTutorialSections.area("settings")!!
        assertEquals(listOf("environment", "capture", "data", "support"), settings.sessions.map { it.id })
        assertEquals(
            listOf("intro", "permission", "quality", "assist", "feedback", "warning",
                "export", "import", "help", "credits"),
            settings.sessions.flatMap { it.guidePages() }.map { it.id },
        )
        assertFalse(settings.sessions.flatMap { it.guidePages() }
            .any { it.title.contains("Pro") || it.title.contains("구매") })
    }
    @Test fun tableEditorHasApproved28PagesWithTypeChooserBeforeSixTypes() {
        val area = DzlogTutorialSections.area("table-detail")!!
        assertEquals(listOf(3, 8, 5, 6, 6), area.sessions.map { it.guidePages().size })
        assertEquals(28, area.sessions.sumOf { it.guidePages().size })
        assertEquals(listOf("cells", "cells/types", "cells/text", "cells/number",
            "cells/counter", "cells/date", "cells/time", "cells/rotation"),
            area.sessions[1].guidePages().map { it.id })
        assertEquals(listOf("structure", "structure/add", "structure/delete",
            "structure/merge", "structure/equal"), area.sessions[2].guidePages().map { it.id })
        assertEquals(listOf("save/template", "save/template-confirm", "save/undo", "save/redo", "save", "save/decision"),
            area.sessions.last().guidePages().map { it.id })
    }
    @Test fun galleryOverviewFollowsActualScreenOrder() {
        val pages = DzlogTutorialSections.area("gallery")!!.sessions.first().guidePages()
        assertEquals(listOf("home", "home/tabs", "home/recent", "home/folders",
            "browse", "browse/folders", "browse/photos"), pages.map { it.id })
        assertTrue(pages.all { it.title.isNotBlank() && it.instruction.isNotBlank() })
    }

    @Test fun galleryViewerGuideCoversAllPhotoDetailControls() {
        val viewer = DzlogTutorialSections.area("gallery")!!.sessions.first { it.id == "viewer" }
        assertEquals(listOf(
            "viewer", "viewer/photo", "viewer/menu", "viewer/original",
            "viewer/favorite", "viewer/share", "viewer/info", "viewer/delete",
            "viewer/delete-confirm", "viewer/filmstrip",
        ), viewer.guidePages().map { it.id })
    }

    @Test fun resumeUsesStableSceneIdsAndFallsBackAfterContentChanges() {
        val pages = DzlogTutorialSections.area("saving")!!.sessions.first { it.id == "counter" }.guidePages()
        val sync = pages.first { it.id == "counter/sync" }
        assertEquals(sync, pages.reversed()[tutorialPageIndex(pages.reversed(), sync.id)])
        assertEquals(0, tutorialPageIndex(pages, "removed-page"))
        assertEquals(0, tutorialPageIndex(pages, ""))
    }
}
