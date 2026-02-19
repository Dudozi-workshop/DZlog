@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.RecoverableSecurityException
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LifecycleOwner
import com.example.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.example.dzlog.data.counter.clampCounterDigits
import com.example.dzlog.data.datastore.AppSettingsStore
import com.example.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.example.dzlog.data.preferences.KEY_CONTINUOUS_PREVIEW_MODE
import com.example.dzlog.data.preferences.KEY_CAMERA_GRID_ON
import com.example.dzlog.data.preferences.KEY_CAMERA_ZOOM_TENTHS
import com.example.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.example.dzlog.data.preferences.KEY_SAVE_MODE
import com.example.dzlog.data.preferences.KEY_SHOW_WM_PREVIEW
import com.example.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.example.dzlog.data.preferences.KEY_WM_LABEL_SCALE
import com.example.dzlog.data.preferences.KEY_WM_OFFSET_X
import com.example.dzlog.data.preferences.KEY_WM_OFFSET_Y
import com.example.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.example.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.example.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.example.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.example.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.example.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.example.dzlog.domain.counter.CounterStreamContext
import com.example.dzlog.domain.counter.buildCounterStreamContext
import com.example.dzlog.domain.counter.policy.CounterSeedInput
import com.example.dzlog.domain.counter.policy.buildCounterScopeSnapshot
import com.example.dzlog.domain.counter.policy.decideCounterSeed
import com.example.dzlog.domain.counter.policy.isNewCounterScope
import com.example.dzlog.domain.counter.toCaptureScopedCounterStream
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.MediaImageItem
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.naming.NamingFormatDefaults
import com.example.dzlog.domain.preview.computeNextDelayMillis
import com.example.dzlog.domain.preview.decideTickUnit
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.feature.capture.io.createCaptureRepository
import com.example.dzlog.feature.capture.permission.hasCameraPermission
import com.example.dzlog.feature.capture.policy.CounterResyncPolicy
import com.example.dzlog.feature.capture.policy.UndoCapturePolicy
import com.example.dzlog.feature.capture.policy.stabilizeStreamNextCounter
import com.example.dzlog.data.mediastore.DzlogMediaStoreReader
import com.example.dzlog.ui.camera.controls.CaptureButtonSection
import com.example.dzlog.ui.camera.controls.ZoomControlSection
import com.example.dzlog.ui.camera.controls.handleCaptureClick
import com.example.dzlog.ui.camera.preview.CameraPreviewArea
import com.example.dzlog.ui.camera.preview.CameraPreviewAreaArgs
import com.example.dzlog.ui.camera.preview.WatermarkUiArgs
import com.example.dzlog.ui.camera.settings.CameraSettingsOverlayPanel
import com.example.dzlog.ui.log.DzThumbnail
import com.example.dzlog.ui.log.parseG1G2FromRelativePath
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

private val USABLE_VERTICAL_MARGIN = 10.dp

// NOTE: buildWatermarkConfig는 다른 파일(핸들러)에서도 사용되므로 file-private 금지
@Composable
fun CameraScreen(
    tableTemplateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, startIndex: Int) -> Unit
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

    Box(modifier = Modifier.fillMaxSize().background(DDZColor.PrimaryDark.copy(alpha = 0f))) {
        if (hasPermission) {
            CameraPreview(
                tableTemplateState = tableTemplateState,
                onTemplateChange = onTemplateChange,
                onOpenTableEditor = onOpenTableEditor,
                onOpenAlbum = onOpenAlbum,
                onOpenRecentCaptureGrid = onOpenRecentCaptureGrid
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
    onOpenRecentCaptureGrid: (g1: String, g2: String, startIndex: Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as? LifecycleOwner ?: return
    val scope = rememberCoroutineScope()
    val repository = remember { createCaptureRepository() }

    var boundImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var zoomPanelExpanded by remember { mutableStateOf(false) }
    val ui = remember { CameraUiState() }
    var settingsButtonBottomY by remember { mutableStateOf<Float?>(null) }
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

    val tableCells = tableTemplateState.cells

    val dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT
    val timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT

    LaunchedEffect(dateFormat, timeFormat) {
        val unit = decideTickUnit(dateFormat, timeFormat)
        while (true) {
            val delayMs = computeNextDelayMillis(unit)
            delay(delayMs)
            ui.capture.now = Date()
        }
    }

    val counterStreamContext = rememberCounterStreamContext(
        tableResolver = tableResolver,
        tableCells = tableCells,
        counterDigits = ui.prefs.counterDigits,
        nextCounter = ui.counter.scopeNextCounter,
    )
    val mediaStoreRefreshTick = rememberMediaStoreRefreshTick(context)
    var latestImage by remember { mutableStateOf<MediaImageItem?>(null) }

    suspend fun reloadLatestImage() {
        latestImage = withContext(Dispatchers.IO) {
            val reader = DzlogMediaStoreReader(context.contentResolver)
            runCatching { reader.loadLatestImage() }.getOrNull()
        }
    }

    suspend fun syncAfterUndoDelete() {
        reloadLatestImage()
        ui.counter.scopeNextCounter = CounterResyncPolicy.refreshNextCounterFromMediaStore(
            context = context,
            streamContext = counterStreamContext,
            counterDigits = ui.prefs.counterDigits,
            fnDelim = fnDelim
        )
    }

    LaunchedEffect(Unit) { reloadLatestImage() }
    LaunchedEffect(mediaStoreRefreshTick) { reloadLatestImage() }

    val sessionCaptureStack = remember { mutableStateListOf<List<Uri>>() }
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

    // ✅ 카운터 단일소스: 표기(ON/OFF)와 무관하게 스트림 nextSeed로 ui.counter를 항상 동기화
    SyncCounterSeedEffect(
        context = context,
        tableCells = tableCells,
        streamContext = counterStreamContext,
        counterDigits = ui.prefs.counterDigits,
        refreshTick = mediaStoreRefreshTick,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DDZColor.PrimaryDark.copy(alpha = 0f))
            .onGloballyPositioned { coordinates ->
                cameraRootHeightPx = coordinates.size.height.toFloat()
            }
    ) {
        val previewAreaArgs = remember(
            context,
            lifecycleOwner,
            scope,
            ui.prefs.captureAspect,
            ui.prefs.saveMode,
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
            ui.prefs.wmLabelScale,
            ui.prefs.wmValueScale,
            settingsButtonBottomY,
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
                saveMode = ui.prefs.saveMode,
                continuousPreviewMode = ui.prefs.continuousPreviewMode,
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
                settingsButtonBottomY = settingsButtonBottomY,
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
                onOpenTableEditor = onOpenTableEditor,
                watermarkUi = WatermarkUiArgs(
                    anchor = ui.prefs.wmTableAnchor,
                    tableWidthRatio = ui.prefs.wmTableWidthRatio,
                    tableHeightRatio = ui.prefs.wmTableHeightRatio,
                    offsetXRatio = ui.prefs.wmOffsetXRatio,
                    offsetYRatio = ui.prefs.wmOffsetYRatio,
                    bgAlpha = ui.prefs.wmBgAlpha,
                    bgStyle = ui.prefs.wmBgStyle,
                    labelScale = ui.prefs.wmLabelScale,
                    valueScale = ui.prefs.wmValueScale
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

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    top = DDZSpacing.screenPadding + DDZSpacing.sectionGap + DDZSpacing.itemGap,
                    start = DDZSpacing.screenPadding,
                    end = DDZSpacing.screenPadding
                )
                .fillMaxWidth()
        ) {
            val topBarMinHeight = 32.dp + (DDZSpacing.itemGap * 2)
            val settingsButtonReservedWidth = 32.dp + (DDZSpacing.cardPadding * 2)
            val filenameMaxWidth = (maxWidth - settingsButtonReservedWidth - DDZSpacing.itemGap)
                .coerceAtLeast(0.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = topBarMinHeight)
            ) {
                Text(
                    text = topDisplayName,
                    color = DDZColor.Primary,
                    style = DDZTypography.Caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .widthIn(max = filenameMaxWidth)
                        .defaultMinSize(minHeight = 32.dp)
                        .background(
                            color = DDZColor.Card.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = DDZSpacing.cardPadding, vertical = DDZSpacing.itemGap)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .background(DDZColor.PrimaryDark.copy(alpha = 0f))
                        .defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                        .onGloballyPositioned { coordinates ->
                            settingsButtonBottomY = coordinates.positionInRoot().y + coordinates.size.height
                        }
                        .clickable { ui.showWizard = true }
                        .padding(horizontal = DDZSpacing.cardPadding, vertical = DDZSpacing.itemGap)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "촬영 설정",
                        tint = DDZColor.Surface
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = DDZSpacing.screenPadding + DDZSpacing.itemGap),
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

                Box(modifier = Modifier.height(DDZSpacing.itemGap))

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
                                onOpenRecentCaptureGrid(g1, g2, 0)
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
                            captureAspect = ui.prefs.captureAspect,
                            saveMode = ui.prefs.saveMode,
                            wmTableAnchor = ui.prefs.wmTableAnchor,
                            wmOffsetXRatio = ui.prefs.wmOffsetXRatio,
                            wmOffsetYRatio = ui.prefs.wmOffsetYRatio,
                            wmTableWidthRatio = ui.prefs.wmTableWidthRatio,
                            wmTableHeightRatio = ui.prefs.wmTableHeightRatio,
                            wmBgAlpha = ui.prefs.wmBgAlpha,
                            wmBgStyle = ui.prefs.wmBgStyle,
                            wmLabelScale = ui.prefs.wmLabelScale,
                            wmValueScale = ui.prefs.wmValueScale,
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
                            onSetCapturedUri = {
                                ui.capture.capturedUri = it
                            },
                            onSetCapturing = { ui.capture.isCapturing = it }
                            )
                            }
                        )
                    }

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
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                    )
                }
            }
        }

        if (ui.showWizard) {
            CameraSettingsOverlayPanel(
                captureAspect = ui.prefs.captureAspect,
                onCaptureAspectChange = { aspect ->
                    ui.prefs.captureAspect = aspect
                    scope.launch { com.example.dzlog.data.preferences.persistCaptureAspect(context, aspect) }
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
private fun UndoCaptureButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DDZColor.Border, RoundedCornerShape(12.dp))
            .background(DDZColor.Card.copy(alpha = if (enabled) 0.7f else 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Undo,
            contentDescription = "Undo",
            tint = DDZColor.TextPrimary.copy(alpha = if (enabled) 1f else 0.45f)
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
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DDZColor.Border, RoundedCornerShape(12.dp))
            .background(DDZColor.Card.copy(alpha = 0.5f))
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
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    tableBgAlpha: Int,
    bgStyle: Int,
    labelScale: Int,
    valueScale: Int
): com.example.dzlog.domain.model.WatermarkConfig {
    return com.example.dzlog.domain.model.WatermarkConfig(
        showLabel = false,
        anchor = anchor,
        offsetXRatio = offsetXRatio,
        offsetYRatio = offsetYRatio,
        tableWidthRatio = tableWidthRatio,
        tableHeightRatio = tableHeightRatio,
        tableBgAlpha = tableBgAlpha,
        bgStyle = bgStyle,
        labelScale = labelScale,
        valueScale = valueScale
    )
}

@Composable
private fun rememberMediaStoreRefreshTick(context: android.content.Context): Int {
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
    tableCells: List<com.example.dzlog.domain.model.TableCellState>,
    counterDigits: Int,
    nextCounter: Int,
): CounterStreamContext {
    val dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT
    val timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT
    val fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
    return remember(tableCells, counterDigits, nextCounter) {
        val scopeNow = Date()
        val planForScope = tableResolver.plan(
            cells = tableCells,
            captureNow = scopeNow,
            config = TableResolver.Config(
                counterDigits = counterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            )
        )
        buildCounterStreamContext(
            resolvedCells = planForScope.resolvedCells,
            nextCounter = nextCounter,
            isManualMode = false,
            fnDelim = fnDelim
        )
    }
}


@Composable
private fun SyncCounterSeedEffect(
    context: android.content.Context,
    tableCells: List<com.example.dzlog.domain.model.TableCellState>,
    streamContext: CounterStreamContext,
    counterDigits: Int,
    refreshTick: Int,
    ui: CameraUiState
) {
    val fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
    LaunchedEffect(streamContext.scopeKey, counterDigits, refreshTick) {
        val appSettings = AppSettingsStore.flow(context).first()
        val scopedStream = toCaptureScopedCounterStream(
            streamContext = streamContext,
            includePathInScope = appSettings.includePathInCounterScope,
            includeFilenameInScope = appSettings.includeFilenameInCounterScope,
        )
        val scopeSnapshot = buildCounterScopeSnapshot(
            streamContext = streamContext,
            includePathInScope = appSettings.includePathInCounterScope,
            includeFilenameInScope = appSettings.includeFilenameInCounterScope,
        )

        val templateCounterSeed = tableCells
            .firstOrNull { it.dataType == TableCellDataType.COUNTER }
            ?.typedValue
            .let { it as? CellValue.CounterSeed }
            ?.start
            ?.coerceAtLeast(1)

        val nextSeedFromStream = CaptureCounterPolicy.getNextCounter(
            context = context,
            scopedStream = scopedStream,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        ).coerceAtLeast(1)

        val isNewStream = isNewCounterScope(
            previous = ui.counter.lastScopeSnapshot,
            current = scopeSnapshot
        )
        val stableStreamNext = stabilizeStreamNextCounter(
            streamNextFromPolicy = nextSeedFromStream,
            currentScopeNext = ui.counter.scopeNextCounter,
            isNewStream = isNewStream
        )
        val input = CounterSeedInput(
            streamNext = stableStreamNext,
            currentSeed = ui.counter.scopeNextCounter,
            isNewStream = isNewStream,
            templateCounterSeed = templateCounterSeed,
        )
        val decision = decideCounterSeed(input)

        ui.counter.scopeNextCounter = decision.desiredSeed
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

        ui.prefs.wmTableWidthRatio = (prefs[KEY_WM_TABLE_WIDTH] ?: 40).coerceIn(40, 100)
        ui.prefs.wmTableHeightRatio = (prefs[KEY_WM_TABLE_HEIGHT] ?: 20).coerceIn(10, 35)
        ui.prefs.wmOffsetXRatio = (prefs[KEY_WM_OFFSET_X] ?: 0).coerceIn(0, 100)
        ui.prefs.wmOffsetYRatio = (prefs[KEY_WM_OFFSET_Y] ?: 0).coerceIn(0, 100)

        ui.prefs.wmBgAlpha = (prefs[KEY_WM_BG_ALPHA] ?: 80).coerceIn(0, 255)
        ui.prefs.wmBgStyle = (prefs[KEY_WM_TABLE_BG_STYLE] ?: 0).coerceIn(0, 2)
        ui.prefs.wmLabelScale = (prefs[KEY_WM_LABEL_SCALE] ?: 100).coerceIn(60, 160)
        ui.prefs.wmValueScale = (prefs[KEY_WM_VALUE_SCALE] ?: 100).coerceIn(60, 160)

        ui.prefs.captureAspect = CaptureAspect.from(
            prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v
        )

        // 표준: 0=원본, 1=워터마크, 2=원본+워터마크
        ui.prefs.saveMode = SaveMode.from(prefs[KEY_SAVE_MODE] ?: SaveMode.BOTH.v)

        ui.prefs.continuousPreviewMode = ContinuousPreviewMode.from(
            prefs[KEY_CONTINUOUS_PREVIEW_MODE] ?: ContinuousPreviewMode.OFF.v
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
        ui.prefs.wmBgAlpha = 80
        ui.prefs.wmBgStyle = 0
        ui.prefs.wmLabelScale = 100
        ui.prefs.wmValueScale = 100
    }
}
