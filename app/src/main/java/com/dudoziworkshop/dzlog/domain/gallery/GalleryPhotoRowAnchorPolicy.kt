package com.dudoziworkshop.dzlog.domain.gallery

/**
 * Re-anchor a LazyColumn photo row after a change in photo columns.
 *
 * The first photo in the visible row remains the anchor even when the row
 * grouping changes (e.g. photo #24: row 8 at 3 columns -> row 12 at 2).
 * Non-photo header rows retain their current scroll position.
 */
object GalleryPhotoRowAnchorPolicy {
    data class Position(val itemIndex: Int, val scrollOffset: Int)

    fun remap(
        visibleItemIndex: Int,
        visibleScrollOffset: Int,
        firstPhotoRowIndex: Int,
        photoCount: Int,
        oldColumns: Int,
        newColumns: Int,
    ): Position {
        val original = Position(visibleItemIndex.coerceAtLeast(0), visibleScrollOffset.coerceAtLeast(0))
        if (photoCount <= 0 || visibleItemIndex < firstPhotoRowIndex) return original
        val previous = GalleryGridColumnsPolicy.normalize(oldColumns)
        val target = GalleryGridColumnsPolicy.normalize(newColumns)
        if (previous == target) return original

        val ordinal = ((visibleItemIndex - firstPhotoRowIndex).coerceAtLeast(0) * previous)
            .coerceAtMost(photoCount - 1)
        return Position(firstPhotoRowIndex + ordinal / target, original.scrollOffset)
    }
}
