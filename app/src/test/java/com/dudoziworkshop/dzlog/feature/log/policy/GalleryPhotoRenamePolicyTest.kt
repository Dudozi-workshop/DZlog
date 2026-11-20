package com.dudoziworkshop.dzlog.feature.log.policy

import android.net.Uri
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GalleryPhotoRenamePolicyTest {
    private fun photo(id: Long, path: String, name: String) =
        MediaImageItem(id, Uri.parse("content://media/${id}"), name, path, 1)

    @Test fun paired_original_uses_same_new_name() {
        val result = photo(1, "Pictures/DZlog/A/", "wrong_001.jpg")
        val original = photo(2, "Pictures/DZlog/A/original/", "wrong_001.jpg")
        val plan = GalleryPhotoRenamePolicy.plan(
            listOf(result), listOf(result, original), "wrong", "right")
        assertEquals("right_001.jpg", plan.newNames[1])
        assertEquals("right_001.jpg", plan.newNames[2])
        assertEquals(1, plan.pairedCount)
    }

    @Test fun unrelated_files_remain_unchanged() {
        val first = photo(1, "Pictures/DZlog/A/", "wrong_001.jpg")
        val other = photo(2, "Pictures/DZlog/A/", "other_002.jpg")
        val plan = GalleryPhotoRenamePolicy.plan(
            listOf(first), listOf(first, other), "wrong", "right")
        assertEquals(setOf(1L), plan.newNames.keys)
    }

    @Test fun refuses_colliding_destination() {
        val first = photo(1, "Pictures/DZlog/A/", "wrong_001.jpg")
        val existing = photo(2, "Pictures/DZlog/A/", "right_001.jpg")
        assertThrows(IllegalArgumentException::class.java) {
            GalleryPhotoRenamePolicy.plan(
                listOf(first), listOf(first, existing), "wrong", "right")
        }
    }
}
