package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryOriginalNavigationPolicyTest {
    @Test fun defaultStorageWithResultsReturnsToDefaultPhotoList() {
        val path = "Pictures/DZlog/original/"
        assertEquals(GalleryFolderIndexPolicy.ROOT,
            GalleryOriginalNavigationPolicy.returnFolderPath(path, parentHasResults = true))
        assertTrue(GalleryOriginalNavigationPolicy.returnToDefaultStorage(path, parentHasResults = true))
    }

    @Test fun defaultStorageWithOnlyOriginalsSkipsEmptyResultList() {
        val path = "Pictures/DZlog/original/"
        assertEquals(GalleryFolderIndexPolicy.ROOT,
            GalleryOriginalNavigationPolicy.returnFolderPath(path, parentHasResults = false))
        assertFalse(GalleryOriginalNavigationPolicy.returnToDefaultStorage(path, parentHasResults = false))
    }

    @Test fun nestedFolderWithResultsReturnsToItsResultGrid() {
        val path = "Pictures/DZlog/A/B/original/"
        assertEquals("Pictures/DZlog/A/B/",
            GalleryOriginalNavigationPolicy.returnFolderPath(path, parentHasResults = true))
        assertFalse(GalleryOriginalNavigationPolicy.returnToDefaultStorage(path, parentHasResults = true))
    }

    @Test fun nestedFolderWithOnlyOriginalsSkipsEmptyResultGrid() {
        val path = "Pictures/DZlog/A/B/original/"
        assertEquals("Pictures/DZlog/A/",
            GalleryOriginalNavigationPolicy.returnFolderPath(path, parentHasResults = false))
    }

    @Test fun topLevelFolderWithOnlyOriginalsReturnsToGalleryRoot() {
        assertEquals(GalleryFolderIndexPolicy.ROOT,
            GalleryOriginalNavigationPolicy.returnFolderPath("Pictures/DZlog/A/original/", false))
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonOriginalPathIsNotAccepted() {
        GalleryOriginalNavigationPolicy.returnFolderPath("Pictures/DZlog/A/", true)
    }
}
