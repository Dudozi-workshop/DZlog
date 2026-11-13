@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import com.dudoziworkshop.dzlog.domain.naming.capturePhysicalSavePaths
import com.dudoziworkshop.dzlog.ui.home.HomeSaveInfoSheet
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.interop.UIKitView
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
        require(widthRatio in 10..100 && heightRatio in 10..100) {
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
    val camera = remember { IosAvFoundationCamera() }
    val repository = remember { PhotoKitCaptureMediaRepository() }
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("카메라 확인 중") }
    var successfulSaves by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showQuickValues by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
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
    var zoom by remember { mutableFloatStateOf(1f) }
    var flash by remember { mutableIntStateOf(0) }
    var settingsRevision by remember { mutableIntStateOf(0) }
    var placementRevision by remember { mutableIntStateOf(0) }
    var counterRevision by remember { mutableIntStateOf(0) }
    var counterSyncing by remember(session) { mutableStateOf(session?.selectedDocument?.usesAutoNext == true) }
    var counterSyncReady by remember(session) { mutableStateOf(session?.selectedDocument?.usesAutoNext != true) }
    var liveTick by remember { mutableIntStateOf(0) }
    var overlayReady by remember { mutableStateOf(false) }
    var overlayEditing by remember { mutableStateOf(false) }
    val liveHasClock = session?.selectedDocument?.template?.templateState?.cells?.any {
        it.dataType == TableCellDataType.DATE || it.dataType == TableCellDataType.TIME
    } == true
    LaunchedEffect(ready, busy, overlayEditing, showSettings, showGallery, showQuickValues, liveHasClock) {
        if (liveHasClock && ready && !busy && !overlayEditing && !showSettings && !showGallery && !showQuickValues) {
            while (true) { delay(1000); liveTick += 1 }
        }
    }
    var settingsFailure by remember { mutableStateOf<String?>(null) }

    // Keep the visible filename and live overlay on the same persisted counter
    // that saveJpeg will verify again immediately before writing Photos.
    LaunchedEffect(session, settingsRevision) {
        if (session == null) return@LaunchedEffect
        counterSyncing = session.selectedDocument.usesAutoNext
        counterSyncReady = !counterSyncing
        try {
            val before = session.selectedDocument.nextCounter
            val next = session.synchronizeAutomaticCounter()
            if (next != before) counterRevision += 1
            counterSyncReady = true
        } catch (error: Throwable) {
            status = "촬영 번호 확인 실패: ${error.message}"
        } finally {
            counterSyncing = false
        }
    }

    LaunchedEffect(camera, showSettings, showGallery, showQuickValues) {
        try {
            ready = false
            if (showSettings || showGallery || showQuickValues) {
                withContext(Dispatchers.Default) { camera.stop() }
            } else if (!permissionGranted) {
                status = "카메라 권한이 없습니다."
            } else if (!camera.configure()) {
                status = "카메라를 사용할 수 없는 기기입니다."
            } else {
                withContext(Dispatchers.Default) { camera.start() }
                ready = true
                status = "카메라 준비 완료"
            }
        } catch (error: Throwable) {
            status = "카메라 준비 실패: ${error.message}"
        }
    }
    DisposableEffect(camera) {
        onDispose { camera.stop() }
    }

    if (showQuickValues && session != null) {
        var expected by remember { mutableStateOf(session.selectedDocument) }
        IosQuickValuesScreen(expected, onBack = { showQuickValues = false },
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

    if (showSettings && session != null) {
        IosCaptureSettingsScreen(session.selectedDocument, onBack = { showSettings = false }) { document ->
            if (session.saveSettings(document)) { settingsRevision += 1; showSettings = false }
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
        IosPhotoKitGalleryScreen(repository, onBack = { showGallery = false })
        return
    }
    val info = remember(successfulSaves, settingsRevision, counterRevision, placementRevision, liveTick, showInfo) {
        session?.previewInfo()
    }
    val latestSessionCaptureId = session?.latestSessionCaptureId
    val recentThumbnail by produceState<ImageBitmap?>(initialValue = null,
        latestSessionCaptureId, successfulSaves) {
        value = session?.readLatestSessionThumbnail()?.let { bytes ->
            runCatching { SkiaImage.makeFromEncoded(uprightIosImage(bytes)).toComposeImageBitmap() }.getOrNull()
        }
    }
    if (showInfo && session != null && info != null) HomeSaveInfoSheet(
        fileName = info.previewNaming.displayName,
        relativePaths = capturePhysicalSavePaths(info.previewNaming.relativePath, session.selectedDocument.saveMode),
        onOpenSaveSettings = { showInfo = false; showSettings = true },
        onDismiss = { showInfo = false }, settingsEnabled = !busy && !session.hasPending(),
    )
    val captureEnabled = ready && !busy && !overlayEditing && !counterSyncing && counterSyncReady &&
        (session == null || session.selectedDocument.saveMode == SaveMode.ORIGINAL_ONLY || overlayReady) &&
        (onBack == null || session != null) && session?.hasPending() != true
    val triggerCapture: () -> Unit = {
        busy = true
        showTools = false
        status = "사진 촬영 중"
        runCatching {
            camera.captureJpeg { result ->
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
                            status = "사진 저장 완료"
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
    val triggerUndo: () -> Unit = {
        if (session != null && session.canUndoLatestCapture && !busy) {
            busy = true
            showTools = false
            status = "직전 촬영 삭제 중"
            scope.launch {
                try {
                    if (session.undoLatestCapture()) {
                        successfulSaves = (successfulSaves - 1).coerceAtLeast(0)
                        counterRevision += 1
                        status = "직전 촬영 삭제·번호 복원 완료"
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
    Column(
        Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding(),
    ) {
        IosCameraTopBar(
            displayName = info?.previewNaming?.displayName ?: "DZlog 촬영",
            infoEnabled = info != null && !busy && !overlayEditing && !counterSyncing && counterSyncReady,
            settingsEnabled = !busy && !overlayEditing && session != null && !session.hasPending(),
            backEnabled = onBack != null && !busy && !overlayEditing,
            onBack = {
                if (showTools) showTools = false else onBack?.invoke()
            },
            onOpenInfo = { showTools = false; showInfo = true },
            onOpenSettings = { showTools = false; showSettings = true },
        )
        IosCameraStatusStrip(status, successfulSaves)
        if (permissionGranted && onBack != null && session == null) Text(
            "촬영할 템플릿과 저장 설정이 준비되지 않았습니다.",
            modifier = Modifier.padding(horizontal = 16.dp), style = com.dudoziworkshop.dzlog.ui.theme.DDZTypography.Caption)
        if (session?.hasPending() == true) Text(
            "이전 촬영 기록을 복구한 후 촬영해 주세요.",
            modifier = Modifier.padding(horizontal = 16.dp), style = com.dudoziworkshop.dzlog.ui.theme.DDZTypography.Caption)
        Box(Modifier.fillMaxWidth().weight(1f).background(androidx.compose.ui.graphics.Color.Black),
            contentAlignment = Alignment.Center) {
            if (ready && session != null) IosLiveTablePreview(
                modifier = Modifier.fillMaxSize(), session = session, previewLayer = camera.previewLayer,
                enabled = !busy && !session.hasPending() && !showTools,
                showGrid = showGrid,
                showTable = showTable,
                revision = successfulSaves + settingsRevision + counterRevision + placementRevision + liveTick,
                onReady = { overlayReady = it }, onEditing = { overlayEditing = it },
                onSaved = { placementRevision += 1; status = "표 배치 저장 완료" },
                onError = { status = it },
            ) else if (ready) UIKitView(
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
            if (showTools) IosCameraToolPanel(
                ready = ready && !busy,
                flashAvailable = camera.flashAvailable,
                flashLabel = listOf("꺼짐", "자동", "켜짐")[flash],
                zoom = zoom,
                maximumZoom = camera.maximumZoom,
                onAutoFocus = {
                    if (!camera.autoFocus()) status = "자동초점을 설정할 수 없습니다."
                },
                onCycleFlash = {
                    val next = (flash + 1) % 3
                    if (camera.setFlash(next)) flash = next
                },
                onZoomChange = { if (camera.setZoom(it)) zoom = it },
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
                enabled = ready && !busy && camera.maximumZoom > 1f,
                onClick = { showTools = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            )
            IosFloatingAssistShutter(
                visible = assistShutterEnabled && !showTools && !overlayEditing,
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
        }
        IosCameraBottomBar(
            captureEnabled = captureEnabled,
            controlsEnabled = !busy && !overlayEditing,
            toolsSelected = showTools,
            quickValuesEnabled = session != null && !busy && !overlayEditing && !session.hasPending(),
            undoEnabled = session?.canUndoLatestCapture == true && !busy && !overlayEditing,
            recentThumbnail = recentThumbnail,
            onOpenGallery = { showTools = false; showGallery = true },
            onToggleTools = { showTools = !showTools },
            onCapture = triggerCapture,
            onOpenQuickValues = { showTools = false; showQuickValues = true },
            onUndo = triggerUndo,
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

