package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentUris
import android.provider.MediaStore
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GalleryPhotoDeletionPlanTest {
    private fun photo(id: Long, original: Boolean): MediaImageItem =
        MediaImageItem(
            id = id,
            uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
            displayName = "sample_${if (original) id - 1000 else id}.jpg",
            relativePath = "Pictures/DZlog/" + if (original) "original/" else "",
            dateAddedSeconds = id,
        )

    @Test
    fun moreThan100ResultsAndPairedOriginalsCanBePlanned() {
        val results = (1L..151L).map { photo(it, false) }
        val originals = (1001L..1151L).map { photo(it, true) }
        val plan = GalleryPhotoDeletion.prepare(results, results + originals, includeOriginals = true)
        assertEquals(302, plan.items.size)
        assertEquals(151, plan.resultCount)
        assertEquals(151, plan.originalCount)
        assertEquals(302, plan.items.map { it.id }.distinct().size)
    }

    @Test
    fun originalPhotosRemainUntouchedWithoutOptIn() {
        val results = (1L..151L).map { photo(it, false) }
        val originals = (1001L..1151L).map { photo(it, true) }
        val plan = GalleryPhotoDeletion.prepare(results, results + originals, includeOriginals = false)
        assertEquals(151, plan.items.size)
        assertEquals(0, plan.originalCount)
        assertTrue(plan.items.none { it.id >= 1001L })
    }
}
