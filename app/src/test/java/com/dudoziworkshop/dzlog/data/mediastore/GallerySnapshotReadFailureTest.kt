package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
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
class GallerySnapshotReadFailureTest {
    private val provider = TestProvider()
    private lateinit var reader: DzlogMediaStoreReader
    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        provider.attachInfo(context, ProviderInfo().apply { authority = "media" })
        ShadowContentResolver.registerProviderInternal("media", provider)
        reader = DzlogMediaStoreReader(context.contentResolver)
    }
    @Test fun nullPrefixQueryIsAnErrorRatherThanAnEmptyGallery() {
        provider.nullCursor = true
        assertThrows(IllegalStateException::class.java) { reader.loadImagesUnderPrefix("Pictures/DZlog/") }
    }
    @Test fun nullQueryCannotProduceOrReplaceAGallerySnapshot() {
        provider.nullCursor = true
        assertThrows(IllegalStateException::class.java) { reader.loadGallerySnapshot("Pictures/DZlog/") }
    }
    @Test fun readableEmptyQueryStillRepresentsAnEmptyGallery() {
        assertEquals(emptyList<Any>(), reader.loadImagesUnderPrefix("Pictures/DZlog/"))
        assertEquals(0, reader.loadGallerySnapshot("Pictures/DZlog/").allPhotos.size)
    }
    @Test fun deniedQueryIsPropagatedForTheCallerToKeepItsLastSnapshot() {
        provider.denied = true
        assertThrows(SecurityException::class.java) { reader.loadGallerySnapshot("Pictures/DZlog/") }
    }
    @Test fun successfulPrefixQueryKeepsPhotosAndOriginals() {
        provider.withPhotos = true
        val photos = reader.loadImagesUnderPrefix("Pictures/DZlog/")
        assertEquals(listOf(2L, 1L), photos.map { it.id })
        assertEquals(listOf("Pictures/DZlog/A/", "Pictures/DZlog/A/original/"), photos.map { it.relativePath })
    }
    private class TestProvider : ContentProvider() {
        var nullCursor = false
        var denied = false
        var withPhotos = false
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            if (denied) throw SecurityException("Denied")
            if (nullCursor) return null
            val columns = requireNotNull(projection)
            return MatrixCursor(columns).apply {
                if (withPhotos) listOf(2L to "Pictures/DZlog/A/", 1L to "Pictures/DZlog/A/original/").forEach { (id, path) ->
                    addRow(columns.map { column -> when (column) {
                        MediaStore.Images.Media._ID -> id
                        MediaStore.Images.Media.DISPLAY_NAME -> "$id.jpg"
                        MediaStore.Images.Media.RELATIVE_PATH -> path
                        MediaStore.Images.Media.DATE_ADDED -> id
                        else -> error(column)
                    } }.toTypedArray())
                }
            }
        }
        override fun getType(uri: Uri) = "image/jpeg"
        override fun insert(uri: Uri, values: ContentValues?): Uri? = error("read only")
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = error("read only")
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = error("read only")
    }
}
