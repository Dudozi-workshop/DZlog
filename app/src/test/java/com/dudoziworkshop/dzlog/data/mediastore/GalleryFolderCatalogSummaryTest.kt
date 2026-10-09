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
}
