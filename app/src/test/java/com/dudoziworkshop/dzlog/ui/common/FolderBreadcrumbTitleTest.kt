package com.dudoziworkshop.dzlog.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderBreadcrumbTitleTest {
    @Test fun shortPathsAreFullyVisible() {
        assertEquals("A / B / C", buildFolderBreadcrumbTitle("A / B / C", 20))
        assertEquals("A/B/C", buildFolderBreadcrumbTitle("A / B / C", 5))
        assertEquals("DZlog", buildFolderBreadcrumbTitle("DZlog", 16))
    }

    @Test fun longerPathsRetainTheCurrentFolderAndParentWhenPossible() {
        assertEquals("…/Photos/Day1", buildFolderBreadcrumbTitle("Project / Photos / Day1", 16))
        assertEquals("…/구역/촬영/2026-10", buildFolderBreadcrumbTitle("매우긴프로젝트 / 구역 / 촬영 / 2026-10", 15))
        assertEquals("…/Inspection", buildFolderBreadcrumbTitle("LongProject / Inspection", 13))
    }

    @Test fun veryNarrowTitlesStillShowTheCurrentFolder() {
        assertEquals("Longish…", buildFolderBreadcrumbTitle("P / LongishLeaf", 8))
        assertEquals("…", buildFolderBreadcrumbTitle("VeryLongName", 1))
        assertEquals("", buildFolderBreadcrumbTitle(" // / ", 10))
    }

    @Test fun returnedTextNeverExceedsTheConfiguredCharacterBudget() {
        val samples = listOf(
            "A", "A / B", "Project / Photos / Day1",
            "매우긴프로젝트 / 구역 / 촬영 / 2026-10",
            "A / VeryVeryVeryLongFolder", "",
        )
        for (sample in samples) {
            for (budget in 1..22) {
                assertTrue("$sample at budget $budget", buildFolderBreadcrumbTitle(sample, budget).length <= budget)
            }
        }
    }
}
