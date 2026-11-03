package com.dudoziworkshop.dzlog.domain.project

import com.dudoziworkshop.dzlog.domain.quickresults.QuickResultCapture
import com.dudoziworkshop.dzlog.domain.quickresults.QuickResultPhoto

/**
 * Read-side projection only: missing MediaStore photos do not destroy the
 * recorded relationship or mutate camera Undo/counter/phrase state.
 */
internal data class VisibleProjectCaptures(
    val uriGroupsOldestFirst: List<List<String>>,
    val captureCount: Int,
    val latestUri: String?,
)

internal object ProjectCaptureReadPolicy {
    /**
     * Project originals/results are identified by immutable shutter receipts,
     * never by their current physical directory. An original may have moved
     * out of /original/ while still belonging to the same capture.
     */
    fun quickResults(
        records: List<CaptureRecord>,
        availablePhotos: List<QuickResultPhoto>,
    ): List<QuickResultCapture> {
        val byUri = availablePhotos.associateBy { it.uri }
        return records.sortedWith(
            compareBy<CaptureRecord> { it.capturedAtMillis }.thenBy { it.captureId },
        ).mapIndexedNotNull { index, record ->
            val result = record.resultUri?.let(byUri::get)
            val original = record.originalUri?.let(byUri::get)
            val display = result ?: original ?: return@mapIndexedNotNull null
            val physical = display.relativePath
            val folder = if (result == null &&
                physical.trimEnd('/').substringAfterLast('/').equals("original", ignoreCase = true)
            ) physical.trimEnd('/').substringBeforeLast('/') + "/" else physical
            QuickResultCapture(
                captureIndex = index,
                folderPath = folder,
                result = result,
                original = original,
            )
        }.asReversed()
    }

    fun visible(
        records: List<CaptureRecord>,
        availableUris: Set<String>,
    ): VisibleProjectCaptures {
        val groups = records.sortedWith(
            compareBy<CaptureRecord> { it.capturedAtMillis }.thenBy { it.captureId },
        ).mapNotNull { record ->
            // Result-first for thumbnails; one URI pair remains one shutter.
            val existing = listOfNotNull(record.resultUri, record.originalUri)
                .filter(availableUris::contains)
            existing.takeIf(List<String>::isNotEmpty)
        }
        return VisibleProjectCaptures(
            uriGroupsOldestFirst = groups,
            captureCount = groups.size,
            latestUri = groups.lastOrNull()?.firstOrNull(),
        )
    }
}
