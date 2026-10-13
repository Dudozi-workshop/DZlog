package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileNotFoundException
import org.junit.After
import org.junit.Assert.assertEquals
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
class MediaStoreSaverPublishTest {
    private lateinit var provider: TestMediaProvider
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val saver = MediaStoreSaverImpl()

    @Before fun setUp() {
        provider = TestMediaProvider()
        ShadowContentResolver.registerProviderInternal("media", provider)
    }

    @After fun tearDown() {
        provider.outputFile.delete()
    }

    @Test fun successfulPublishReturnsCommittedUriWithoutCleanup() {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        try {
            val saved = saver.saveJpeg(
                context, bitmap, "Capture_1.jpg", "Pictures/DZlog/", 90
            )
            assertEquals(71L, saved.mediaStoreId)
            assertEquals("Capture_1.jpg", saved.displayName)
            assertEquals(1, provider.publishCalls)
            assertEquals(0, provider.deleted.size)
            // Robolectric does not guarantee JPEG bytes from Bitmap.compress; real JPEG bytes are checked by emulator E2E.
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun updateZeroMustFailAndDeleteThePendingMediaItem() {
        provider.publishRows = 0
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        try {
            assertThrows(IllegalStateException::class.java) {
                saver.saveJpeg(
                    context, bitmap, "Capture_2.jpg", "Pictures/DZlog/", 90
                )
            }
            assertEquals(1, provider.publishCalls)
            assertEquals(listOf(71L), provider.deleted)
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun publishExceptionMustFailAndCleanUp() {
        provider.failPublish = true
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        try {
            assertThrows(IllegalStateException::class.java) {
                saver.saveJpeg(
                    context, bitmap, "Capture_3.jpg", "Pictures/DZlog/", 90
                )
            }
            assertEquals(listOf(71L), provider.deleted)
        } finally {
            bitmap.recycle()
        }
    }

    private class TestMediaProvider : ContentProvider() {
        val outputFile: File = File.createTempFile("dzlog-publish-test-", ".jpg")
        var publishRows = 1
        var failPublish = false
        var publishCalls = 0
        val deleted = mutableListOf<Long>()

        override fun onCreate() = true
        override fun getType(uri: Uri) = "image/jpeg"
        override fun query(
            uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?
        ): Cursor = MatrixCursor(requireNotNull(projection))

        override fun insert(uri: Uri, values: ContentValues?): Uri =
            ContentUris.withAppendedId(uri, 71L)

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            if (uri.lastPathSegment != "71") throw FileNotFoundException(uri.toString())
            return ParcelFileDescriptor.open(
                outputFile,
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_WRITE_ONLY
            )
        }

        override fun update(
            uri: Uri, values: ContentValues?, selection: String?,
            selectionArgs: Array<out String>?
        ): Int {
            publishCalls++
            if (failPublish) throw IllegalStateException("Provider publish failed")
            return publishRows
        }

        override fun delete(
            uri: Uri, selection: String?, selectionArgs: Array<out String>?
        ): Int {
            deleted += ContentUris.parseId(uri)
            return 1
        }
    }
}
