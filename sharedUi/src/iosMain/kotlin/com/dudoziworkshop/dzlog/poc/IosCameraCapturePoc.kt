@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.data.datastore.CaptureVibrationStrength
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import com.dudoziworkshop.dzlog.domain.naming.capturePhysicalSavePaths
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.media.PhotoKitCaptureMediaRepository
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.buildCaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.naming.buildCounterPath
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import platform.Foundation.NSUUID
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView

private const val CAMERA_POC_PATH = "Pictures/DZlog/Camera-M3A/"

/** Explicit placement mapped from the selected iOS document. */
internal data class IosCaptureTablePlacement(
    val anchor: IosTableAnchor,
    val offsetXPercent: Int,
    val offsetYPercent: Int,
    val widthRatio: Int,
    val heightRatio: Int,
    val rotationCwDeg: Int = 0,
) {
    init {
        require(offsetXPercent in 0..100 && offsetYPercent in 0..100) {
            "표 위치 비율이 올바르지 않습니다."
        }
        require(widthRatio in 10..TableLayoutCalculator.CAMERA_MAX_RATIO && heightRatio in 10..TableLayoutCalculator.CAMERA_MAX_RATIO) {
            "표 크기 비율이 올바르지 않습니다."
        }
        require(rotationCwDeg in 0..359) {
            "표 회전 각도가 올바르지 않습니다."
        }
    }
}

/**
 * Device-only AVFoundation -> PhotoKit proof of flow, behind --camera-flow.
 * Does not claim Android table overlay rendering or camera feature parity.
 * Home captures use a persisted document; the standalone route is an isolated fixture.
 */
@Composable
internal fun IosCameraCapturePoc(
    permissionGranted: Boolean,
    onBack: (() -> Unit)? = null,
    session: IosCameraCaptureSession? = null,
) {
    val permissions = rememberIosPermissions(if (permissionGranted) IosAccess.ALLOWED else IosAccess.UNKNOWN)
    val cameraAllowed = permissions.camera == IosAccess.ALLOWED
    val camera = remember { IosAvFoundationCamera() }
    val repository = remember(session) { session?.mediaRepository ?: PhotoKitCaptureMediaRepository() }
    val strength = session?.captureVibrationStrength ?: CaptureVibrationStrength.MEDIUM
    val captureFeedback = remember(strength) { IosCaptureFeedback(strength) }
    val scope = rememberCoroutineScope()
    var ready by remember(camera, permissions.captureAllowed) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("카메라 확인 중") }
    var successfulSaves by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showSettingsDetails by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var workspacePage by remember { mutableStateOf<CameraWorkspacePage?>(null) }
    var showQuickValues by remember { mutableStateOf(false) }
    var showProjectPicker by remember { mutableStateOf(false) }
    var showTools by remember(permissions.captureAllowed) { mutableStateOf(false) }
    var projectRegistry by remember(session) {
        mutableStateOf(session?.let { runCatching { it.loadProjectRegistry() }.getOrNull() })
    }
    var showGrid by remember(session) { mutableStateOf(session?.showCameraGrid ?: false) }
    var showTable by remember(session) { mutableStateOf(session?.showCameraTable ?: true) }
    var assistShutterEnabled by remember(session) {
        mutableStateOf(session?.assistShutterEnabled ?: false)
    }
    var assistShutterXRatio by remember(session) {
        mutableFloatStateOf(session?.assistShutterXRatio ?: 0.82f)
    }
    var assistShutterYRatio by remember(session) {
        mutableFloatStateOf(session?.assistShutterYRatio ?: 0.62f)
    }
    var zoom by remember(session) { mutableFloatStateOf(session?.cameraZoom ?: 1f) }
    var flash by remember(session) { mutableIntStateOf(session?.cameraFlashMode ?: 0) }
    var settingsRevision by remember { mutableIntStateOf(0) }
    var placementRevision by remember { mutableIntStateOf(0) }
    var counterRevision by remember { mutableIntStateOf(0) }
    var counterSyncing by remember(session) { mutableStateOf(session?.selectedDocument?.usesAutoNext == true) }
    var counterSyncReady by remember(session, permissions.captureAllowed, permissions.photoScopeVersion) { mutableStateOf(session?.selectedDocument?.usesAutoNext != true) }
    var liveTick by remember { mutableIntStateOf(0) }
    var overlayReady by remember(permissions.captureAllowed) { mutableStateOf(false) }
    var overlayEditing by remember(permissions.captureAllowed) { mutableStateOf(false) }
    var focusCollapseToken by remember { mutableIntStateOf(0) }
    var reviewVisible by remember(permissions.photoScopeVersion) { mutableStateOf(false) }
    val liveHasClock = session?.selectedDocument?.template?.templateState?.cells?.any {
        it.dataType == TableCellDataType.DATE || it.dataType == TableCellDataType.TIME
    } == true
    val cameraCovered = workspacePage != null || showSettingsDetails || showGallery || showQuickValues
    LaunchedEffect(ready, busy, overlayEditing, cameraCovered, liveHasClock) {
        if (liveHasClock && ready && !busy && !overlayEditing && !cameraCovered) {
            while (true) { delay(1000); liveTick += 1 }
        }
    }
    var settingsFailure by remember { mutableStateOf<String?>(null) }
    val continuousPreviewMode = session?.continuousPreviewMode ?: 0
    LaunchedEffect(reviewVisible, successfulSaves, continuousPreviewMode) {
        if (reviewVisible && continuousPreviewMode == 1) {
            delay(1_500)
            reviewVisible = false
        } else if (continuousPreviewMode == 0) reviewVisible = false
    }

    // Keep the visible filename and live overlay on the same persisted counter
    // that saveJpeg will verify again immediately before writing Photos.
    LaunchedEffect(session, settingsRevision, permissions.photoScopeVersion, permissions.captureAllowed) {
        if (session == null) return@LaunchedEffect
        counterSyncing = session.selectedDocument.usesAutoNext
        counterSyncReady = !counterSyncing
        if (!permissions.captureAllowed) {
            counterSyncing = false
            return@LaunchedEffect
        }
        try {
            val before = session.selectedDocument.nextCounter
            val next = session.synchronizeAutomaticCounter()
            if (next != before) counterRevision += 1
            counterSyncReady = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            status = "촬영 번호 확인 실패: ${error.message}"
        } finally {
            counterSyncing = false
        }
    }

    LaunchedEffect(camera, cameraCovered, permissions.captureAllowed, cameraAllowed) {
        try {
            ready = false
            if (cameraCovered || !permissions.captureAllowed) {
                withContext(Dispatchers.Default) { camera.stop() }
                if (!cameraAllowed) status = "카메라 권한이 없습니다."
                else if (!permissions.photos.readable) status = "촬영한 사진을 저장하려면 사진 접근을 허용해 주세요."
            } else if (!camera.configure()) {
                status = "카메라를 사용할 수 없는 기기입니다."
            } else {
                val restoredZoom = (session?.cameraZoom ?: zoom).coerceIn(1f, camera.maximumZoom)
                if (camera.setZoom(restoredZoom)) zoom = restoredZoom
                flash = session?.cameraFlashMode ?: flash
                camera.setFlash(flash)
                withContext(Dispatchers.Default) { camera.start() }
                ready = true
                status = "카메라 준비 완료"
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            status = "카메라 준비 실패: ${error.message}"
        }
    }
    DisposableEffect(camera) {
        onDispose { camera.stop() }
    }

    val page = workspacePage
    if (page != null && session != null) {
        HomeNavigationPoc(session.selectedDocument.template, editorKey = session.editorKey,
            cameraPage = page, onReturnToCamera = {
                val projects = runCatching { session.reloadAfterEditing(projectRegistry?.activeId) }
                if (projects.isFailure) {
                    // The selected template may have been deleted. Home handles an empty catalog.
                    onBack?.invoke()
                } else {
                    projectRegistry = projects.getOrThrow()
                    settingsRevision += 1
                    val document = session.selectedDocument
                    showSettingsDetails = document.nextCounter == null ||
                        (document.saveMode != SaveMode.ORIGINAL_ONLY && document.capturePlacement == null)
                    workspacePage = null
                }
            })
        return
    }
    if (showQuickValues && session != null) {
        var expected by remember { mutableStateOf(session.selectedDocument) }
        IosQuickValuesScreen(expected, session.quickSelection, onBack = { showQuickValues = false },
            availableDocuments = session.documents,
            onSwitchTemplate = { targetId, changes ->
                session.switchQuickTemplate(expected, targetId, changes).also { switched ->
                    if (switched) { expected = session.selectedDocument; settingsRevision += 1 }
                }
            }) { changes ->
            session.saveQuickValues(expected, changes).also { saved ->
                if (saved) { settingsRevision += 1; showQuickValues = false }
            }
        }
        return
    }

    if (showSettingsDetails && session != null) {
        IosCaptureSettingsScreen(session.selectedDocument, session.continuousPreviewMode,
            session.captureHapticEnabled, session.captureAspect.v, onBack = { showSettingsDetails = false }) {
                document, previewMode, hapticEnabled, aspectMode ->
            if (session.saveCaptureSettings(document, previewMode, hapticEnabled, aspectMode)) {
                settingsRevision += 1
                showSettingsDetails = false
            }
            else settingsFailure = "촬영 설정을 저장하지 못했습니다. 다시 시도해 주세요."
        }
        settingsFailure?.let { message ->
            com.dudoziworkshop.dzlog.ui.common.DDZConfirmDialog(title = "안내", message = message,
                confirmText = "확인", dismissText = "닫기", onConfirm = { settingsFailure = null },
                onDismiss = { settingsFailure = null })
        }
        return
    }
    if (showGallery) {
        if (session == null) {
            IosPhotoKitGalleryScreen(repository, onBack = { showGallery = false })
        } else {
            val registry = remember(session) { runCatching { session.loadProjectRegistry() } }
            val active = registry.getOrNull()?.active
            IosQuickResultsScreen(repository, active?.captureIds ?: session.sessionResultIds(),
                projectTitle = active?.title, returnTarget = IosQuickResultsReturnTarget.CAMERA,
                onBack = { showGallery = false },
                capturePathImpact = session::capturePathImpact,
                scopeError = if (registry.isFailure) "프로젝트 촬영 기록을 읽지 못했습니다." else null)
        }
        return
    }
    val info = remember(successfulSaves, settingsRevision, counterRevision, placementRevision, liveTick, showInfo) {
        session?.previewInfo()
    }
    val latestSessionCaptureId = session?.latestSessionCaptureId
    val recentThumbnail by key(permissions.photoScopeVersion) {
        produceState<ImageBitmap?>(null, latestSessionCaptureId, successfulSaves) {
            value = null
            if (permissions.photos.readable) {
                value = session?.readLatestSessionThumbnail()?.let { bytes ->
                    runCatching { SkiaImage.makeFromEncoded(uprightIosImage(bytes)).toComposeImageBitmap() }.getOrNull()
                }
            }
        }
    }
    if (showInfo && session != null && info != null) IosCameraInfoDialog(
        nextFileName = info.previewNaming.displayName,
        relativePaths = capturePhysicalSavePaths(info.previewNaming.relativePath, session.selectedDocument.saveMode),
        projectName = projectRegistry?.active?.title ?: "일반 촬영",
        templateName = session.selectedDocument.template.name,
        canChangeSelection = !busy && !counterSyncing && !session.hasPending(),
        projectSelectionEnabled = projectRegistry != null,
        onOpenProjectSelection = { showInfo = false; showProjectPicker = true },
        onOpenTemplateSelection = { showInfo = false; workspacePage = CameraWorkspacePage.TEMPLATES },
        onOpenTableEditor = { showInfo = false; workspacePage = CameraWorkspacePage.TABLE_EDITOR },
        onOpenSaveSettings = { showInfo = false; workspacePage = CameraWorkspacePage.SAVE_SETTINGS },
        onDismiss = { showInfo = false },
    )
    val captureEnabled = permissions.captureAllowed && ready && !showInfo && !showSettings && !showProjectPicker && !busy && !reviewVisible &&
        !overlayEditing && !counterSyncing && counterSyncReady &&
        (session == null || session.selectedDocument.saveMode == SaveMode.ORIGINAL_ONLY || overlayReady) &&
        (onBack == null || session != null) && session?.hasPending() != true
    val triggerCapture: () -> Unit = capture@{
        // Both shutter surfaces can deliver a tap before recomposition disables
        // them. Re-read mutable busy state here so a duplicate request cannot
        // fail in AVFoundation and clear the first capture's saving indicator.
        if (busy || showSettings || !captureEnabled) return@capture
        focusCollapseToken += 1
        busy = true
        showTools = false
        status = "사진 촬영 중"
        captureFeedback.prepare(session?.captureHapticEnabled ?: true)
        runCatching {
            camera.captureJpeg(onCaptureStarted = {
                captureFeedback.play(session?.captureHapticEnabled ?: true)
            }) { result ->
                scope.launch {
                    try {
                        val jpeg = result.getOrThrow()
                        val committed = if (session != null) {
                            session.saveConfiguredJpeg(jpeg)
                        } else {
                            check(onBack == null) { "선택한 iOS 촬영 문서가 없습니다." }
                            saveStandaloneCameraFixture(jpeg, repository)
                            true
                        }
                        if (committed) {
                            successfulSaves += 1
                            reviewVisible = continuousPreviewMode != 0
                            status = session?.takeProjectAssociationWarning() ?: "사진 저장 완료"
                        } else {
                            status = "사진 저장됨 · 촬영 기록 복구 필요"
                        }
                    } catch (error: Throwable) {
                        status = "사진 저장 실패: ${error.message}"
                    } finally {
                        busy = false
                    }
                }
            }
        }.onFailure { error ->
            status = "촬영 실패: ${error.message}"
            busy = false
        }
    }
    var pendingUndo by remember(session) { mutableStateOf<IosCameraCaptureSession.UndoPathWarning?>(null) }
    fun performUndo(confirmedId: String?, disableWarning: Boolean) {
        if (session != null && session.canUndoLatestCapture && !busy && !showSettings) {
            busy = true
            showTools = false
            status = "직전 촬영 삭제 중"
            scope.launch {
                try {
                    if (session.undoLatestCapture(confirmedId, disableWarning)) {
                        successfulSaves = (successfulSaves - 1).coerceAtLeast(0)
                        counterRevision += 1
                        status = session.takeProjectAssociationWarning()
                            ?: "직전 촬영 삭제·번호 복원 완료"
                    } else {
                        status = "직전 촬영 삭제 취소"
                    }
                } catch (error: Throwable) {
                    status = "직전 촬영 복구 필요: ${error.message}"
                } finally {
                    busy = false
                }
            }
        }
    }
    val triggerUndo: () -> Unit = {
        if (!busy && pendingUndo == null && session?.canUndoLatestCapture == true) {
            pendingUndo = session.undoPathWarning()
            if (pendingUndo == null) performUndo(null, false)
        }
    }
    pendingUndo?.let { warning ->
        com.dudoziworkshop.dzlog.ui.camera.CameraUndoPathWarningDialog(
            capturedFolder = warning.capturedFolder, currentFolder = warning.currentFolder,
            onCancel = { pendingUndo = null },
            onConfirm = { disableWarning ->
                pendingUndo = null
                performUndo(warning.captureId, disableWarning)
            },
        )
    }
    Column(
        Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding(),
    ) {
        IosCameraTopBar(
            displayName = info?.previewNaming?.displayName ?: "DZlog 촬영",
            infoEnabled = info != null && !busy && !reviewVisible && !overlayEditing,
            settingsEnabled = !busy && !reviewVisible && !overlayEditing && session != null && !session.hasPending(),
            backEnabled = onBack != null && !busy && !reviewVisible && !overlayEditing,
            onBack = {
                if (showTools) showTools = false else onBack?.invoke()
            },
            onOpenInfo = { showTools = false; showInfo = true },
            onOpenSettings = { showTools = false; showSettings = true },
        )
        if (session != null) IosCameraProjectBar(
            projectName = projectRegistry?.active?.title,
            enabled = projectRegistry != null && !busy && !reviewVisible && !overlayEditing &&
                !session.hasPending(),
            onClick = { showTools = false; showProjectPicker = true },
        )
        IosCameraStatusStrip(status, successfulSaves)
        if (cameraAllowed && onBack != null && session == null) Text(
            "촬영할 템플릿과 저장 설정이 준비되지 않았습니다.",
            modifier = Modifier.padding(horizontal = 16.dp), style = com.dudoziworkshop.dzlog.ui.theme.DDZTypography.Caption)
        if (session?.hasPending() == true) Text(
            "이전 촬영 기록을 복구한 후 촬영해 주세요.",
            modifier = Modifier.padding(horizontal = 16.dp), style = com.dudoziworkshop.dzlog.ui.theme.DDZTypography.Caption)
        Box(Modifier.fillMaxWidth().weight(1f).background(androidx.compose.ui.graphics.Color.Black),
            contentAlignment = Alignment.Center) {
            if (permissions.captureAllowed && ready && session != null) IosLiveTablePreview(
                modifier = Modifier.fillMaxSize(), session = session, previewLayer = camera.previewLayer,
                captureAspect = session.captureAspect.ratioF.toDouble(),
                enabled = !busy && !reviewVisible && !session.hasPending() && !showTools &&
                    !showSettings && !showProjectPicker,
                showGrid = showGrid,
                showTable = showTable,
                focusEnabled = ready && !busy && !reviewVisible && !showTools && !overlayEditing &&
                    !showSettings && !showProjectPicker,
                focusCollapseToken = focusCollapseToken,
                minimumExposureBias = camera.minimumExposureBias,
                maximumExposureBias = camera.maximumExposureBias,
                onFocusAt = camera::focusAt,
                onExposureLock = camera::setExposureLocked,
                onExposureBias = camera::setExposureBias,
                zoomEnabled = ready && !busy && !reviewVisible && !showTools && !overlayEditing &&
                    !showSettings && !showProjectPicker,
                currentZoom = zoom,
                maximumZoom = camera.maximumZoom,
                onZoomChange = { next ->
                    camera.setZoom(next).also { applied -> if (applied) zoom = next }
                },
                onZoomFinished = { next ->
                    if (session.setCameraZoom(next)) zoom = session.cameraZoom
                    else status = "카메라 배율을 저장하지 못했습니다."
                },
                revision = successfulSaves + settingsRevision + counterRevision + placementRevision + liveTick,
                onReady = { overlayReady = it }, onEditing = { overlayEditing = it },
                onSaved = { placementRevision += 1; status = "표 배치 저장 완료" },
                onError = { status = it },
            ) else if (permissions.captureAllowed && ready) UIKitView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    object : UIView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0)) {
                        override fun layoutSubviews() {
                            super.layoutSubviews()
                            camera.previewLayer.frame = bounds
                        }
                    }.apply { layer.addSublayer(camera.previewLayer) }
                },
            )
            if (!showTools && !busy && !reviewVisible) {
                if (!cameraAllowed) PermissionCard(camera = true, access = permissions.camera)
                else if (!permissions.photos.readable) PermissionCard(camera = false, access = permissions.photos)
            }
            if (showTools) IosCameraToolPanel(
                ready = ready && !busy && !reviewVisible,
                flashAvailable = camera.flashAvailable,
                flashLabel = listOf("꺼짐", "자동", "켜짐")[flash],
                zoom = zoom,
                maximumZoom = camera.maximumZoom,
                onAutoFocus = {
                    if (!camera.autoFocus()) status = "자동초점을 설정할 수 없습니다."
                },
                onCycleFlash = {
                    val next = (flash + 1) % 3
                    if (camera.setFlash(next)) {
                        if (session == null || session.setCameraFlashMode(next)) flash = next
                        else {
                            camera.setFlash(flash)
                            status = "플래시 설정을 저장하지 못했습니다."
                        }
                    }
                },
                onZoomChange = { if (camera.setZoom(it)) zoom = it },
                onZoomFinished = {
                    if (session == null || session.setCameraZoom(zoom)) {
                        zoom = session?.cameraZoom ?: zoom
                    } else status = "카메라 배율을 저장하지 못했습니다."
                },
                showGrid = showGrid,
                showTable = showTable,
                assistShutterEnabled = assistShutterEnabled,
                onToggleGrid = {
                    val next = !showGrid
                    if (session == null || session.setShowCameraGrid(next)) showGrid = next
                    else status = "격자 표시 설정을 저장하지 못했습니다."
                },
                onToggleTable = {
                    val next = !showTable
                    if (session == null || session.setShowCameraTable(next)) showTable = next
                    else status = "표 미리보기 설정을 저장하지 못했습니다."
                },
                onToggleAssistShutter = {
                    val next = !assistShutterEnabled
                    if (session == null || session.setAssistShutterEnabled(next)) assistShutterEnabled = next
                    else status = "보조 셔터 설정을 저장하지 못했습니다."
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            ) else IosCameraZoomChip(
                zoom = zoom,
                enabled = ready && !busy && !reviewVisible && camera.maximumZoom > 1f,
                onClick = { showTools = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            )
            IosFloatingAssistShutter(
                visible = assistShutterEnabled && !busy && !reviewVisible && !showTools &&
                    !overlayEditing && !showProjectPicker,
                captureEnabled = captureEnabled,
                xRatio = assistShutterXRatio,
                yRatio = assistShutterYRatio,
                onPositionRatioChange = { x, y ->
                    if (session == null || session.setAssistShutterPosition(x, y)) {
                        assistShutterXRatio = x
                        assistShutterYRatio = y
                    } else status = "보조 셔터 위치를 저장하지 못했습니다."
                },
                onCapture = triggerCapture,
                modifier = Modifier.fillMaxSize(),
            )
            if (reviewVisible && continuousPreviewMode != 0) Box(
                Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                    .clickable { reviewVisible = false },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (recentThumbnail != null) Image(
                        bitmap = recentThumbnail!!,
                        contentDescription = "촬영 결과 미리보기",
                        modifier = Modifier.fillMaxWidth(0.76f).fillMaxHeight(0.72f),
                        contentScale = ContentScale.Fit,
                    ) else Text("사진 불러오는 중", color = androidx.compose.ui.graphics.Color.White)
                    Text(if (continuousPreviewMode == 2) "화면을 터치하면 닫힙니다" else "저장 완료",
                        color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
        IosCameraBottomBar(
            captureEnabled = captureEnabled,
            controlsEnabled = !busy && !reviewVisible && !overlayEditing && !showProjectPicker,
            toolsSelected = showTools,
            quickValuesEnabled = session != null && !busy && !reviewVisible && !overlayEditing &&
                !showProjectPicker && !session.hasPending(),
            undoEnabled = permissions.photos.readable && session?.canUndoLatestCapture == true && !busy && !reviewVisible &&
                !overlayEditing && !showProjectPicker,
            recentThumbnail = recentThumbnail,
            galleryLabel = if (session == null) "앨범 보기" else "촬영 결과 보기",
            onOpenGallery = { showTools = false; showGallery = true },
            onToggleTools = { showTools = !showTools },
            onCapture = triggerCapture,
            onOpenQuickValues = { showTools = false; showQuickValues = true },
            onUndo = triggerUndo,
        )
    }
    if (showSettings && session != null) {
        fun applySetting(change: () -> Boolean, failure: String = "촬영 설정을 저장하지 못했습니다.") {
            if (runCatching(change).getOrDefault(false)) {
                settingsFailure = null
                settingsRevision += 1
            } else {
                settingsFailure = failure
            }
        }
        IosCaptureSettingsOverlay(
            captureAspect = session.captureAspect,
            mode = session.selectedDocument.saveMode,
            previewMode = continuousPreviewMode,
            hapticEnabled = session.captureHapticEnabled,
            error = settingsFailure,
            onAspectChange = { value ->
                if (value != session.captureAspect) applySetting({ session.applyCameraPanelSettings(aspect = value) })
            },
            onModeChange = { value ->
                if (value != session.selectedDocument.saveMode) {
                    if (value != SaveMode.ORIGINAL_ONLY && session.tablePlacement == null) {
                        settingsFailure = "표·번호 설정에서 표 위치를 먼저 저장해 주세요."
                    } else {
                        applySetting({ session.applyCameraPanelSettings(mode = value) })
                    }
                }
            },
            onPreviewChange = { value ->
                if (value != continuousPreviewMode) applySetting({ session.applyCameraPanelSettings(previewMode = value) })
            },
            onHapticChange = { value -> applySetting({ session.applyCameraPanelSettings(hapticEnabled = value) }) },
            onOpenDetails = {
                settingsFailure = null
                showSettings = false
                showSettingsDetails = true
            },
            onDismiss = {
                settingsFailure = null
                showSettings = false
            },
        )
    }
    if (showProjectPicker && session != null && projectRegistry != null) {
        IosProjectPickerSheet(
            registry = requireNotNull(projectRegistry),
            onSelect = { projectId ->
                if (session.selectProject(projectId)) {
                    projectRegistry = runCatching { session.loadProjectRegistry() }.getOrNull()
                    settingsRevision += 1
                    counterRevision += 1
                    showProjectPicker = false
                    status = "촬영 프로젝트 변경 완료"
                } else {
                    status = "촬영 프로젝트를 변경하지 못했습니다."
                }
            },
            onCreate = {
                showProjectPicker = false
                workspacePage = CameraWorkspacePage.CREATE_PROJECT
            },
            onManage = {
                showProjectPicker = false
                workspacePage = CameraWorkspacePage.PROJECTS
            },
            onDismiss = { showProjectPicker = false },
        )
    }
}

/** Dedicated legacy --camera-flow fixture; never used by an actual home capture. */
private suspend fun saveStandaloneCameraFixture(
    jpeg: ByteArray,
    repository: PhotoKitCaptureMediaRepository,
) {
    val captureId = NSUUID().UUIDString.lowercase()
    val mode = SaveMode.ORIGINAL_ONLY
    val plan = buildCaptureMediaSavePlan(
        FinalCapturePreview(
            resolvedCells = emptyList(),
            tablePatch = TablePatch.EMPTY,
            displayName = "CAM_${captureId.take(8)}.jpg",
            usedCounter = 1,
            nextPhraseProgressCursor = 0,
            counterScope = CounterScope(buildCounterPath(CAMERA_POC_PATH, mode), "CAM_", 1, false),
            relativePathPreview = CAMERA_POC_PATH,
        ), mode, captureId,
    )
    val receipt = repository.saveAllOrRollback(plan, mapOf(CaptureMediaRole.ORIGINAL to jpeg))
    check(receipt.assets.size == 1)
}

