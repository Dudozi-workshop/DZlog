package com.dudoziworkshop.dzlog.ui.log

import android.net.Uri
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class GalleryPhotoSortTest {
    private fun photo(id: Long, name: String, added: Long) =
        MediaImageItem(id, Uri.parse("content://media/external/images/media/$id"), name,
            "Pictures/DZlog/Folder/", added)

    @Test
    fun dateSortsUseStableIdsWhenDatesAreEqual() {
        val photos = listOf(photo(2, "B.jpg", 200), photo(1, "A.jpg", 100), photo(3, "C.jpg", 200))
        assertEquals(listOf(3L, 2L, 1L), GalleryPhotoSort.NEWEST.sorted(photos).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), GalleryPhotoSort.OLDEST.sorted(photos).map { it.id })
        assertEquals(listOf(2L, 1L, 3L), photos.map { it.id })
    }

    @Test
    fun nameSortIgnoresCaseAndDoesNotDependOnDeviceLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr", "TR"))
            val photos = listOf(photo(1, "z.jpg", 1), photo(2, "I.jpg", 2), photo(3, "iA.jpg", 3))
            assertEquals(listOf(2L, 3L, 1L), GalleryPhotoSort.NAME.sorted(photos).map { it.id })
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun equalNamesKeepDeterministicOrderAndTheSameViewerUris() {
        val first = photo(1, "Same.jpg", 100)
        val tapped = photo(2, "Same.jpg", 100)
        val sorted = GalleryPhotoSort.NAME.sorted(listOf(first, tapped))
        assertEquals(listOf(2L, 1L), sorted.map { it.id })
        val viewerIndex = sorted.indexOfFirst { it.id == tapped.id }
        assertEquals(0, viewerIndex)
        assertEquals(tapped.uri, sorted[viewerIndex].uri)
        assertEquals(listOf(tapped.uri, first.uri), sorted.map { it.uri })
    }

    @Test
    fun everySortHandlesEmptyAndSinglePhotoLists() {
        val only = photo(7, "Only.jpg", 0)
        GalleryPhotoSort.entries.forEach { sort ->
            assertEquals(emptyList<MediaImageItem>(), sort.sorted(emptyList()))
            assertEquals(listOf(only), sort.sorted(listOf(only)))
        }
    }
}
