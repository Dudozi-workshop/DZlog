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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
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
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.dudoziworkshop.dzlog.domain.counter.CounterStreamContext
import com.dudoziworkshop.dzlog.domain.counter.CounterScopeOptions
import com.dudoziworkshop.dzlog.domain.counter.policy.normalizeTimeToMinute
import com.dudoziworkshop.dzlog.domain.counter.buildCounterStreamContext
import com.dudoziworkshop.dzlog.domain.counter.policy.buildCounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.counter.policy.isNewCounterScope
import com.dudoziworkshop.dzlog.domain.counter.toCaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.WatermarkConfig
import com.dudoziworkshop.dzlog.domain.model.CellKey
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.RotatingCounterMode
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode
import com.dudoziworkshop.dzlog.domain.model.WatermarkManualTextColor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.computeNextDelayMillis
import com.dudoziworkshop.dzlog.domain.preview.decideTickUnitFromTemplate
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.capture.permission.hasCameraPermission
import com.dudoziworkshop.dzlog.feature.capture.policy.UndoCapturePolicy
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.mediastore.MediaStoreSaverImpl
import com.dudoziworkshop.dzlog.data.repository.DzlogRepositoryImpl
import com.dudoziworkshop.dzlog.ui.camera.controls.CaptureButtonSection
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
            blankWarningEnabled = true,
        )
    )
    val haptic = LocalHapticFeedback.current

    var boundImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var zoomPanelExpanded by remember { mutableStateOf(false) }
    val ui = remember { CameraUiState() }
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
    val scopeDateTimeValues = remember(
        tableTemplateState.cells,
        tableTemplateState.fileNameSlots,
        ui.capture.now,
        ui.prefs.counterDigits,
        ui.counter.scopeNextCounter,
    ) {
        val plan = tableResolver.plan(
            cells = tableTemplateState.cells,
            captureNow = ui.capture.now,
            config = TableResolver.Config(
                counterDigits = ui.prefs.counterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
            ),
            counterSeedOverride = ui.counter.scopeNextCounter,
            phraseSets = tableTemplateState.phraseSets,
        )
        // 정책 변경: 문구별 스코프는 fileNameSlots에 포함된 PER_PHRASE ROTATING_TEXT의 resolvedText(rp_)만 반영한다.
        buildCameraCounterScopeDateTimeValues(
            cells = tableTemplateState.cells,
            fileNameSlots = tableTemplateState.fileNameSlots,
            resolvedCells = plan.resolvedCells
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

    val counterStreamContext = rememberCounterStreamContext(
        tableResolver = tableResolver,
        tableCells = tableCells,
        phraseSets = tableTemplateState.phraseSets,
        fileNameSlots = tableTemplateState.fileNameSlots,
        counterDigits = ui.prefs.counterDigits,
        nextCounter = ui.counter.scopeNextCounter,
        includeFilenameInCounterScope = appSettings.includeFilenameInCounterScope,
        dateScopeValues = scopeDateTimeValues.dateScopeValues,
        timeScopeValues = scopeDateTimeValues.timeScopeValues,
        phraseScopeValues = scopeDateTimeValues.phraseScopeValues,
        captureNow = ui.capture.now,
    )
    val mediaStoreRefreshTick = rememberMediaStoreRefreshTick(context)
    var resumeResyncTick by remember { mutableIntStateOf(0) }
    var undoResyncTick by remember { mutableIntStateOf(0) }
    var latestImage by remember { mutableStateOf<MediaImageItem?>(null) }

    suspend fun reloadLatestImage() {
        latestImage = withContext(Dispatchers.IO) {
            val reader = DzlogMediaStoreReader(context.contentResolver)
            val baseRelativePath = counterStreamContext.relativePathKey
                .substringBefore("|g2=", counterStreamContext.relativePathKey)
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
    LaunchedEffect(appSettings.saveMode, counterStreamContext.relativePathKey) { reloadLatestImage() }

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
    val hasAnyFilenameSlot = tableTemplateState.fileNameSlots.any { it != null }
    val allFilenameSlotsOff = tableTemplateState.fileNameSlots.all { it == null }
    val isTemplateReady = hasTemplateCells && (
        !appSettings.includeFilenameInCounterScope ||
            allFilenameSlotsOff ||
            hasAnyFilenameSlot
    )

    // ✅ 카운터 단일소스: 표기(ON/OFF)와 무관하게 스트림 nextSeed로 ui.counter를 항상 동기화
    SyncCounterSeedEffect(
        context = context,
        streamContext = counterStreamContext,
        counterDigits = ui.prefs.counterDigits,
        resumeTick = resumeResyncTick,
        undoTick = undoResyncTick,
        isTemplateReady = isTemplateReady,
        appSettings = appSettings,
        ui = ui
    )

    val topDisplayName = remember(
        tableTemplateState,
        ui.capture.now,
        ui.prefs.counterDigits,
        ui.counter.scopeNextCounter,
        dateFormat,
        timeFormat,
        fnDelim
    ) {
        val plan = tableResolver.plan(
            cells = tableTemplateState.cells,
            captureNow = ui.capture.now,
            config = TableResolver.Config(
                counterDigits = ui.prefs.counterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            ),
            counterSeedOverride = ui.counter.scopeNextCounter,
            phraseSets = tableTemplateState.phraseSets
        )
        CaptureNamingPolicy.buildDisplayNameForCounter(
            resolvedCells = plan.resolvedCells,
            fnDelim = fnDelim,
            counterDigits = ui.prefs.counterDigits,
            usedCounter = ui.counter.scopeNextCounter,
            now = ui.capture.now,
            fileNameSlots = tableTemplateState.fileNameSlots,
            includeDate = false,
            includeTime = false
        )
    }

    fun resetZoomToDefault() {
        ui.prefs.zoomRatioTenths = 10
        ui.capture.actualZoomTenths = 10
        ui.capture.maxZoomTenths = 20
        scope.launch { context.dataStore.edit { it[KEY_CAMERA_ZOOM_TENTHS] = 10 } }
    }

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
                onOpenTableEditor = onOpenTableEditor,
                onOpenSettings = { ui.showWizard = true },
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
                            (boundImageCapture != null && ui.capture.capturedUri == null && !ui.capture.isCapturing && !zoomPanelExpanded)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ZoomControlSection(
                                zoomRatioTenths = ui.capture.actualZoomTenths,
                                maxZoomTenths = ui.capture.maxZoomTenths,
                                expanded = zoomPanelExpanded,
                                onToggleExpanded = { zoomPanelExpanded = !zoomPanelExpanded },
                                onZoomTenthsChange = { next ->
                                    val normalized = next.coerceIn(10, ui.capture.maxZoomTenths.coerceAtLeast(10))
                                    ui.prefs.zoomRatioTenths = normalized
                                    scope.launch { context.dataStore.edit { it[KEY_CAMERA_ZOOM_TENTHS] = normalized } }
                                }
                            )

                            Box(modifier = Modifier.height(2.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        shutterButtonTopY = coordinates.positionInRoot().y
                                    }
                            ) {
                                RecentCaptureThumbButton(
                                    latestImage = latestImage,
                                    onClick = {
                                        val it = latestImage
                                        if (it == null) {
                                            onOpenAlbum()
                                        } else {
                                            val (g1, g2) = parseG1G2FromRelativePath(it.relativePath)
                                            onOpenRecentCaptureGrid(g1, g2, it.relativePath, 0)
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 16.dp)
                                )

                                Box(modifier = Modifier.align(Alignment.Center)) {
                                    CaptureButtonSection(
                                        ready = enabledNow,
                                        onClick = {
                                            if (zoomPanelExpanded) {
                                                zoomPanelExpanded = false
                                                return@CaptureButtonSection
                                            }
                                            handleCaptureClick(
                                                context = context,
                                                gate = ui.capture.captureGate,
                                                imageCapture = boundImageCapture,
                                                capturedUriPresent = (ui.capture.capturedUri != null),
                                                continuousPreviewMode = ui.prefs.continuousPreviewMode,
                                                tableResolver = tableResolver,
                                                tableTemplateState = tableTemplateState,
                                                counterDigits = ui.prefs.counterDigits,
                                                dateFormat = dateFormat,
                                                timeFormat = timeFormat,
                                                fnDelim = fnDelim,
                                                scopeNextCounter = ui.counter.scopeNextCounter,
                                                includePathInCounterScope = appSettings.includePathInCounterScope,
                                                includeFilenameInCounterScope = appSettings.includeFilenameInCounterScope,
                                                dateScopeValues = scopeDateTimeValues.dateScopeValues,
                                                timeScopeValues = scopeDateTimeValues.timeScopeValues,
                                                phraseScopeValues = scopeDateTimeValues.phraseScopeValues,
                                                captureHapticEnabled = appSettings.captureHapticEnabled,
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
                                                onUpdateScopeNextCounter = { ui.counter.scopeNextCounter = it },
                                                onAddToSessionStack = { uris ->
                                                    UndoCapturePolicy.pushCapture(sessionCaptureStack, uris)
                                                    scope.launch { reloadLatestImage() }
                                                },
                                                onHaptic = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                },
                                                onSetCapturedUri = {
                                                    ui.capture.capturedUri = it
                                                },
                                                onSetCapturing = { ui.capture.isCapturing = it }
                                            )
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
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
        val settingsButtonReservedWidth = 32.dp + (DDZSpacing.cardPadding * 2)
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
                    .defaultMinSize(minHeight = 28.dp)
                    .background(
                        color = DDZColor.Card.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onOpenTableEditor() }
                    .padding(horizontal = DDZSpacing.cardPadding, vertical = 4.dp)
            ) {
                CounterAwareFileNameText(
                    fileName = topDisplayName,
                    style = DDZTypography.Caption,
                    color = DDZColor.TextStrong,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(DDZColor.PrimaryDark.copy(alpha = 0f))
                    .defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
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
private fun WatermarkRotateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
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
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(12.dp))
            .background(if (enabled) DDZColor.SagePrimary else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            tint = if (enabled) androidx.compose.ui.graphics.Color.White else DDZColor.SageDark
        )
    }
}

@Composable
private fun RecentCaptureThumbButton(
    latestImage: MediaImageItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
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

@Composable
private fun rememberCounterStreamContext(
    tableResolver: TableResolver,
    tableCells: List<TableCellState>,
    phraseSets: List<RotatingPhraseSet>,
    fileNameSlots: List<CellKey?>,
    counterDigits: Int,
    nextCounter: Int,
    includeFilenameInCounterScope: Boolean,
    dateScopeValues: List<String>,
    timeScopeValues: List<String>,
    phraseScopeValues: List<String>,
    captureNow: Date,
): CounterStreamContext {
    val dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT
    val timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT
    val fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
    return remember(
        tableCells,
        phraseSets,
        fileNameSlots,
        counterDigits,
        nextCounter,
        includeFilenameInCounterScope,
        dateScopeValues,
        timeScopeValues,
        phraseScopeValues,
        captureNow,
    ) {
        val planForScope = tableResolver.plan(
            cells = tableCells,
            captureNow = captureNow,
            config = TableResolver.Config(
                counterDigits = counterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            ),
            counterSeedOverride = null,
            phraseSets = phraseSets,
        )
        buildCounterStreamContext(
            resolvedCells = planForScope.resolvedCells,
            fileNameSlots = fileNameSlots,
            nextCounter = nextCounter,
            isManualMode = false,
            fnDelim = fnDelim,
            includeFilenameInScope = includeFilenameInCounterScope,
            scopeOptions = CounterScopeOptions(
                dateScopeValues = dateScopeValues,
                timeScopeValues = timeScopeValues,
                phraseScopeValues = phraseScopeValues,
            ),
        )
    }
}


@Composable
private fun SyncCounterSeedEffect(
    context: Context,
    streamContext: CounterStreamContext,
    counterDigits: Int,
    resumeTick: Int,
    undoTick: Int,
    captureTick: Int,
    isTemplateReady: Boolean,
    appSettings: AppSettings,
    ui: CameraUiState
) {
    var lastResumeTick by remember { mutableIntStateOf(-1) }
    var lastUndoTick by remember { mutableIntStateOf(-1) }
    var lastSaveMode by remember { mutableStateOf<SaveMode?>(null) }
    val scopedStream = remember(
        streamContext.relativePathKey,
        streamContext.streamPrefix,
        appSettings.includePathInCounterScope,
        appSettings.includeFilenameInCounterScope,
        isTemplateReady,
    ) {
        toCaptureScopedCounterStream(
            streamContext = streamContext,
            includePathInScope = appSettings.includePathInCounterScope,
            includeFilenameInScope = appSettings.includeFilenameInCounterScope,
        )
    }
    val fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
    var scopeKeySnapshot by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resumeTick) {
        scopeKeySnapshot = scopedStream.scopeParts.scopeKey
    }
    val activeScopeKey = scopeKeySnapshot ?: scopedStream.scopeParts.scopeKey

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
            streamContext = streamContext,
            includePathInScope = appSettings.includePathInCounterScope,
            includeFilenameInScope = appSettings.includeFilenameInCounterScope,
        )

        if (ui.capture.isCapturing) {
            ui.counter.lastScopeSnapshot = scopeSnapshot
            return@LaunchedEffect
        }

        val saveModeChanged = (lastSaveMode != null && lastSaveMode != appSettings.saveMode)
        val isExternalResync =
            (resumeTick != lastResumeTick) ||
                (undoTick != lastUndoTick) ||
                saveModeChanged

        suspend fun readNextSeed(): Int = CaptureCounterPolicy.getNextCounter(
            context = context,
            scopedStream = scopedStream,
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
            streamContext.streamPrefix.contains("d_") ||
                streamContext.streamPrefix.contains("t_") ||
                streamContext.streamPrefix.contains("rp_")
        ui.counter.scopeNextCounter = when {
            // 규칙 A: 외부 리싱크(undo/resume)는 seed 하향 반영이 가능해야 한다.
            isExternalResync -> nextSeedFromStream

            // 규칙 B: 새 스트림 판정 시, 임시 상태에서 next=1로 내려오는 경우의 덮어쓰기를 방지한다.
            isNewStream && !allowResetToOneOnNewStream && nextSeedFromStream == 1 && currentScopeSeed > 1 -> currentScopeSeed
            isNewStream -> nextSeedFromStream

            // 규칙 C: 일반 케이스는 기존처럼 상향 동기화(max) 유지.
            else -> maxOf(currentScopeSeed, nextSeedFromStream)
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


private data class CameraCounterScopeDateTimeValues(
    val dateScopeValues: List<String>,
    val timeScopeValues: List<String>,
    val phraseScopeValues: List<String>
)


private fun buildCameraCounterScopeDateTimeValues(
    cells: List<TableCellState>,
    fileNameSlots: List<CellKey?>,
    resolvedCells: List<com.dudoziworkshop.dzlog.domain.table.ResolvedCell>
): CameraCounterScopeDateTimeValues {
    val resolvedById = resolvedCells.associateBy { it.id }
    val ordered = cells.sortedWith(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex }.thenBy { it.cellId })
    val dateValues = ordered
        .asSequence()
        .filter { it.dataType == TableCellDataType.DATE && it.counterScopeMode == CounterScopeMode.INCLUDE }
        .mapNotNull { resolvedById[it.cellId]?.resolvedText?.takeIf { text -> text.isNotBlank() } }
        .toList()
    val timeValues = ordered
        .asSequence()
        .filter { it.dataType == TableCellDataType.TIME && it.counterScopeMode == CounterScopeMode.INCLUDE }
        .mapNotNull { resolvedById[it.cellId]?.resolvedText }
        .map(::normalizeTimeToMinute)
        .filter { it.isNotBlank() }
        .toList()
    val fileNameCellIds = fileNameSlots.mapNotNull { it }.toSet()
    val phraseValues = ordered
        .asSequence()
        .filter { cell ->
            cell.dataType == TableCellDataType.ROTATING_TEXT &&
                cell.rotatingCounterMode == RotatingCounterMode.PER_PHRASE &&
                cell.cellId in fileNameCellIds
        }
        .mapNotNull { resolvedById[it.cellId]?.resolvedText?.trim() }
        .filter { it.isNotBlank() }
        .map { "rp_$it" }
        .toList()
    return CameraCounterScopeDateTimeValues(dateScopeValues = dateValues, timeScopeValues = timeValues, phraseScopeValues = phraseValues)
}
