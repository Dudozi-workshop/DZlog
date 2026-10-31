package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.data.media.IosCaptureSaveCoordinator
import com.dudoziworkshop.dzlog.data.media.PhotoKitCaptureMediaRepository
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.buildCaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.CaptureScopeInput
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildCapturePreview
import com.dudoziworkshop.dzlog.domain.preview.buildScope
import com.dudoziworkshop.dzlog.ui.table.mock.tableEditorNow
import platform.Foundation.NSUUID

/**
 * The native camera receives a snapshot of the selected iOS editor document.
 * Kotlin/Native only: Android storage and preferences are never consulted.
 */
internal class IosCameraCaptureSession(
    private val editorKey: String,
    private var workspace: EditorWorkspace,
) {
    private val store = IosWorkspaceStore(editorKey)
    private val media = PhotoKitCaptureMediaRepository()
    private val coordinator = IosCaptureSaveCoordinator(media)

    fun hasPending(): Boolean = coordinator.hasPendingCapture()

    suspend fun saveJpeg(jpeg: ByteArray, renderedJpeg: ByteArray? = null): Boolean {
        val doc = checkNotNull(workspace.active) { "촬영 템플릿이 선택되지 않았습니다." }
        // Fail closed: RESULT/BOTH must never silently persist an uncomposited original.
        val payloads = when (doc.saveMode) {
            SaveMode.ORIGINAL_ONLY -> mapOf(CaptureMediaRole.ORIGINAL to jpeg)
            SaveMode.WATERMARK_ONLY -> mapOf(
                CaptureMediaRole.RESULT to checkNotNull(renderedJpeg) { "표 합성 이미지가 필요합니다." },
            )
            SaveMode.BOTH -> mapOf(
                CaptureMediaRole.RESULT to checkNotNull(renderedJpeg) { "표 합성 이미지가 필요합니다." },
                CaptureMediaRole.ORIGINAL to jpeg,
            )
        }
        val counter = checkNotNull(doc.nextCounter) { "촬영 번호 동기화가 필요합니다." }
        check(!hasPending()) { "이전 촬영 기록 복구가 필요합니다." }

        val now = tableEditorNow()
        val scope = buildScope(CaptureScopeInput(
            templateState = doc.template.templateState, captureNow = now,
            counterDigits = doc.counterPadding,
            dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
            timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            includeFilenameInCounterScope = doc.includeFilenameInScope,
            saveMode = doc.saveMode, phraseProgressCursor = 0,
        ))
        val preview = buildCapturePreview(scope, FinalCapturePreviewInput(
            templateState = doc.template.templateState, captureNow = now,
            counterDigits = doc.counterPadding,
            dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
            timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            includePathInCounterScope = doc.includePathInScope,
            includeFilenameInCounterScope = doc.includeFilenameInScope,
            saveMode = doc.saveMode, syncedCounter = counter, phraseProgressCursor = 0,
        ))
        val plan = buildCaptureMediaSavePlan(
            preview, doc.saveMode, NSUUID().UUIDString.lowercase(),
        )
        plan.validatePayloads(payloads)
        // A Photos permission denial must not leave a no-asset pending journal.
        media.ensureWritePermission()
        val committed = coordinator.saveAndCommit(
            originalWorkspace = workspace, selectedDocument = doc, plan = plan,
            jpegByRole = payloads,
            loadWorkspace = { store.load(doc.template) },
            persistWorkspace = { store.save(it) },
        )
        if (committed) workspace = store.load(doc.template)
        return committed
    }

    companion object {
        fun hasPending(): Boolean =
            IosCaptureSaveCoordinator(PhotoKitCaptureMediaRepository()).hasPendingCapture()

        suspend fun recover(store: IosWorkspaceStore, seed: SavedTableTemplate): Boolean {
            val coordinator = IosCaptureSaveCoordinator(PhotoKitCaptureMediaRepository())
            return coordinator.recoverPending(
                loadWorkspace = { store.load(seed) },
                persistWorkspace = { store.save(it) },
            )
        }
    }
}

/** Native modal retains one session only while presented; no process-wide photo cache. */
internal object IosCameraWorkspaceBridge {
    var current: IosCameraCaptureSession? = null
        private set

    fun prepare(editorKey: String, workspace: EditorWorkspace): String? {
        val document = workspace.active ?: return "촬영 전 템플릿을 선택해 주세요."
        if (document.saveMode != SaveMode.ORIGINAL_ONLY) {
            return "현재 iOS 촬영 검증은 원본 저장 모드만 지원합니다."
        }
        if (document.nextCounter == null) return "먼저 촬영 번호를 확정해 주세요."
        if (IosCameraCaptureSession.hasPending()) {
            return "이전 사진의 저장 기록을 복구하기 전에는 촬영할 수 없습니다."
        }
        current = IosCameraCaptureSession(editorKey, workspace)
        return null
    }

    fun clear() {
        current = null
    }
}
