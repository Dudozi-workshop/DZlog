@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import com.dudoziworkshop.dzlog.domain.naming.capturePhysicalSavePaths
import com.dudoziworkshop.dzlog.ui.home.HomeSaveInfoSheet
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    var zoom by remember { mutableFloatStateOf(1f) }
    var flash by remember { mutableIntStateOf(0) }
    var settingsRevision by remember { mutableIntStateOf(0) }
    var placementRevision by remember { mutableIntStateOf(0) }
    var liveTick by remember { mutableIntStateOf(0) }
    var overlayReady by remember { mutableStateOf(false) }
    var overlayEditing by remember { mutableStateOf(false) }
    val liveHasClock = session?.selectedDocument?.template?.templateState?.cells?.any {
        it.dataType == TableCellDataType.DATE || it.dataType == TableCellDataType.TIME
    } == true
    LaunchedEffect(ready, busy, overlayEditing, showSettings, showGallery, liveHasClock) {
        if (liveHasClock && ready && !busy && !overlayEditing && !showSettings && !showGallery) {
            while (true) { delay(1000); liveTick += 1 }
        }
    }
    var settingsFailure by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(camera, showSettings, showGallery) {
        try {
            ready = false
            if (showSettings || showGallery) {
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
    val info = remember(successfulSaves, settingsRevision, placementRevision, liveTick, showInfo) { session?.previewInfo() }
    if (showInfo && session != null && info != null) HomeSaveInfoSheet(
        fileName = info.previewNaming.displayName,
        relativePaths = capturePhysicalSavePaths(info.previewNaming.relativePath, session.selectedDocument.saveMode),
        onOpenSaveSettings = { showInfo = false; showSettings = true },
        onDismiss = { showInfo = false }, settingsEnabled = !busy && !session.hasPending(),
    )
    Column(
        Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) TextButton(onClick = onBack, enabled = !busy && !overlayEditing) { Text("뒤로가기") }
            Text("카메라")
            TextButton(onClick = { showSettings = true }, enabled = !busy && !overlayEditing && session != null && !session.hasPending()) {
                Text("촬영 설정")
            }
        }
        if (info != null) TextButton(onClick = { showInfo = true }, enabled = !busy && !overlayEditing) {
            Text(info.previewNaming.displayName.removeSuffix(".jpg"), maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Text(status)
        Text("이번 촬영: ${successfulSaves}장")
        if (permissionGranted && onBack != null && session == null) Text("촬영할 템플릿과 저장 설정이 준비되지 않았습니다.")
        if (session?.hasPending() == true) Text("이전 촬영 기록을 복구한 후 촬영해 주세요.")
        Box(Modifier.fillMaxWidth().weight(1f).background(androidx.compose.ui.graphics.Color.Black),
            contentAlignment = Alignment.Center) {
            if (ready && session != null) IosLiveTablePreview(
                modifier = Modifier.fillMaxSize(), session = session, previewLayer = camera.previewLayer,
                enabled = !busy && !session.hasPending(),
                revision = successfulSaves + settingsRevision + placementRevision + liveTick,
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
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { if (!camera.autoFocus()) status = "자동초점을 설정할 수 없습니다." }, enabled = ready && !busy) {
                Text("자동초점")
            }
            TextButton(enabled = ready && !busy && camera.flashAvailable, onClick = {
                val next = (flash + 1) % 3
                if (camera.setFlash(next)) flash = next
            }) { Text("플래시 " + listOf("꺼짐", "자동", "켜짐")[flash]) }
        }
        Text("줌 ${((zoom * 10).toInt() / 10f)}배")
        Slider(value = zoom, valueRange = 1f..maxOf(1.01f, camera.maximumZoom),
            enabled = ready && !busy && camera.maximumZoom > 1f,
            onValueChange = { if (camera.setZoom(it)) zoom = it })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { showGallery = true }, enabled = !busy && !overlayEditing) { Text("앨범 보기") }
        Button(shape = CircleShape, modifier = Modifier.size(76.dp), enabled = ready && !busy && !overlayEditing &&
            (session == null || session.selectedDocument.saveMode == SaveMode.ORIGINAL_ONLY || overlayReady) && (onBack == null || session != null) &&
            (session?.hasPending() != true), onClick = {
            busy = true
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
        }) { Text(if (busy) "저장 중" else "촬영") }
        Text("${successfulSaves}장")
        }
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

