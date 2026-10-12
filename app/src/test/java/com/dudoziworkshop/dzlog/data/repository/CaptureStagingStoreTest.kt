package com.dudoziworkshop.dzlog.data.repository

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class CaptureStagingStoreTest {
    @Test fun stagedImageSurvivesReopeningStore() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = UUID.randomUUID().toString()
        val input = File(context.cacheDir, "$id-source.jpg")
        val bytes = byteArrayOf(0x01, 0x23, 0x45, 0x67)
        input.writeBytes(bytes)
        try {
            val record = CaptureStagingStore(context).stage(
                id = id,
                sourceJpeg = input,
                counter = 7,
                relativePathKey = "Pictures/DZlog/test/",
                streamPrefix = "A_",
                displayName = "A_7.jpg",
                createdAtMillis = 1234L,
            )
            assertArrayEquals(bytes, record.originalFile.readBytes())
            val recovered = CaptureStagingStore(context).recoverable().first { it.id == id }
            assertEquals(7, recovered.counter)
            assertEquals("A_7.jpg", recovered.displayName)
            assertArrayEquals(bytes, recovered.originalFile.readBytes())
        } finally {
            input.delete()
            File(context.filesDir, "capture-staging-v1/$id.jpg").delete()
            File(context.filesDir, "capture-staging-v1/$id.json").delete()
        }
    }

    @Test fun rejectsMissingSourceWithoutCreatingJournal() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = UUID.randomUUID().toString()
        val invalid = File(context.cacheDir, "$id-missing.jpg")
        var rejected = false
        try {
            CaptureStagingStore(context).stage(
                id, invalid, 1, "Pictures/DZlog/", "A_", "A_1.jpg", 1L
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertTrue(!File(context.filesDir, "capture-staging-v1/$id.json").exists())
    }
}
