package com.dudoziworkshop.dzlog.domain.project

/**
 * An exact MediaStore URI probe, never a folder-scan inference.
 *
 * Unknown (including revoked permissions or a null cursor) MUST NOT remove
 * recorded media. A successful query returning zero rows is ConfirmedMissing.
 */
internal sealed interface CaptureMediaPresence {
    data class Present(val relativePath: String) : CaptureMediaPresence {
        init { require(relativePath.isNotBlank()) }
    }
    data object ConfirmedMissing : CaptureMediaPresence
    data object Unknown : CaptureMediaPresence
}

/**
 * External moves only change stored paths. Missing one image of a BOTH capture
 * preserves the shutter and its remaining image. The project ID, capture ID,
 * template, timestamp and counter/Undo state are never modified.
 */
internal object CaptureMediaReconcilePolicy {
    fun reconcile(
        record: CaptureRecord,
        probes: Map<String, CaptureMediaPresence>,
    ): CaptureRecord? {
        fun update(uri: String?, path: String?): Pair<String?, String?> {
            if (uri == null) return null to null
            return when (val result = probes[uri] ?: CaptureMediaPresence.Unknown) {
                is CaptureMediaPresence.Present -> uri to result.relativePath
                CaptureMediaPresence.ConfirmedMissing -> null to null
                CaptureMediaPresence.Unknown -> uri to path
            }
        }

        val (newOriginal, newOriginalPath) = update(record.originalUri, record.originalRelativePath)
        val (newResult, newResultPath) = update(record.resultUri, record.resultRelativePath)
        if (newOriginal == null && newResult == null) return null
        if (newOriginal == record.originalUri && newOriginalPath == record.originalRelativePath &&
            newResult == record.resultUri && newResultPath == record.resultRelativePath
        ) return record
        return record.copy(
            originalUri = newOriginal,
            resultUri = newResult,
            originalRelativePath = newOriginalPath,
            resultRelativePath = newResultPath,
        )
    }
}
