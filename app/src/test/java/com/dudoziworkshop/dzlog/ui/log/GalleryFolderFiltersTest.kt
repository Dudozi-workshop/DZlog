package com.dudoziworkshop.dzlog.ui.log

import com.dudoziworkshop.dzlog.feature.log.policy.GalleryChildFolder
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryFolderFiltersTest {
    private val empty = GalleryChildFolder("Empty", "Pictures/DZlog/Empty/", 0)
    private val originals = GalleryChildFolder("Originals", "Pictures/DZlog/Originals/", 2)
    private val nested = GalleryChildFolder("Nested", "Pictures/DZlog/Nested/", 3)
    private val folders = listOf(empty, originals, nested)
    private val summaries = mapOf(
        originals.relativePath to GalleryFolderSummary(2, 0, 2, 0, 300L, "2"),
        nested.relativePath to GalleryFolderSummary(3, 0, 0, 1, 100L, "3"),
    )

    @Test fun originalOnlyAndDescendantPhotosDoNotCountAsEmpty() {
        assertEquals(listOf(nested, originals), filterGalleryFolders(folders, summaries, GalleryFolderFilter.WITH_PHOTOS, GalleryFolderSort.NAME))
        assertEquals(listOf(empty), filterGalleryFolders(folders, summaries, GalleryFolderFilter.EMPTY, GalleryFolderSort.NAME))
    }

    @Test fun recentSortPutsUndatedEmptyFoldersLastAndDoesNotChangeInput() {
        assertEquals(listOf(originals, nested, empty), filterGalleryFolders(folders, summaries, GalleryFolderFilter.ALL, GalleryFolderSort.RECENT))
        assertEquals(listOf(empty, originals, nested), folders)
    }

    @Test fun unknownSummaryUsesIndexedCountAndDatesTieByName() {
        assertEquals(listOf(nested, originals), filterGalleryFolders(folders, emptyMap(), GalleryFolderFilter.WITH_PHOTOS, GalleryFolderSort.RECENT))
    }
}
