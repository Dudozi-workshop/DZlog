package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryFolderIndexTest {
    @Test
    fun originals_only_leaf_uses_direct_originals_but_back_still_reaches_its_parent() {
        val path = "Pictures/DZlog/A/B/"
        val index = GalleryFolderIndexPolicy.index(path, listOf(path + "original/", path + "original/"))
        assertTrue(GalleryFolderIndexPolicy.showsOriginalsDirectly(index))
        assertEquals("Pictures/DZlog/A/", GalleryFolderIndexPolicy.parentOf(index.relativePath))
    }

    @Test
    fun originals_do_not_hide_results_or_even_an_empty_child_folder() {
        val path = "Pictures/DZlog/A/"
        assertEquals(false, GalleryFolderIndexPolicy.showsOriginalsDirectly(
            GalleryFolderIndexPolicy.index(path, listOf(path, path + "original/"))))
        assertEquals(false, GalleryFolderIndexPolicy.showsOriginalsDirectly(
            GalleryFolderIndexPolicy.index(path, listOf(path + "original/"), listOf(path + "Empty/"))))
    }

    @Test
    fun root_empty_unknown_and_explicit_original_paths_do_not_redirect() {
        val root = GalleryFolderIndexPolicy.ROOT
        assertEquals(false, GalleryFolderIndexPolicy.showsOriginalsDirectly(null))
        assertEquals(false, GalleryFolderIndexPolicy.showsOriginalsDirectly(
            GalleryFolderIndexPolicy.index(root, listOf(root + "original/"))))
        assertEquals(false, GalleryFolderIndexPolicy.showsOriginalsDirectly(
            GalleryFolderIndexPolicy.index(root + "Empty/", emptyList())))
        assertEquals(false, GalleryFolderIndexPolicy.showsOriginalsDirectly(
            GalleryFolderIndexPolicy.index(root + "A/original/", listOf(root + "A/original/original/"))))
    }

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
    @Test
    fun parent_navigation_reaches_root_from_five_levels() {
        var path: String? = "Pictures/DZlog/A/B/C/D/E/"
        for (expected in listOf(
            "Pictures/DZlog/A/B/C/D/",
            "Pictures/DZlog/A/B/C/",
            "Pictures/DZlog/A/B/",
            "Pictures/DZlog/A/",
            "Pictures/DZlog/",
        )) {
            path = GalleryFolderIndexPolicy.parentOf(requireNotNull(path))
            assertEquals(expected, path)
        }
        assertEquals(null, GalleryFolderIndexPolicy.parentOf(GalleryFolderIndexPolicy.ROOT))
    }

    @Test
    fun breadcrumbs_keep_full_relative_paths_at_arbitrary_depth() {
        val result = GalleryFolderIndexPolicy.breadcrumbs("Pictures/DZlog/A/B/C/D/E/")
        assertEquals(listOf("DZlog", "A", "B", "C", "D", "E"), result.map { it.label })
        assertEquals("Pictures/DZlog/A/B/C/", result[3].relativePath)
        assertEquals(1, GalleryFolderIndexPolicy.breadcrumbs("Pictures/DZlog/").size)
    }
}
