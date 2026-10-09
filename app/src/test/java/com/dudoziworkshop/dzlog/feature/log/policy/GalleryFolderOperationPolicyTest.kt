package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryFolderOperationPolicyTest {
    @Test
    fun camera_paths_are_flagged_for_current_folder_and_descendants_only() {
        val capture = listOf("Pictures/DZlog/A/B/C/")
        assertTrue(GalleryFolderOperationPolicy.affectsCapturePath("Pictures/DZlog/A/", capture))
        assertTrue(GalleryFolderOperationPolicy.affectsCapturePath("Pictures/DZlog/A/B/C/", capture))
        assertFalse(GalleryFolderOperationPolicy.affectsCapturePath("Pictures/DZlog/AB/", capture))
        assertFalse(GalleryFolderOperationPolicy.affectsCapturePath("Pictures/DZlog/A/B/C/D/", capture))
    }

    @Test
    fun moving_folder_into_itself_or_child_is_rejected() {
        listOf("Pictures/DZlog/A/", "Pictures/DZlog/A/B/").forEach { destination ->
            assertThrows(IllegalArgumentException::class.java) {
                GalleryFolderOperationPolicy.validateMove("Pictures/DZlog/A/", destination)
            }
        }
        GalleryFolderOperationPolicy.validateMove("Pictures/DZlog/A/", "Pictures/DZlog/B/")
    }
}
