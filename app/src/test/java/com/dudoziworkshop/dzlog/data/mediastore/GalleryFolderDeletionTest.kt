package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GalleryFolderDeletionTest {
    private val rootId = "primary:Pictures/DZlog"
    private val path = "Pictures/DZlog/A/"
    private lateinit var provider: TreeProvider
    private lateinit var storage: GalleryFolderStorage

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("dzlog_gallery_folder_tree", Context.MODE_PRIVATE).edit().clear().commit()
        provider = TreeProvider()
        provider.attachInfo(context, ProviderInfo().apply {
            authority = "com.android.externalstorage.documents"
            exported = true
            grantUriPermissions = true
        })
        ShadowContentResolver.registerProviderInternal("com.android.externalstorage.documents", provider)
        provider.rows[rootId] = DocumentsContract.Document.MIME_TYPE_DIR
        provider.rows["$rootId/A"] = DocumentsContract.Document.MIME_TYPE_DIR
        storage = GalleryFolderStorage(context)
        storage.connect(DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", rootId))
    }

    @Test fun deletesOriginalsNestedAndOtherFilesButPreservesSibling() {
        provider.rows["$rootId/A/original"] = DocumentsContract.Document.MIME_TYPE_DIR
        provider.rows["$rootId/A/original/x.jpg"] = "image/jpeg"
        provider.rows["$rootId/A/note.txt"] = "text/plain"
        provider.rows["$rootId/AA"] = DocumentsContract.Document.MIME_TYPE_DIR
        provider.rows["$rootId/AA/x.jpg"] = "image/jpeg"
        val plan = storage.prepareDeletion(path)
        assertEquals(2, plan.fileCount)
        assertEquals(1, plan.originalCount)
        assertEquals(emptyList<Any>(), storage.deleteFolder(plan).entries)
        assertEquals(setOf(rootId, "$rootId/AA", "$rootId/AA/x.jpg"), provider.rows.keys)
    }

    @Test fun partialFailureLeavesNonemptyParentAndCanRetryRemainingFiles() {
        val denied = "$rootId/A/x.jpg"
        provider.rows[denied] = "image/jpeg"
        provider.rows["$rootId/A/y.jpg"] = "image/jpeg"
        provider.denied += denied
        val remaining = storage.deleteFolder(storage.prepareDeletion(path))
        assertEquals(1, remaining.fileCount)
        assertEquals(true, provider.rows.containsKey("$rootId/A"))
        provider.denied.clear()
        assertEquals(0, storage.deleteFolder(remaining).entries.size)
    }

    @Test fun inventoryChangeBlocksAllWrites() {
        val plan = storage.prepareDeletion(path)
        provider.rows["$rootId/A/new.jpg"] = "image/jpeg"
        assertThrows(IllegalArgumentException::class.java) { storage.deleteFolder(plan) }
        assertEquals(0, provider.deletes)
    }

    @Test fun unreadableInventoryIsNeverEmptyOrSuccessful() {
        provider.nullParent = "$rootId/A"
        assertThrows(IllegalStateException::class.java) { storage.prepareDeletion(path) }
        assertEquals(0, provider.deletes)
    }

    @Test fun absentPhysicalFolderIsVerifiedWithoutWrites() {
        provider.rows.remove("$rootId/A")
        val plan = storage.prepareDeletion(path)
        assertEquals(0, storage.deleteFolder(plan).entries.size)
        assertEquals(0, provider.deletes)
    }

    @Test fun newlyCreatedFileKeepsParentAndIsNotDeleted() {
        provider.rows["$rootId/A/x.jpg"] = "image/jpeg"
        provider.injectFile = "$rootId/A/new.jpg"
        val remaining = storage.deleteFolder(storage.prepareDeletion(path))
        assertEquals(1, remaining.fileCount)
        assertEquals(true, provider.rows.containsKey("$rootId/A/new.jpg"))
        assertEquals(true, provider.rows.containsKey("$rootId/A"))
    }

    @Test fun verificationFailureIsReportedEvenAfterSomeDeletion() {
        provider.rows["$rootId/A/x.jpg"] = "image/jpeg"
        provider.failQueryAfterDelete = rootId
        assertThrows(IllegalStateException::class.java) { storage.deleteFolder(storage.prepareDeletion(path)) }
        assertEquals(true, provider.deletes > 0)
    }

    @Test fun rejectsEveryFolderExceptTheExactDzlogRootAndKeepsTheExistingGrant() {
        listOf(
            "primary:Pictures",
            "primary:Pictures/Dzlog",
            "primary:Pictures/DZlog/A",
            "primary:Download/DZlog",
        ).forEach { wrongId ->
            val wrong = DocumentsContract.buildTreeDocumentUri(
                "com.android.externalstorage.documents",
                wrongId,
            )
            assertThrows(IllegalArgumentException::class.java) { storage.connect(wrong) }
        }
        assertTrue(storage.isConnected())
    }

    @Test fun deletedOrRenamedDzlogRootIsNoLongerConnected() {
        provider.rows.remove(rootId)
        assertFalse(storage.isConnected())
    }


    @Test fun firstInstallPicturesGrantCreatesDzlogWithoutTouchingSiblingAndScopesWrites() {
        provider.rows.clear()
        provider.rows["primary:Pictures"] = DocumentsContract.Document.MIME_TYPE_DIR
        provider.rows["primary:Pictures/Family"] = DocumentsContract.Document.MIME_TYPE_DIR
        provider.rows["primary:Pictures/Family/photo.jpg"] = "image/jpeg"
        val pictures = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents", "primary:Pictures",
        )
        storage.connect(pictures, allowPicturesForOnboarding = true)
        assertTrue(storage.isConnected())
        assertEquals(DocumentsContract.Document.MIME_TYPE_DIR, provider.rows[rootId])
        assertEquals(1, provider.creates)
        assertEquals("image/jpeg", provider.rows["primary:Pictures/Family/photo.jpg"])
        assertThrows(IllegalArgumentException::class.java) {
            storage.createFolder("Pictures/", "Outside")
        }
        storage.createFolder("Pictures/DZlog/", "Album")
        assertTrue(provider.rows.containsKey("$rootId/Album"))
        assertFalse(provider.rows.containsKey("primary:Pictures/Outside"))
    }

    @Test fun onboardingExistingDzlogReusesItWithoutOverwritingPhotos() {
        provider.rows["primary:Pictures"] = DocumentsContract.Document.MIME_TYPE_DIR
        provider.rows["$rootId/Existing.jpg"] = "image/jpeg"
        val pictures = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents", "primary:Pictures",
        )
        storage.connect(pictures, allowPicturesForOnboarding = true)
        assertTrue(storage.isConnected())
        assertEquals(0, provider.creates)
        assertEquals("image/jpeg", provider.rows["$rootId/Existing.jpg"])
        assertTrue(storage.listImmediateFolderPaths("Pictures/DZlog/").contains(path))
    }

    @Test fun caseVariantCollisionBlocksCreationAndRetainsExistingConnection() {
        provider.rows["primary:Pictures"] = DocumentsContract.Document.MIME_TYPE_DIR
        provider.rows["primary:Pictures/dzlog"] = DocumentsContract.Document.MIME_TYPE_DIR
        val pictures = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents", "primary:Pictures",
        )
        assertThrows(IllegalArgumentException::class.java) {
            storage.connect(pictures, allowPicturesForOnboarding = true)
        }
        assertEquals(0, provider.creates)
        assertTrue(storage.isConnected())
        assertTrue(provider.rows.containsKey("primary:Pictures/dzlog"))
    }

    @Test fun onboardingRejectsWrongTreeWithoutChangingSavedGrant() {
        provider.rows["primary:Pictures"] = DocumentsContract.Document.MIME_TYPE_DIR
        val wrong = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents", "primary:Pictures/Other",
        )
        assertThrows(IllegalArgumentException::class.java) {
            storage.connect(wrong, allowPicturesForOnboarding = true)
        }
        assertTrue(storage.isConnected())
        assertEquals(0, provider.creates)
    }

    private class TreeProvider : ContentProvider() {
        val rows = linkedMapOf<String, String>()
        val denied = mutableSetOf<String>()
        var nullParent: String? = null
        var deletes = 0
        var creates = 0
        var injectFile: String? = null
        var failQueryAfterDelete: String? = null
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            val parent = DocumentsContract.getDocumentId(uri)
            if (parent == nullParent) return null
            val columns = requireNotNull(projection)
            return MatrixCursor(columns).apply {
                val matches = if (uri.pathSegments.lastOrNull() == "children") {
                    rows.filterKeys { it.substringBeforeLast('/') == parent }
                } else {
                    rows.filterKeys { it == parent }
                }
                matches.forEach { (id, mime) ->
                    addRow(columns.map { column -> when (column) {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID -> id
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME -> if (id == "primary:Pictures") "Pictures" else id.substringAfterLast('/')
                        DocumentsContract.Document.COLUMN_MIME_TYPE -> mime
                        else -> null
                    } }.toTypedArray())
                }
            }
        }
        override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
            @Suppress("DEPRECATION")
            val arguments = requireNotNull(extras)
            val uri = requireNotNull(arguments.getParcelable<Uri>("uri"))
            return when (method) {
                "android:createDocument" -> {
                    val parentId = DocumentsContract.getDocumentId(uri)
                    val name = requireNotNull(arguments.getString(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                    val mime = requireNotNull(arguments.getString(DocumentsContract.Document.COLUMN_MIME_TYPE))
                    val id = "$parentId/$name"
                    check(id !in rows) { "Do not replace existing user files or folders" }
                    rows[id] = mime
                    creates++
                    Bundle().apply {
                        putParcelable("uri", DocumentsContract.buildDocumentUriUsingTree(uri, id))
                    }
                }
                "android:deleteDocument" -> {
                    val id = DocumentsContract.getDocumentId(uri)
                    if (id in denied) throw SecurityException("Denied")
                    check(rows.keys.none { it.startsWith("$id/") }) { "Nonempty directory" }
                    rows.remove(id)
                    deletes++
                    injectFile?.let { rows[it] = "image/jpeg"; injectFile = null }
                    failQueryAfterDelete?.let { nullParent = it }
                    Bundle()
                }
                else -> error("Unexpected document provider method: $method")
            }
        }
        override fun getType(uri: Uri) = "vnd.android.document/directory"
        override fun insert(uri: Uri, values: ContentValues?) = null
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
