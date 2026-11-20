package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GalleryBatchRenamePolicyTest {
    @Test fun replaces_only_stem_and_preserves_extension() {
        assertEquals("Draper_001.jpg",
            GalleryBatchRenamePolicy.renamedName("Drapre_001.jpg", "Drapre", "Draper", true))
        assertEquals("Drapre_001.jpg",
            GalleryBatchRenamePolicy.renamedName("Drapre_001.jpg", "jpg", "png", true))
    }

    @Test fun can_remove_wrong_phrase_without_changing_original_extension() {
        assertEquals("A_002.JPG",
            GalleryBatchRenamePolicy.renamedName("A_wrong_002.JPG", "wrong_", "", true))
    }

    @Test fun rejects_invalid_name_and_existing_collisions() {
        assertThrows(IllegalArgumentException::class.java) {
            GalleryBatchRenamePolicy.renamedName("wrong.jpg", "wrong", "/", true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GalleryBatchRenamePolicy.requireNoCollisions(
                listOf("bad.jpg" to "good.jpg"), listOf("bad.jpg", "GOOD.JPG"))
        }
    }

    @Test fun rejects_two_entries_mapping_to_same_target() {
        assertThrows(IllegalArgumentException::class.java) {
            GalleryBatchRenamePolicy.requireNoCollisions(
                listOf("a_bad.jpg" to "a.jpg", "a_wrong.jpg" to "a.jpg"),
                listOf("a_bad.jpg", "a_wrong.jpg"))
        }
    }
}
