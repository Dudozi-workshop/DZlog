package com.dudoziworkshop.dzlog.data.media

enum class SavedGalleryEmptyState { NO_PHOTOS, NO_FAVORITES }

/** Match Android's scope-first empty-state precedence without disguising a read error as no data. */
object SavedGalleryEmptyStatePolicy {
    fun resolve(
        loading: Boolean,
        failed: Boolean,
        scopeCount: Int,
        visibleCount: Int,
        favoritesOnly: Boolean,
    ): SavedGalleryEmptyState? = when {
        loading || failed || visibleCount > 0 -> null
        scopeCount == 0 -> SavedGalleryEmptyState.NO_PHOTOS
        favoritesOnly -> SavedGalleryEmptyState.NO_FAVORITES
        else -> null
    }
}
