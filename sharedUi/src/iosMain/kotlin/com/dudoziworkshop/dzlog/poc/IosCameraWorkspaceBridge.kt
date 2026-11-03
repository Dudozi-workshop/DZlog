package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.data.media.IosCaptureSaveCoordinator
import com.dudoziworkshop.dzlog.data.media.PhotoKitCaptureMediaRepository
import com.dudoziworkshop.dzlog.data.template.EditorCaptureAnchor
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.buildCaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.CaptureScopeInput
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildCapturePreview
import com.dudoziworkshop.dzlog.domain.preview.buildScope
import com.dudoziworkshop.dzlog.ui.table.mock.tableEditorNow
import org.jetbrains.skia.Image
import platform.Foundation.NSUUID

/**
 * The native camera receives a snapshot of the selected iOS editor document.
 * Kotlin/Native only: Android storage and preferences are never consulted.
 */
internal class IosCameraCaptureSession(
    private val editorKey: String,
    private var workspace: EditorWorkspace,
    private val persistWorkspace: ((EditorWorkspace) -> Boolean)? = null,
) {
    private val store = IosWorkspaceStore(editorKey)
    private val media = PhotoKitCaptureMediaRepository()
    private val coordinator = IosCaptureSaveCoordinator(media)

    fun hasPending(): Boolean = coordinator.hasPendingCapture()

    /** One camera entry point for all modes, using only this document's saved placement. */
    suspend fun saveConfiguredJpeg(jpeg: ByteArray): Boolean {
        val doc = checkNotNull(workspace.active)
        if (doc.saveMode == SaveMode.ORIGINAL_ONLY) return saveJpeg(jpeg)
        val placement = checkNotNull(tablePlacement) { "촬영 전 표 배치를 저장해 주세요." }
        return saveAndroidPlacedJpeg(jpeg, placement.anchor, placement.offsetXPercent,
            placement.offsetYPercent, placement.widthRatio, placement.heightRatio, placement.rotationCwDeg)
    }

    /** Only explicitly persisted placement is passed to the camera. */
    val tablePlacement: IosCaptureTablePlacement?
        get() = workspace.active?.capturePlacement?.let { saved ->
            IosCaptureTablePlacement(
                anchor = when (saved.anchor) {
                    EditorCaptureAnchor.TOP_LEFT -> IosTableAnchor.TOP_LEFT
                    EditorCaptureAnchor.TOP_RIGHT -> IosTableAnchor.TOP_RIGHT
                    EditorCaptureAnchor.BOTTOM_LEFT -> IosTableAnchor.BOTTOM_LEFT
                    EditorCaptureAnchor.BOTTOM_RIGHT -> IosTableAnchor.BOTTOM_RIGHT
                    EditorCaptureAnchor.CUSTOM -> IosTableAnchor.CUSTOM
                },
                offsetXPercent = saved.offsetXPercent,
                offsetYPercent = saved.offsetYPercent,
                widthRatio = saved.widthRatio,
                heightRatio = saved.heightRatio,
                rotationCwDeg = saved.rotationCwDeg,
            )
        }

    /**
     * Camera-facing entry point for a fully specified placement. Using the
     * Android free-space anchor convention prevents callers from guessing
     * absolute offsets. Home capture passes only the persisted iOS placement.
     */
    suspend fun saveAndroidPlacedJpeg(
        jpeg: ByteArray,
        anchor: IosTableAnchor,
        offsetXPercent: Int,
        offsetYPercent: Int,
        widthRatio: Int,
        heightRatio: Int,
        rotationCwDeg: Int = 0,
    ): Boolean {
        val doc = checkNotNull(workspace.active) { "촬영 템플릿이 선택되지 않았습니다." }
        return saveJpeg(jpeg, renderForPreview = { preview ->
            val image = Image.makeFromEncoded(jpeg)
            val overlay = IosTableOverlayRenderer.renderAndroidAnchoredPng(
                imageWidth = image.width,
                imageHeight = image.height,
                template = doc.template.templateState,
                preview = preview,
                style = doc.template.styleState,
                anchor = anchor,
                offsetXPercent = offsetXPercent,
                offsetYPercent = offsetYPercent,
                widthRatio = widthRatio,
                heightRatio = heightRatio,
                rotationCwDeg = rotationCwDeg,
            )
            IosCaptureResultRenderer.compositeJpeg(jpeg, overlay)
        })
    }

    suspend fun saveCompositedJpeg(
        jpeg: ByteArray,
        widthRatio: Int,
        heightRatio: Int,
        offsetXRatio: Float,
        offsetYRatio: Float,
        rotationDegrees: Float,
    ): Boolean {
        val doc = checkNotNull(workspace.active) { "촬영 템플릿이 선택되지 않았습니다." }
        return saveJpeg(jpeg, renderForPreview = { preview ->
            val image = Image.makeFromEncoded(jpeg)
            val overlay = IosTableOverlayRenderer.renderResolvedPreviewPng(
                imageWidth = image.width,
                imageHeight = image.height,
                template = doc.template.templateState,
                preview = preview,
                style = doc.template.styleState,
                widthRatio = widthRatio,
                heightRatio = heightRatio,
                offsetXRatio = offsetXRatio,
                offsetYRatio = offsetYRatio,
                rotationDegrees = rotationDegrees,
            )
            IosCaptureResultRenderer.compositeJpeg(jpeg, overlay)
        })
    }


    suspend fun saveJpeg(
        jpeg: ByteArray,
        renderedJpeg: ByteArray? = null,
        renderForPreview: ((FinalCapturePreview) -> ByteArray)? = null,
    ): Boolean {
        val doc = checkNotNull(workspace.active) { "촬영 템플릿이 선택되지 않았습니다." }
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
        // Render from the exact preview used for the save plan. Never allow a
        // separately recomputed counter, timestamp, or phrase value.
        require(renderedJpeg == null || renderForPreview == null) {
            "표 합성 이미지는 한 가지 방식으로만 제공해야 합니다."
        }
        val resultJpeg = when (doc.saveMode) {
            SaveMode.ORIGINAL_ONLY -> null
            SaveMode.WATERMARK_ONLY, SaveMode.BOTH ->
                renderedJpeg ?: renderForPreview?.invoke(preview)
        }
        val payloads = when (doc.saveMode) {
            SaveMode.ORIGINAL_ONLY -> mapOf(CaptureMediaRole.ORIGINAL to jpeg)
            SaveMode.WATERMARK_ONLY -> mapOf(
                CaptureMediaRole.RESULT to checkNotNull(resultJpeg) { "표 합성 이미지가 필요합니다." },
            )
            SaveMode.BOTH -> mapOf(
                CaptureMediaRole.RESULT to checkNotNull(resultJpeg) { "표 합성 이미지가 필요합니다." },
                CaptureMediaRole.ORIGINAL to jpeg,
            )
        }
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
            persistWorkspace = { persistWorkspace?.invoke(it) ?: store.save(it) },
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
        current = null
        val document = workspace.active ?: return "촬영 전 템플릿을 선택해 주세요."
        if (document.saveMode != SaveMode.ORIGINAL_ONLY) {
            val placement = document.capturePlacement ?: return "촬영 전 표 배치를 저장해 주세요."
            if (placement.rotationCwDeg !in listOf(0, 90) ||
                (placement.rotationCwDeg != 0 && placement.anchor != EditorCaptureAnchor.CUSTOM)) {
                return "90도 회전은 사용자 지정 위치에서 설정해 주세요."
            }
            if (document.template.templateState.cells.any { it.rowSpan != 1 || it.colSpan != 1 }) {
                return "병합된 표는 아직 결과 사진 저장을 지원하지 않습니다. 원본 저장을 선택해 주세요."
            }
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

