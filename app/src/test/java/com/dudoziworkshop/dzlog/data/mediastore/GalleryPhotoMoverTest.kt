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
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GalleryPhotoMoverTest {
    private val source = "Pictures/DZlog/A/"
    private val destination = "Pictures/DZlog/B/"
    private lateinit var provider: TestProvider
    private lateinit var mover: GalleryPhotoMover
    private fun photo(id: Long, name: String, path: String = source) =
        MediaImageItem(id, Uri.parse("content://media/external/images/media/$id"), name, path, 1)

    @Before
    fun setup() {
        provider = TestProvider()
        ShadowContentResolver.registerProviderInternal("media", provider)
        mover = GalleryPhotoMover(ApplicationProvider.getApplicationContext<Context>().contentResolver)
    }

    @Test
    fun collisionIsDetectedBeforeAnyWriteIncludingCaseDifferences() {
        val first = photo(1, "One.jpg")
        val second = photo(2, "Two.jpg")
        provider.rows += listOf(first, second, photo(3, "TWO.JPG", destination))
        val result = mover.move(GalleryPhotoMovePolicy.plan(listOf(first, second), emptyList(), destination, false))
        assertEquals(0, result.moved)
        assertEquals(setOf(1L, 2L), result.failedIds)
        assertEquals(emptyList<Long>(), provider.updates)
        assertEquals(source, provider.rows.first().relativePath)
    }

    @Test
    fun partialPermissionFailureReportsExactIdsAndRetrySkipsAlreadyMovedFiles() {
        val first = photo(1, "One.jpg")
        val second = photo(2, "Two.jpg")
        provider.rows += listOf(first, second)
        provider.deniedIds += 2L
        val plan = GalleryPhotoMovePolicy.plan(listOf(first, second), emptyList(), destination, false)
        val result = mover.move(plan)
        assertEquals(setOf(1L), result.movedIds)
        assertEquals(setOf(2L), result.failedIds)
        assertEquals(1, result.moved)
        assertEquals(1, result.failed)
        assertEquals(source, provider.rows.first { it.id == 2L }.relativePath)
        provider.deniedIds.clear()
        val retry = mover.move(plan)
        assertEquals(setOf(1L, 2L), retry.movedIds)
        assertEquals(emptySet<Long>(), retry.failedIds)
        assertEquals(listOf(1L, 2L, 2L), provider.updates)
    }

    @Test
    fun failedQueryDoesNotWriteOrPretendTheDestinationIsEmpty() {
        val selected = photo(1, "One.jpg")
        provider.rows += selected
        provider.failQuery = true
        val result = mover.move(GalleryPhotoMovePolicy.plan(listOf(selected), emptyList(), destination, false))
        assertEquals(setOf(1L), result.failedIds)
        assertEquals(emptyList<Long>(), provider.updates)
    }

    @Test
    fun changedSourceMetadataBlocksTheWholeBatchBeforeAnyWrite() {
        val first = photo(1, "One.jpg")
        val second = photo(2, "Two.jpg")
        provider.rows += listOf(first, second.copy(relativePath = "Pictures/DZlog/Changed/"))
        val result = mover.move(GalleryPhotoMovePolicy.plan(listOf(first, second), emptyList(), destination, false))
        assertEquals(setOf(1L, 2L), result.failedIds)
        assertEquals(emptyList<Long>(), provider.updates)
    }

    @Test
    fun providerRenamingAFileIsReportedAsUnverifiedRatherThanComplete() {
        val selected = photo(1, "One.jpg")
        provider.rows += selected
        provider.renameOnUpdate = true
        val result = mover.move(GalleryPhotoMovePolicy.plan(listOf(selected), emptyList(), destination, false))
        assertEquals(0, result.moved)
        assertEquals(setOf(1L), result.failedIds)
        assertEquals("Changed.jpg", provider.rows.single().displayName)
    }

    private class TestProvider : ContentProvider() {
        val rows = mutableListOf<MediaImageItem>()
        val updates = mutableListOf<Long>()
        val deniedIds = mutableSetOf<Long>()
        var failQuery = false
        var renameOnUpdate = false
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            check(!failQuery) { "Provider unavailable" }
            val columns = requireNotNull(projection)
            val id = uri.lastPathSegment?.toLongOrNull()
            return MatrixCursor(columns).apply {
                rows.filter { if (id != null) it.id == id else selectionArgs == null || it.relativePath in selectionArgs }
                    .forEach { photo -> addRow(columns.map { column -> when (column) {
                        MediaStore.Images.Media.DISPLAY_NAME -> photo.displayName
                        MediaStore.Images.Media.RELATIVE_PATH -> photo.relativePath
                        else -> null
                    } }.toTypedArray()) }
            }
        }
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
            val id = requireNotNull(uri.lastPathSegment?.toLongOrNull())
            updates += id
            if (id in deniedIds) throw SecurityException("Denied")
            val index = rows.indexOfFirst { it.id == id }
            if (index < 0) return 0
            rows[index] = rows[index].copy(relativePath = requireNotNull(values?.getAsString(MediaStore.Images.Media.RELATIVE_PATH)),
                displayName = if (renameOnUpdate) "Changed.jpg" else rows[index].displayName)
            return 1
        }
        override fun getType(uri: Uri) = "image/jpeg"
        override fun insert(uri: Uri, values: ContentValues?) = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
