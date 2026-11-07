package com.dudoziworkshop.dzlog.data.project

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.project.CaptureMediaPresence

/**
 * Exact MediaStore lookups (including images moved outside DZlog).
 * Null cursors and read errors are UNKNOWN; only a successful zero-row
 * response confirms absence. Run on Dispatchers.IO.
 */
internal class CaptureMediaPresenceReader(private val resolver: ContentResolver) {
    internal data class Inspection(
        val observations: Map<String, CaptureMediaPresence>,
        val found: List<MediaImageItem>,
    )

    fun inspect(
        referencedUris: Set<String>,
        knownDzlogPhotos: List<MediaImageItem>,
    ): Inspection {
        val known = knownDzlogPhotos.associateBy { it.uri.toString() }
        val observations = linkedMapOf<String, CaptureMediaPresence>()
        val movedPhotos = mutableListOf<MediaImageItem>()
        referencedUris.forEach { raw ->
            val existing = known[raw]
            if (existing != null) {
                observations[raw] = CaptureMediaPresence.Present(existing.relativePath)
            } else {
                val (presence, photo) = inspectOne(raw)
                observations[raw] = presence
                photo?.let(movedPhotos::add)
            }
        }
        return Inspection(observations, movedPhotos)
    }

    private fun inspectOne(raw: String): Pair<CaptureMediaPresence, MediaImageItem?> {
        return try {
            val uri = Uri.parse(raw)
            if (uri.scheme != "content" || uri.authority != "media" ||
                uri.pathSegments.dropLast(1).takeLast(2) != listOf("images", "media")
            ) return CaptureMediaPresence.Unknown to null
            val expectedId = ContentUris.parseId(uri)
            if (expectedId < 0) return CaptureMediaPresence.Unknown to null
            val columns = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.DATE_ADDED,
            )
            val cursor = resolver.query(uri, columns, null, null, null)
                ?: return CaptureMediaPresence.Unknown to null
            cursor.use {
                if (!it.moveToFirst()) return CaptureMediaPresence.ConfirmedMissing to null
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val path = it.getString(
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                ).orEmpty()
                if (it.count != 1 || id != expectedId || path.isBlank()) {
                    return CaptureMediaPresence.Unknown to null
                }
                val photo = MediaImageItem(
                    id = id,
                    uri = uri,
                    displayName = it.getString(
                        it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    ).orEmpty(),
                    relativePath = path,
                    dateAddedSeconds = it.getLong(
                        it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    ),
                )
                CaptureMediaPresence.Present(path) to photo
            }
        } catch (_: Exception) {
            CaptureMediaPresence.Unknown to null
        }
    }
}
