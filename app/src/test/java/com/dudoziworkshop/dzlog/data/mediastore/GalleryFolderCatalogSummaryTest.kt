package com.dudoziworkshop.dzlog.data.mediastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GalleryFolderCatalogSummaryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val root = GalleryFolderIndexPolicy.ROOT
    private val catalog = GalleryFolderCatalog(context)
    private val reader = DzlogMediaStoreReader(context.contentResolver)

    @Before
    fun clearCatalog() {
        context.getSharedPreferences("dzlog_gallery_folder_catalog", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun emptyNestedFoldersAreCountedAtEveryNavigationDepth() {
        val a = catalog.create(root, "A", emptyList())
        val b = catalog.create(a, "B", emptyList())
        catalog.create(b, "C", emptyList())
        val rootSnapshot = reader.loadGallerySnapshot(root, catalog.listDescendantPaths(root))
        val aSummary = requireNotNull(rootSnapshot.summariesByPath[a])
        assertEquals(1, aSummary.directChildFolderCount)
        assertEquals(0, aSummary.totalPhotoCount)
        assertNull(aSummary.coverImageId)

        val aSnapshot = reader.loadGallerySnapshot(a, catalog.listDescendantPaths(a))
        assertEquals(1, requireNotNull(aSnapshot.summariesByPath[b]).directChildFolderCount)
        assertEquals(1, requireNotNull(aSnapshot.summariesByPath[a]).directChildFolderCount)
    }

    @Test
    fun missingCatalogAncestorIsInferredFromAnEmptyDescendant() {
        val a = root + "A/"
        val b = catalog.create(a, "B", emptyList())
        assertEquals(emptyList<String>(), catalog.listImmediatePaths(root))
        assertEquals(listOf(b), catalog.listDescendantPaths(root))
        val snapshot = reader.loadGallerySnapshot(root, catalog.listDescendantPaths(root))
        assertEquals(listOf(a), snapshot.index.children.map { it.relativePath })
        assertEquals(1, requireNotNull(snapshot.summariesByPath[a]).directChildFolderCount)
    }

    @Test
    fun subtreeEnumerationExcludesSimilarNamedSiblingsAndTheParent() {
        val a = catalog.create(root, "A", emptyList())
        val b = catalog.create(a, "B", emptyList())
        val aa = catalog.create(root, "AA", emptyList())
        catalog.create(aa, "Other", emptyList())
        assertEquals(listOf(b), catalog.listDescendantPaths(a.trimEnd('/')))
        val snapshot = reader.loadGallerySnapshot(a, catalog.listDescendantPaths(a))
        assertEquals(listOf(b), snapshot.index.children.map { it.relativePath })
        assertEquals(1, requireNotNull(snapshot.summariesByPath[a]).directChildFolderCount)
    }

    @Test
    fun renameRemapsRegisteredSubtreeAndPreservesSimilarNamedSibling() {
        val a = catalog.create(root, "A", emptyList())
        val b = catalog.create(a, "B", emptyList())
        catalog.create(b, "C", emptyList())
        val aa = catalog.create(root, "AA", emptyList())
        val target = root + "Renamed/"
        assertEquals(target, catalog.relocateFolder(a, target) { target })
        val paths = listOf(target, target + "B/", target + "B/C/", aa).sorted()
        assertEquals(paths, catalog.listDescendantPaths(root))
        assertEquals(emptyList<String>(), catalog.listDescendantPaths(a))
        // Read again through a new catalog instance to verify persistence.
        assertEquals(paths, GalleryFolderCatalog(context).listDescendantPaths(root))
        val snapshot = reader.loadGallerySnapshot(root, paths)
        assertEquals(1, requireNotNull(snapshot.summariesByPath[target]).directChildFolderCount)
        assertNull(snapshot.summariesByPath[a])
    }

    @Test
    fun moveRemapsEmptyDescendantEvenWhenSourceWasNeverRecorded() {
        val source = root + "Physical/"
        catalog.create(source, "Empty", emptyList())
        val destination = catalog.create(root, "Destination", emptyList())
        val target = destination + "Physical/"
        catalog.relocateFolder(source, target) { target }
        val paths = catalog.listDescendantPaths(root)
        assertEquals(listOf(destination, target + "Empty/"), paths)
        val snapshot = reader.loadGallerySnapshot(root, paths)
        assertEquals(1, requireNotNull(snapshot.summariesByPath[destination]).directChildFolderCount)
        assertNull(snapshot.summariesByPath[source])
    }

    @Test
    fun failedStorageChangePreservesAllCatalogPaths() {
        val source = catalog.create(root, "A", emptyList())
        catalog.create(source, "Empty", emptyList())
        val before = catalog.listDescendantPaths(root)
        val result = runCatching {
            catalog.relocateFolder(source, root + "Renamed/") { error("Permission denied") }
        }
        assertEquals("Permission denied", result.exceptionOrNull()?.message)
        assertEquals(before, catalog.listDescendantPaths(root))
    }

    @Test fun deletionPrunesOnlyVerifiedSubtreeAndPersistsSibling() {
        val a = catalog.create(root, "A", emptyList())
        catalog.create(a, "Empty", emptyList())
        val aa = catalog.create(root, "AA", emptyList())
        assertEquals(true, catalog.deleteFolder(a) { true })
        assertEquals(listOf(aa), GalleryFolderCatalog(context).listDescendantPaths(root))
    }

    @Test fun partialOrUnverifiedDeletionPreservesCatalog() {
        val a = catalog.create(root, "A", emptyList())
        catalog.create(a, "Empty", emptyList())
        val before = catalog.listDescendantPaths(root)
        assertEquals(false, catalog.deleteFolder(a) { false })
        assertEquals(before, catalog.listDescendantPaths(root))
        assertEquals(true, runCatching { catalog.deleteFolder(a) { error("Unreadable") } }.isFailure)
        assertEquals(before, catalog.listDescendantPaths(root))
    }

    @Test fun deletingRootNeverInvokesStorage() {
        var calls = 0
        assertEquals(true, runCatching { catalog.deleteFolder(root) { calls++; true } }.isFailure)
        assertEquals(0, calls)
    }

    @Test
    fun virtualDestinationCollisionIsRejectedBeforeChangingStorage() {
        val source = catalog.create(root, "Source", emptyList())
        // The destination parent itself need not be recorded for a collision to exist.
        catalog.create(root + "TAKEN/", "Empty", emptyList())
        val before = catalog.listDescendantPaths(root)
        var storageCalls = 0
        val result = runCatching {
            catalog.relocateFolder(source, root + "Taken/") {
                storageCalls++
                root + "Taken/"
            }
        }
        assertEquals(true, result.isFailure)
        assertEquals(0, storageCalls)
        assertEquals(before, catalog.listDescendantPaths(root))
    }
}
