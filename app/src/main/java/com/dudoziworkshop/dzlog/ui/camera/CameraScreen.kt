@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.dudoziworkshop.dzlog.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.mediastore.MediaStoreSaverImpl
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.repository.DzlogRepositoryImpl
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.VolumeKeyAction
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.capture.permission.hasCameraPermission
import com.dudoziworkshop.dzlog.feature.capture.policy.UndoCapturePolicy
import com.dudoziworkshop.dzlog.feature.counter.camera.CameraCounterController
import com.dudoziworkshop.dzlog.ui.camera.controls.buildCameraTriggerCapture
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraBottomControls
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraTopBar
import com.dudoziworkshop.dzlog.ui.camera.effects.CameraNowTickEffect
import com.dudoziworkshop.dzlog.ui.camera.effects.CameraPrefsEffect
import com.dudoziworkshop.dzlog.ui.camera.effects.CameraVolumeKeyEffect
import com.dudoziworkshop.dzlog.ui.camera.effects.rememberLatestImageController
import com.dudoziworkshop.dzlog.ui.camera.effects.rememberUndoDeleteController
import com.dudoziworkshop.dzlog.ui.camera.presenter.rememberCameraLayoutState
import com.dudoziworkshop.dzlog.ui.camera.presenter.rememberCameraPreviewAreaArgs
import com.dudoziworkshop.dzlog.ui.camera.preview.CameraPreviewArea
import com.dudoziworkshop.dzlog.ui.camera.preview.buildWatermarkConfig
import com.dudoziworkshop.dzlog.ui.camera.settings.CameraSettingsOverlayPanel
import com.dudoziworkshop.dzlog.ui.camera.settings.CameraSettingsWriter
import com.dudoziworkshop.dzlog.ui.camera.state.CameraViewModel
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFlashMode
import com.dudoziworkshop.dzlog.ui.camera.state.computeCameraDerivedState
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.common.rememberThreeButtonNavEquivalentBottomPadding
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val USABLE_VERTICAL_MARGIN = 10.dp

@Composable
fun CameraScreen(
    tableTemplateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
    sessionCaptureStack: SnapshotStateList<List<Uri>>,
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(hasCameraPermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.dzScreen()) {
        if (hasPermission) {
            CameraPreview(
                tableTemplateState = tableTemplateState,
                onTemplateChange = onTemplateChange,
                onOpenTableEditor = onOpenTableEditor,
                onOpenAlbum = onOpenAlbum,
                onOpenRecentCaptureGrid = onOpenRecentCaptureGrid,
                sessionCaptureStack = sessionCaptureStack,
            )
        } else {
            Text(
                text = "카메라 권한이 필요합니다.\n설정에서 권한을 허용해주세요.",
                modifier = Modifier.align(Alignment.Center),
                color = DDZColor.Surface
            )
        }
    }
}

@SuppressLint("AutoboxingStateCreation")
@Composable
fun CameraPreview(
    tableTemplateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
    sessionCaptureStack: SnapshotStateList<List<Uri>>,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as? LifecycleOwner ?: return
    val scope = rememberCoroutineScope()
    val cameraViewModel: CameraViewModel = viewModel()
    val settingsWriter = remember(context) { CameraSettingsWriter(context) }
    val repository: DzlogRepositoryImpl = remember {
        DzlogRepositoryImpl(
            saver = MediaStoreSaverImpl(),
            watermarkRenderer = com.dudoziworkshop.dzlog.watermark.WatermarkRendererImpl()
        )
    }

    val appSettings by AppSettingsStore.flow(context).collectAsState(
        initial = AppSettings(
            saveMode = SaveMode.BOTH,
            continuousPreviewMode = ContinuousPreviewMode.OFF,
            photoQualityMode = PhotoQualityMode.BALANCED,
            counterPadding = 3,
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
            toastEnabled = true,
            hapticEnabled = true,
            captureHapticEnabled = true,
            captureSoundEnabled = true,
            volumeKeyAction = VolumeKeyAction.NONE,
            blankWarningEnabled = true,
        )
    )
    val captureFeedback = remember(context) { CaptureFeedback(context) }

    var boundImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val ui = cameraViewModel.ui
    val threeButtonEquivalentBottomPadding = rememberThreeButtonNavEquivalentBottomPadding()
    val layout = rememberCameraLayoutState(usableVerticalMargin = USABLE_VERTICAL_MARGIN)

    val tableResolver = remember { TableResolver() }
    val fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
    val dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT
    val timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT
    val derivedState = remember(
        tableTemplateState,
        ui.capture.now,
        ui.prefs.counterDigits,
        cameraViewModel.phraseProgressCounter,
        appSettings.includePathInCounterScope,
        appSettings.includeFilenameInCounterScope,
        appSettings.saveMode,
        ui.counter.scopeNextCounter,
    ) {
        computeCameraDerivedState(
            tableTemplateState = tableTemplateState,
            now = ui.capture.now,
            counterDigits = ui.prefs.counterDigits,
            phraseProgressCounter = cameraViewModel.phraseProgressCounter,
            includePathInCounterScope = appSettings.includePathInCounterScope,
            includeFilenameInCounterScope = appSettings.includeFilenameInCounterScope,
            saveMode = appSettings.saveMode,
            syncedNextCounter = ui.counter.scopeNextCounter,
            tableResolver = tableResolver,
        )
    }
    CameraNowTickEffect(
        lifecycleOwner = lifecycleOwner,
        cells = tableTemplateState.cells,
        onNowChange = { ui.capture.now = it },
    )
    val captureScopeState = derivedState.captureScopeState
    val scopedCounterStream = derivedState.scopedCounterStream
    val finalCapturePreview = derivedState.finalCapturePreview
    val displayCounter = derivedState.displayCounter
    val latestImageController = rememberLatestImageController(
        context = context,
        counterScopeRelativePathKey = captureScopeState.counterScope.relativePathKey,
        saveMode = appSettings.saveMode
    )
    val latestImage = latestImageController.latestImage

    fun syncAfterUndoDelete() {
        latestImageController.reload()
        // 실제 undo 삭제 완료(미디어 삭제 성공) 시점 이벤트다.
        // 버튼 클릭 시점이 아니라 완료 시점에만 발행해 카운터 재동기화 타이밍을 맞춘다.
        cameraViewModel.onUndoCommitted()
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            cameraViewModel.bumpResumeResyncTick()
            awaitCancellation()
        }
    }

    val undoDeleteController = rememberUndoDeleteController(
        context = context,
        onCommitted = { syncAfterUndoDelete() },
        onRestore = { targetUris -> UndoCapturePolicy.restoreCapture(sessionCaptureStack, targetUris) },
    )

    val isTemplateReady = derivedState.isTemplateReady

    // ✅ 카운터 단일소스: 표기(ON/OFF)와 무관하게 스트림 nextSeed로 ui.counter를 항상 동기화
    CameraCounterController(
        counterScope = captureScopeState.counterScope,
        scanPrefix = captureScopeState.scanPrefix,
        resumeTick = cameraViewModel.resumeResyncTick,
        counterEventTick = cameraViewModel.counterEventTick,
        latestCounterEvent = cameraViewModel.latestCounterEvent,
        isTemplateReady = isTemplateReady,
        appSettings = appSettings,
        ui = ui,
    )

    val topDisplayName = derivedState.topDisplayName

    fun resetZoomToDefault() {
        ui.prefs.zoomRatioTenths = 10
        ui.capture.actualZoomTenths = 10
        ui.capture.maxZoomTenths = 20
        scope.launch { settingsWriter.setZoomTenths(10) }
    }

    fun commitZoomTenths(next: Int) {
        val normalized = next.coerceIn(10, ui.capture.maxZoomTenths.coerceAtLeast(10))
        ui.prefs.zoomRatioTenths = normalized
        scope.launch { settingsWriter.setZoomTenths(normalized) }
    }

    // 정책 정리: 셔터 버튼/음량키 모두 같은 촬영 실행 경로를 사용한다.
    val triggerCapture = buildCameraTriggerCapture(
        context = context,
        ui = ui,
        appSettings = appSettings,
        boundImageCapture = boundImageCapture,
        finalCapturePreview = finalCapturePreview,
        scopedCounterStream = scopedCounterStream,
        tableTemplateState = tableTemplateState,
        fnDelim = fnDelim,
        repository = repository,
        cameraViewModel = cameraViewModel,
        captureFeedback = captureFeedback,
        sessionCaptureStack = sessionCaptureStack,
        latestImageController = latestImageController,
        onTemplateChange = onTemplateChange,
        buildWatermarkConfig = ::buildWatermarkConfig,
    )

    LaunchedEffect(ui.capture.capturedUri, ui.prefs.continuousPreviewMode) {
        if (ui.capture.capturedUri != null && ui.prefs.continuousPreviewMode == ContinuousPreviewMode.SHORT) {
            delay(1500)
            ui.capture.capturedUri = null
        }
    }

    CameraPrefsEffect(
        lifecycleOwner = lifecycleOwner,
        readPrefs = { context.dataStore.data.first() },
        ui = ui
    )

    DisposableEffect(Unit) {
        onDispose {
            resetZoomToDefault()
        }
    }

    CameraVolumeKeyEffect(
        volumeKeyAction = appSettings.volumeKeyAction,
        onCapture = {
            ui.dismissToolOverlays()
            triggerCapture()
        },
        onZoomDelta = { deltaTenths ->
            commitZoomTenths(ui.prefs.zoomRatioTenths + deltaTenths)
        }
    )

    LaunchedEffect(boundImageCapture, ui.prefs.flashMode) {
        val capture = boundImageCapture ?: return@LaunchedEffect
        // 바인딩된 ImageCapture 인스턴스가 교체되어도 선택된 flash mode를 즉시 유지한다.
        capture.flashMode = when (ui.prefs.flashMode) {
            CameraFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
            CameraFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            CameraFlashMode.ON -> ImageCapture.FLASH_MODE_ON
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DDZColor.PrimaryDark.copy(alpha = 0f))
                .onGloballyPositioned { coordinates ->
                    layout.onCameraRootHeightPxChange(coordinates.size.height.toFloat())
                }
        ) {
            CameraTopBar(
                topDisplayName = topDisplayName,
                onOpenTableEditor = {
                    ui.dismissToolOverlays()
                    onOpenTableEditor()
                },
                onOpenSettings = {
                    ui.dismissToolOverlays()
                    ui.showWizard = true
                },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val previewAreaArgs = rememberCameraPreviewAreaArgs(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    ui = ui,
                    appSettings = appSettings,
                    settingsWriter = settingsWriter,
                    tableTemplateState = tableTemplateState,
                    tableResolver = tableResolver,
                    scopeNextCounter = displayCounter,
                    phraseProgressCursor = cameraViewModel.phraseProgressCounter,
                    dateFormat = dateFormat,
                    timeFormat = timeFormat,
                    fnDelim = fnDelim,
                    shutterButtonTopY = layout.shutterButtonTopY,
                    safeTopY = layout.safeTopY,
                    safeBottomY = layout.safeBottomY,
                    usableVerticalMarginPx = layout.usableVerticalMarginPx,
                    onOpenTableEditor = {
                        ui.dismissToolOverlays()
                        onOpenTableEditor()
                    },
                )

                CameraPreviewArea(
                    args = previewAreaArgs,
                    boundCamera = boundCamera,
                    onBoundCameraChange = { boundCamera = it },
                    onBoundImageCaptureChange = { boundImageCapture = it },
                    capturedUri = ui.capture.capturedUri,
                    onDismissCaptured = { ui.capture.capturedUri = null },
                    tapFocusUi = ui.capture.tapFocusUi,
                    onTapFocusUiChange = { ui.capture.tapFocusUi = it }
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = threeButtonEquivalentBottomPadding)
                ) {
                    CameraBottomControls(
                        scope = scope,
                        ui = ui,
                        settingsWriter = settingsWriter,
                        boundImageCaptureAvailable = (boundImageCapture != null),
                        latestImage = latestImage,
                        onOpenAlbum = onOpenAlbum,
                        onOpenRecentCaptureGrid = onOpenRecentCaptureGrid,
                        sessionCaptureStack = sessionCaptureStack,
                        undoPending = (undoDeleteController.pendingUris != null),
                        onUndoDelete = { uris -> undoDeleteController.delete(uris) },
                        onTriggerCapture = {
                            ui.dismissToolOverlays()
                            triggerCapture()
                        },
                        onShutterButtonTopYChange = { layout.onShutterButtonTopYChange(it) },
                        hapticEnabled = appSettings.hapticEnabled,
                    )
                }
            }
        }

        if (ui.capture.capturedUri != null && ui.prefs.continuousPreviewMode != ContinuousPreviewMode.OFF) {
            // UX 보정: 결과 미리보기 노출 시에는 화면 어디를 눌러도 닫히도록 전체 영역 dismiss를 제공한다.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { ui.capture.capturedUri = null }
            )
        }

        if (ui.showWizard) {
            CameraSettingsOverlayPanel(
                captureAspect = ui.prefs.captureAspect,
                onCaptureAspectChange = { aspect ->
                    ui.prefs.captureAspect = aspect
                    scope.launch { settingsWriter.setCaptureAspect(aspect) }
                },
                saveMode = ui.prefs.saveMode,
                onSaveModeChange = { mode ->
                    ui.prefs.saveMode = mode
                    scope.launch { settingsWriter.setSaveMode(mode) }
                },
                showGrid = ui.prefs.showGrid,
                onShowGridChange = { checked ->
                    ui.prefs.showGrid = checked
                    scope.launch { settingsWriter.setShowGrid(checked) }
                },
                showTable = ui.prefs.showWmPreview,
                onShowTableChange = { checked ->
                    ui.prefs.showWmPreview = checked
                    scope.launch { settingsWriter.setShowWmPreview(checked) }
                },
                continuousPreviewMode = ui.prefs.continuousPreviewMode,
                onContinuousPreviewModeChange = { mode ->
                    ui.prefs.continuousPreviewMode = mode
                    scope.launch { settingsWriter.setContinuousPreviewMode(mode) }
                },
                volumeKeyAction = appSettings.volumeKeyAction,
                onVolumeKeyActionChange = { action ->
                    scope.launch { settingsWriter.setVolumeKeyAction(action) }
                },
                onDismiss = { ui.showWizard = false }
            )
        }
    }
}
