package com.dudoziworkshop.dzlog.data.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedGalleryViewerRemovalTest {
    @Test fun middlePageAdvancesToTheNextViewablePhotoInDisplayOrder() {
        val pages = listOf(listOf("before"), listOf("current"), emptyList(), listOf("next"))
        assertEquals("next", savedGalleryPhotoAfterRemoval(pages, 1, setOf("current")))
    }

    @Test fun lastPageFallsBackToTheNearestPreviousPhoto() {
        val pages = listOf(listOf("first"), listOf("previous"), listOf("third"), listOf("last"))
        assertEquals("previous", savedGalleryPhotoAfterRemoval(pages, 3, setOf("third", "last")))
    }

    @Test fun resultPageRetainsItsSurvivingCompanion() {
        val pages = listOf(listOf("before"), listOf("result", "original"), listOf("after"))
        assertEquals("original", savedGalleryPhotoAfterRemoval(pages, 1, setOf("result")))
        assertEquals("result", savedGalleryPhotoAfterRemoval(pages, 1, setOf("original")))
    }

    @Test fun emptyScopeClosesTheViewer() {
        assertNull(savedGalleryPhotoAfterRemoval(emptyList(), 0, emptySet()))
        assertNull(savedGalleryPhotoAfterRemoval(listOf(listOf("last"), emptyList()), 0, setOf("last")))
    }

    @Test fun detachedPhotoUsesTheFirstViewablePage() {
        val pages = listOf(emptyList(), listOf("first"), listOf("second"))
        assertEquals("first", savedGalleryPhotoAfterRemoval(pages, -1, setOf("detached")))
    }
}
