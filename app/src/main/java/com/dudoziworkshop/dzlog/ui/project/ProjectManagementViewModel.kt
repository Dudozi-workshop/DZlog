package com.dudoziworkshop.dzlog.ui.project

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dudoziworkshop.dzlog.data.project.CaptureProjectStore
import com.dudoziworkshop.dzlog.data.project.CaptureRecordRoomStore
import com.dudoziworkshop.dzlog.data.project.LegacyCaptureMigration
import com.dudoziworkshop.dzlog.data.project.LegacyCapturePhoto
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletePlan
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletion
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.project.ProjectMediaDeletePolicy
import com.dudoziworkshop.dzlog.domain.project.CaptureRecord
import com.dudoziworkshop.dzlog.data.repository.CapturePendingStore
import com.dudoziworkshop.dzlog.domain.project.CaptureProjectRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Project metadata operations. Creating, renaming and browsing never select a
 * capture target; switching or completing the selected project must not race
 * a durable in-flight save or a pending receipt review.
 */
internal data class PreparedProjectMediaDeletion(
    val projectId: String,
    val originalCaptureIds: Set<String>,
    val expectedUris: List<String>,
    val photos: List<MediaImageItem>,
    val previouslyAbsentUris: Set<String>,
) {
    val fileCount: Int get() = photos.size
}

/**
 * The destructive workflow never deletes a folder. Only the existing gallery
 * deletion action may ask Android to delete these explicitly validated files.
 */
internal class ProjectManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val store = CaptureProjectStore(context)
    private val pending = CapturePendingStore(context)
    private val captureRecords = CaptureRecordRoomStore(context)
    private val legacy = LegacyCaptureMigration(captureRecords)
    private val galleryReader = DzlogMediaStoreReader(context.contentResolver)

    val registry: StateFlow<CaptureProjectRegistry> = store.changes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CaptureProjectRegistry(),
    )

    suspend fun create(name: String, initialTemplateId: String?): String =
        store.createProject(name, initialTemplateId).projects.last().id

    suspend fun rename(projectId: String, name: String) {
        require(name.trim().isNotBlank()) { "프로젝트 이름을 입력해 주세요." }
        store.rename(projectId, name)
    }

    suspend fun canChangeCaptureTarget(): Boolean = withContext(Dispatchers.IO) {
        val recovery = pending.inspectRecovery()
        recovery.ready.isEmpty() && recovery.needsReview.isEmpty() &&
            recovery.completedAwaitingApply.isEmpty() && recovery.orphanFiles.isEmpty() &&
            recovery.corruptJournals.isEmpty()
    }

    suspend fun select(projectId: String?) {
        check(canChangeCaptureTarget()) { "사진 저장 완료 후 프로젝트를 변경할 수 있습니다." }
        store.selectProject(projectId)
    }

    suspend fun rememberTemplate(templateId: String) {
        store.rememberTemplate(templateId)
    }

    suspend fun complete(projectId: String) {
        if (store.load().activeId == projectId) {
            check(canChangeCaptureTarget()) { "사진 저장 완료 후 프로젝트를 완료할 수 있습니다." }
        }
        store.completeProject(projectId)
    }

    suspend fun reopen(projectId: String) {
        // Reopening a completed project changes only its status, not the camera.
        store.reopenProject(projectId)
    }

    suspend fun deleteMetadataOnly(projectId: String) {
        check(canChangeCaptureTarget()) { "사진 저장 처리가 끝나면 삭제할 수 있습니다." }
        store.deleteProject(projectId)
        // Retain orphaned Room receipts for safe provenance. Files are untouched;
        // no project capture becomes a general-session capture.
    }

    private fun requireMediaUri(raw: String): Uri {
        val uri = Uri.parse(raw)
        require(uri.scheme == "content" && uri.authority == "media" &&
            uri.pathSegments.dropLast(1).takeLast(2) == listOf("images", "media") &&
            runCatching { ContentUris.parseId(uri) >= 0 }.getOrDefault(false)) {
            "DZlog MediaStore 사진의 URI만 삭제할 수 있습니다."
        }
        return uri
    }

    /** Missing from DZlog does NOT imply deleted; check the exact URI. */
    private fun isVerifiedAbsent(raw: String): Boolean {
        val uri = requireMediaUri(raw)
        val cursor = context.contentResolver.query(
            uri, arrayOf(MediaStore.Images.Media._ID), null, null, null,
        ) ?: error("사진의 존재 여부를 확인할 수 없습니다.")
        return cursor.use { !it.moveToFirst() }
    }

    suspend fun prepareMediaDeletion(projectId: String): PreparedProjectMediaDeletion =
        withContext(Dispatchers.IO) {
            check(canChangeCaptureTarget()) { "사진 저장 처리가 끝나면 삭제할 수 있습니다." }
            val registrySnapshot = store.load()
            val project = requireNotNull(registrySnapshot.find(projectId)) { "Unknown project" }
            val photos = galleryReader.loadImagesUnderPrefix("Pictures/DZlog/")
            val migration = legacy.importAvailable(
                registrySnapshot,
                photos.map { LegacyCapturePhoto(
                    uri = it.uri.toString(), relativePath = it.relativePath,
                    dateAddedSeconds = it.dateAddedSeconds,
                ) },
            )
            check(migration.conflicts.isEmpty()) {
                "프로젝트 간 사진 소속 충돌을 확인해 주세요. 사진은 삭제하지 않았습니다."
            }
            check(migration.unresolved.none { issue ->
                issue.projectId == projectId &&
                    issue.reason.name in setOf("TOO_MANY_URIS", "AMBIGUOUS_MEDIA_ROLES")
            }) { "기존 촬영 기록에 중복·모호한 사진이 있어 안전하게 삭제할 수 없습니다." }
            val captures = captureRecords.forProject(projectId)
            val byUri = photos.associateBy { it.uri.toString() }
            val plan = ProjectMediaDeletePolicy.plan(
                projectId, registrySnapshot, captures, byUri.keys,
            )
            val otherLegacyUris = registrySnapshot.projects.filterNot { it.id == project.id }
                .flatMap { it.captureUriGroups.flatten() }.toSet()
            check(plan.targetUris.none(otherLegacyUris::contains)) {
                "다른 프로젝트에서도 참조 중인 사진은 삭제하지 않습니다."
            }
            // An unlisted URI may have been moved out of DZlog, not deleted.
            // In that case fail closed rather than silently deleting project metadata.
            val verifiedAbsent = plan.unresolvedUris.filter { isVerifiedAbsent(it) }.toSet()
            check(verifiedAbsent.size == plan.unresolvedUris.size) {
                "DZlog 밖으로 이동한 사진이 있습니다. 사진 위치를 확인해 주세요."
            }
            val selected = plan.targetUris.map { uri ->
                requireNotNull(byUri[uri]) { "사진 목록이 바뀌었습니다." }
            }
            if (selected.isNotEmpty()) {
                GalleryPhotoDeletion.prepare(
                    selected, photos, includeOriginals = false,
                )
            }
            PreparedProjectMediaDeletion(
                projectId = projectId,
                originalCaptureIds = captures.mapTo(mutableSetOf()) { it.captureId },
                expectedUris = plan.targetUris + plan.unresolvedUris,
                photos = selected,
                previouslyAbsentUris = verifiedAbsent,
            )
        }

    /**
     * Android has already checked successfully removed file IDs. Re-verify
     * absence (including legacy/moved URIs), then delete project metadata only
     * when no outstanding photo or new capture remains.
     */
    suspend fun finalizeMediaDeletion(
        prepared: PreparedProjectMediaDeletion,
        verifiedDeletedIds: Set<Long>,
    ): Boolean = withContext(Dispatchers.IO) {
        check(canChangeCaptureTarget()) { "사진 저장 처리가 끝나면 삭제할 수 있습니다." }
        requireNotNull(store.load().find(prepared.projectId)) { "프로젝트가 이미 삭제됐습니다." }
        val verifiedUris = prepared.photos
            .filter { it.id in verifiedDeletedIds }
            .mapTo(mutableSetOf()) { it.uri.toString() }
        // An Android result is authoritative only after the exact URI is absent.
        val trulyAbsent = (verifiedUris + prepared.previouslyAbsentUris)
            .filter(::isVerifiedAbsent).toSet()
        if (trulyAbsent.isNotEmpty()) {
            captureRecords.afterVerifiedDeletion(trulyAbsent)
        }
        val current = captureRecords.forProject(prepared.projectId)
        val newCaptures = current.any { it.captureId !in prepared.originalCaptureIds }
        val allAbsentNow = prepared.expectedUris.filter(::isVerifiedAbsent).toSet()
        val safeToFinish = ProjectMediaDeletePolicy.canFinalize(
            prepared.expectedUris, allAbsentNow, newCaptures,
        ) && current.none { record -> record.mediaUris.any { it !in allAbsentNow } }
        if (safeToFinish) store.deleteProject(prepared.projectId)
        safeToFinish
    }
}
