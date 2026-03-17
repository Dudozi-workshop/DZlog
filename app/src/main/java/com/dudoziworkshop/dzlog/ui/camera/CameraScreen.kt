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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
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
import com.dudoziworkshop.dzlog.domain.model.WatermarkConfig
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.capture.permission.hasCameraPermission
import com.dudoziworkshop.dzlog.feature.capture.policy.UndoCapturePolicy
import com.dudoziworkshop.dzlog.feature.counter.camera.CameraCounterController
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraBottomControls
import com.dudoziworkshop.dzlog.ui.camera.controls.CaptureClickCallbacks
import com.dudoziworkshop.dzlog.ui.camera.controls.handleCaptureClick
import com.dudoziworkshop.dzlog.ui.camera.effects.CameraNowTickEffect
import com.dudoziworkshop.dzlog.ui.camera.effects.CameraPrefsEffect
import com.dudoziworkshop.dzlog.ui.camera.effects.CameraVolumeKeyEffect
import com.dudoziworkshop.dzlog.ui.camera.effects.rememberLatestImageController
import com.dudoziworkshop.dzlog.ui.camera.effects.rememberUndoDeleteController
import com.dudoziworkshop.dzlog.ui.camera.presenter.rememberCameraLayoutState
import com.dudoziworkshop.dzlog.ui.camera.presenter.rememberCameraPreviewAreaArgs
import com.dudoziworkshop.dzlog.ui.camera.preview.CameraPreviewArea
import com.dudoziworkshop.dzlog.ui.camera.settings.CameraSettingsOverlayPanel
import com.dudoziworkshop.dzlog.ui.camera.settings.CameraSettingsWriter
import com.dudoziworkshop.dzlog.ui.camera.state.CameraViewModel
import com.dudoziworkshop.dzlog.ui.camera.state.computeCameraDerivedState
import com.dudoziworkshop.dzlog.ui.common.CounterAwareFileNameText
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val USABLE_VERTICAL_MARGIN = 10.dp

// NOTE: buildWatermarkConfig는 다른 파일(핸들러)에서도 사용되므로 file-private 금지
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
    var zoomPanelExpanded by remember { mutableStateOf(false) }
    val ui = cameraViewModel.ui
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
        scope.launch { context.dataStore.edit { it[KEY_CAMERA_ZOOM_TENTHS] = 10 } }
    }

    fun commitZoomTenths(next: Int) {
        val normalized = next.coerceIn(10, ui.capture.maxZoomTenths.coerceAtLeast(10))
        ui.prefs.zoomRatioTenths = normalized
        scope.launch { context.dataStore.edit { it[KEY_CAMERA_ZOOM_TENTHS] = normalized } }
    }

    // 정책 정리: 셔터 버튼/음량키 모두 같은 촬영 실행 경로를 사용한다.
    val triggerCapture: () -> Unit = trigger@{
        // 오작동 방지: 캡처 불가 상태에서는 입력 피드백/촬영 로직을 모두 실행하지 않는다.
        if (boundImageCapture == null || ui.capture.capturedUri != null || ui.capture.isCapturing) return@trigger
        // counter 미동기화(null) 상태에서는 최종 preview가 없으므로 캡처를 시작하지 않는다.
        val capturePreview = finalCapturePreview ?: return@trigger

        // 정책 변경: 촬영 피드백은 저장 완료가 아니라 촬영 트리거(버튼/음량키) 시점에 즉시 제공한다.
        captureFeedback.play(
            successVibrationEnabled = appSettings.hapticEnabled && appSettings.captureHapticEnabled,
            soundEnabled = appSettings.captureSoundEnabled
        )

        // 구조 정리: capture 후처리 콜백을 하나의 묶음으로 전달해 호출부 가독성을 유지한다.
        val callbacks = CaptureClickCallbacks(
            // 정책 유지: 저장 성공 직후 프리뷰 숫자를 즉시 다음 값으로 반영한다.
            onAdvancePreviewCounter = { nextCounter ->
                val resolved = nextCounter.coerceAtLeast(1)
                ui.counter.scopeNextCounter = resolved
            },
            onAddToSessionStack = { uris ->
                UndoCapturePolicy.pushCapture(sessionCaptureStack, uris)
                // 실제 촬영 저장 완료(세션 stack 반영 완료) 시점 이벤트다.
                // 버튼 클릭 시점이 아니라 완료 시점에만 발행해 본체 카운터 동기화가 즉시 반영되게 한다.
                cameraViewModel.onCaptureCommitted()
                latestImageController.reload()
            },
            onSetCapturedUri = { capturedUri -> ui.capture.capturedUri = capturedUri },
            // 정책 유지: 저장 성공 후 다음 순환문구 cursor를 반영한다.
            onAdvancePhraseProgress = { nextCursor -> cameraViewModel.advancePhraseProgress(nextCursor) },
            onSetCapturing = { ui.capture.isCapturing = it }
        )

        handleCaptureClick(
            context = context,
            gate = ui.capture.captureGate,
            imageCapture = boundImageCapture,
            capturedUriPresent = (ui.capture.capturedUri != null),
            continuousPreviewMode = ui.prefs.continuousPreviewMode,
            finalCapturePreview = capturePreview,
            scopedCounterStream = scopedCounterStream,
            tableTemplateState = tableTemplateState,
            counterDigits = ui.prefs.counterDigits,
            fnDelim = fnDelim,
            captureAspect = ui.prefs.captureAspect,
            saveMode = appSettings.saveMode,
            photoQualityMode = appSettings.photoQualityMode,
            wmTableAnchor = ui.prefs.wmTableAnchor,
            wmOffsetXRatio = ui.prefs.wmOffsetXRatio,
            wmOffsetYRatio = ui.prefs.wmOffsetYRatio,
            wmBoundsOffsetX10000 = ui.prefs.wmBoundsOffsetX10000,
            wmBoundsOffsetY10000 = ui.prefs.wmBoundsOffsetY10000,
            wmTableWidthRatio = ui.prefs.wmTableWidthRatio,
            wmTableHeightRatio = ui.prefs.wmTableHeightRatio,
            wmBgAlpha = ui.prefs.wmBgAlpha,
            wmBgStyle = ui.prefs.wmBgStyle,
            wmValueScale = ui.prefs.wmValueScale,
            wmTextColorMode = ui.prefs.wmTextColorMode,
            wmManualTextColor = ui.prefs.wmManualTextColor,
            wmTextAlign = ui.prefs.wmTextAlign,
            wmGridEnabled = ui.prefs.wmGridEnabled,
            wmRotationCwDeg = ui.prefs.wmRotationCwDeg,
            usableTopRatio = ui.capture.usableTopRatio,
            usableBottomRatio = ui.capture.usableBottomRatio,
            repository = repository,
            buildWatermarkConfig = ::buildWatermarkConfig,
            onApplyTemplatePatch = { onTemplateChange(it) },
            // 정책 변경: 촬영 성공 직후에는 optimistic UI를 우선하고 즉시 강한 readback resync는 생략한다.
            // 최종 정합성 보정은 resume/undo/saveMode 변경 경로의 CameraCounterSyncEffect가 담당한다.
            onRequestCounterResync = { },
            callbacks = callbacks
        )
    }

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
            zoomPanelExpanded = false
            triggerCapture()
        },
        onZoomDelta = { deltaTenths ->
            commitZoomTenths(ui.prefs.zoomRatioTenths + deltaTenths)
        }
    )

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
                    zoomPanelExpanded = false
                    onOpenTableEditor()
                },
                onOpenSettings = {
                    zoomPanelExpanded = false
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
                    onOpenTableEditor = onOpenTableEditor,
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
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        CameraBottomControls(
                            context = context,
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
                            onTriggerCapture = { triggerCapture() },
                            onShutterButtonTopYChange = { layout.onShutterButtonTopYChange(it) },
                            zoomPanelExpanded = zoomPanelExpanded,
                            onZoomPanelExpandedChange = { zoomPanelExpanded = it },
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

// 2단계 라운딩 토큰: 카메라 상/하단의 자주 노출되는 소형 컨트롤은 Small로 통일한다.
private val CameraCompactControlShape = RoundedCornerShape(DDZLayout.Radius.Small)

@Composable
private fun CameraTopBar(
    topDisplayName: String,
    onOpenTableEditor: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .padding(
                start = DDZSpacing.screenPadding,
                end = DDZSpacing.screenPadding
            )
            .fillMaxWidth()
    ) {
        val topBarMinHeight = 28.dp + DDZSpacing.itemGap
        // 토큰 정책: 반복되는 32dp 터치 영역은 DDZLayout.Icon.Touch로 고정한다.
        val settingsButtonReservedWidth = DDZLayout.Icon.Touch + (DDZSpacing.cardPadding * 2)
        val filenameMaxWidth = (maxWidth - settingsButtonReservedWidth - DDZSpacing.itemGap)
            .coerceAtLeast(0.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = topBarMinHeight)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .widthIn(max = filenameMaxWidth)
                    .defaultMinSize(minHeight = 30.dp)
                    .background(
                        color = DDZColor.Card.copy(alpha = 0.5f),
                        shape = CameraCompactControlShape
                    )
                    .border(1.dp, DDZColor.SageBorder, CameraCompactControlShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onOpenTableEditor() }
                    .padding(horizontal = DDZSpacing.cardPadding, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                // 시각적 중앙 보정: 파일명 텍스트가 위로 떠 보이지 않도록 lineHeight/padding을 균형화한다.
                CounterAwareFileNameText(
                    fileName = topDisplayName,
                    style = DDZTypography.Caption.copy(lineHeight = 14.sp),
                    color = DDZColor.TextStrong,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(DDZColor.PrimaryDark.copy(alpha = 0f))
                    .defaultMinSize(minWidth = DDZLayout.Icon.Touch, minHeight = DDZLayout.Icon.Touch)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = DDZSpacing.cardPadding, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "촬영 설정",
                    tint = DDZColor.SageDarkStrong
                )
            }
        }
    }
}

internal fun buildWatermarkConfig(
    anchor: WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    boundsOffsetX10000: Int,
    boundsOffsetY10000: Int,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    tableBgAlpha: Int,
    bgStyle: Int,
    valueScale: Int,
    textColorMode: Int,
    manualTextColor: Int,
    textAlign: Int,
    gridEnabled: Boolean,
    rotationCwDeg: Int
): WatermarkConfig {
    return WatermarkConfig(
        anchor = anchor,
        offsetXRatio = offsetXRatio,
        offsetYRatio = offsetYRatio,
        boundsOffsetX10000 = boundsOffsetX10000.coerceIn(0, 10000),
        boundsOffsetY10000 = boundsOffsetY10000.coerceIn(0, 10000),
        tableWidthRatio = tableWidthRatio,
        tableHeightRatio = tableHeightRatio,
        tableBgAlpha = tableBgAlpha,
        bgStyle = bgStyle,
        valueScale = valueScale,
        textColorMode = textColorMode,
        manualTextColor = manualTextColor,
        textAlign = textAlign,
        gridEnabled = gridEnabled,
        rotationCwDeg = if (rotationCwDeg == 90) 90 else 0
    )
}

