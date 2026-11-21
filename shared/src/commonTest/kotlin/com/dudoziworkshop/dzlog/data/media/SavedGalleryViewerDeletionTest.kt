package com.dudoziworkshop.dzlog.data.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedGalleryViewerDeletionTest {
    @Test fun middlePageAdvancesToTheNextViewablePhotoInDisplayOrder() {
        val pages = listOf(listOf("before"), listOf("current"), emptyList(), listOf("next"))
        assertEquals("next", savedGalleryPhotoAfterDeletion(pages, 1, setOf("current")))
    }

    @Test fun lastPageFallsBackToTheNearestPreviousPhoto() {
        val pages = listOf(listOf("first"), listOf("previous"), listOf("third"), listOf("last"))
        assertEquals("previous", savedGalleryPhotoAfterDeletion(pages, 3, setOf("third", "last")))
    }

    @Test fun resultPageRetainsItsSurvivingCompanion() {
        val pages = listOf(listOf("before"), listOf("result", "original"), listOf("after"))
        assertEquals("original", savedGalleryPhotoAfterDeletion(pages, 1, setOf("result")))
        assertEquals("result", savedGalleryPhotoAfterDeletion(pages, 1, setOf("original")))
    }

    @Test fun emptyScopeClosesTheViewer() {
        assertNull(savedGalleryPhotoAfterDeletion(emptyList(), 0, emptySet()))
        assertNull(savedGalleryPhotoAfterDeletion(listOf(listOf("last"), emptyList()), 0, setOf("last")))
    }

    @Test fun detachedPhotoUsesTheFirstViewablePage() {
        val pages = listOf(emptyList(), listOf("first"), listOf("second"))
        assertEquals("first", savedGalleryPhotoAfterDeletion(pages, -1, setOf("detached")))
    }
}
