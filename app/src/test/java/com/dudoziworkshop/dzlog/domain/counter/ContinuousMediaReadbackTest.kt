package com.dudoziworkshop.dzlog.domain.counter

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContinuousMediaReadbackTest {
    @Test
    fun real_scanner_aggregates_known_prefixes_respects_paths_and_refreshes_after_delete() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val path = "Pictures/DZlog/test/${UUID.randomUUID()}/"
        val provider = TestMediaProvider()
        ShadowContentResolver.registerProviderInternal("media", provider)
        val scan = CounterScanPrefixes.encode(listOf(listOf("T1"), listOf("A", "B", "C")))
        provider.rows += listOf(
            "T1_A_0001.jpg" to path, "T1_B_0004.jpg" to path,
            "T1_C_0003.jpg" to path, "OTHER_A_9999.jpg" to path,
            "T1_A_8888.jpg" to "Pictures/Other/", "T1_B_0007.jpg" to "${path}original/",
        )
        suspend fun next(mode: SaveMode, scopePath: String = path) = CounterManager.computeNext(
            context, scopePath, "stream", scan, 4, "_", mode)
        assertEquals(5, next(SaveMode.WATERMARK_ONLY))
        assertEquals(8, next(SaveMode.ORIGINAL_ONLY))
        assertEquals(8, next(SaveMode.BOTH))
        provider.rows.remove("T1_B_0004.jpg" to path)
        assertEquals(4, next(SaveMode.WATERMARK_ONLY))
        // Original-only request already carries original/ after resolver normalization.
        assertEquals(8, next(SaveMode.ORIGINAL_ONLY, "${path}original/"))
    }

    @Test
    fun failed_media_scan_is_not_reported_as_empty_history_or_next_one() = runBlocking {
        val provider = TestMediaProvider().apply { failQuery = true }
        ShadowContentResolver.registerProviderInternal("media", provider)
        val result = runCatching { CounterManager.computeNext(ApplicationProvider.getApplicationContext(),
            "Pictures/DZlog/", "stream", "T1_A", 4, "_", SaveMode.WATERMARK_ONLY) }
        assertEquals(true, result.isFailure)
    }

    private class TestMediaProvider : ContentProvider() {
        val rows = mutableListOf<Pair<String, String>>()
        var failQuery = false
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            check(!failQuery) { "MediaStore unavailable" }
            val columns = projection ?: arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.RELATIVE_PATH)
            return MatrixCursor(columns).apply {
                rows.filter { selectionArgs == null || it.second in selectionArgs }.forEach { (name, path) ->
                    addRow(columns.map { column -> when (column) {
                        MediaStore.Images.Media.DISPLAY_NAME -> name
                        MediaStore.Images.Media.RELATIVE_PATH -> path
                        else -> null
                    } }.toTypedArray())
                }
            }
        }
        override fun getType(uri: Uri) = "image/jpeg"
        override fun insert(uri: Uri, values: ContentValues?) = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
