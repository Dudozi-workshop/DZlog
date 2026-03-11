@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.dudoziworkshop.dzlog.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dudoziworkshop.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.dudoziworkshop.dzlog.data.counter.clampCounterDigits
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.dudoziworkshop.dzlog.data.preferences.KEY_CONTINUOUS_PREVIEW_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAMERA_GRID_ON
import com.dudoziworkshop.dzlog.data.preferences.KEY_CAMERA_ZOOM_TENTHS
import com.dudoziworkshop.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.dudoziworkshop.dzlog.data.preferences.KEY_SAVE_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_PHOTO_QUALITY_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_SHOW_WM_PREVIEW
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_GRID_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_OFFSET_X
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_X_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_OFFSET_Y
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BOUNDS_OFFSET_Y_10000
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_ROTATION_CW_90
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MANUAL
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_ALIGN
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.captureplan.CapturePlan
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.counter.CounterStore
import com.dudoziworkshop.dzlog.domain.counter.policy.buildCounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.counter.policy.isNewCounterScope
import com.dudoziworkshop.dzlog.domain.counter.toScopedCounter
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.VolumeKeyAction
import com.dudoziworkshop.dzlog.domain.model.WatermarkConfig
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.deriveFileNameCellSlotsFromDrafts
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode
import com.dudoziworkshop.dzlog.domain.model.WatermarkManualTextColor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.computeNextDelayMillis
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.PreviewState
import com.dudoziworkshop.dzlog.domain.preview.buildPreviewState
import com.dudoziworkshop.dzlog.domain.preview.decideTickUnitFromTemplate
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.capture.permission.hasCameraPermission
import com.dudoziworkshop.dzlog.feature.capture.policy.UndoCapturePolicy
import com.dudoziworkshop.dzlog.feature.capture.policy.resolveSyncedScopeNext
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.mediastore.MediaStoreSaverImpl
import com.dudoziworkshop.dzlog.data.repository.DzlogRepositoryImpl
import com.dudoziworkshop.dzlog.ui.camera.controls.CaptureButtonSection
import com.dudoziworkshop.dzlog.ui.camera.controls.CaptureClickCallbacks
import com.dudoziworkshop.dzlog.ui.camera.controls.ZoomControlSection
import com.dudoziworkshop.dzlog.ui.camera.controls.handleCaptureClick
import com.dudoziworkshop.dzlog.ui.camera.preview.CameraPreviewArea
import com.dudoziworkshop.dzlog.ui.camera.preview.CameraPreviewAreaArgs
import com.dudoziworkshop.dzlog.ui.camera.preview.WatermarkUiArgs
import com.dudoziworkshop.dzlog.ui.camera.settings.CameraSettingsOverlayPanel
import com.dudoziworkshop.dzlog.ui.common.CounterAwareFileNameText
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.log.DzThumbnail
import com.dudoziworkshop.dzlog.ui.log.parseG1G2FromRelativePath
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import com.dudoziworkshop.dzlog.data.preferences.persistCaptureAspect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.util.Date

private val USABLE_VERTICAL_MARGIN = 10.dp

// NOTE: buildWatermarkConfig는 다른 파일(핸들러)에서도 사용되므로 file-private 금지
@Composable
fun CameraScreen(
    tableTemplateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
    sessionCaptureStack: SnapshotStateList<List<Uri>>
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
                sessionCaptureStack = sessionCaptureStack
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
    sessionCaptureStack: SnapshotStateList<List<Uri>>
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as? LifecycleOwner ?: return
    val scope = rememberCoroutineScope()
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
    val ui = remember { CameraUiState() }
    // 정책 변경: ROTATING_TEXT 문구 진행 커서(파일 카운터와 독립) 상태.
    var phraseProgressCounter by remember { mutableIntStateOf(1) }
    var shutterButtonTopY by remember { mutableStateOf<Float?>(null) }
    var cameraRootHeightPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val safeTopInsetPx = with(density) { safeDrawingPadding.calculateTopPadding().toPx() }
    val safeBottomInsetPx = with(density) { safeDrawingPadding.calculateBottomPadding().toPx() }
    val safeTopY = safeTopInsetPx.takeIf { it > 0f }
    val safeBottomY = (cameraRootHeightPx - safeBottomInsetPx).takeIf { cameraRootHeightPx > 0f }
    val usableVerticalMarginPx = with(density) { USABLE_VERTICAL_MARGIN.toPx() }

    val tableResolver = remember { TableResolver() }
    val fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
    val dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT
    val timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT

    // 프리뷰 계산 경로를 공용 pipeline으로 통일한다(정상 동작 유지 목적, 저장 흐름 불변).
    val previewPipeline = remember(
        tableTemplateState,
        ui.capture.now,
        ui.prefs.counterDigits,
        ui.counter.scopeNextCounter,
        phraseProgressCounter,
        appSettings.includePathInCounterScope,
        appSettings.includeFilenameInCounterScope,
    ) {
        buildPreviewState(
            input = PreviewInput(
                templateState = tableTemplateState,
                captureNow = ui.capture.now,
                counterDigits = ui.prefs.counterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
                fnDelim = fnDelim,
                includePathInCounterScope = appSettings.includePathInCounterScope,
                includeFilenameInCounterScope = appSettings.includeFilenameInCounterScope,
                scopeNextCounter = ui.counter.scopeNextCounter,
                phraseProgressCursor = phraseProgressCounter,
            ),
            tableResolver = tableResolver,
        )
    }

    LaunchedEffect(tableTemplateState.cells, lifecycleOwner) {
        val unit = decideTickUnitFromTemplate(tableTemplateState.cells)
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val delayMs = computeNextDelayMillis(unit)
                delay(delayMs)
                ui.capture.now = Date()
            }
        }
    }

    // 핵심 정책(1차 리팩터링): 프리뷰/저장은 같은 activePlan을 공유한다.
    val activePlan = remember(
        previewPipeline,
        phraseProgressCounter,
    ) {
        buildActiveCapturePlan(
            previewState = previewPipeline,
            phraseProgressCounter = phraseProgressCounter,
        )
    }
    val counterScope = activePlan.streamContext
    val mediaStoreRefreshTick = rememberMediaStoreRefreshTick(context)
    var resumeResyncTick by remember { mutableIntStateOf(0) }
    var undoResyncTick by remember { mutableIntStateOf(0) }
    var latestImage by remember { mutableStateOf<MediaImageItem?>(null) }

    suspend fun reloadLatestImage() {
        latestImage = withContext(Dispatchers.IO) {
            val reader = DzlogMediaStoreReader(context.contentResolver)
            val baseRelativePath = counterScope.relativePathKey
                .substringBefore("|g2=", counterScope.relativePathKey)
                .let { if (it.endsWith('/')) it else "$it/" }
            val targetRelativePath = if (appSettings.saveMode == SaveMode.ORIGINAL_ONLY) {
                "${baseRelativePath}original/"
            } else {
                baseRelativePath
            }
            runCatching { reader.loadLatestImageInRelativePath(targetRelativePath) }.getOrNull()
        }
    }

    suspend fun syncAfterUndoDelete() {
        reloadLatestImage()
        undoResyncTick += 1
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            resumeResyncTick += 1
            awaitCancellation()
        }
    }


    LaunchedEffect(Unit) { reloadLatestImage() }
    LaunchedEffect(mediaStoreRefreshTick) { reloadLatestImage() }
    LaunchedEffect(appSettings.saveMode, counterScope.relativePathKey) { reloadLatestImage() }

    var pendingUndoDeleteUris by remember { mutableStateOf<List<Uri>?>(null) }

    val undoDeleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pendingUris = pendingUndoDeleteUris ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch { syncAfterUndoDelete() }
        } else {
            UndoCapturePolicy.restoreCapture(sessionCaptureStack, pendingUris)
        }
        pendingUndoDeleteUris = null
    }

    fun launchScopedDeleteRequest(targetUris: List<Uri>): Boolean {
        if (targetUris.isEmpty()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, targetUris)
            pendingUndoDeleteUris = targetUris
            undoDeleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            return true
        }
        return false
    }

    fun performUndoDelete(targetUris: List<Uri>) {
        if (targetUris.isEmpty()) return

        val deletedAll = runCatching {
            targetUris.all { uri ->
                context.contentResolver.delete(uri, null, null) > 0
            }
        }.getOrElse { throwable ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && throwable is RecoverableSecurityException) {
                pendingUndoDeleteUris = targetUris
                undoDeleteLauncher.launch(
                    IntentSenderRequest.Builder(throwable.userAction.actionIntent.intentSender).build()
                )
                return
            }
            false
        }

        if (!deletedAll) {
            UndoCapturePolicy.restoreCapture(sessionCaptureStack, targetUris)
            return
        }

        scope.launch { syncAfterUndoDelete() }
    }

    val hasTemplateCells = tableTemplateState.cells.isNotEmpty()
    val hasAnyFilenameSlot = deriveFileNameCellSlotsFromDrafts(tableTemplateState.fileNameSlotDrafts).any { it != null }
    val allFilenameSlotsOff = deriveFileNameCellSlotsFromDrafts(tableTemplateState.fileNameSlotDrafts).all { it == null }
    val isTemplateReady = hasTemplateCells && (
        !appSettings.includeFilenameInCounterScope ||
            allFilenameSlotsOff ||
            hasAnyFilenameSlot
    )

    // ✅ 카운터 단일소스: 표기(ON/OFF)와 무관하게 스트림 nextSeed로 ui.counter를 항상 동기화
    SyncCounterSeedEffect(
        context = context,
        counterScope = counterScope,
        counterDigits = ui.prefs.counterDigits,
        resumeTick = resumeResyncTick,
        undoTick = undoResyncTick,
        isTemplateReady = isTemplateReady,
        appSettings = appSettings,
        ui = ui
    )

    val topDisplayName = activePlan.displayName

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

        // 정책 변경: 촬영 피드백은 저장 완료가 아니라 촬영 트리거(버튼/음량키) 시점에 즉시 제공한다.
        captureFeedback.play(
            successVibrationEnabled = appSettings.captureHapticEnabled,
            soundEnabled = appSettings.captureSoundEnabled
        )

        // 구조 정리: capture 후처리 콜백을 하나의 묶음으로 전달해 호출부 가독성을 유지한다.
        val callbacks = CaptureClickCallbacks(
            // 정책 유지: 저장 성공 직후 프리뷰 숫자를 즉시 다음 값으로 반영한다.
            onAdvancePreviewCounter = { nextCounter -> ui.counter.scopeNextCounter = nextCounter.coerceAtLeast(1) },
            onAddToSessionStack = { uris ->
                UndoCapturePolicy.pushCapture(sessionCaptureStack, uris)
                scope.launch { reloadLatestImage() }
            },
            onSetCapturedUri = { capturedUri -> ui.capture.capturedUri = capturedUri },
            // 정책 유지: 통합/문구별과 무관하게 저장 성공 후 다음 cursor를 반영한다.
            onAdvancePhraseProgress = { nextCursor -> phraseProgressCounter = nextCursor.coerceAtLeast(1) },
            onSetCapturing = { ui.capture.isCapturing = it }
        )

        handleCaptureClick(
            context = context,
            gate = ui.capture.captureGate,
            imageCapture = boundImageCapture,
            capturedUriPresent = (ui.capture.capturedUri != null),
            continuousPreviewMode = ui.prefs.continuousPreviewMode,
            activePlan = activePlan,
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
            // 최종 정합성 보정은 resume/undo/saveMode 변경 경로의 SyncCounterSeedEffect가 담당한다.
            onRequestCounterResync = { },
            callbacks = callbacks
        )
    }
    val latestVolumeKeyAction by rememberUpdatedState(appSettings.volumeKeyAction)
    val latestTriggerCapture by rememberUpdatedState(triggerCapture)

    LaunchedEffect(ui.capture.capturedUri, ui.prefs.continuousPreviewMode) {
        if (ui.capture.capturedUri != null && ui.prefs.continuousPreviewMode == ContinuousPreviewMode.SHORT) {
            delay(1500)
            ui.capture.capturedUri = null
        }
    }

    LaunchedEffect(Unit) {
        loadCameraPrefsIntoUi(context.dataStore.data.first(), ui)
    }

    DisposableEffect(Unit) {
        onDispose {
            resetZoomToDefault()
        }
    }

    DisposableEffect(appSettings.volumeKeyAction) {
        VolumeKeyInputBus.setVolumeKeyAction(appSettings.volumeKeyAction)
        onDispose { VolumeKeyInputBus.setVolumeKeyAction(VolumeKeyAction.NONE) }
    }

    LaunchedEffect(Unit) {
        VolumeKeyInputBus.events.collect { press ->
            when (latestVolumeKeyAction) {
                VolumeKeyAction.CAPTURE -> {
                    zoomPanelExpanded = false
                    latestTriggerCapture()
                }

                VolumeKeyAction.ZOOM -> {
                    val delta = if (press == VolumeKeyPress.UP) 1 else -1
                    commitZoomTenths(ui.prefs.zoomRatioTenths + delta)
                }

                VolumeKeyAction.NONE -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DDZColor.PrimaryDark.copy(alpha = 0f))
                .onGloballyPositioned { coordinates ->
                    cameraRootHeightPx = coordinates.size.height.toFloat()
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
                    val previewAreaArgs = remember(
                        context,
                        lifecycleOwner,
                        scope,
                        ui.prefs.captureAspect,
                        appSettings.saveMode,
                        ui.prefs.continuousPreviewMode,
                        ui.prefs.counterDigits,
                        dateFormat,
                        timeFormat,
                        fnDelim,
                        ui.counter.scopeNextCounter,
                        tableTemplateState,
                        tableResolver,
                        ui.capture.now,
                        ui.prefs.showWmPreview,
                        ui.prefs.showGrid,
                        ui.prefs.zoomRatioTenths,
                        ui.prefs.wmTableAnchor,
                        ui.prefs.wmTableWidthRatio,
                        ui.prefs.wmTableHeightRatio,
                        ui.prefs.wmOffsetXRatio,
                        ui.prefs.wmOffsetYRatio,
                        ui.prefs.wmBgAlpha,
                        ui.prefs.wmBgStyle,
                        ui.prefs.wmValueScale,
                        ui.prefs.wmTextColorMode,
                        ui.prefs.wmManualTextColor,
                        ui.prefs.wmTextAlign,
                        ui.prefs.wmGridEnabled,
                        ui.prefs.wmRotationCwDeg,
                        shutterButtonTopY,
                        safeTopY,
                        safeBottomY,
                        usableVerticalMarginPx
                    ) {
                        CameraPreviewAreaArgs(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            scope = scope,
                            captureAspect = ui.prefs.captureAspect,
                            saveMode = appSettings.saveMode,
                            continuousPreviewMode = ui.prefs.continuousPreviewMode,
                            photoQualityMode = appSettings.photoQualityMode,
                            counterDigits = ui.prefs.counterDigits,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat,
                            fnDelim = fnDelim,
                            scopeNextCounter = ui.counter.scopeNextCounter,
                            phraseProgressCursor = phraseProgressCounter,
                            tableTemplateState = tableTemplateState,
                            tableResolver = tableResolver,
                            now = ui.capture.now,
                            showWmPreview = ui.prefs.showWmPreview,
                            showGrid = ui.prefs.showGrid,
                            zoomRatioTenths = ui.prefs.zoomRatioTenths,
                            maxZoomTenths = ui.capture.maxZoomTenths,
                            onActualZoomTenthsChange = { ui.capture.actualZoomTenths = it },
                            onRequestedZoomTenthsCommit = { next ->
                                val normalized = next.coerceIn(10, ui.capture.maxZoomTenths.coerceAtLeast(10))
                                ui.prefs.zoomRatioTenths = normalized
                                scope.launch { context.dataStore.edit { it[KEY_CAMERA_ZOOM_TENTHS] = normalized } }
                            },
                            onMaxZoomTenthsChange = { ui.capture.maxZoomTenths = it.coerceAtLeast(10) },
                            shutterButtonTopY = shutterButtonTopY,
                            safeTopY = safeTopY,
                            safeBottomY = safeBottomY,
                            usableVerticalMarginPx = usableVerticalMarginPx,
                            onUsableVerticalRatioChange = { topRatio, bottomRatio ->
                                ui.capture.usableTopRatio = topRatio
                                ui.capture.usableBottomRatio = bottomRatio
                            },
                            onWatermarkOffsetRatioPreview = { x, y ->
                                ui.prefs.wmTableAnchor = WatermarkTableAnchor.CUSTOM
                                ui.prefs.wmOffsetXRatio = x
                                ui.prefs.wmOffsetYRatio = y
                            },
                            onWatermarkOffsetRatioCommit = { x, y ->
                                val nx = x.coerceIn(0, 100)
                                val ny = y.coerceIn(0, 100)
                                ui.prefs.wmTableAnchor = WatermarkTableAnchor.CUSTOM
                                ui.prefs.wmOffsetXRatio = nx
                                ui.prefs.wmOffsetYRatio = ny
                                scope.launch {
                                    context.dataStore.edit {
                                        it[KEY_WM_TABLE_ANCHOR] = 4
                                        it[KEY_WM_OFFSET_X] = nx
                                        it[KEY_WM_OFFSET_Y] = ny
                                    }
                                }
                            },
                            onWatermarkBoundsOffset10000Preview = { x10000, y10000 ->
                                val nx10000 = x10000.coerceIn(0, 10000)
                                val ny10000 = y10000.coerceIn(0, 10000)
                                ui.prefs.wmTableAnchor = WatermarkTableAnchor.CUSTOM
                                ui.prefs.wmBoundsOffsetX10000 = nx10000
                                ui.prefs.wmBoundsOffsetY10000 = ny10000
                                ui.prefs.wmOffsetXRatio = (nx10000 / 100f).roundToInt().coerceIn(0, 100)
                                ui.prefs.wmOffsetYRatio = (ny10000 / 100f).roundToInt().coerceIn(0, 100)
                            },
                            onWatermarkBoundsOffset10000Commit = { x10000, y10000 ->
                                val nx10000 = x10000.coerceIn(0, 10000)
                                val ny10000 = y10000.coerceIn(0, 10000)
                                val nx = (nx10000 / 100f).roundToInt().coerceIn(0, 100)
                                val ny = (ny10000 / 100f).roundToInt().coerceIn(0, 100)
                                ui.prefs.wmTableAnchor = WatermarkTableAnchor.CUSTOM
                                ui.prefs.wmBoundsOffsetX10000 = nx10000
                                ui.prefs.wmBoundsOffsetY10000 = ny10000
                                ui.prefs.wmOffsetXRatio = nx
                                ui.prefs.wmOffsetYRatio = ny
                                scope.launch {
                                    context.dataStore.edit {
                                        it[KEY_WM_TABLE_ANCHOR] = 4
                                        it[KEY_WM_BOUNDS_OFFSET_X_10000] = nx10000
                                        it[KEY_WM_BOUNDS_OFFSET_Y_10000] = ny10000
                                        it[KEY_WM_OFFSET_X] = nx
                                        it[KEY_WM_OFFSET_Y] = ny
                                    }
                                }
                            },
                            onOpenTableEditor = onOpenTableEditor,
                            watermarkUi = WatermarkUiArgs(
                                anchor = ui.prefs.wmTableAnchor,
                                tableWidthRatio = ui.prefs.wmTableWidthRatio,
                                tableHeightRatio = ui.prefs.wmTableHeightRatio,
                                offsetXRatio = ui.prefs.wmOffsetXRatio,
                                offsetYRatio = ui.prefs.wmOffsetYRatio,
                                boundsOffsetX10000 = ui.prefs.wmBoundsOffsetX10000,
                                boundsOffsetY10000 = ui.prefs.wmBoundsOffsetY10000,
                                bgAlpha = ui.prefs.wmBgAlpha,
                                bgStyle = ui.prefs.wmBgStyle,
                                valueScale = ui.prefs.wmValueScale,
                                textColorMode = ui.prefs.wmTextColorMode,
                                manualTextColor = ui.prefs.wmManualTextColor,
                                textAlign = ui.prefs.wmTextAlign,
                                wmGridEnabled = ui.prefs.wmGridEnabled,
                                rotationCwDeg = ui.prefs.wmRotationCwDeg
                            )
                        )
                    }

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


                    if (zoomPanelExpanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { zoomPanelExpanded = false }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
                            .padding(bottom = DDZSpacing.screenPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        val enabledNow =
                            (boundImageCapture != null && ui.capture.capturedUri == null && !ui.capture.isCapturing)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ZoomControlSection(
                                zoomRatioTenths = ui.capture.actualZoomTenths,
                                maxZoomTenths = ui.capture.maxZoomTenths,
                                expanded = zoomPanelExpanded,
                                onToggleExpanded = { zoomPanelExpanded = !zoomPanelExpanded },
                                onZoomTenthsChange = ::commitZoomTenths
                            )

                            Box(modifier = Modifier.height(2.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        shutterButtonTopY = coordinates.positionInRoot().y
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 하단 조작부 정책: 15/23/24/23/15 비율로 중심축(anchor-3)과 2·4 midpoint 균형을 비율 기반으로 유지한다.
                                    // slot1: 최근(anchor-1)
                                    Box(
                                        modifier = Modifier.weight(15f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        RecentCaptureThumbButton(
                                            latestImage = latestImage,
                                            onClick = {
                                                zoomPanelExpanded = false
                                                val it = latestImage
                                                if (it == null) {
                                                    onOpenAlbum()
                                                } else {
                                                    val (g1, g2) = parseG1G2FromRelativePath(it.relativePath)
                                                    onOpenRecentCaptureGrid(g1, g2, it.relativePath, 0)
                                                }
                                            }
                                        )
                                    }

                                    // slot2: midpoint(1-3), 추후 확장용 빈 슬롯
                                    Box(
                                        modifier = Modifier.weight(23f),
                                        contentAlignment = Alignment.Center
                                    ) {}

                                    // slot3: 촬영(anchor-center)
                                    Box(
                                        modifier = Modifier.weight(24f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CaptureButtonSection(
                                            ready = enabledNow,
                                            onClick = {
                                                // UX 정책: 패널이 열려 있어도 촬영 버튼은 즉시 촬영하고, 패널만 최소화한다.
                                                zoomPanelExpanded = false
                                                triggerCapture()
                                            }
                                        )
                                    }

                                    // slot4: midpoint(3-5)
                                    Box(
                                        modifier = Modifier.weight(23f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        WatermarkRotateButton(
                                            onClick = {
                                                val nextRotation = if (ui.prefs.wmRotationCwDeg == 90) 0 else 90
                                                ui.prefs.wmRotationCwDeg = nextRotation
                                                // 회전은 0°/90°만 토글한다. 위치/크기는 사용자가 직접 이동/조절한다.
                                                scope.launch {
                                                    context.dataStore.edit {
                                                        it[KEY_WM_ROTATION_CW_90] = nextRotation
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    // slot5: undo(anchor-5)
                                    Box(
                                        modifier = Modifier.weight(15f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        UndoCaptureButton(
                                            enabled = sessionCaptureStack.isNotEmpty() && pendingUndoDeleteUris == null,
                                            onClick = {
                                                if (pendingUndoDeleteUris != null) return@UndoCaptureButton
                                                val targetUris = UndoCapturePolicy.consumeLatestCapture(
                                                    stack = sessionCaptureStack
                                                )
                                                if (targetUris.isEmpty()) return@UndoCaptureButton

                                                if (launchScopedDeleteRequest(targetUris)) {
                                                    return@UndoCaptureButton
                                                }
                                                performUndoDelete(targetUris)
                                            }
                                        )
                                    }
                                }
                            }
                        }
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
                    scope.launch { persistCaptureAspect(context, aspect) }
                },
                saveMode = ui.prefs.saveMode,
                onSaveModeChange = { mode ->
                    ui.prefs.saveMode = mode
                    scope.launch { context.dataStore.edit { it[KEY_SAVE_MODE] = mode.v } }
                },
                showGrid = ui.prefs.showGrid,
                onShowGridChange = { checked ->
                    ui.prefs.showGrid = checked
                    scope.launch { context.dataStore.edit { it[KEY_CAMERA_GRID_ON] = checked } }
                },
                showTable = ui.prefs.showWmPreview,
                onShowTableChange = { checked ->
                    ui.prefs.showWmPreview = checked
                    scope.launch { context.dataStore.edit { it[KEY_SHOW_WM_PREVIEW] = if (checked) 1 else 0 } }
                },
                continuousPreviewMode = ui.prefs.continuousPreviewMode,
                onContinuousPreviewModeChange = { mode ->
                    ui.prefs.continuousPreviewMode = mode
                    scope.launch { context.dataStore.edit { it[KEY_CONTINUOUS_PREVIEW_MODE] = mode.v } }
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

@Composable
private fun CameraControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = DDZColor.SageBorder,
    content: @Composable BoxScope.() -> Unit
) {
    // 4단계 정책: 하단 보조 버튼 3종의 공통 외곽(size/clip/border/background/clickable)만 통합한다.
    Box(
        modifier = modifier
            .size(DDZLayout.Control.CameraSmall)
            .clip(CameraCompactControlShape)
            .border(1.dp, borderColor, CameraCompactControlShape)
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun WatermarkRotateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CameraControlButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.RotateRight,
            contentDescription = "워터마크 90도 회전",
            tint = DDZColor.SageDarkStrong
        )
    }
}

@Composable
private fun UndoCaptureButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CameraControlButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = if (enabled) DDZColor.SagePrimary else Color.Transparent
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            tint = if (enabled) Color.White else DDZColor.SageDark
        )
    }
}

@Composable
private fun RecentCaptureThumbButton(
    latestImage: MediaImageItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CameraControlButton(
        onClick = onClick,
        modifier = modifier
    ) {
        latestImage?.let {
            DzThumbnail(it.uri.toString())
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

@Composable
private fun rememberMediaStoreRefreshTick(context: Context): Int {
    var refreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refreshTick += 1
            }
        }

        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        onDispose {
            resolver.unregisterContentObserver(observer)
        }
    }

    return refreshTick
}

private fun buildActiveCapturePlan(
    previewState: PreviewState,
    phraseProgressCounter: Int,
): CapturePlan {
    return CapturePlan(
        resolvedCells = previewState.plan.resolvedCells,
        tablePatch = previewState.plan.patch,
        displayName = previewState.previewNaming.displayName,
        usedCounter = previewState.previewNaming.usedCounter,
        // 정책 정리(2차): 저장 성공 시 phrase cursor는 plan이 제공한 다음 값으로만 이동한다.
        nextPhraseProgressCursor = phraseProgressCounter.coerceAtLeast(1) + 1,
        streamContext = previewState.previewNaming.counterScope,
        relativePathPreview = previewState.previewNaming.relativePath,
    )
}

@Composable
private fun SyncCounterSeedEffect(
    context: Context,
    counterScope: CounterScope,
    counterDigits: Int,
    resumeTick: Int,
    undoTick: Int,
    isTemplateReady: Boolean,
    appSettings: AppSettings,
    ui: CameraUiState
) {
    var lastResumeTick by remember { mutableIntStateOf(-1) }
    var lastUndoTick by remember { mutableIntStateOf(-1) }
    var lastSaveMode by remember { mutableStateOf<SaveMode?>(null) }
    val scopedCounter = remember(
        counterScope.relativePathKey,
        counterScope.streamPrefix,
        appSettings.includePathInCounterScope,
        appSettings.includeFilenameInCounterScope,
        isTemplateReady,
    ) {
        toScopedCounter(
            counterScope = counterScope,
            includePathInScope = appSettings.includePathInCounterScope,
            includeFilenameInScope = appSettings.includeFilenameInCounterScope,
        )
    }
    val fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
    var scopeKeySnapshot by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resumeTick) {
        scopeKeySnapshot = scopedCounter.scopeParts.scopeKey
    }
    val activeScopeKey = scopeKeySnapshot ?: scopedCounter.scopeParts.scopeKey

    LaunchedEffect(
        activeScopeKey,
        counterDigits,
        resumeTick,
        undoTick,
        isTemplateReady,
        appSettings.saveMode,
        appSettings.includePathInCounterScope,
        appSettings.includeFilenameInCounterScope,
    ) {
        if (!isTemplateReady) {
            // 템플릿 미준비(초기/임시 상태)에서는 seed 계산/스냅샷 갱신을 수행하지 않는다.
            // 초기 prefix(DZlog)로 잘못 계산된 next=1이 UI seed를 덮어쓰지 않도록 방지한다.
            return@LaunchedEffect
        }

        val scopeSnapshot = buildCounterScopeSnapshot(
            streamContext = counterScope,
            includePathInScope = appSettings.includePathInCounterScope,
            includeFilenameInScope = appSettings.includeFilenameInCounterScope,
        )

        if (ui.capture.isCapturing) {
            ui.counter.lastScopeSnapshot = scopeSnapshot
            return@LaunchedEffect
        }

        val saveModeChanged = (lastSaveMode != null && lastSaveMode != appSettings.saveMode)
        val isUndoResync = (undoTick != lastUndoTick)
        // 정책 변경: 촬영 직후 readback은 줄이고, resume/undo/saveMode 변경 시 외부 resync로 취급한다.
        val isExternalResync =
            (resumeTick != lastResumeTick) ||
                isUndoResync ||
                saveModeChanged

        suspend fun readNextSeed(): Int = CounterStore.next(
            context = context,
            scopedStream = scopedCounter,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = appSettings.saveMode,
        ).coerceAtLeast(1)

        val nextSeedFromStream = if (isExternalResync) {
            val a = readNextSeed()
            delay(200)
            val b = readNextSeed()
            if (a == b) a else b
        } else {
            readNextSeed()
        }

        val isNewStream = isNewCounterScope(
            previous = ui.counter.lastScopeSnapshot,
            current = scopeSnapshot
        )
        val currentScopeSeed = ui.counter.scopeNextCounter
        val allowResetToOneOnNewStream =
            counterScope.streamPrefix.contains("d_") ||
                counterScope.streamPrefix.contains("t_") ||
                counterScope.streamPrefix.contains("rp_")
        val syncAllowsDownward = isUndoResync
        ui.counter.scopeNextCounter = when {
            // 규칙 A: 새 스트림 판정 시, 임시 상태에서 next=1로 내려오는 경우의 덮어쓰기를 방지한다.
            isNewStream && !allowResetToOneOnNewStream && nextSeedFromStream == 1 && currentScopeSeed > 1 -> currentScopeSeed

            // 규칙 B: 같은 scope에서는 화면 이동/빠른 재진입(resume)으로 낮은 값이 내려오더라도 rollback을 막는다.
            // 단, undo 기반 외부 재동기화는 실제 삭제 반영을 위해 하향 동기화를 허용한다.
            else -> resolveSyncedScopeNext(
                streamNextFromPolicy = nextSeedFromStream,
                currentScopeNext = currentScopeSeed,
                isNewScope = isNewStream,
                allowDownwardSync = syncAllowsDownward,
            )
        }
        lastResumeTick = resumeTick
        lastUndoTick = undoTick
        lastSaveMode = appSettings.saveMode
        ui.counter.lastScopeSnapshot = scopeSnapshot
    }
}
private fun loadCameraPrefsIntoUi(prefs: Preferences, ui: CameraUiState) {
    try {
        ui.prefs.wmTableAnchor = when (prefs[KEY_WM_TABLE_ANCHOR] ?: 3) {
            0 -> WatermarkTableAnchor.TOP_LEFT
            1 -> WatermarkTableAnchor.TOP_RIGHT
            2 -> WatermarkTableAnchor.BOTTOM_LEFT
            3 -> WatermarkTableAnchor.BOTTOM_RIGHT
            else -> WatermarkTableAnchor.CUSTOM
        }

        ui.prefs.wmTableWidthRatio = (prefs[KEY_WM_TABLE_WIDTH] ?: 40).coerceIn(10, 100)
        ui.prefs.wmTableHeightRatio = (prefs[KEY_WM_TABLE_HEIGHT] ?: 20).coerceIn(10, 100)
        ui.prefs.wmBoundsOffsetX10000 = (prefs[KEY_WM_BOUNDS_OFFSET_X_10000] ?: 0).coerceIn(0, 10000)
        ui.prefs.wmBoundsOffsetY10000 = (prefs[KEY_WM_BOUNDS_OFFSET_Y_10000] ?: 0).coerceIn(0, 10000)
        ui.prefs.wmOffsetXRatio = (ui.prefs.wmBoundsOffsetX10000 / 100f).roundToInt().coerceIn(0, 100)
        ui.prefs.wmOffsetYRatio = (ui.prefs.wmBoundsOffsetY10000 / 100f).roundToInt().coerceIn(0, 100)

        ui.prefs.wmBgAlpha = (prefs[KEY_WM_BG_ALPHA] ?: 80).coerceIn(0, 255)
        ui.prefs.wmBgStyle = (prefs[KEY_WM_TABLE_BG_STYLE] ?: 0).coerceIn(0, 2)
        ui.prefs.wmValueScale = (prefs[KEY_WM_VALUE_SCALE] ?: 100).coerceIn(60, 160)
        ui.prefs.wmTextColorMode = (prefs[KEY_WM_TEXT_COLOR_MODE] ?: WatermarkTextColorMode.AUTO).coerceIn(0, 1)
        ui.prefs.wmManualTextColor = (prefs[KEY_WM_TEXT_COLOR_MANUAL] ?: WatermarkManualTextColor.BLACK).coerceIn(0, 1)
        ui.prefs.wmTextAlign = (prefs[KEY_WM_TEXT_ALIGN] ?: WatermarkTextAlign.LEFT).coerceIn(0, 2)
        ui.prefs.wmGridEnabled = prefs[KEY_WM_GRID_ENABLED] ?: true
        ui.prefs.wmRotationCwDeg = if ((prefs[KEY_WM_ROTATION_CW_90] ?: 0) == 90) 90 else 0

        ui.prefs.captureAspect = CaptureAspect.from(
            prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v
        )

        // 표준: 0=원본, 1=워터마크, 2=원본+워터마크
        ui.prefs.saveMode = SaveMode.from(prefs[KEY_SAVE_MODE] ?: SaveMode.BOTH.v)

        ui.prefs.continuousPreviewMode = ContinuousPreviewMode.from(
            prefs[KEY_CONTINUOUS_PREVIEW_MODE] ?: ContinuousPreviewMode.OFF.v
        )
        ui.prefs.photoQualityMode = PhotoQualityMode.from(
            prefs[KEY_PHOTO_QUALITY_MODE] ?: PhotoQualityMode.BALANCED.v
        )

        ui.prefs.counterDigits = clampCounterDigits(prefs[KEY_COUNTER_DIGITS] ?: COUNTER_DIGITS_DEFAULT)
        ui.prefs.showWmPreview = (prefs[KEY_SHOW_WM_PREVIEW] ?: 1) == 1
        ui.prefs.showGrid = prefs[KEY_CAMERA_GRID_ON] ?: false
        ui.prefs.zoomRatioTenths = (prefs[KEY_CAMERA_ZOOM_TENTHS] ?: 10).coerceIn(10, 100)
        ui.capture.actualZoomTenths = ui.prefs.zoomRatioTenths
        ui.capture.maxZoomTenths = maxOf(ui.capture.maxZoomTenths, 20)
    } catch (_: Exception) {
        ui.prefs.captureAspect = CaptureAspect.R3_4
        ui.prefs.saveMode = SaveMode.WATERMARK_ONLY
        ui.prefs.continuousPreviewMode = ContinuousPreviewMode.OFF
        ui.prefs.photoQualityMode = PhotoQualityMode.BALANCED
        ui.prefs.counterDigits = COUNTER_DIGITS_DEFAULT
        ui.prefs.showWmPreview = true
        ui.prefs.showGrid = false
        ui.prefs.zoomRatioTenths = 10
        ui.capture.actualZoomTenths = 10
        ui.capture.maxZoomTenths = 20
        ui.prefs.wmTableAnchor = WatermarkTableAnchor.BOTTOM_RIGHT
        ui.prefs.wmTableWidthRatio = 40
        ui.prefs.wmTableHeightRatio = 20
        ui.prefs.wmOffsetXRatio = 0
        ui.prefs.wmOffsetYRatio = 0
        ui.prefs.wmBoundsOffsetX10000 = 0
        ui.prefs.wmBoundsOffsetY10000 = 0
        ui.prefs.wmBgAlpha = 80
        ui.prefs.wmBgStyle = 0
        ui.prefs.wmValueScale = 100
        ui.prefs.wmTextColorMode = WatermarkTextColorMode.AUTO
        ui.prefs.wmManualTextColor = WatermarkManualTextColor.BLACK
        ui.prefs.wmTextAlign = WatermarkTextAlign.LEFT
        ui.prefs.wmGridEnabled = true
        ui.prefs.wmRotationCwDeg = 0
    }
}
