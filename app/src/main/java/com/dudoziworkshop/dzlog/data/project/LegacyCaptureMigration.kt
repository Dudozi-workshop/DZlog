package com.dudoziworkshop.dzlog.data.project

import com.dudoziworkshop.dzlog.domain.project.CaptureProjectRegistry
import com.dudoziworkshop.dzlog.domain.project.CaptureRecord
import com.dudoziworkshop.dzlog.domain.project.CaptureReservation
import java.security.MessageDigest

/**
 * A MediaStore row that was actually observed and remains readable.
 * Never infer a media item from a v1 URI reference alone.
 */
internal data class LegacyCapturePhoto(
    val uri: String,
    val relativePath: String,
    val dateAddedSeconds: Long = 0L,
)

internal enum class LegacyCaptureGap {
    EMPTY_GROUP,
    TOO_MANY_URIS,
    MISSING_MEDIA,
    PARTIAL_MEDIA,
    AMBIGUOUS_MEDIA_ROLES,
}

internal data class LegacyCaptureIssue(
    val projectId: String,
    val groupIndex: Int,
    val reason: LegacyCaptureGap,
)

internal data class LegacyCaptureCandidate(
    val projectId: String,
    val groupIndex: Int,
    val record: CaptureRecord,
)

internal data class LegacyCapturePlan(
    val candidates: List<LegacyCaptureCandidate>,
    val issues: List<LegacyCaptureIssue>,
)

/**
 * Read-only projection of the old DataStore v1 URI groups. It never touches
 * the original DataStore or any physical photos. A group ID depends on the
 * entire original v1 URI set, not the currently available media subset.
 */
internal object LegacyCaptureMigrationPolicy {
    fun plan(
        registry: CaptureProjectRegistry,
        availablePhotos: List<LegacyCapturePhoto>,
    ): LegacyCapturePlan {
        val byUri = availablePhotos
            .filter { it.uri.isNotBlank() && it.relativePath.startsWith("Pictures/DZlog/") }
            .associateBy { it.uri }
        val records = mutableListOf<LegacyCaptureCandidate>()
        val issues = mutableListOf<LegacyCaptureIssue>()

        registry.projects.forEach { project ->
            project.captureUriGroups.forEachIndexed { index, savedGroup ->
                val savedUris = savedGroup.map(String::trim).filter(String::isNotBlank).distinct()
                fun issue(reason: LegacyCaptureGap) {
                    issues += LegacyCaptureIssue(project.id, index, reason)
                }

                when {
                    savedUris.isEmpty() -> issue(LegacyCaptureGap.EMPTY_GROUP)
                    savedUris.size > 2 -> issue(LegacyCaptureGap.TOO_MANY_URIS)
                    else -> {
                        val found = savedUris.mapNotNull(byUri::get)
                        if (found.isEmpty()) {
                            issue(LegacyCaptureGap.MISSING_MEDIA)
                        } else {
                            val originals = found.filter { isOriginalPath(it.relativePath) }
                            val results = found.filterNot { isOriginalPath(it.relativePath) }
                            if (originals.size > 1 || results.size > 1) {
                                issue(LegacyCaptureGap.AMBIGUOUS_MEDIA_ROLES)
                            } else {
                                if (found.size < savedUris.size) issue(LegacyCaptureGap.PARTIAL_MEDIA)
                                // The old format has no shutter timestamp. Use the
                                // observed media date as an approximation, not a claim
                                // that an exact capture-time value was recovered.
                                val mediaMillis = found.mapNotNull { photo ->
                                    photo.dateAddedSeconds.takeIf {
                                        it > 0L && it < Long.MAX_VALUE / 1000L
                                    }?.times(1000L)
                                }.maxOrNull()
                                val base = maxOf(project.createdAt.coerceAtLeast(0L), mediaMillis ?: 0L)
                                val offset = index.toLong()
                                val at = if (base <= Long.MAX_VALUE - offset) base + offset else base
                                val captured = requireNotNull(
                                    CaptureReservation(
                                        captureId = stableLegacyId(project.id, savedUris),
                                        projectId = project.id,
                                        templateId = null,
                                        capturedAtMillis = at,
                                    ).committed(
                                        originalUri = originals.singleOrNull()?.uri,
                                        resultUri = results.singleOrNull()?.uri,
                                        originalRelativePath = originals.singleOrNull()?.relativePath,
                                        resultRelativePath = results.singleOrNull()?.relativePath,
                                    )
                                )
                                records += LegacyCaptureCandidate(project.id, index, captured)
                            }
                        }
                    }
                }
            }
        }
        return LegacyCapturePlan(records, issues)
    }

    private fun isOriginalPath(path: String): Boolean =
        path.trimEnd('/').substringAfterLast('/')
            .equals("original", ignoreCase = true)

    private fun stableLegacyId(projectId: String, originalUris: List<String>): String {
        val input = ("dzq021:v1\u0000" + projectId + "\u0000" +
            originalUris.sorted().joinToString("\u0000")).toByteArray(Charsets.UTF_8)
        val hash = MessageDigest.getInstance("SHA-256").digest(input)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "legacy-v1-$hash"
    }
}

internal data class LegacyCaptureMigrationReport(
    val inserted: Int,
    val alreadyIndexed: Int,
    val unresolved: List<LegacyCaptureIssue>,
    val conflicts: List<LegacyCaptureCandidate>,
)

/**
 * Incremental, idempotent import: the v1 JSON groups are deliberately kept
 * untouched as a recovery source. Fail closed if ownership conflicts.
 * The caller must first perform a real MediaStore read (e.g. on results entry).
 */
internal class LegacyCaptureMigration(private val store: CaptureRecordRoomStore) {
    suspend fun importAvailable(
        registry: CaptureProjectRegistry,
        availablePhotos: List<LegacyCapturePhoto>,
    ): LegacyCaptureMigrationReport {
        val plan = LegacyCaptureMigrationPolicy.plan(registry, availablePhotos)
        var inserted = 0
        var indexed = 0
        val conflicts = mutableListOf<LegacyCaptureCandidate>()

        for (candidate in plan.candidates) {
            val record = candidate.record
            val sameId = store.findById(record.captureId)
            if (sameId != null) {
                // Do not re-add media that was previously verified as deleted.
                if (sameId.projectId == record.projectId) indexed++ else conflicts += candidate
                continue
            }
            val owners = record.mediaUris.mapNotNull { store.findByMediaUri(it) }
                .distinctBy { it.captureId }
            if (owners.isNotEmpty()) {
                // DataStore v1 and the new journal may both reference a capture.
                // Do not rewrite an existing capture ID or project association.
                if (owners.size == 1 && owners.single().projectId == record.projectId) {
                    indexed++
                } else {
                    conflicts += candidate
                }
                continue
            }
            try {
                if (store.appendCommitted(record)) inserted++ else indexed++
            } catch (conflict: IllegalArgumentException) {
                conflicts += candidate
            } catch (conflict: IllegalStateException) {
                conflicts += candidate
            }
        }
        return LegacyCaptureMigrationReport(
            inserted = inserted,
            alreadyIndexed = indexed,
            unresolved = plan.issues,
            conflicts = conflicts,
        )
    }
}
