package com.dudoziworkshop.dzlog.domain.project

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
