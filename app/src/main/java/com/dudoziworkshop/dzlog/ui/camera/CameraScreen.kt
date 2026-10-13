@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.dudoziworkshop.dzlog.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
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
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.mediastore.MediaStoreSaverImpl
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.repository.CaptureAsyncRuntime
import com.dudoziworkshop.dzlog.data.repository.CaptureQueueCoordinator
import com.dudoziworkshop.dzlog.data.repository.CaptureReceiptRecoveryPolicy
import com.dudoziworkshop.dzlog.data.repository.CapturePendingStore
import com.dudoziworkshop.dzlog.data.repository.DzlogRepositoryImpl
import com.dudoziworkshop.dzlog.feature.capture.policy.CapturePendingProjection
import com.dudoziworkshop.dzlog.domain.table.applyPatch
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.model.VolumeKeyAction
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.naming.capturePhysicalSavePaths
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.capture.permission.hasCameraPermission
import com.dudoziworkshop.dzlog.feature.capture.policy.UndoCapturePolicy
import com.dudoziworkshop.dzlog.feature.counter.camera.CameraCounterController
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraBottomControls
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraTableResizeBaseline
import com.dudoziworkshop.dzlog.ui.camera.controls.resizeCameraTableKeepingCenter
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderStyle
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraQuickValueSheet
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraTopBar
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraNextCaptureInfoPopup
import com.dudoziworkshop.dzlog.ui.camera.controls.FloatingAssistShutterButton
import com.dudoziworkshop.dzlog.ui.camera.controls.buildCameraTriggerCapture
import com.dudoziworkshop.dzlog.ui.camera.controls.isCaptureReady
import com.dudoziworkshop.dzlog.ui.camera.effects.CameraCaptureRecoveryEffect
import com.dudoziworkshop.dzlog.ui.camera.effects.CameraNowTickEffect
import com.dudoziworkshop.dzlog.ui.camera.effects.CameraPrefsEffect
import com.dudoziworkshop.dzlog.ui.camera.effects.CameraVolumeKeyEffect
import com.dudoziworkshop.dzlog.ui.camera.effects.rememberLatestImageController
import com.dudoziworkshop.dzlog.ui.camera.effects.rememberUndoDeleteController
import com.dudoziworkshop.dzlog.ui.camera.interop.applyFocusModeToBoundCamera
import com.dudoziworkshop.dzlog.ui.camera.presenter.rememberCameraLayoutState
import com.dudoziworkshop.dzlog.ui.camera.presenter.rememberCameraPreviewAreaArgs
import com.dudoziworkshop.dzlog.ui.camera.preview.CameraPreviewArea
import com.dudoziworkshop.dzlog.ui.camera.preview.buildWatermarkConfig
import com.dudoziworkshop.dzlog.ui.camera.settings.CameraSettingsOverlayPanel
import com.dudoziworkshop.dzlog.ui.camera.settings.CameraSettingsWriter
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFlashMode
import com.dudoziworkshop.dzlog.ui.camera.state.CameraViewModel
import com.dudoziworkshop.dzlog.ui.camera.state.computeCameraDerivedState
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.common.rememberThreeButtonNavEquivalentBottomPadding
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    tableTemplateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onPersistCapturedTemplate: suspend (TableTemplateState) -> Boolean = { false },
    onOpenTableEditor: () -> Unit,
    onOpenSaveSettings: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
    sessionCaptureStack: SnapshotStateList<List<Uri>>,
    onCaptureCommitted: () -> Unit = {},
    quickTemplateName: String = "",
    quickTemplates: List<SavedTableTemplate> = emptyList(),
    quickTemplateId: String? = null,
    onQuickTemplateSelect: suspend (String, TableTemplateState) -> Boolean = { _, _ -> false },
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
                onPersistCapturedTemplate = onPersistCapturedTemplate,
                quickTemplateName = quickTemplateName,
                quickTemplates = quickTemplates,
                quickTemplateId = quickTemplateId,
                onQuickTemplateSelect = onQuickTemplateSelect,
                onOpenTableEditor = onOpenTableEditor,
                onOpenSaveSettings = onOpenSaveSettings,
                onOpenAlbum = onOpenAlbum,
                onOpenRecentCaptureGrid = onOpenRecentCaptureGrid,
                sessionCaptureStack = sessionCaptureStack,
                onCaptureCommitted = onCaptureCommitted,
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

@Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@SuppressLint("AutoboxingStateCreation")
@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraPreview(
    tableTemplateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onPersistCapturedTemplate: suspend (TableTemplateState) -> Boolean = { false },
    onOpenTableEditor: () -> Unit,
    onOpenSaveSettings: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
    sessionCaptureStack: SnapshotStateList<List<Uri>>,
    onCaptureCommitted: () -> Unit = {},
    quickTemplateName: String = "",
    quickTemplates: List<SavedTableTemplate> = emptyList(),
    quickTemplateId: String? = null,
    onQuickTemplateSelect: suspend (String, TableTemplateState) -> Boolean = { _, _ -> false },
) {
    val context = LocalContext.current
    // Recovery and live completion are reconciled by a single durable state observer.
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
    val asyncCaptureService = remember(context.applicationContext) {
        CaptureAsyncRuntime.get(context.applicationContext, repository)
    }
    val pendingProjection = remember(asyncCaptureService) { asyncCaptureService.pendingProjection }
    val activeTemplate = rememberUpdatedState(tableTemplateState)
    val persistCapturedTemplate = rememberUpdatedState(onPersistCapturedTemplate)
    val latestCommittedCallback = rememberUpdatedState(onCaptureCommitted)
    // UI acknowledges durable commit receipts only after seeing the applied template.
    var pendingAcknowledgement by remember {
        mutableStateOf<Pair<List<String>, TableTemplateState>?>(null)
    }

    var boundImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var showQuickValueSheet by remember { mutableStateOf(false) }
    var cornerResizeBaseline by remember { mutableStateOf<CameraTableResizeBaseline?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val ui = cameraViewModel.ui

    // StateFlow delivers the current version after a screen recreation, even if
    // the original SharedFlow completion event was missed while off-screen.
    // The journal, not the event, is authoritative about committed media.
    LaunchedEffect(asyncCaptureService) {
        asyncCaptureService.stateVersion.collect {
            try {
                val recovered = withContext(Dispatchers.IO) {
                    asyncCaptureService.inspectRecovery()
                }
                val activeIds = asyncCaptureService.activeIds()
                // READY/PROCESSING/RESERVED are normal while the application-owned
                // worker or CameraX staging is still active. Orphaned jobs, corrupt
                // journals, and explicit NEEDS_REVIEW always require intervention.
                val unresolved = recovered.ready.any {
                    it.reservation.id !in activeIds
                } || recovered.needsReview.any {
                    it.status == CapturePendingStore.Status.NEEDS_REVIEW ||
                        it.reservation.id !in activeIds
                } || recovered.corruptJournals.isNotEmpty() ||
                    recovered.orphanFiles.isNotEmpty()

                recovered.completedAwaitingApply.forEach { receipt ->
                    val id = receipt.reservation.id
                    val pending = pendingProjection.snapshot()
                    check(pending.none { it.id == id } || pending.firstOrNull()?.id == id) {
                        "Out-of-order capture receipt must be reviewed"
                    }
                    val appliesPending = pending.firstOrNull()?.id == id
                    val base = pendingProjection.committedBase(
                        pendingAcknowledgement?.second ?: activeTemplate.value
                    )
                    val applied = if (appliesPending) {
                        base.applyPatch(receipt.snapshot.tablePatch)
                    } else {
                        CaptureReceiptRecoveryPolicy.resolve(base, listOf(receipt))?.template
                            ?: error("Capture receipt conflicts with active template")
                    }
                    // Never acknowledge a receipt until the catalog is durably saved.
                    check(persistCapturedTemplate.value(applied)) {
                        "Unable to persist captured template progression"
                    }
                    if (appliesPending) pendingProjection.completeFirst(id)
                    pendingProjection.recordCommittedTemplate(applied)
                    pendingAcknowledgement = listOf(id) to applied
                    withContext(Dispatchers.IO) {
                        asyncCaptureService.acknowledgeTemplateApplied(id)
                    }
                    pendingAcknowledgement = null
                    val uris = receipt.outputUris.map(Uri::parse)
                        .filter { it != Uri.EMPTY }.distinct()
                    if (uris.isNotEmpty()) {
                        UndoCapturePolicy.pushCapture(sessionCaptureStack, uris)
                    }
                    cameraViewModel.onCaptureCommitted()
                    latestCommittedCallback.value()
                }

                ui.capture.pendingSaveCount = pendingProjection.pendingCount()
                ui.capture.pendingProjectionVersion++
                // A failed stage/reservation must not be dismissed merely because
                // the save worker has no active jobs.
                ui.capture.pendingSaveError = unresolved
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ui.capture.pendingSaveError = true
                android.util.Log.e("DZlogCaptureRecovery", "Capture reconciliation failed", error)
                Toast.makeText(
                    context, "저장된 사진의 번호·문구 상태를 확인해주세요.", Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    BackHandler(enabled = (ui.capture.pendingSaveCount > 0 && !ui.capture.pendingSaveError) ||
        ui.capture.capturedUri != null || ui.showWizard || ui.isTableResizePanelOpen || ui.showToolMenu ||
        ui.isToolPanelExpanded || ui.isZoomChipExpanded || ui.selectedTool != null || ui.isTableSelected) {
        when {
            ui.capture.pendingSaveCount > 0 && !ui.capture.pendingSaveError ->
                Toast.makeText(context, "사진 저장 완료 후 이동할 수 있습니다.", Toast.LENGTH_SHORT).show()
            ui.capture.capturedUri != null -> ui.capture.capturedUri = null
            ui.showWizard -> ui.showWizard = false
            ui.isTableResizePanelOpen -> ui.isTableResizePanelOpen = false
            ui.showToolMenu || ui.isToolPanelExpanded || ui.isZoomChipExpanded || ui.selectedTool != null ->
                ui.dismissToolOverlays()
            ui.isTableSelected -> ui.isTableSelected = false
        }
    }
    val threeButtonEquivalentBottomPadding = rememberThreeButtonNavEquivalentBottomPadding()
    val layout = rememberCameraLayoutState()

    val tableResolver = remember { TableResolver() }
    val fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
    val dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT
    val timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT
    // A persisted catalog update may lag behind the committed receipt.
    // Recompute from the committed base even before Compose delivers that update.
    val effectiveTemplate = remember(
        tableTemplateState,
        ui.capture.pendingProjectionVersion,
        pendingAcknowledgement,
    ) {
        pendingProjection.previewTemplate(tableTemplateState)
    }
    val derivedState = remember(
        effectiveTemplate,
        ui.capture.now,
        ui.prefs.counterDigits,
        appSettings.phraseProgressCursor,
        appSettings.includePathInCounterScope,
        appSettings.includeFilenameInCounterScope,
        appSettings.saveMode,
        ui.counter.scopeNextCounter,
        ui.counter.syncedRequest,
    ) {
        computeCameraDerivedState(
            tableTemplateState = effectiveTemplate,
            now = ui.capture.now,
            counterDigits = ui.prefs.counterDigits,
            phraseProgressCounter = appSettings.phraseProgressCursor,
            includePathInCounterScope = appSettings.includePathInCounterScope,
            includeFilenameInCounterScope = appSettings.includeFilenameInCounterScope,
            saveMode = appSettings.saveMode,
            syncedNextCounter = ui.counter.scopeNextCounter,
            syncedRequest = ui.counter.syncedRequest,
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
    LaunchedEffect(asyncCaptureService, latestImageController) {
        asyncCaptureService.events.collect { event ->
            if (event is CaptureQueueCoordinator.Event.Completed) {
                latestImageController.reload()
            }
        }
    }
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
        pendingProjection = pendingProjection,
    )

    // Show only a complete, counter-synchronized filename in the top bar.
    // While a rotating phrase changes streams, keep the previous valid label;
    // the shutter still remains gated until finalCapturePreview is ready.
    var lastReadyDisplayName by remember(quickTemplateId) { mutableStateOf<String?>(null) }
    LaunchedEffect(finalCapturePreview?.displayName) {
        finalCapturePreview?.displayName?.let { lastReadyDisplayName = it }
    }
    val topDisplayName = finalCapturePreview?.displayName
        ?: lastReadyDisplayName
        ?: derivedState.topDisplayName
    var showNextCaptureInfo by remember { mutableStateOf(false) }

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
        undoPending = undoDeleteController.isBusy,
        finalCapturePreview = finalCapturePreview,
        scopedCounterStream = scopedCounterStream,
        tableTemplateState = effectiveTemplate,
        fnDelim = fnDelim,
        repository = repository,
        asyncService = asyncCaptureService,
        pendingProjection = pendingProjection,
        cameraViewModel = cameraViewModel,
        captureFeedback = captureFeedback,
        sessionCaptureStack = sessionCaptureStack,
        latestImageController = latestImageController,
        onCaptureCommitted = onCaptureCommitted,
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
        volumeKeyAction = if (showQuickValueSheet) VolumeKeyAction.NONE else appSettings.volumeKeyAction,
        onCapture = {
            ui.dismissCameraInteractions()
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


    LaunchedEffect(boundCamera, ui.focusMode, ui.focusUiValue) {
        val activeCamera = boundCamera ?: return@LaunchedEffect
        applyFocusModeToBoundCamera(
            camera = activeCamera,
            mode = ui.focusMode,
            focusUiValue = ui.focusUiValue,
        )
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
                hasAutoCounter = displayCounter != null,
                onOpenCaptureInfo = {
                    ui.isTableSelected = false
                    ui.dismissToolOverlays()
                    showNextCaptureInfo = true
                },
                onOpenSettings = {
                    if (ui.capture.pendingSaveCount == 0) {
                        ui.isTableSelected = false
                        ui.dismissToolOverlays()
                        ui.showWizard = true
                    } else {
                        Toast.makeText(context, "사진 저장 완료 후 설정을 변경할 수 있습니다.", Toast.LENGTH_SHORT).show()
                    }
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
                    tableTemplateState = effectiveTemplate,
                    tableResolver = tableResolver,
                    scopeNextCounter = displayCounter,
                    phraseProgressCursor = appSettings.phraseProgressCursor,
                    dateFormat = dateFormat,
                    timeFormat = timeFormat,
                    fnDelim = fnDelim,
                    shutterButtonTopY = layout.shutterButtonTopY,
                    safeTopY = layout.safeTopY,
                    safeBottomY = layout.safeBottomY,
                    usableVerticalMarginPx = layout.usableVerticalMarginPx,
                    onOpenTableEditor = {
                        if (ui.capture.pendingSaveCount == 0) {
                            ui.dismissToolOverlays()
                            onOpenTableEditor()
                        } else {
                            Toast.makeText(context, "저장 완료 후 표를 편집할 수 있습니다.", Toast.LENGTH_SHORT).show()
                        }
                    },
                )

                CameraPreviewArea(
                    args = previewAreaArgs,
                    isTableSelected = ui.isTableSelected,
                    isTableLocked = ui.prefs.wmTableLocked,
                    onTableSelectionChange = { selected ->
                        if (selected) ui.dismissToolOverlays()
                        ui.isTableSelected = selected
                    },
                    onCornerResizeScale = { scale, finished ->
                        if (!finished && !ui.prefs.wmTableLocked) {
                            val baseline = cornerResizeBaseline ?: CameraTableResizeBaseline(
                                widthRatio = ui.prefs.wmTableWidthRatio,
                                heightRatio = ui.prefs.wmTableHeightRatio,
                                rotationCwDeg = ui.prefs.wmRotationCwDeg,
                                anchor = ui.prefs.wmTableAnchor,
                                x10000 = ui.prefs.wmBoundsOffsetX10000,
                                y10000 = ui.prefs.wmBoundsOffsetY10000,
                                photoAspect = ui.prefs.captureAspect.ratioF,
                            ).also { cornerResizeBaseline = it }
                            val result = resizeCameraTableKeepingCenter(baseline, scale)
                            ui.prefs.wmTableWidthRatio = result.widthRatio
                            ui.prefs.wmTableHeightRatio = result.heightRatio
                            ui.prefs.wmTableAnchor = WatermarkTableAnchor.CUSTOM
                            ui.prefs.wmBoundsOffsetX10000 = result.x10000
                            ui.prefs.wmBoundsOffsetY10000 = result.y10000
                            ui.prefs.wmOffsetXRatio = (result.x10000 / 100f).toInt()
                            ui.prefs.wmOffsetYRatio = (result.y10000 / 100f).toInt()
                        } else if (cornerResizeBaseline != null) {
                            cornerResizeBaseline = null
                            scope.launch {
                                settingsWriter.setWmTableSizeAndPosition(
                                    ui.prefs.wmTableWidthRatio,
                                    ui.prefs.wmTableHeightRatio,
                                    ui.prefs.wmBoundsOffsetX10000,
                                    ui.prefs.wmBoundsOffsetY10000,
                                )
                            }
                        }
                    },
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
                    val canTriggerCapture = isCaptureReady(
                        scopeNextCounter = displayCounter,
                        capturedUri = ui.capture.capturedUri,
                        isCapturing = ui.capture.isCapturing,
                        boundImageCaptureAvailable = (boundImageCapture != null),
                    )
                    CameraBottomControls(
                        scope = scope,
                        ui = ui,
                        settingsWriter = settingsWriter,
                        captureReady = canTriggerCapture && !undoDeleteController.isBusy &&
                            !ui.capture.pendingSaveError && ui.capture.pendingSaveCount < 3,
                        latestImage = latestImage,
                        onOpenAlbum = {
                            if (ui.capture.pendingSaveCount == 0 || ui.capture.pendingSaveError) onOpenAlbum()
                            else Toast.makeText(context, "저장 완료 후 앨범으로 이동할 수 있습니다.", Toast.LENGTH_SHORT).show()
                        },
                        onOpenRecentCaptureGrid = { g1, g2, path, start ->
                            if (ui.capture.pendingSaveCount == 0 || ui.capture.pendingSaveError) {
                                onOpenRecentCaptureGrid(g1, g2, path, start)
                            } else {
                                Toast.makeText(context, "저장 완료 후 이동할 수 있습니다.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        sessionCaptureStack = sessionCaptureStack,
                        undoPending = undoDeleteController.isBusy || ui.capture.pendingSaveCount > 0,
                        onUndoDelete = {
                            if (ui.capture.pendingSaveCount == 0) {
                                undoDeleteController.deleteLatest(sessionCaptureStack)
                            }
                        },
                        onTriggerCapture = {
                            ui.dismissCameraInteractions()
                            triggerCapture()
                        },
                        onShutterButtonTopYChange = { layout.onShutterButtonTopYChange(it) },
                        onOpenQuickValues = {
                            if (ui.capture.pendingSaveCount == 0) {
                                ui.dismissCameraInteractions()
                                showQuickValueSheet = true
                            } else {
                                Toast.makeText(context, "저장 완료 후 값을 변경할 수 있습니다.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenTableEditor = {
                            if (ui.capture.pendingSaveCount == 0) {
                                ui.isTableSelected = false
                                ui.dismissToolOverlays()
                                onOpenTableEditor()
                            } else {
                                Toast.makeText(context, "저장 완료 후 표를 편집할 수 있습니다.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        hapticEnabled = appSettings.hapticEnabled,
                    )
                }

                FloatingAssistShutterButton(
                    enabled = ui.prefs.assistShutterEnabled,
                    xRatio = ui.prefs.assistShutterXRatio,
                    yRatio = ui.prefs.assistShutterYRatio,
                    onPositionRatioChange = { xRatio, yRatio ->
                        ui.prefs.assistShutterXRatio = xRatio
                        ui.prefs.assistShutterYRatio = yRatio
                        scope.launch {
                            settingsWriter.setAssistShutterPositionRatio(
                                xRatio = xRatio,
                                yRatio = yRatio
                            )
                        }
                    },
                    onTapCapture = {
                        ui.dismissCameraInteractions()
                        triggerCapture()
                    },
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(80f)
                )
            }
        }
        if (showQuickValueSheet) {
            key(quickTemplateId) {
                CameraQuickValueSheet(
                    template = tableTemplateState,
                    templateName = quickTemplateName,
                    counterDigits = ui.prefs.counterDigits,
                    counterSeed = ui.counter.scopeNextCounter,
                    counterRequest = ui.counter.syncedRequest,
                    includePathInCounterScope = appSettings.includePathInCounterScope,
                    includeFilenameInCounterScope = appSettings.includeFilenameInCounterScope,
                    phraseCursor = appSettings.phraseProgressCursor,
                    now = ui.capture.now,
                    captureAspect = ui.prefs.captureAspect,
                    style = TableRenderStyle(bgStyle = ui.prefs.wmBgStyle, bgAlpha = ui.prefs.wmBgAlpha,
                        valueScale = ui.prefs.wmValueScale, textColorMode = ui.prefs.wmTextColorMode,
                        manualTextColor = ui.prefs.wmManualTextColor, textAlign = ui.prefs.wmTextAlign),
                    tableWidthRatio = ui.prefs.wmTableWidthRatio,
                    tableHeightRatio = ui.prefs.wmTableHeightRatio,
                    saveMode = appSettings.saveMode,
                    templates = quickTemplates,
                    activeTemplateId = quickTemplateId,
                    onTemplateSelect = onQuickTemplateSelect,
                    onDismiss = { showQuickValueSheet = false },
                    onCommit = onTemplateChange,
                )
            }
        }

        if (ui.capture.capturedUri != null && ui.prefs.continuousPreviewMode != ContinuousPreviewMode.OFF) {
            // CaptureResultOverlay covers only the photo area. Keep the original
            // full-screen tap-to-dismiss shield so top/bottom controls cannot run
            // while the captured photo is being reviewed.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { ui.capture.capturedUri = null }
            )
        }

        if (showNextCaptureInfo) {
            CameraNextCaptureInfoPopup(
                nextFileName = topDisplayName,
                relativePaths = capturePhysicalSavePaths(
                    finalCapturePreview?.relativePathPreview ?: captureScopeState.relativePathPreview,
                    appSettings.saveMode,
                ),
                onOpenSaveSettings = {
                    if (ui.capture.pendingSaveCount == 0) {
                        showNextCaptureInfo = false
                        onOpenSaveSettings()
                    } else {
                        Toast.makeText(context, "저장 완료 후 설정을 변경할 수 있습니다.", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showNextCaptureInfo = false },
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
                continuousPreviewMode = ui.prefs.continuousPreviewMode,
                onContinuousPreviewModeChange = { mode ->
                    ui.prefs.continuousPreviewMode = mode
                    scope.launch { settingsWriter.setContinuousPreviewMode(mode) }
                },
                captureSoundEnabled = appSettings.captureSoundEnabled,
                onCaptureSoundChange = { checked ->
                    scope.launch { AppSettingsStore.setCaptureSoundEnabled(context, checked) }
                },
                captureHapticEnabled = appSettings.captureHapticEnabled,
                onCaptureHapticChange = { checked ->
                    scope.launch { AppSettingsStore.setCaptureHapticEnabled(context, checked) }
                },
                volumeKeyAction = if (showQuickValueSheet) VolumeKeyAction.NONE else appSettings.volumeKeyAction,
                onVolumeKeyActionChange = { action ->
                    scope.launch { settingsWriter.setVolumeKeyAction(action) }
                },
                onDismiss = { ui.showWizard = false }
            )
        }
    }
}

