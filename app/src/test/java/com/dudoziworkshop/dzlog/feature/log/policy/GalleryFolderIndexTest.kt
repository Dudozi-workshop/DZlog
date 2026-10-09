package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryFolderIndexTest {
    @Test
    fun root_recognizes_arbitrary_depth_and_direct_photos() {
        val root = GalleryFolderIndexPolicy.ROOT
        val index = GalleryFolderIndexPolicy.index(
            root,
            listOf(
                root,
                root + "A/",
                root + "A/B/C/D/E/",
                root + "A/B/C/D/E/original/",
                root + "original/",
                root + "DZlog/",
            ),
        )
        assertEquals(1, index.directImageCount)
        assertEquals(1, index.directOriginalCount)
        assertEquals(listOf("A", "DZlog"), index.children.map { it.name })
        assertEquals(3, index.children.first().totalImageCount)
    }

    @Test
    fun nested_folder_shows_only_its_direct_children_and_images() {
        val index = GalleryFolderIndexPolicy.index(
            "Pictures/DZlog/A/B/",
            listOf(
                "Pictures/DZlog/A/B/",
                "Pictures/DZlog/A/B/original/",
                "Pictures/DZlog/A/B/C/D/",
                "Pictures/DZlog/A/OTHER/",
            ),
        )
        assertEquals(1, index.directImageCount)
        assertEquals(1, index.directOriginalCount)
        assertEquals(listOf("C"), index.children.map { it.name })
        assertEquals("Pictures/DZlog/A/B/C/", index.children.single().relativePath)
    }

    @Test
    fun empty_folder_is_preserved_and_root_label_never_collides() {
        val index = GalleryFolderIndexPolicy.index(
            "Pictures/DZlog/",
            emptyList(),
            listOf("Pictures/DZlog/DZlog/Empty/Nested/", "Pictures/DZlog/Empty/"),
        )
        assertEquals(2, index.children.size)
        assertTrue(index.children.all { it.totalImageCount == 0 })
    }
}
