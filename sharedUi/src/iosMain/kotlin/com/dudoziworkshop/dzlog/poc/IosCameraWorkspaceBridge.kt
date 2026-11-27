package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderOperationPolicy
import com.dudoziworkshop.dzlog.feature.table.policy.QuickEditSelectionMemory
import com.dudoziworkshop.dzlog.data.media.IosCaptureSaveCoordinator
import com.dudoziworkshop.dzlog.data.media.IosCaptureUndoCoordinator
import com.dudoziworkshop.dzlog.data.media.IosCaptureUndoRequest
import com.dudoziworkshop.dzlog.data.media.PhotoKitCaptureMediaRepository
import com.dudoziworkshop.dzlog.data.media.CaptureMediaAvailability
import com.dudoziworkshop.dzlog.data.project.IosCaptureContext
import com.dudoziworkshop.dzlog.data.project.IosCaptureProjectRegistry
import com.dudoziworkshop.dzlog.data.template.EditorCaptureAnchor
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.buildCaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.media.prepareCaptureWorkspaceUpdate
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.CaptureScopeInput
import com.dudoziworkshop.dzlog.domain.preview.CaptureScopeState
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildCapturePreview
import com.dudoziworkshop.dzlog.domain.preview.buildScope
import com.dudoziworkshop.dzlog.ui.table.mock.tableEditorNow
import org.jetbrains.skia.Image
import platform.Foundation.NSUserDefaults
import com.dudoziworkshop.dzlog.data.media.IosCaptureWorkspaceJournal
import com.dudoziworkshop.dzlog.data.media.IosCaptureUndoJournal
import platform.Foundation.NSUUID
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureUndoPathWarningPolicy
import com.dudoziworkshop.dzlog.feature.capture.policy.DurableCaptureQueuePolicy
import com.dudoziworkshop.dzlog.feature.capture.policy.DurableCaptureRecord
import com.dudoziworkshop.dzlog.feature.capture.policy.DurableCaptureReviewReason
import com.dudoziworkshop.dzlog.feature.capture.policy.DurableCaptureSnapshot
import com.dudoziworkshop.dzlog.feature.capture.policy.DurableCaptureStage
import com.dudoziworkshop.dzlog.feature.capture.policy.FrozenCapturePreview
import com.dudoziworkshop.dzlog.feature.capture.policy.IosDurableCaptureStagingStore
import com.dudoziworkshop.dzlog.feature.capture.policy.IosDurableCaptureWorker
import com.dudoziworkshop.dzlog.feature.capture.policy.IosDurableCaptureWorkerResult
import com.dudoziworkshop.dzlog.feature.capture.policy.IosDurableCaptureWorkerStatus
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import kotlin.math.roundToInt

/**
 * The native camera receives a snapshot of the selected iOS editor document.
 * Kotlin/Native only: Android storage and preferences are never consulted.
 */
internal class IosCameraCaptureSession(
    val editorKey: String,
    private var workspace: EditorWorkspace,
    private val persistWorkspace: ((EditorWorkspace) -> Boolean)? = null,
    mediaDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
    stagingRootPath: String = iosCaptureStagingRoot(editorKey),
) {
    val quickSelection = QuickEditSelectionMemory()
    private val store = IosWorkspaceStore(editorKey)
    private val projectStore = IosCaptureProjectStore(editorKey)
    internal val mediaRepository = PhotoKitCaptureMediaRepository(mediaDefaults)
    private val media get() = mediaRepository
    private val coordinator = IosCaptureSaveCoordinator(media, IosCaptureWorkspaceJournal(mediaDefaults))
    private val undoCoordinator = IosCaptureUndoCoordinator(media, IosCaptureUndoJournal(mediaDefaults))
    private val staging = IosDurableCaptureStagingStore(stagingRootPath)
    private val durableWorker = IosDurableCaptureWorker(
        staging = staging,
        media = media,
        loadWorkspace = { store.load(checkNotNull(workspace.active).template) },
        persistWorkspace = { persistWorkspace?.invoke(it) ?: store.save(it) },
        buildPayloads = ::buildDurablePayloads,
        journal = IosCaptureWorkspaceJournal(mediaDefaults),
    )
    private val durableTerminalStages = setOf(DurableCaptureStage.COMMITTED, DurableCaptureStage.CANCELLED)
    private var reservationWorkspace: EditorWorkspace = staging.listSnapshots()
        .filter { it.record.stage !in durableTerminalStages }
        .maxByOrNull { it.record.reservationOrder }?.workspaceAfter ?: workspace
    private var nextReservationOrder: Long = (staging.listSnapshots()
        .maxOfOrNull { it.record.reservationOrder } ?: -1L) + 1L
    private data class SessionCapture(val undo: IosCaptureUndoRequest, val folder: String)
    data class UndoPathWarning(val captureId: String, val capturedFolder: String, val currentFolder: String)

    private val sessionCaptures = mutableListOf<SessionCapture>()
    private val confirmedCaptureIds = mutableListOf<String>()
    fun capturePathImpact(folder: String) = GalleryFolderOperationPolicy.captureImpact(folder, workspace)
    fun sessionResultIds(): List<String> = confirmedCaptureIds.toList()
    private var projectAssociationWarning: String? = null

    fun takeProjectAssociationWarning(): String? = projectAssociationWarning.also {
        projectAssociationWarning = null
    }

    fun loadProjectRegistry(): IosCaptureProjectRegistry = projectStore.load()

    /** Browsing leaves Undo intact. A changed document invalidates its workspace snapshot. */
    fun reloadAfterEditing(previousProjectId: String?): IosCaptureProjectRegistry {
        val next = store.load(selectedDocument.template)
        check(next.active != null)
        val projects = projectStore.load()
        if (next != workspace || previousProjectId != projects.activeId) sessionCaptures.clear()
        if (previousProjectId != projects.activeId) confirmedCaptureIds.clear()
        workspace = next
        resetReservationToCommittedIfIdle()
        return projects
    }

    /** Switch capture context and its remembered shared template as one user action. */
    fun selectProject(projectId: String?): Boolean {
        if (hasPending()) return false
        val currentProjects = runCatching { projectStore.load() }.getOrNull() ?: return false
        val before = IosCaptureContext(currentProjects, workspace)
        val next = runCatching { before.select(projectId) }.getOrNull() ?: return false
        if (next == before) return true
        if (!projectStore.saveContext(before, next, selectedDocument.template)) return false
        workspace = store.load(selectedDocument.template)
        resetReservationToCommittedIfIdle()
        sessionCaptures.clear()
        confirmedCaptureIds.clear()
        return true
    }

    fun hasPending(): Boolean = coordinator.hasPendingCapture() || undoCoordinator.hasPendingUndo() ||
        runCatching { staging.listSnapshots().any { it.record.stage !in durableTerminalStages } }
            .getOrDefault(true)

    val canAcceptCapture: Boolean
        get() = runCatching {
            if (undoCoordinator.hasPendingUndo()) return@runCatching false
            val records = staging.listSnapshots().map(DurableCaptureSnapshot::record)
            val document = checkNotNull(reservationWorkspace.active)
            DurableCaptureQueuePolicy.canReserve(
                records = records,
                activeWorkerIds = durableWorker.activeCaptureIds(),
                expectedTemplateId = document.template.id,
                expectedProjectId = projectStore.load().activeId,
            )
        }.getOrDefault(false)

    data class CaptureQueueUiState(
        val pendingCount: Int,
        val needsReview: Boolean,
        val canCancelBeforeSave: Boolean,
        val atCapacity: Boolean,
        val capacity: Int,
    )

    fun captureQueueUiState(): CaptureQueueUiState = runCatching {
        val snapshots = staging.listSnapshots()
        val stages = snapshots.map { it.record.stage }
        val pending = stages.count {
            it == DurableCaptureStage.RESERVED || it == DurableCaptureStage.READY ||
                it == DurableCaptureStage.PROCESSING || it == DurableCaptureStage.MEDIA_SAVED
        }
        val outstanding = snapshots.filter { it.record.stage !in durableTerminalStages }
        CaptureQueueUiState(
            pendingCount = pending,
            needsReview = DurableCaptureStage.NEEDS_REVIEW in stages,
            canCancelBeforeSave = canCancelBeforeSave(outstanding),
            atCapacity = pending >= DurableCaptureQueuePolicy.MAX_OUTSTANDING,
            capacity = DurableCaptureQueuePolicy.MAX_OUTSTANDING,
        )
    }.getOrElse { CaptureQueueUiState(0, needsReview = true, canCancelBeforeSave = false, atCapacity = false,
        capacity = DurableCaptureQueuePolicy.MAX_OUTSTANDING) }

    fun cancelCaptureQueueBeforeSave(): Boolean = runCatching {
        check(!coordinator.hasPendingCapture() && !undoCoordinator.hasPendingUndo())
        val outstanding = staging.listSnapshots()
            .filter { it.record.stage !in durableTerminalStages }
        check(canCancelBeforeSave(outstanding))
        outstanding.forEachIndexed { index, snapshot ->
            if (index == 0) {
                staging.cancelReviewedBeforeSave(snapshot.record.captureId)
            } else {
                staging.transition(snapshot.record.captureId, DurableCaptureStage.CANCELLED)
            }
        }
        outstanding.forEach { staging.removeTerminal(it.record.captureId) }
        workspace = store.load(checkNotNull(workspace.active).template)
        reservationWorkspace = workspace
        true
    }.getOrDefault(false)

    private fun canCancelBeforeSave(outstanding: List<DurableCaptureSnapshot>): Boolean {
        val first = outstanding.firstOrNull() ?: return false
        if (first.record.stage != DurableCaptureStage.NEEDS_REVIEW ||
            first.record.reviewReason != DurableCaptureReviewReason.WORKSPACE_CHANGED_BEFORE_SAVE) {
            return false
        }
        return outstanding.drop(1).all { it.record.stage == DurableCaptureStage.READY } &&
            !coordinator.hasPendingCapture() && !undoCoordinator.hasPendingUndo()
    }

    val latestSessionCaptureId: String? get() = confirmedCaptureIds.lastOrNull()
    val canUndoLatestCapture: Boolean get() = sessionCaptures.isNotEmpty() && !hasPending()

    val selectedDocument get() = checkNotNull(workspace.active)
    val documents get() = workspace.documents
    val showCameraGrid get() = workspace.settings.showCameraGrid
    val showCameraTable get() = workspace.settings.showCameraTable
    val assistShutterEnabled get() = workspace.settings.assistShutterEnabled
    val assistShutterXRatio get() = workspace.settings.assistShutterXRatio
    val assistShutterYRatio get() = workspace.settings.assistShutterYRatio
    val cameraZoom get() = workspace.settings.cameraZoomTenths / 10f
    val cameraFlashMode get() = workspace.settings.cameraFlashMode
    val continuousPreviewMode get() = workspace.settings.continuousPreviewMode
    val captureVibrationStrength get() = workspace.settings.captureVibrationStrength
    val captureHapticEnabled get() = workspace.settings.captureHapticEnabled
    val captureAspect get() = com.dudoziworkshop.dzlog.domain.model.CaptureAspect.from(
        workspace.settings.captureAspectMode,
    )

    fun setShowCameraGrid(enabled: Boolean): Boolean = saveCameraPreferences(
        workspace.settings.copy(showCameraGrid = enabled),
    )

    fun setShowCameraTable(enabled: Boolean): Boolean = saveCameraPreferences(
        workspace.settings.copy(showCameraTable = enabled),
    )

    fun setAssistShutterEnabled(enabled: Boolean): Boolean = saveCameraPreferences(
        workspace.settings.copy(assistShutterEnabled = enabled),
    )

    fun setAssistShutterPosition(xRatio: Float, yRatio: Float): Boolean = saveCameraPreferences(
        workspace.settings.copy(
            assistShutterXRatio = xRatio.coerceIn(0f, 1f),
            assistShutterYRatio = yRatio.coerceIn(0f, 1f),
        ),
    )

    fun setCameraZoom(zoom: Float): Boolean = saveCameraPreferences(
        workspace.settings.copy(cameraZoomTenths = (zoom * 10f).roundToInt().coerceIn(10, 100)),
    )

    fun setCameraFlashMode(mode: Int): Boolean {
        if (mode !in 0..2) return false
        return saveCameraPreferences(workspace.settings.copy(cameraFlashMode = mode))
    }

    private fun saveCameraPreferences(
        settings: com.dudoziworkshop.dzlog.data.template.WorkspaceSettings,
    ): Boolean {
        if (hasPending() || store.load(selectedDocument.template) != workspace) return false
        val updated = workspace.copy(settings = settings)
        if (!store.save(updated)) return false
        workspace = store.load(selectedDocument.template)
        resetReservationToCommittedIfIdle()
        // Undo snapshots contain the whole workspace. Never roll a later camera preference
        // change back as a side effect of deleting a photo.
        sessionCaptures.clear()
        return workspace.settings == settings
    }

    suspend fun readLatestSessionThumbnail(): ByteArray? {
        val captureId = latestSessionCaptureId ?: return null
        val assets = media.listSavedAssets().filter {
            it.captureId == captureId && it.availability == CaptureMediaAvailability.PRESENT
        }
        val selected = assets.firstOrNull { it.asset.target.role == CaptureMediaRole.RESULT }
            ?: assets.firstOrNull { it.asset.target.role == CaptureMediaRole.ORIGINAL }
            ?: return null
        return media.readThumbnail(selected.asset.assetId)
    }

    fun undoPathWarning(): UndoPathWarning? {
        val last = sessionCaptures.lastOrNull() ?: return null
        if (!workspace.settings.undoSavePathWarningEnabled) return null
        val current = previewInfo().previewNaming.relativePath
        return if (CaptureUndoPathWarningPolicy.requiresConfirmation(last.folder, current))
            UndoPathWarning(last.undo.captureId, last.folder, current) else null
    }

    suspend fun undoLatestCapture(confirmedId: String?, disableWarning: Boolean): Boolean {
        check(!coordinator.hasPendingCapture()) { "이전 촬영 저장 기록을 먼저 복구해 주세요." }
        val last = sessionCaptures.lastOrNull()?.undo ?: return false
        if (confirmedId != null && confirmedId != last.captureId) return false
        if (confirmedId == null && undoPathWarning() != null) return false
        fun withWarningDisabled(value: EditorWorkspace) =
            value.copy(settings = value.settings.copy(undoSavePathWarningEnabled = false))
        val optOut = confirmedId != null && disableWarning
        val request = if (optOut) last.copy(before = withWarningDisabled(last.before)) else last
        val rolledBack = undoCoordinator.deleteAndRollback(
            request = request,
            loadWorkspace = { store.load(request.after.active!!.template) },
            persistWorkspace = { persistWorkspace?.invoke(it) ?: store.save(it) },
        )
        if (rolledBack) {
            workspace = store.load(request.before.active!!.template)
            resetReservationToCommittedIfIdle()
            sessionCaptures.removeAt(sessionCaptures.lastIndex)
            if (optOut) {
                // Older Undo entries must preserve this preference through subsequent rollbacks.
                for (index in sessionCaptures.indices) {
                    val capture = sessionCaptures[index]
                    sessionCaptures[index] = capture.copy(undo = capture.undo.copy(
                        before = withWarningDisabled(capture.undo.before),
                        after = withWarningDisabled(capture.undo.after)))
                }
            }
            confirmedCaptureIds.remove(request.captureId)
            if (projectStore.removeCapture(request.captureId) == null) {
                projectAssociationWarning = "사진은 삭제됐지만 프로젝트 촬영 기록을 갱신하지 못했습니다."
            }
        }
        return rolledBack
    }

    /** Reject a draft opened before another save/settings change. */
    fun saveQuickValues(expected: com.dudoziworkshop.dzlog.data.template.EditorDocument,
        changes: IosQuickValueChanges): Boolean {
        if (selectedDocument != expected || hasPending()) return false
        val updated = runCatching { applyIosQuickValueChanges(expected, changes) }.getOrNull() ?: return false
        if (updated == expected) return store.load(expected.template) == workspace
        return saveSettings(updated)
    }

    /** Persist the current quick draft before atomically selecting another iOS document. */
    fun switchQuickTemplate(
        expected: com.dudoziworkshop.dzlog.data.template.EditorDocument,
        targetTemplateId: String,
        changes: IosQuickValueChanges,
    ): Boolean {
        if (selectedDocument != expected || hasPending()) return false
        if (workspace.documents.none { it.template.id == targetTemplateId }) return false
        val updatedCurrent = runCatching { applyIosQuickValueChanges(expected, changes) }.getOrNull() ?: return false
        val projects = runCatching { projectStore.load() }.getOrNull() ?: return false
        val before = IosCaptureContext(projects, workspace)
        val next = before.withWorkspace(workspace.save(updatedCurrent).copy(activeTemplateId = targetTemplateId))
        if (!projectStore.saveContext(before, next, expected.template)) return false
        workspace = store.load(selectedDocument.template)
        resetReservationToCommittedIfIdle()
        sessionCaptures.clear()
        return workspace.activeTemplateId == targetTemplateId
    }

    fun saveSettings(document: com.dudoziworkshop.dzlog.data.template.EditorDocument): Boolean {
        if (hasPending() || document.template.id != workspace.activeTemplateId) return false
        if (store.load(document.template) != workspace) return false
        val updated = workspace.save(document)
        if (!store.save(updated)) return false
        workspace = store.load(document.template)
        resetReservationToCommittedIfIdle()
        sessionCaptures.clear()
        return true
    }

    fun applyCameraPanelSettings(
        mode: SaveMode = selectedDocument.saveMode,
        previewMode: Int = continuousPreviewMode,
        hapticEnabled: Boolean = captureHapticEnabled,
        aspect: com.dudoziworkshop.dzlog.domain.model.CaptureAspect = captureAspect,
    ): Boolean {
        val document = selectedDocument
        if (mode == document.saveMode && previewMode == continuousPreviewMode &&
            hapticEnabled == captureHapticEnabled && aspect == captureAspect) return true
        if (mode != document.saveMode && mode != SaveMode.ORIGINAL_ONLY &&
            (document.capturePlacement == null ||
                runCatching { iosCaptureCellRects(document.template.templateState) }.isFailure)) return false
        return saveCaptureSettings(document.copy(saveMode = mode), previewMode, hapticEnabled, aspect.v)
    }

    fun saveCaptureSettings(
        document: com.dudoziworkshop.dzlog.data.template.EditorDocument,
        continuousPreviewMode: Int,
        captureHapticEnabled: Boolean,
        captureAspectMode: Int,
    ): Boolean {
        if (continuousPreviewMode !in 0..2 || captureAspectMode !in 0..2 || hasPending() ||
            document.template.id != workspace.activeTemplateId) return false
        if (store.load(document.template) != workspace) return false
        val updated = workspace.save(document).copy(settings = workspace.settings.copy(
            continuousPreviewMode = continuousPreviewMode,
            captureHapticEnabled = captureHapticEnabled,
            captureAspectMode = captureAspectMode))
        if (!store.save(updated)) return false
        workspace = store.load(document.template)
        resetReservationToCommittedIfIdle()
        sessionCaptures.clear()
        return workspace.active == document && workspace.settings.continuousPreviewMode == continuousPreviewMode &&
            workspace.settings.captureHapticEnabled == captureHapticEnabled &&
            workspace.settings.captureAspectMode == captureAspectMode
    }

    fun previewInfo(): com.dudoziworkshop.dzlog.domain.preview.PreviewState {
        val document = checkNotNull(reservationWorkspace.active)
        return com.dudoziworkshop.dzlog.domain.preview.buildPreview(
        com.dudoziworkshop.dzlog.domain.preview.PreviewInput(
            document.template.templateState, tableEditorNow(), document.counterPadding,
            NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            NamingFormatDefaults.FILE_NAME_DELIMITER, document.includePathInScope,
            document.includeFilenameInScope, document.saveMode, document.nextCounter, 0))
    }

    private fun buildCaptureScope(
        doc: com.dudoziworkshop.dzlog.data.template.EditorDocument,
        now: com.dudoziworkshop.dzlog.domain.time.CaptureTime,
    ): CaptureScopeState = buildScope(CaptureScopeInput(
        templateState = doc.template.templateState, captureNow = now,
        counterDigits = doc.counterPadding,
        dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
        timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
        fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
        includeFilenameInCounterScope = doc.includeFilenameInScope,
        saveMode = doc.saveMode, phraseProgressCursor = 0,
    ))

    private fun resolveCapturePreview(
        doc: com.dudoziworkshop.dzlog.data.template.EditorDocument = selectedDocument,
        now: com.dudoziworkshop.dzlog.domain.time.CaptureTime = tableEditorNow(),
        scope: CaptureScopeState = buildCaptureScope(doc, now),
    ): FinalCapturePreview {
        val counter = checkNotNull(doc.nextCounter) { "촬영 번호 동기화가 필요합니다." }
        return buildCapturePreview(scope, FinalCapturePreviewInput(
            templateState = doc.template.templateState, captureNow = now,
            counterDigits = doc.counterPadding,
            dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
            timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            includePathInCounterScope = doc.includePathInScope,
            includeFilenameInCounterScope = doc.includeFilenameInScope,
            saveMode = doc.saveMode, syncedCounter = counter, phraseProgressCursor = 0,
        ))
    }

    /**
     * Refresh the automatic number from saved DZlog filenames without replacing
     * an explicit manual number. Same-stream refreshes never move backwards.
     */
    suspend fun synchronizeAutomaticCounter(): Int {
        val doc = selectedDocument
        val current = checkNotNull(doc.nextCounter) { "촬영 번호 동기화가 필요합니다." }
        if (!doc.usesAutoNext) return current
        check(!hasPending()) { "이전 촬영 기록 복구가 필요합니다." }

        val now = tableEditorNow()
        val scope = buildCaptureScope(doc, now)
        media.ensureWritePermission()
        val next = media.resolveStableAutomaticNext(
            relativePathKey = if (doc.includePathInScope) scope.counterScope.relativePathKey else "*",
            scanPrefix = scope.scanPrefix,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            currentNext = current,
        )
        if (next == current) return current

        // Refuse to overwrite settings changed while the Photos read was in flight.
        check(store.load(doc.template) == workspace) {
            "촬영 번호 확인 중 설정이 변경되었습니다. 다시 시도해 주세요."
        }
        val updated = workspace.save(doc.copy(nextCounter = next, usesAutoNext = true))
        check(store.save(updated)) { "동기화한 촬영 번호를 저장하지 못했습니다." }
        workspace = store.load(doc.template)
        resetReservationToCommittedIfIdle()
        check(selectedDocument.nextCounter == next) { "동기화한 촬영 번호를 확인하지 못했습니다." }
        // The previous session Undo snapshot no longer matches this externally advanced stream.
        sessionCaptures.clear()
        return next
    }

    private suspend fun resolveSynchronizedCapture(): Pair<
        com.dudoziworkshop.dzlog.data.template.EditorDocument,
        FinalCapturePreview
    > {
        synchronizeAutomaticCounter()
        val doc = selectedDocument
        val now = tableEditorNow()
        val scope = buildCaptureScope(doc, now)
        return doc to resolveCapturePreview(doc, now, scope)
    }

    fun savePlacement(placement: IosCaptureTablePlacement): Boolean {
        val value = com.dudoziworkshop.dzlog.data.template.EditorCapturePlacement(
            EditorCaptureAnchor.valueOf(placement.anchor.name), placement.offsetXPercent,
            placement.offsetYPercent, placement.widthRatio, placement.heightRatio, placement.rotationCwDeg)
        return saveSettings(selectedDocument.copy(capturePlacement = value))
    }

    private var liveAspect: Double? = null

    fun renderLiveOverlay(width: Int, height: Int, onContentAspect: (Float) -> Unit = {}): ByteArray {
        val document = checkNotNull(reservationWorkspace.active)
        val placement = checkNotNull(tablePlacement)
        val png = IosTableOverlayRenderer.renderAndroidAnchoredPng(width, height,
            document.template.templateState, resolveCapturePreview(document), document.template.styleState,
            placement.anchor, placement.offsetXPercent, placement.offsetYPercent,
            placement.widthRatio, placement.heightRatio, placement.rotationCwDeg, onContentAspect)
        liveAspect = width.toDouble() / height
        return png
    }

    fun checkLiveCaptureAspect(width: Int, height: Int) {
        liveAspect?.let { expected ->
            require(kotlin.math.abs(width.toDouble() / height - expected) <= 0.02) {
                "사진과 미리보기 비율이 달라 결과를 저장할 수 없습니다. 원본만 모드로 전환하거나 카메라를 다시 열어 주세요."
            }
        }
    }

    /** Freeze and durably stage one shutter result without waiting for PhotoKit. */
    suspend fun reserveConfiguredJpeg(jpeg: ByteArray): String {
        media.ensureWritePermission()
        val existing = staging.listSnapshots()
        val outstanding = existing.filter { it.record.stage !in durableTerminalStages }
        if (outstanding.isEmpty()) {
            synchronizeAutomaticCounter()
            reservationWorkspace = workspace
        }
        val document = checkNotNull(reservationWorkspace.active) {
            "촬영 템플릿이 선택되지 않았습니다."
        }
        val projectId = projectStore.load().activeId
        check(DurableCaptureQueuePolicy.canReserve(
            records = existing.map(DurableCaptureSnapshot::record),
            activeWorkerIds = durableWorker.activeCaptureIds(),
            expectedTemplateId = document.template.id,
            expectedProjectId = projectId,
        )) { "저장 대기열이 가득 찼거나 이전 촬영 복구가 필요합니다." }

        val aspect = com.dudoziworkshop.dzlog.domain.model.CaptureAspect.from(
            reservationWorkspace.settings.captureAspectMode)
        val sourceJpeg = cropIosCaptureJpeg(jpeg, aspect)
        val now = tableEditorNow()
        val preview = resolveCapturePreview(document, now, buildCaptureScope(document, now))
        val captureId = NSUUID().UUIDString.lowercase()
        val plan = buildCaptureMediaSavePlan(preview, document.saveMode, captureId)
        val after = prepareCaptureWorkspaceUpdate(reservationWorkspace, document, plan)
        val snapshot = DurableCaptureSnapshot(
            record = DurableCaptureRecord(
                captureId = captureId,
                reservationOrder = nextReservationOrder,
                templateId = document.template.id,
                projectId = projectId,
                counterValue = preview.usedCounter,
                phraseProgressCursor = preview.nextPhraseProgressCursor,
                stage = DurableCaptureStage.READY,
            ),
            capturedDocument = document,
            workspaceBefore = reservationWorkspace,
            workspaceAfter = after,
            preview = FrozenCapturePreview.freeze(preview),
            savePlan = plan,
            sourceJpeg = IosDurableCaptureStagingStore.sourceIdentity(sourceJpeg),
        )
        staging.stage(snapshot, sourceJpeg)
        reservationWorkspace = after
        nextReservationOrder += 1L
        return captureId
    }

    /** May be called after every reservation; a running owner absorbs later wake-ups. */
    suspend fun processReservedCaptures(): List<IosDurableCaptureWorkerResult> {
        val results = durableWorker.processAvailable()
        val terminal = staging.listSnapshots().filter {
            it.record.stage == DurableCaptureStage.COMMITTED ||
                it.record.stage == DurableCaptureStage.CANCELLED
        }
        terminal.filter { it.record.stage == DurableCaptureStage.COMMITTED }.forEach { snapshot ->
            if (snapshot.record.captureId in confirmedCaptureIds) return@forEach
            sessionCaptures += SessionCapture(
                IosCaptureUndoRequest(snapshot.record.captureId,
                    snapshot.workspaceBefore, snapshot.workspaceAfter),
                snapshot.preview.relativePathPreview,
            )
            confirmedCaptureIds += snapshot.record.captureId
            if (snapshot.record.projectId != null &&
                projectStore.appendCapture(snapshot.record.captureId) == null) {
                projectAssociationWarning = "사진은 저장됐지만 프로젝트 연결을 기록하지 못했습니다."
            }
        }
        terminal.forEach { staging.removeTerminal(it.record.captureId) }
        workspace = store.load(checkNotNull(workspace.active).template)
        val outstanding = staging.listSnapshots().filter { it.record.stage !in durableTerminalStages }
        reservationWorkspace = outstanding.maxByOrNull { it.record.reservationOrder }?.workspaceAfter ?: workspace
        return results
    }

    private fun buildDurablePayloads(
        snapshot: DurableCaptureSnapshot,
        sourceJpeg: ByteArray,
    ): Map<CaptureMediaRole, ByteArray> {
        val document = snapshot.capturedDocument
        if (document.saveMode == SaveMode.ORIGINAL_ONLY) {
            return mapOf(CaptureMediaRole.ORIGINAL to sourceJpeg)
        }
        val placement = checkNotNull(document.capturePlacement) {
            "촬영 시점의 표 배치 정보가 없습니다."
        }
        val upright = uprightIosImage(sourceJpeg)
        val image = Image.makeFromEncoded(upright)
        checkLiveCaptureAspect(image.width, image.height)
        val cellsById = document.template.templateState.cells.associateBy { it.cellId }
        val renderPreview = snapshot.preview.thaw().let { frozen ->
            frozen.copy(resolvedCells = frozen.resolvedCells.map { cell ->
                cell.copy(raw = checkNotNull(cellsById[cell.id]) {
                    "촬영 시점의 표 셀을 찾을 수 없습니다."
                })
            })
        }
        val overlay = IosTableOverlayRenderer.renderAndroidAnchoredPng(
            imageWidth = image.width,
            imageHeight = image.height,
            template = document.template.templateState,
            preview = renderPreview,
            style = document.template.styleState,
            anchor = when (placement.anchor) {
                EditorCaptureAnchor.TOP_LEFT -> IosTableAnchor.TOP_LEFT
                EditorCaptureAnchor.TOP_RIGHT -> IosTableAnchor.TOP_RIGHT
                EditorCaptureAnchor.BOTTOM_LEFT -> IosTableAnchor.BOTTOM_LEFT
                EditorCaptureAnchor.BOTTOM_RIGHT -> IosTableAnchor.BOTTOM_RIGHT
                EditorCaptureAnchor.CUSTOM -> IosTableAnchor.CUSTOM
            },
            offsetXPercent = placement.offsetXPercent,
            offsetYPercent = placement.offsetYPercent,
            widthRatio = placement.widthRatio,
            heightRatio = placement.heightRatio,
            rotationCwDeg = placement.rotationCwDeg,
        )
        val result = IosCaptureResultRenderer.compositeJpeg(
            upright, overlay, snapshot.workspaceBefore.settings.qualityMode)
        return when (document.saveMode) {
            SaveMode.ORIGINAL_ONLY -> error("Original-only capture was handled above.")
            SaveMode.WATERMARK_ONLY -> mapOf(CaptureMediaRole.RESULT to result)
            SaveMode.BOTH -> mapOf(
                CaptureMediaRole.RESULT to result,
                CaptureMediaRole.ORIGINAL to sourceJpeg,
            )
        }
    }

    private fun resetReservationToCommittedIfIdle() {
        val hasOutstanding = runCatching {
            staging.listSnapshots().any { it.record.stage !in durableTerminalStages }
        }.getOrDefault(true)
        if (!hasOutstanding) reservationWorkspace = workspace
    }

    /** One camera entry point for all modes, using only this document's saved placement. */
    suspend fun saveConfiguredJpeg(jpeg: ByteArray): Boolean {
        val doc = checkNotNull(workspace.active)
        val croppedJpeg = cropIosCaptureJpeg(jpeg, captureAspect)
        if (doc.saveMode == SaveMode.ORIGINAL_ONLY) return saveJpeg(croppedJpeg)
        val placement = checkNotNull(tablePlacement) { "촬영 전 표 배치를 저장해 주세요." }
        return saveAndroidPlacedJpeg(croppedJpeg, placement.anchor, placement.offsetXPercent,
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
            val upright = uprightIosImage(jpeg)
            val image = Image.makeFromEncoded(upright)
            checkLiveCaptureAspect(image.width, image.height)
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
            IosCaptureResultRenderer.compositeJpeg(upright, overlay, workspace.settings.qualityMode)
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
            val upright = uprightIosImage(jpeg)
            val image = Image.makeFromEncoded(upright)
            checkLiveCaptureAspect(image.width, image.height)
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
            IosCaptureResultRenderer.compositeJpeg(upright, overlay, workspace.settings.qualityMode)
        })
    }


    suspend fun saveJpeg(
        jpeg: ByteArray,
        renderedJpeg: ByteArray? = null,
        renderForPreview: ((FinalCapturePreview) -> ByteArray)? = null,
    ): Boolean {
        check(!hasPending()) { "이전 촬영 기록 복구가 필요합니다." }

        // Permission and readback happen before rendering, planning, or journaling.
        // Manual mode returns its explicit number unchanged.
        media.ensureWritePermission()
        val (doc, preview) = resolveSynchronizedCapture()
        val beforeCaptureWorkspace = workspace
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
        if (committed) {
            workspace = store.load(doc.template)
            resetReservationToCommittedIfIdle()
            sessionCaptures += SessionCapture(IosCaptureUndoRequest(plan.captureId, beforeCaptureWorkspace, workspace),
                preview.relativePathPreview)
            confirmedCaptureIds += plan.captureId
            val projects = runCatching { projectStore.load() }.getOrNull()
            if (projects?.activeId != null && projectStore.appendCapture(plan.captureId) == null) {
                projectAssociationWarning = "사진은 저장됐지만 프로젝트 연결을 기록하지 못했습니다."
            }
        }
        return committed
    }

    companion object {
        fun hasPending(mediaDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults): Boolean =
            PhotoKitCaptureMediaRepository(mediaDefaults).let { media ->
                IosCaptureSaveCoordinator(media, IosCaptureWorkspaceJournal(mediaDefaults)).hasPendingCapture() ||
                    IosCaptureUndoCoordinator(media, IosCaptureUndoJournal(mediaDefaults)).hasPendingUndo()
            }

        suspend fun recover(store: IosWorkspaceStore, seed: SavedTableTemplate,
            mediaDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults): Boolean {
            val media = PhotoKitCaptureMediaRepository(mediaDefaults)
            val captureRecovered = IosCaptureSaveCoordinator(media, IosCaptureWorkspaceJournal(mediaDefaults)).recoverPending(
                loadWorkspace = { store.load(seed) },
                persistWorkspace = { store.save(it) },
            )
            if (!captureRecovered) return false
            return IosCaptureUndoCoordinator(media, IosCaptureUndoJournal(mediaDefaults)).recoverPending(
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

    fun prepare(editorKey: String, workspace: EditorWorkspace,
        mediaDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults): String? {
        current = null
        val document = workspace.active ?: return "촬영 전 템플릿을 선택해 주세요."
        if (document.saveMode != SaveMode.ORIGINAL_ONLY) {
            val placement = document.capturePlacement ?: return "촬영 전 표 배치를 저장해 주세요."
            if (placement.rotationCwDeg !in listOf(0, 90) ||
                (placement.rotationCwDeg != 0 && placement.anchor != EditorCaptureAnchor.CUSTOM)) {
                return "90도 회전은 사용자 지정 위치에서 설정해 주세요."
            }
            runCatching { iosCaptureCellRects(document.template.templateState) }.onFailure {
                return it.message ?: "표 병합 구조를 확인해 주세요."
            }
        }
        if (document.nextCounter == null) return "먼저 촬영 번호를 확정해 주세요."
        val durableOutstanding = runCatching {
            IosDurableCaptureStagingStore(iosCaptureStagingRoot(editorKey)).listSnapshots()
                .any { it.record.stage != DurableCaptureStage.COMMITTED &&
                    it.record.stage != DurableCaptureStage.CANCELLED }
        }.getOrElse {
            return "이전 촬영 임시 파일을 확인할 수 없습니다."
        }
        val media = PhotoKitCaptureMediaRepository(mediaDefaults)
        val capturePending = IosCaptureSaveCoordinator(
            media, IosCaptureWorkspaceJournal(mediaDefaults)).hasPendingCapture()
        val undoPending = IosCaptureUndoCoordinator(
            media, IosCaptureUndoJournal(mediaDefaults)).hasPendingUndo()
        if (undoPending || capturePending && !durableOutstanding) {
            return "이전 사진의 저장 기록을 복구하기 전에는 촬영할 수 없습니다."
        }
        current = IosCameraCaptureSession(editorKey, workspace, mediaDefaults = mediaDefaults)
        return null
    }

    fun clear() {
        current = null
    }
}

private fun iosCaptureStagingRoot(editorKey: String): String {
    val applicationSupport = NSSearchPathForDirectoriesInDomains(
        NSApplicationSupportDirectory, NSUserDomainMask, true,
    ).firstOrNull() as? String ?: error("앱 지원 저장소를 찾을 수 없습니다.")
    val namespace = IosDurableCaptureStagingStore.stableNamespace(editorKey)
    return "$applicationSupport/DZlog/CaptureQueue/$namespace"
}

