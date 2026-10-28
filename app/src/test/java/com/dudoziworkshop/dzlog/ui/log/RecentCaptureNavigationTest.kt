package com.dudoziworkshop.dzlog.ui.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentCaptureNavigationTest {
    @Test fun rootFolderTitle() {
        assertEquals("DZlog", folderTitleFromRelativePath("Pictures/DZlog/"))
        assertEquals("DZlog", folderTitleFromRelativePath("Pictures/DZlog/original/"))
    }

    @Test fun folderTitlesRespectAnyDepthAndTrailingSlash() {
        assertEquals("Project", folderTitleFromRelativePath("Pictures/DZlog/Project/"))
        assertEquals("Project / Photos", folderTitleFromRelativePath("Pictures/DZlog/Project/Photos"))
        assertEquals("Project / Photos / Day1", folderTitleFromRelativePath("Pictures/DZlog/Project/Photos/Day1/"))
        assertEquals("Project / Photos / Day1", folderTitleFromRelativePath("Pictures/DZlog/Project/Photos/Day1/original"))
        assertEquals("Project", folderTitleFromRelativePath("Pictures/DZlog/Project/original/"))
    }

    @Test fun invalidFolderTitleFallsBackToRoot() {
        assertEquals("DZlog", folderTitleFromRelativePath("Pictures/Other/Folder"))
        assertEquals("DZlog", folderTitleFromRelativePath(""))
    }

    @Test fun originalSuffixRecognizedAtAnyDepth() {
        assertTrue(isOriginalRelativePath("Pictures/DZlog/original/"))
        assertTrue(isOriginalRelativePath("Pictures/DZlog/original"))
        assertTrue(isOriginalRelativePath("Pictures/DZlog/P/original/"))
        assertTrue(isOriginalRelativePath("Pictures/DZlog/P/Q/R/original"))
        assertFalse(isOriginalRelativePath("Pictures/DZlog/P/Q/R/"))
        assertFalse(isOriginalRelativePath("Pictures/DZlog/P/originals/"))
    }
}
