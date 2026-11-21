package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GalleryFolderBatchPolicyTest {
    private val root = "Pictures/DZlog/"

    @Test fun moves_two_siblings_together() {
        val a = root + "A/"
        val b = root + "B/"
        val dest = root + "Archive/"
        assertEquals(mapOf(a to dest + "A/", b to dest + "B/"),
            GalleryFolderBatchPolicy.moveTargets(listOf(a, b), dest, listOf(a, b, dest)))
    }

    @Test fun rejects_moving_parent_and_child_in_same_operation() {
        assertThrows(IllegalArgumentException::class.java) {
            GalleryFolderBatchPolicy.moveTargets(
                listOf(root + "A/", root + "A/B/"), root + "Archive/",
                listOf(root + "Archive/"))
        }
    }

    @Test fun detects_existing_destination_collision() {
        assertThrows(IllegalArgumentException::class.java) {
            GalleryFolderBatchPolicy.moveTargets(
                listOf(root + "A/"), root + "Archive/",
                listOf(root + "Archive/", root + "Archive/A/"))
        }
    }

    @Test fun renames_only_matching_folder_names() {
        val a = root + "Drapre_01/"
        val b = root + "Other/"
        val renamed = GalleryFolderBatchPolicy.renameTargets(
            listOf(a, b), listOf(a, b), "Drapre", "Draper")
        assertEquals(listOf(GalleryFolderRenameItem(a, "Draper_01")), renamed)
    }
}
