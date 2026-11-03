package com.dudoziworkshop.dzlog.data.project

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletion
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.project.CaptureMediaPresence
import com.dudoziworkshop.dzlog.domain.project.ProjectMediaDeletePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Snapshot of exact per-file Android deletion targets, never a folder. */
internal data class PreparedProjectMediaDeletion(
    val projectId: String,
    val originalCaptureIds: Set<String>,
    val expectedUris: List<String>,
    val photos: List<MediaImageItem>,
) {
    val fileCount: Int get() = photos.size
}

/**
 * Destructive MediaStore planning and verification.
 *
 * This class never deletes files itself: the Android gallery confirmation
 * requests explicit permission for the frozen [PreparedProjectMediaDeletion.photos].
 * Metadata is removed only after every referenced URI is confirmed absent.
 */
internal class ProjectMediaDeleteCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val projects = CaptureProjectStore(appContext)
    private val records = CaptureRecordRoomStore(appContext)
    private val legacy = LegacyCaptureMigration(records)
    private val reader = DzlogMediaStoreReader(appContext.contentResolver)
    private val presence = CaptureMediaPresenceReader(appContext.contentResolver)

    suspend fun prepare(projectId: String): PreparedProjectMediaDeletion = withContext(Dispatchers.IO) {
        val registry = projects.load()
        requireNotNull(registry.find(projectId)) { "프로젝트가 존재하지 않습니다." }
        val dzlogPhotos = reader.loadImagesUnderPrefix("Pictures/DZlog/")
        val report = legacy.importAvailable(registry, dzlogPhotos.map {
            LegacyCapturePhoto(
                uri = it.uri.toString(),
                relativePath = it.relativePath,
                dateAddedSeconds = it.dateAddedSeconds,
            )
        })
        check(report.conflicts.isEmpty()) {
            "다른 촬영 기록과 소속이 충돌합니다. 삭제하지 않고 기록을 유지했습니다."
        }
        check(report.unresolved.none {
            it.projectId == projectId && it.reason in setOf(
                LegacyCaptureGap.EMPTY_GROUP,
                LegacyCaptureGap.TOO_MANY_URIS,
                LegacyCaptureGap.AMBIGUOUS_MEDIA_ROLES,
            )
        }) { "구형 촬영 기록이 불명확합니다. 삭제 전에 확인이 필요합니다." }

        val projectRecords = records.forProject(projectId)
        val rawPlan = ProjectMediaDeletePolicy.plan(
            projectId = projectId,
            registry = registry,
            records = projectRecords,
            availableUris = dzlogPhotos.mapTo(mutableSetOf()) { it.uri.toString() },
        )
        val expected = (rawPlan.targetUris + rawPlan.unresolvedUris).distinct()

        // No current record or v1 group in another project may own a target.
        val otherV1Uris = registry.projects.filterNot { it.id == projectId }
            .flatMapTo(mutableSetOf()) { it.captureUriGroups.flatten() }
        check(expected.none(otherV1Uris::contains)) {
            "다른 프로젝트가 참조하는 사진은 삭제할 수 없습니다."
        }
        expected.forEach { raw ->
            val uri = Uri.parse(raw)
            val id = runCatching { ContentUris.parseId(uri) }.getOrNull()
            require(uri.scheme == "content" && uri.authority == "media" &&
                uri.pathSegments.dropLast(1).takeLast(2) == listOf("images", "media") &&
                id != null && id >= 0L) {
                "올바르지 않은 사진 URI가 있어 삭제를 중단했습니다."
            }
            val owner = records.findByMediaUri(raw)
            check(owner == null || owner.projectId == projectId) {
                "다른 촬영 대상과 연결된 사진은 삭제할 수 없습니다."
            }
        }

        val inspected = presence.inspect(expected.toSet(), dzlogPhotos)
        check(inspected.observations.values.none { it == CaptureMediaPresence.Unknown }) {
            "사진을 확인할 수 없습니다. 권한과 파일 위치를 확인한 뒤 다시 시도해 주세요."
        }
        check(inspected.observations.values.none {
            it is CaptureMediaPresence.Present &&
                !it.relativePath.startsWith("Pictures/DZlog/")
        }) {
            "DZlog 폴더 밖으로 이동한 사진은 현재 일괄 삭제할 수 없습니다. 위치를 확인해 주세요."
        }
        val inFolder = dzlogPhotos.associateBy { it.uri.toString() }
        val selected = expected.mapNotNull { raw ->
            if (inspected.observations[raw] is CaptureMediaPresence.Present) {
                requireNotNull(inFolder[raw]) { "사진 목록이 변경됐습니다. 다시 확인해 주세요." }
            } else null
        }
        if (selected.isNotEmpty()) {
            // The same gallery validator checks IDs, names and paths later,
            // immediately before Android's per-photo delete permission request.
            GalleryPhotoDeletion.prepare(selected, dzlogPhotos, includeOriginals = false)
        }
        PreparedProjectMediaDeletion(
            projectId = projectId,
            originalCaptureIds = projectRecords.mapTo(mutableSetOf()) { it.captureId },
            expectedUris = expected,
            photos = selected,
        )
    }

    /**
     * Called after Android's deletion UI closes. A cancelled or partially
     * successful batch cannot finalize a project. Unknown reads fail closed.
     * Verified missing URIs may still be reflected in Room to avoid phantom counts.
     */
    suspend fun finalizeAfterAndroid(
        prepared: PreparedProjectMediaDeletion,
    ): Boolean = withContext(Dispatchers.IO) {
        val registry = projects.load()
        requireNotNull(registry.find(prepared.projectId)) { "프로젝트가 이미 삭제됐습니다." }
        val inspected = presence.inspect(prepared.expectedUris.toSet(), emptyList())
        val confirmedAbsent = inspected.observations.filterValues {
            it == CaptureMediaPresence.ConfirmedMissing
        }.keys
        if (confirmedAbsent.isNotEmpty()) {
            records.afterVerifiedDeletion(confirmedAbsent)
        }
        val current = records.forProject(prepared.projectId)
        val newCaptureFound = current.any { it.captureId !in prepared.originalCaptureIds }
        val canFinish = ProjectMediaDeletePolicy.canFinalize(
            expectedUris = prepared.expectedUris,
            verifiedAbsentUris = confirmedAbsent,
            newCaptureIdsDetected = newCaptureFound,
        )
        if (!canFinish) return@withContext false
        // Recheck the project metadata (including legacy groups) before removal.
        val latest = projects.load()
        val project = requireNotNull(latest.find(prepared.projectId))
        val nowExpected = (current.flatMap { it.mediaUris } +
            project.captureUriGroups.flatten()).distinct()
        check(nowExpected.all(prepared.expectedUris::contains)) {
            "삭제 확인 중 새로운 사진이 연결됐습니다. 프로젝트를 유지합니다."
        }
        projects.deleteProject(prepared.projectId)
        true
    }
}
