package com.dudoziworkshop.dzlog.feature.log.policy

data class GalleryViewerDeleteUpdate(val ids: List<Long>, val currentIndex: Int)

/** Remove only verified IDs, preserving viewer order and the current photo whenever it survives. */
object GalleryViewerDeletePolicy {
    fun reconcile(ids: List<Long>, currentIndex: Int, deletedIds: Set<Long>): GalleryViewerDeleteUpdate {
        val remaining = ids.filterNot { it in deletedIds }
        if (remaining.isEmpty()) return GalleryViewerDeleteUpdate(emptyList(), 0)
        val index = currentIndex.coerceIn(0, ids.lastIndex)
        val anchor = ids.drop(index).firstOrNull { it !in deletedIds } ?: remaining.last()
        return GalleryViewerDeleteUpdate(remaining, remaining.indexOf(anchor))
    }
}
