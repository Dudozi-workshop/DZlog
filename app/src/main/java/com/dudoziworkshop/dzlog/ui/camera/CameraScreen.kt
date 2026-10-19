@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.dudoziworkshop.dzlog.ui.camera

import com.dudoziworkshop.dzlog.feature.help.ui.guideAspect
import com.dudoziworkshop.dzlog.feature.help.ui.guideSaveMode
import com.dudoziworkshop.dzlog.feature.help.ui.guidePreviewMode
import com.dudoziworkshop.dzlog.feature.help.ui.guideCycleIndex
import com.dudoziworkshop.dzlog.feature.help.ui.guidePendingCount
import com.dudoziworkshop.dzlog.feature.help.ui.TutorialCapturePreview
import com.dudoziworkshop.dzlog.ui.camera.controls.SaveStatusDetails
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialDemoProgress
import com.dudoziworkshop.dzlog.feature.help.ui.guideQuickTemplate
import com.dudoziworkshop.dzlog.feature.help.ui.guidePreviewArgs
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialLocation
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialTarget
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialReadOnly
import com.dudoziworkshop.dzlog.feature.help.ui.tutorialTarget
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
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
import androidx.lifecycle.compose.LifecycleEventEffect
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
import com.dudoziworkshop.dzlog.data.repository.CaptureRecoveryStatusPolicy
import com.dudoziworkshop.dzlog.data.repository.DzlogRepositoryImpl
import com.dudoziworkshop.dzlog.feature.capture.policy.CapturePendingProjection
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureReviewPolicy
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureTemplateCommitPolicy
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
import com.dudoziworkshop.dzlog.feature.capture.policy.CapturePhraseUndoHistory
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureUndoPathWarningPolicy
import androidx.compose.foundation.layout.fillMaxWidth
import com.dudoziworkshop.dzlog.feature.capture.policy.UndoCapturePolicy
import com.dudoziworkshop.dzlog.feature.counter.camera.CameraCounterController
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraBottomControls
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraTableResizeBaseline
import com.dudoziworkshop.dzlog.ui.camera.controls.resizeCameraTableKeepingCenter
import com.dudoziworkshop.dzlog.ui.camera.controls.fitCameraTableForPhotoChange
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderStyle
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraQuickValueSheet
import com.dudoziworkshop.dzlog.feature.table.policy.QuickEditSelectionMemory
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraTopBar
import com.dudoziworkshop.dzlog.ui.camera.controls.CameraNextCaptureInfoPopup
import com.dudoziworkshop.dzlog.ui.camera.controls.FloatingAssistShutterButton
import com.dudoziworkshop.dzlog.ui.camera.controls.buildCameraTriggerCapture
import com.dudoziworkshop.dzlog.ui.camera.controls.isCaptureReady
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
    phraseUndoHistory: CapturePhraseUndoHistory,
    verifiedTier: com.dudoziworkshop.dzlog.feature.table.policy.SaveRuleTier =
        com.dudoziworkshop.dzlog.feature.table.policy.SaveRuleTier.FREE,
    onCaptureCommitted: () -> Unit = {},
    onFirstCaptureReceipt: (String, String) -> Unit = { _, _ -> },
    onFirstQuickEditOpened: () -> Unit = {},
    tutorialFocus: com.dudoziworkshop.dzlog.ui.onboarding.FirstCaptureGuidePhase? = null,
    quickTemplateName: String = "",
    quickTemplates: List<SavedTableTemplate> = emptyList(),
    quickTemplateId: String? = null,
    quickEditSelectionMemory: QuickEditSelectionMemory,
    onQuickTemplateSelect: suspend (String, TableTemplateState) -> Boolean = { _, _ -> false },
) {
    val readOnlyTour = LocalTutorialReadOnly.current
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(hasCameraPermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // The user may change camera access in Android Settings while this screen
    // remains composed. Refresh the cached permission on every foreground return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = hasCameraPermission(context)
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !readOnlyTour) launcher.launch(Manifest.permission.CAMERA)
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
                quickEditSelectionMemory = quickEditSelectionMemory,
                onQuickTemplateSelect = onQuickTemplateSelect,
                onOpenTableEditor = onOpenTableEditor,
                onOpenSaveSettings = onOpenSaveSettings,
                onOpenAlbum = onOpenAlbum,
                onOpenRecentCaptureGrid = onOpenRecentCaptureGrid,
                sessionCaptureStack = sessionCaptureStack,
                phraseUndoHistory = phraseUndoHistory,
                verifiedTier = verifiedTier,
                onCaptureCommitted = onCaptureCommitted,
                onFirstCaptureReceipt = onFirstCaptureReceipt,
                onFirstQuickEditOpened = onFirstQuickEditOpened,
                tutorialFocus = tutorialFocus,
            )
        } else {
            Text(
                text = "카메라 권한이 필요합니다.\n설정에서 권한을 허용해주세요.",
                modifier = Modifier.align(Alignment.Center),
                color = DDZColor.TextPrimary
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
    phraseUndoHistory: CapturePhraseUndoHistory,
    verifiedTier: com.dudoziworkshop.dzlog.feature.table.policy.SaveRuleTier =
        com.dudoziworkshop.dzlog.feature.table.policy.SaveRuleTier.FREE,
    onCaptureCommitted: () -> Unit = {},
    onFirstCaptureReceipt: (String, String) -> Unit = { _, _ -> },
    onFirstQuickEditOpened: () -> Unit = {},
    tutorialFocus: com.dudoziworkshop.dzlog.ui.onboarding.FirstCaptureGuidePhase? = null,
    quickTemplateName: String = "",
    quickTemplates: List<SavedTableTemplate> = emptyList(),
    quickTemplateId: String? = null,
    quickEditSelectionMemory: QuickEditSelectionMemory,
    onQuickTemplateSelect: suspend (String, TableTemplateState) -> Boolean = { _, _ -> false },
) {
    val readOnlyTour = LocalTutorialReadOnly.current
    val context = LocalContext.current
    // Recovery and live completion are reconciled by a single durable state observer.
    val lifecycleOwner = LocalContext.current as? LifecycleOwner ?: return
    val scope = rememberCoroutineScope()
    val cameraViewModel: CameraViewModel = viewModel()
    val settingsWriter = remember(context, readOnlyTour) { CameraSettingsWriter(context, readOnly = readOnlyTour) }
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
    val pendingProjection = remember(asyncCaptureService, readOnlyTour) {
        com.dudoziworkshop.dzlog.feature.help.model.tutorialCaptureProjection(
            asyncCaptureService.pendingProjection, readOnlyTour,
        )
    }
    val templateSessionSafe = remember(asyncCaptureService, quickTemplateId) {
        pendingProjection.bindTemplate(quickTemplateId)
    }
    val activeTemplate = rememberUpdatedState(tableTemplateState)
    val persistCapturedTemplate = rememberUpdatedState(onPersistCapturedTemplate)
    val latestCommittedCallback = rememberUpdatedState(onCaptureCommitted)
    val latestFirstReceiptCallback = rememberUpdatedState(onFirstCaptureReceipt)
    // UI acknowledges durable commit receipts only after seeing the applied template.
    var pendingAcknowledgement by remember {
        mutableStateOf<Pair<List<String>, TableTemplateState>?>(null)
    }

    var boundImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var showQuickValueSheet by remember { mutableStateOf(false) }
    var cornerResizeBaseline by remember { mutableStateOf<CameraTableResizeBaseline?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val ui = cameraViewModel.ui
    val tourLocation = LocalTutorialLocation.current
    val tourProgress = LocalTutorialDemoProgress.current
    LaunchedEffect(tourLocation) {
        if (readOnlyTour) {
            ui.showWizard = tourLocation?.sessionId == "settings" && tourLocation.pageId in listOf("ratio", "quality", "capture-confirmation", "feedback", "controls")
            ui.isTableSelected = tourLocation?.sessionId == "table" && tourLocation.pageId in listOf("select-table", "select", "move", "resize-button", "size", "corner-size", "rotation", "lock", "details")
        }
    }
    LaunchedEffect(templateSessionSafe) {
        if (!readOnlyTour && !templateSessionSafe) ui.capture.pendingSaveError = true
    }

    // StateFlow delivers the current version after a screen recreation, even if
    // the original SharedFlow completion event was missed while off-screen.
    // The journal, not the event, is authoritative about committed media.
    LaunchedEffect(asyncCaptureService, quickTemplateId) {
        if (readOnlyTour) return@LaunchedEffect
        // Wait until navigation has restored the active catalog. Writing a
        // receipt against an uninitialized template would corrupt progress.
        if (quickTemplateId == null) return@LaunchedEffect
        asyncCaptureService.stateVersion.collect {
            try {
                val recovered = withContext(Dispatchers.IO) {
                    // Only verified READY work may resume automatically. PROCESSING,
                    // SAVED_UNCOMMITTED and damaged journals are never retried.
                    asyncCaptureService.resumeVerifiedReadyCaptures(quickTemplateId)
                    asyncCaptureService.inspectRecovery()
                }
                val activeIds = asyncCaptureService.activeIds()
                val unclaimedReady = CaptureRecoveryStatusPolicy.resumableReadyIds(
                    recovered, activeIds
                )
                // Unclaimed READY records may wait for the bounded worker to
                // release capacity; block new shots while recovery is draining.
                val unavailableForThisTemplate = recovered.ready.any {
                    it.reservation.id !in activeIds &&
                        it.snapshot.templateId != quickTemplateId
                }
                val unableToResume = unavailableForThisTemplate ||
                    (unclaimedReady.isNotEmpty() &&
                        asyncCaptureService.outstandingCount() == 0)
                val unresolved = CaptureRecoveryStatusPolicy.needsManualReview(
                    recovered, activeIds + unclaimedReady
                ) || unableToResume

                recovered.completedAwaitingApply.forEach { receipt ->
                    // Each receipt belongs to the template selected when its
                    // shutter was pressed. Identical tables can have different
                    // catalog IDs; never silently update a different entry.
                    check(CaptureTemplateCommitPolicy.matchesReceipt(
                        receipt.snapshot.templateId, quickTemplateId
                    )) {
                        "Receipt belongs to a different quick template"
                    }
                    val id = receipt.reservation.id
                    val pending = pendingProjection.snapshot()
                    check(pending.none { it.id == id } || pending.firstOrNull()?.id == id) {
                        "Out-of-order capture receipt must be reviewed"
                    }
                    val appliesPending = pending.firstOrNull()?.id == id
                    if (appliesPending) {
                        check(pending.first().templatePatch == receipt.snapshot.tablePatch) {
                            "Capture projection and durable receipt disagree"
                        }
                    }
                    val base = pendingProjection.committedBase(
                        pendingAcknowledgement?.second ?: activeTemplate.value
                    )
                    // Validate the saved snapshot against the active template,
                    // even for an in-memory pending shot. Never apply a receipt
                    // to a different template merely because its ID is pending.
                    val applied = CaptureReceiptRecoveryPolicy.resolve(base, listOf(receipt))
                        ?.template ?: error("Capture receipt conflicts with active template")
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
                        phraseUndoHistory.record(
                            templateId = quickTemplateId,
                            uris = uris.map(Uri::toString),
                            before = base,
                            after = applied,
                            captureRelativePath = receipt.snapshot.request.relativePath,
                        )
                        UndoCapturePolicy.pushCapture(sessionCaptureStack, uris)
                    }
                    // Preserve legacy SHORT/HOLD behavior: review the final
                    // saved output, not the unprocessed camera JPEG.
                    if (CaptureReviewPolicy.shouldOpenSavedPreview(
                            ui.capture.pendingPreviewCaptureId, id
                        )
                    ) {
                        check(uris.isNotEmpty()) { "Committed preview has no saved media" }
                        ui.capture.capturedUri = uris.first()
                        ui.capture.pendingPreviewCaptureId = null
                    }
                    cameraViewModel.onCaptureCommitted()
                    latestCommittedCallback.value()
                    // An onboarding receipt is shown only after the saved media
                    // and its matching template progress have both been committed.
                    latestFirstReceiptCallback.value(
                        receipt.snapshot.request.displayName,
                        receipt.snapshot.request.relativePath,
                    )
                }

                val ownedProjectionIds = pendingProjection.snapshot()
                    .map { it.id }.toSet()
                ui.capture.isRecoveringPendingCaptures =
                    (activeIds - ownedProjectionIds).isNotEmpty() ||
                    unclaimedReady.isNotEmpty()
                ui.capture.pendingSaveCount = maxOf(
                    pendingProjection.pendingCount(),
                    asyncCaptureService.outstandingCount() + unclaimedReady.size,
                )
                ui.capture.pendingProjectionVersion++
                // A failed stage/reservation must not be dismissed merely because
                // the save worker has no active jobs.
                ui.capture.pendingSaveError = unresolved || !templateSessionSafe
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
        sessionCaptureStack = sessionCaptureStack,
    )
    LaunchedEffect(asyncCaptureService, latestImageController) {
        asyncCaptureService.events.collect { event ->
            if (event is CaptureQueueCoordinator.Event.Completed) {
                latestImageController.reload()
            }
        }
    }
    val latestImage = latestImageController.latestImage

    var undoPhraseSaving by remember { mutableStateOf(false) }
    var undoPathWarning by remember { mutableStateOf<PendingCameraUndoPathWarning?>(null) }
    var pendingUndoWarningDisable by remember { mutableStateOf<List<String>?>(null) }
    var undoDeletionIncomplete by remember { mutableStateOf(phraseUndoHistory.hasIncompleteDeletion()) }
    fun syncAfterUndoDelete(deletedUris: List<Uri>) {
        val entry = phraseUndoHistory.deleted(deletedUris.map(Uri::toString))
        val optOut = pendingUndoWarningDisable
        if (entry != null && optOut != null && entry.initialUris == optOut.toSet()) {
            pendingUndoWarningDisable = null
            scope.launch {
                try {
                    AppSettingsStore.setUndoSavePathWarningEnabled(context, false)
                    Toast.makeText(context, "삭제 경고를 껐습니다. 전체 설정에서 언제든 다시 켤 수 있어요.", Toast.LENGTH_LONG).show()
                } catch (error: Exception) {
                    Toast.makeText(context, "삭제 경고 설정을 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        undoDeletionIncomplete = phraseUndoHistory.hasIncompleteDeletion()
        latestImageController.reload()
        cameraViewModel.onUndoCommitted()
        if (entry == null) return
        if (entry.templateId != quickTemplateId) {
            ui.capture.pendingSaveError = true
            return
        }
        undoPhraseSaving = true
        scope.launch {
            try {
                val beforeRestore = pendingProjection.committedBase(activeTemplate.value)
                val restored = phraseUndoHistory.restore(beforeRestore, entry)
                if (restored != null && persistCapturedTemplate.value(restored)) {
                    pendingProjection.recordCommittedTemplate(restored)
                    phraseUndoHistory.acknowledgeRestored(entry)
                    ui.capture.pendingProjectionVersion++
                } else {
                    ui.capture.pendingSaveError = true
                    Toast.makeText(
                        context, "문구 복원 상태를 확인하지 못했습니다. 추가 촬영 전에 확인해주세요.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ui.capture.pendingSaveError = true
                Toast.makeText(context, "문구 복원 저장에 실패했습니다. 추가 촬영 전에 확인해주세요.", Toast.LENGTH_LONG).show()
            } finally {
                undoPhraseSaving = false
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            cameraViewModel.bumpResumeResyncTick()
            awaitCancellation()
        }
    }

    val undoDeleteController = rememberUndoDeleteController(
        context = context,
        onCommitted = { deletedUris -> syncAfterUndoDelete(deletedUris) },
        onRestore = { targetUris ->
            if (pendingUndoWarningDisable?.any { it in targetUris.map(Uri::toString) } == true) {
                pendingUndoWarningDisable = null
            }
            UndoCapturePolicy.restoreCapture(sessionCaptureStack, targetUris)
        },
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

    // Keep the previous synchronized label only while the underlying template,
    // phrase position and naming settings are unchanged. After an edit, never
    // display a filename derived from an older template during resynchronization.
    var lastReadyDisplayName by remember(
        quickTemplateId,
        effectiveTemplate,
        appSettings.phraseProgressCursor,
        appSettings.includePathInCounterScope,
        appSettings.includeFilenameInCounterScope,
        appSettings.saveMode,
        ui.prefs.counterDigits,
    ) { mutableStateOf<String?>(null) }
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
        verifiedTier = verifiedTier,
        boundImageCapture = boundImageCapture,
        undoPending = undoDeleteController.isBusy || undoPhraseSaving || undoDeletionIncomplete || undoPathWarning != null,
        finalCapturePreview = finalCapturePreview,
        scopedCounterStream = scopedCounterStream,
        tableTemplateState = effectiveTemplate,
        templateId = quickTemplateId,
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
            if (!readOnlyTour) resetZoomToDefault()
        }
    }

    CameraVolumeKeyEffect(
        volumeKeyAction = if (readOnlyTour || showQuickValueSheet) VolumeKeyAction.NONE else appSettings.volumeKeyAction,
        onCapture = {
            ui.dismissCameraInteractions()
            triggerCapture()
        },
        onZoomDelta = { deltaTenths ->
            commitZoomTenths(ui.prefs.zoomRatioTenths + deltaTenths)
        }
    )

    LaunchedEffect(boundImageCapture, ui.prefs.flashMode) {
        if (readOnlyTour) return@LaunchedEffect
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
                tutorialFocus = tutorialFocus,
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
                    .fillMaxWidth().tutorialTarget("camera.preview")
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
                    args = if (readOnlyTour) guidePreviewArgs(previewAreaArgs, tourLocation?.pageId, tourProgress) else previewAreaArgs,
                    isTableSelected = ui.isTableSelected,
                    isTableLocked = if (readOnlyTour && tourLocation?.pageId in listOf("select-table", "select", "move", "resize-button", "size", "corner-size", "rotation", "lock", "details")) tourLocation?.pageId == "lock" && tourProgress in 0.33f..0.75f else ui.prefs.wmTableLocked,
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

                if (readOnlyTour && tourLocation?.pageId == "preview") {
                    TutorialCapturePreview(tourProgress, ui.prefs.captureAspect.ratioF)
                }
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
                        captureReady = !readOnlyTour && canTriggerCapture && !undoDeleteController.isBusy && !undoPhraseSaving && !undoDeletionIncomplete && undoPathWarning == null &&
                            ui.capture.pendingPreviewCaptureId == null &&
                            !ui.capture.pendingSaveError &&
                            !ui.capture.isRecoveringPendingCaptures &&
                            ui.capture.pendingSaveCount < 3,
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
                        undoPending = undoDeleteController.isBusy || undoPhraseSaving || ui.capture.pendingSaveCount > 0 || undoPathWarning != null,
                        onUndoDelete = {
                            if (!readOnlyTour && ui.capture.pendingSaveCount == 0 && !undoPhraseSaving) {
                                val lastUris = sessionCaptureStack.lastOrNull().orEmpty()
                                if (phraseUndoHistory.canUndo(lastUris.map(Uri::toString), quickTemplateId)) {
                                    val savedPath = phraseUndoHistory.pathForUndo(lastUris.map(Uri::toString))
                                    val currentPath = captureScopeState.relativePathPreview
                                    if (appSettings.undoSavePathWarningEnabled &&
                                        CaptureUndoPathWarningPolicy.requiresConfirmation(savedPath, currentPath)) {
                                        undoPathWarning = PendingCameraUndoPathWarning(
                                            captureUris = lastUris.map(Uri::toString),
                                            capturedFolder = savedPath!!,
                                            currentFolder = currentPath,
                                        )
                                    } else {
                                        undoDeleteController.deleteLatest(sessionCaptureStack)
                                    }
                                } else {
                                    Toast.makeText(context, "다른 양식의 촬영 기록입니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onTriggerCapture = {
                            if (!readOnlyTour) { ui.dismissCameraInteractions(); triggerCapture() }
                        },
                        onShutterButtonTopYChange = { layout.onShutterButtonTopYChange(it) },
                        tutorialFocus = tutorialFocus,
                        onOpenQuickValues = {
                            if (ui.capture.pendingSaveCount == 0) {
                                ui.dismissCameraInteractions()
                                showQuickValueSheet = true
                                onFirstQuickEditOpened()
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
                    enabled = if (readOnlyTour && tourLocation?.pageId == "assist") tourProgress >= 0.15f else ui.prefs.assistShutterEnabled,
                    xRatio = if (readOnlyTour && tourLocation?.pageId == "assist") 0.25f + 0.5f * tourProgress else ui.prefs.assistShutterXRatio,
                    yRatio = if (readOnlyTour && tourLocation?.pageId == "assist") 0.65f - 0.15f * tourProgress else ui.prefs.assistShutterYRatio,
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
        if (readOnlyTour && tourLocation?.pageId == "saving") Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 180.dp),
        ) {
            SaveStatusDetails(guidePendingCount(tourProgress), false, {}, Modifier.tutorialTarget("camera.save-detail"))
        }
        val guideQuick = readOnlyTour && ((tourLocation?.pageId?.startsWith("quick") == true && tourLocation?.pageId != "quick-button") || tourLocation?.pageId in listOf("template-name", "template"))
        if (showQuickValueSheet || guideQuick) {
            key(if (guideQuick) tourLocation?.pageId else null) {
            key(quickTemplateId) {
                CameraQuickValueSheet(
                    template = if (guideQuick) remember { guideQuickTemplate() } else tableTemplateState,
                    templateName = if (guideQuick) "기능 안내 예시" else quickTemplateName,
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
                    selectionMemory = quickEditSelectionMemory,
                    onTemplateSelect = onQuickTemplateSelect,
                    onDismiss = { showQuickValueSheet = false },
                    onCommit = if (guideQuick) ({ _: TableTemplateState -> }) else onTemplateChange,
                )
            }
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

        undoPathWarning?.let { pending ->
            CameraUndoPathWarningDialog(
                capturedFolder = pending.capturedFolder,
                currentFolder = pending.currentFolder,
                onCancel = { undoPathWarning = null },
                onConfirm = { disableWarning ->
                    undoPathWarning = null
                    val lastUris = sessionCaptureStack.lastOrNull().orEmpty()
                    val currentUris = lastUris.map(Uri::toString)
                    if (currentUris == pending.captureUris &&
                        ui.capture.pendingSaveCount == 0 && !undoPhraseSaving &&
                        phraseUndoHistory.canUndo(currentUris, quickTemplateId)
                    ) {
                        pendingUndoWarningDisable = if (disableWarning) currentUris else null
                        undoDeleteController.deleteLatest(sessionCaptureStack)
                    }
                },
            )
        }

        if (readOnlyTour && tourLocation?.pageId == "warning") {
            CameraUndoPathWarningDialog(
                capturedFolder = "Pictures/DZlog/예시/이전 폴더/",
                currentFolder = "Pictures/DZlog/예시/현재 폴더/",
                onCancel = {}, onConfirm = {},
            )
        }
        if (showNextCaptureInfo || (readOnlyTour && tourLocation?.pageId in listOf("filename", "path", "counter"))) {
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
                captureAspect = if (readOnlyTour && tourLocation?.pageId == "ratio") guideAspect(tourProgress) else ui.prefs.captureAspect,
                onCaptureAspectChange = { aspect ->
                    val current = ui.prefs.captureAspect
                    if (aspect != current) {
                        val fitted = fitCameraTableForPhotoChange(
                            CameraTableResizeBaseline(
                                widthRatio = ui.prefs.wmTableWidthRatio,
                                heightRatio = ui.prefs.wmTableHeightRatio,
                                rotationCwDeg = ui.prefs.wmRotationCwDeg,
                                anchor = ui.prefs.wmTableAnchor,
                                x10000 = ui.prefs.wmBoundsOffsetX10000,
                                y10000 = ui.prefs.wmBoundsOffsetY10000,
                                photoAspect = current.ratioF,
                            ),
                            newPhotoAspect = aspect.ratioF,
                        )
                        ui.prefs.captureAspect = aspect
                        ui.prefs.wmTableWidthRatio = fitted.widthRatio
                        ui.prefs.wmTableHeightRatio = fitted.heightRatio
                        ui.prefs.wmTableAnchor = WatermarkTableAnchor.CUSTOM
                        ui.prefs.wmBoundsOffsetX10000 = fitted.x10000
                        ui.prefs.wmBoundsOffsetY10000 = fitted.y10000
                        ui.prefs.wmOffsetXRatio = fitted.x10000 / 100
                        ui.prefs.wmOffsetYRatio = fitted.y10000 / 100
                        scope.launch {
                            settingsWriter.setWmTableSizeAndPosition(
                                fitted.widthRatio, fitted.heightRatio,
                                fitted.x10000, fitted.y10000,
                            )
                            settingsWriter.setCaptureAspect(aspect)
                        }
                    }
                },
                saveMode = if (readOnlyTour && tourLocation?.pageId == "quality") guideSaveMode(tourProgress) else ui.prefs.saveMode,
                onSaveModeChange = { mode ->
                    ui.prefs.saveMode = mode
                    scope.launch { settingsWriter.setSaveMode(mode) }
                },
                continuousPreviewMode = if (readOnlyTour && tourLocation?.pageId == "capture-confirmation") guidePreviewMode(tourProgress) else ui.prefs.continuousPreviewMode,
                onContinuousPreviewModeChange = { mode ->
                    ui.prefs.continuousPreviewMode = mode
                    scope.launch { settingsWriter.setContinuousPreviewMode(mode) }
                },
                captureSoundEnabled = if (readOnlyTour && tourLocation?.pageId == "feedback") tourProgress >= 0.34f else appSettings.captureSoundEnabled,
                onCaptureSoundChange = { checked ->
                    if (!readOnlyTour) scope.launch { AppSettingsStore.setCaptureSoundEnabled(context, checked) }
                },
                captureHapticEnabled = if (readOnlyTour && tourLocation?.pageId == "feedback") tourProgress >= 0.67f else appSettings.captureHapticEnabled,
                onCaptureHapticChange = { checked ->
                    if (!readOnlyTour) scope.launch { AppSettingsStore.setCaptureHapticEnabled(context, checked) }
                },
                volumeKeyAction = if (readOnlyTour && tourLocation?.pageId == "controls") listOf(VolumeKeyAction.NONE, VolumeKeyAction.ZOOM, VolumeKeyAction.CAPTURE)[guideCycleIndex(tourProgress, 3)] else if (showQuickValueSheet) VolumeKeyAction.NONE else appSettings.volumeKeyAction,
                onVolumeKeyActionChange = { action ->
                    scope.launch { settingsWriter.setVolumeKeyAction(action) }
                },
                onDismiss = { ui.showWizard = false }
            )
        }
    }
}

