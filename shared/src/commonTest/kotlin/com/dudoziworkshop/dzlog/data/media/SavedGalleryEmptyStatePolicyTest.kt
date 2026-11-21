package com.dudoziworkshop.dzlog.data.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedGalleryEmptyStatePolicyTest {
    @Test fun emptyScopeWinsOverFavoriteFilter() {
        for (favorite in listOf(false, true)) {
            assertEquals(SavedGalleryEmptyState.NO_PHOTOS,
                SavedGalleryEmptyStatePolicy.resolve(false, false, 0, 0, favorite))
        }
    }

    @Test fun populatedScopeWithNoMatchingFavoritesExplainsFilter() {
        assertEquals(SavedGalleryEmptyState.NO_FAVORITES,
            SavedGalleryEmptyStatePolicy.resolve(false, false, 3, 0, true))
    }

    @Test fun existingPhotosNeverShowEmptyState() {
        for (favorite in listOf(false, true)) {
            assertNull(SavedGalleryEmptyStatePolicy.resolve(false, false, 3, 1, favorite))
        }
    }

    @Test fun loadingAndFailureDoNotClaimThereAreNoPhotos() {
        for (favorite in listOf(false, true)) {
            for (count in listOf(0, 3)) {
                assertNull(SavedGalleryEmptyStatePolicy.resolve(true, false, count, 0, favorite))
                assertNull(SavedGalleryEmptyStatePolicy.resolve(false, true, count, 0, favorite))
            }
        }
    }

    @Test fun clearingFavoritesRestoresVisibleContentAndRefreshCanBecomeEmpty() {
        assertEquals(SavedGalleryEmptyState.NO_FAVORITES,
            SavedGalleryEmptyStatePolicy.resolve(false, false, 2, 0, true))
        assertNull(SavedGalleryEmptyStatePolicy.resolve(false, false, 2, 2, false))
        assertNull(SavedGalleryEmptyStatePolicy.resolve(true, false, 2, 2, false))
        assertEquals(SavedGalleryEmptyState.NO_PHOTOS,
            SavedGalleryEmptyStatePolicy.resolve(false, false, 0, 0, false))
    }
}
