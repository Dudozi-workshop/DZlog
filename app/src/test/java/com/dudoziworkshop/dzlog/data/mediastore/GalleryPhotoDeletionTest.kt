package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GalleryPhotoDeletionTest {
    private val path = "Pictures/DZlog/A/"
    private lateinit var provider: TestProvider
    private lateinit var deletion: GalleryPhotoDeletion
    private fun photo(id: Long, name: String = "One.jpg", folder: String = path) =
        MediaImageItem(id, Uri.parse("content://media/external/images/media/$id"), name, folder, 1)

    @Before fun setup() {
        provider = TestProvider()
        ShadowContentResolver.registerProviderInternal("media", provider)
        deletion = GalleryPhotoDeletion(ApplicationProvider.getApplicationContext<Context>().contentResolver)
    }

    @Test fun originalsAreExplicitAndDuplicateSelectionsCountOneFile() {
        val result = photo(1)
        val original = photo(2, folder = path + "original/")
        val snapshot = listOf(result, original)
        val resultOnly = GalleryPhotoDeletion.prepare(listOf(result, result), snapshot, false)
        assertEquals(listOf(result), resultOnly.items)
        val both = GalleryPhotoDeletion.prepare(listOf(result, result), snapshot, true)
        assertEquals(1, both.resultCount)
        assertEquals(1, both.originalCount)
        assertEquals(2, both.items.size)
    }

    @Test fun missingSnapshotAndNonMediaUriAreRejected() {
        val result = photo(1)
        assertThrows(IllegalArgumentException::class.java) { GalleryPhotoDeletion.prepare(listOf(result), emptyList(), false) }
        val other = result.copy(uri = Uri.parse("content://other/images/media/1"))
        assertThrows(IllegalArgumentException::class.java) { GalleryPhotoDeletion.prepare(listOf(other), listOf(other), false) }
    }

    @Test fun changedMetadataBlocksTheBatchBeforePermissionOrWrites() {
        val first = photo(1)
        val second = photo(2, "Two.jpg")
        val plan = GalleryPhotoDeletion.prepare(listOf(first, second), listOf(first, second), false)
        provider.rows += listOf(first, second.copy(relativePath = "Pictures/DZlog/B/"))
        assertThrows(IllegalStateException::class.java) { deletion.pendingItems(plan) }
        assertEquals(0, provider.deletes)
    }

    @Test fun partialDeletionReportsExactIdsAndRetryOmitsAlreadyAbsentFiles() {
        val first = photo(1)
        val second = photo(2, "Two.jpg")
        val plan = GalleryPhotoDeletion.prepare(listOf(first, second), listOf(first, second), false)
        provider.rows += second
        val result = deletion.verifyResult(plan)
        assertEquals(setOf(1L), result.deletedIds)
        assertEquals(setOf(2L), result.failedIds)
        assertEquals(listOf(second), deletion.pendingItems(plan))
        assertEquals(0, provider.deletes)
    }

    @Test fun nullCursorAndPermissionDenialNeverCountAsDeleted() {
        val first = photo(1)
        val second = photo(2, "Two.jpg")
        val plan = GalleryPhotoDeletion.prepare(listOf(first, second), listOf(first, second), false)
        provider.nullIds += 1L
        provider.deniedIds += 2L
        assertEquals(emptySet<Long>(), deletion.verifyResult(plan).deletedIds)
        assertEquals(setOf(1L, 2L), deletion.verifyResult(plan).failedIds)
        assertThrows(IllegalStateException::class.java) { deletion.pendingItems(plan) }
    }

    @Test fun completeAbsenceIsVerifiedWithoutDeletingAnythingAgain() {
        val result = photo(1)
        val plan = GalleryPhotoDeletion.prepare(listOf(result), listOf(result), false)
        assertEquals(setOf(1L), deletion.verifyResult(plan).deletedIds)
        assertEquals(emptySet<Long>(), deletion.verifyResult(plan).failedIds)
        assertEquals(emptyList<MediaImageItem>(), deletion.pendingItems(plan))
        assertEquals(0, provider.deletes)
    }

    private class TestProvider : ContentProvider() {
        val rows = mutableListOf<MediaImageItem>()
        val nullIds = mutableSetOf<Long>()
        val deniedIds = mutableSetOf<Long>()
        var deletes = 0
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            val id = requireNotNull(uri.lastPathSegment?.toLongOrNull())
            if (id in deniedIds) throw SecurityException("Denied")
            if (id in nullIds) return null
            val columns = requireNotNull(projection)
            return MatrixCursor(columns).apply {
                rows.filter { it.id == id }.forEach { item -> addRow(columns.map { column -> when (column) {
                    MediaStore.Images.Media._ID -> item.id
                    MediaStore.Images.Media.DISPLAY_NAME -> item.displayName
                    MediaStore.Images.Media.RELATIVE_PATH -> item.relativePath
                    else -> null
                } }.toTypedArray()) }
            }
        }
        override fun getType(uri: Uri) = "image/jpeg"
        override fun insert(uri: Uri, values: ContentValues?) = null
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int { deletes++; return 0 }
    }
}
