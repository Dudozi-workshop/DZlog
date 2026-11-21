package com.dudoziworkshop.dzlog.data.media

/** Each page lists its viewable assets in display priority, in the viewer's original page order. */
fun savedGalleryPhotoAfterRemoval(pages: List<List<String>>, currentPage: Int, removedIds: Set<String>): String? {
    if (pages.isEmpty()) return null
    val anchor = currentPage.coerceIn(pages.indices)
    fun survivingPhoto(index: Int) = pages[index].firstOrNull { it !in removedIds }
    for (index in anchor..pages.lastIndex) {
        survivingPhoto(index)?.let { return it }
    }
    for (index in anchor - 1 downTo 0) {
        survivingPhoto(index)?.let { return it }
    }
    return null
}
