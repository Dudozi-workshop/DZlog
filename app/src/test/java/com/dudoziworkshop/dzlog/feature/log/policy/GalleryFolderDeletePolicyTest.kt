package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GalleryFolderDeletePolicyTest {
    private val root = "Pictures/DZlog/A/"
    private fun dir(id: String, path: String) = GalleryFolderDeleteEntry(id, path, "vnd.android.document/directory")
    private fun photo(id: String, path: String) = GalleryFolderDeleteEntry(id, path, "image/jpeg")

    @Test fun countsAllDepthsAndOtherFilesWithoutTechnicalFolder() {
        val plan = GalleryFolderDeletePolicy.plan(root, listOf(dir("a", root), dir("o", root + "original/"),
            dir("b", root + "B/"), dir("bo", root + "B/original/"), photo("1", root + "x.jpg"),
            photo("2", root + "original/x.jpg"), photo("3", root + "B/original/y.jpg"),
            GalleryFolderDeleteEntry("4", root + "note.txt", "text/plain")))
        assertEquals(1, plan.folderCount)
        assertEquals(1, plan.resultCount)
        assertEquals(2, plan.originalCount)
        assertEquals(1, plan.otherCount)
        assertEquals(4, plan.fileCount)
    }

    @Test fun refusesRootTraversalAndTechnicalFolders() {
        listOf("Pictures/DZlog/", "Pictures/DZlog/A/../", "Pictures/DZlog/A/original/", "Pictures/Other/").forEach {
            assertThrows(IllegalArgumentException::class.java) { GalleryFolderDeletePolicy.plan(it, emptyList()) }
        }
    }

    @Test fun refusesSiblingAndDuplicateInventory() {
        assertThrows(IllegalArgumentException::class.java) {
            GalleryFolderDeletePolicy.plan(root, listOf(dir("a", root), photo("2", "Pictures/DZlog/AA/x.jpg")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            GalleryFolderDeletePolicy.plan(root, listOf(dir("a", root), photo("a", root + "x.jpg")))
        }
    }

    @Test fun changedContentsRequireConfirmationAgain() {
        val plan = GalleryFolderDeletePolicy.plan(root, listOf(dir("a", root)))
        GalleryFolderDeletePolicy.requireUnchanged(plan, plan.copy(entries = plan.entries.reversed()))
        val changed = GalleryFolderDeletePolicy.plan(root, plan.entries + photo("2", root + "new.jpg"))
        assertThrows(IllegalArgumentException::class.java) { GalleryFolderDeletePolicy.requireUnchanged(plan, changed) }
    }

    @Test fun supportsCatalogOnlyEmptyFolder() {
        assertEquals(0, GalleryFolderDeletePolicy.plan(root, emptyList()).fileCount)
    }
}
