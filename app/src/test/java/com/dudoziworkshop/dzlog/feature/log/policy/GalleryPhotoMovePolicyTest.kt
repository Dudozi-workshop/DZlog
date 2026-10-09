package com.dudoziworkshop.dzlog.feature.log.policy

import android.net.Uri
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GalleryPhotoMovePolicyTest {
    private fun photo(id: Long, folder: String, name: String = "0001.jpg") =
        MediaImageItem(id, Uri.parse("content://media/$id"), name, folder, 1)

    @Test
    fun matches_only_exact_original_filename() {
        val source = photo(1, "Pictures/DZlog/A/")
        val original = photo(2, "Pictures/DZlog/A/original/")
        val unrelated = photo(3, "Pictures/DZlog/B/original/")
        assertEquals(listOf(original),
            GalleryPhotoMovePolicy.pairedOriginals(listOf(source), listOf(source, original, unrelated)))
    }

    @Test
    fun original_match_ambiguous_is_not_auto_included() {
        val source = photo(1, "Pictures/DZlog/A/")
        val originals = listOf(photo(2, "Pictures/DZlog/A/original/"),
            photo(3, "Pictures/DZlog/A/original/"))
        assertEquals(emptyList<MediaImageItem>(),
            GalleryPhotoMovePolicy.pairedOriginals(listOf(source), listOf(source) + originals))
    }

    @Test
    fun optional_original_keeps_original_subfolder() {
        val main = photo(1, "Pictures/DZlog/A/")
        val original = photo(2, "Pictures/DZlog/A/original/")
        val plan = GalleryPhotoMovePolicy.plan(
            listOf(main), listOf(original), "Pictures/DZlog/B/", includeOriginals = true)
        assertEquals("Pictures/DZlog/B/", plan.destinations[1])
        assertEquals("Pictures/DZlog/B/original/", plan.destinations[2])
        assertEquals(1, plan.pairedOriginalCount)
    }

    @Test
    fun refuses_current_folder_and_duplicate_names() {
        val main = photo(1, "Pictures/DZlog/A/")
        assertThrows(IllegalArgumentException::class.java) {
            GalleryPhotoMovePolicy.plan(listOf(main), emptyList(), "Pictures/DZlog/A/", false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GalleryPhotoMovePolicy.plan(listOf(main, photo(3, "Pictures/DZlog/C/")), emptyList(),
                "Pictures/DZlog/B/", false)
        }
    }
}
