package com.dudoziworkshop.dzlog.domain.project

/**
 * Immutable shutter-time assignment. Never resolve the currently selected
 * project or template again when an asynchronous save finishes.
 */
data class CaptureReservation(
    val captureId: String,
    val projectId: String?,
    val templateId: String?,
    val capturedAtMillis: Long,
) {
    init {
        require(captureId.isNotBlank()) { "Capture ID is required" }
        require(projectId == null || projectId.isNotBlank()) { "Project ID must not be blank" }
        require(templateId == null || templateId.isNotBlank()) { "Template ID must not be blank" }
        require(capturedAtMillis >= 0L) { "Capture timestamp must be nonnegative" }
    }

    /**
     * Call only after the save coordinator confirms its committed media.
     * An empty or failed receipt cannot create a capture record.
     */
    fun committed(
        originalUri: String? = null,
        resultUri: String? = null,
        originalRelativePath: String? = null,
        resultRelativePath: String? = null,
    ): CaptureRecord? {
        if (originalUri == null && resultUri == null) return null
        return CaptureRecord(
            captureId = captureId,
            projectId = projectId,
            templateId = templateId,
            capturedAtMillis = capturedAtMillis,
            originalUri = originalUri,
            resultUri = resultUri,
            originalRelativePath = originalRelativePath,
            resultRelativePath = resultRelativePath,
        )
    }
}

/** One successfully committed shutter press, regardless of save mode. */
data class CaptureRecord(
    val captureId: String,
    val projectId: String?,
    val templateId: String?,
    val capturedAtMillis: Long,
    val originalUri: String? = null,
    val resultUri: String? = null,
    val originalRelativePath: String? = null,
    val resultRelativePath: String? = null,
) {
    init {
        require(captureId.isNotBlank()) { "Capture ID is required" }
        require(projectId == null || projectId.isNotBlank()) { "Project ID must not be blank" }
        require(templateId == null || templateId.isNotBlank()) { "Template ID must not be blank" }
        require(capturedAtMillis >= 0L) { "Capture timestamp must be nonnegative" }
        require(!originalUri.isNullOrBlank() || !resultUri.isNullOrBlank()) {
            "A committed capture must have at least one media URI"
        }
        require(originalUri == null || originalUri.isNotBlank()) { "Original URI must not be blank" }
        require(resultUri == null || resultUri.isNotBlank()) { "Result URI must not be blank" }
        require(originalUri == null || resultUri == null || originalUri != resultUri) {
            "Original and result must have different URIs"
        }
        require(originalRelativePath == null || originalUri != null) {
            "Original path requires an original URI"
        }
        require(resultRelativePath == null || resultUri != null) {
            "Result path requires a result URI"
        }
    }

    val displayUri: String get() = resultUri ?: requireNotNull(originalUri)
    val mediaUris: List<String> get() = listOfNotNull(originalUri, resultUri)

    /** Pass only *verified* deleted URIs, never attempted or cancelled deletes. */
    fun afterVerifiedDeletion(deletedUris: Set<String>): CaptureRecord? {
        if (deletedUris.isEmpty()) return this
        val remainingOriginal = originalUri?.takeUnless(deletedUris::contains)
        val remainingResult = resultUri?.takeUnless(deletedUris::contains)
        if (remainingOriginal == null && remainingResult == null) return null
        return copy(
            originalUri = remainingOriginal,
            resultUri = remainingResult,
            originalRelativePath = originalRelativePath.takeIf { remainingOriginal != null },
            resultRelativePath = resultRelativePath.takeIf { remainingResult != null },
        )
    }
}

/**
 * Small immutable-snapshot policy; long-term storage and lookups must use a
 * repository with indexed capture IDs and media URIs (not a growing JSON list).
 */
object CaptureRecordPolicy {
    fun append(records: List<CaptureRecord>, incoming: CaptureRecord): List<CaptureRecord> {
        val previous = records.firstOrNull { it.captureId == incoming.captureId }
        if (previous != null) {
            require(previous == incoming) { "Conflicting receipt for capture ID" }
            return records
        }
        val incomingUris = incoming.mediaUris.toSet()
        require(records.none { record -> record.mediaUris.any(incomingUris::contains) }) {
            "Media URI already belongs to another capture"
        }
        return records + incoming
    }

    fun afterVerifiedDeletion(
        records: List<CaptureRecord>,
        deletedUris: Set<String>,
    ): List<CaptureRecord> = records.mapNotNull { it.afterVerifiedDeletion(deletedUris) }

    fun forProject(records: List<CaptureRecord>, projectId: String): List<CaptureRecord> =
        records.filter { it.projectId == projectId }
            .sortedWith(compareByDescending<CaptureRecord> { it.capturedAtMillis }
                .thenByDescending { it.captureId })
}
