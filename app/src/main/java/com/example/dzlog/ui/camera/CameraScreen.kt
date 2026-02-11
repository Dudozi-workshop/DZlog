@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog.ui.camera


// NOTE: 패키지 이동(기계적 이동)으로 인해 참조 대상이 하위 패키지로 내려감

// CameraX 바인딩 유틸은 controller로 이동됨(직접 호출이 남아있다면 이 import로 해결)

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.LifecycleOwner
import com.example.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.example.dzlog.data.counter.clampCounterDigits
import com.example.dzlog.data.mediastore.MediaStoreSaverImpl
import com.example.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.example.dzlog.data.preferences.KEY_CONTINUOUS_PREVIEW_MODE
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
import com.example.dzlog.data.repository.DzlogRepositoryImpl
import com.example.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.example.dzlog.domain.counter.CounterManager
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.naming.buildGalleryRelativePath
import com.example.dzlog.domain.naming.resolveGroupValue
import com.example.dzlog.domain.preview.computeNextDelayMillis
import com.example.dzlog.domain.preview.decideTickUnit
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.ui.camera.controls.CameraTopBarSection
import com.example.dzlog.ui.camera.controls.CaptureButtonSection
import com.example.dzlog.ui.camera.controls.handleCaptureClick
import com.example.dzlog.ui.camera.preview.CameraPreviewArea
import com.example.dzlog.ui.camera.preview.CameraPreviewAreaArgs
import com.example.dzlog.ui.camera.preview.WatermarkUiArgs
import com.example.dzlog.ui.camera.settings.CameraSettingsDialog
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.watermark.WatermarkRendererImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.Date

// NOTE: buildWatermarkConfig는 다른 파일(핸들러)에서도 사용되므로 file-private 금지
@Composable
fun CameraScreen(
    onExitToHome: () -> Unit,
    tableTemplateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onOpenTableEditor: () -> Unit
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(DDZColor.PrimaryDark)) {
        if (hasPermission) {
            CameraPreview(
                onExitToHome = onExitToHome,
                tableTemplateState = tableTemplateState,
                onTemplateChange = onTemplateChange,
                onOpenTableEditor = onOpenTableEditor
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
    onExitToHome: () -> Unit,
    tableTemplateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onOpenTableEditor: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as? LifecycleOwner ?: return
    val scope = rememberCoroutineScope()
    val repository = remember {
        val saver = MediaStoreSaverImpl()
        DzlogRepositoryImpl(
            saver = saver,
            watermarkRenderer = WatermarkRendererImpl()
        )
    }

    var boundImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val ui = remember { CameraUiState() }

    val tableResolver = remember { TableResolver() }
    val fnDelim = "_"

    val tableCells = tableTemplateState.cells

    val dateFormat = "yyyy.MM.dd"
    val timeFormat = "HH.mm.ss"

    LaunchedEffect(dateFormat, timeFormat) {
        val unit = decideTickUnit(dateFormat, timeFormat)
        while (true) {
            val delayMs = computeNextDelayMillis(unit)
            delay(delayMs)
            ui.capture.now = Date()
        }
    }

    val scopeKeyInfo = rememberScopeKeyInfo(
        tableResolver = tableResolver,
        tableCells = tableCells,
        counterDigits = ui.prefs.counterDigits,
        dateFormat = dateFormat,
        timeFormat = timeFormat,
        fnDelim = fnDelim
    )

    // ✅ 카운터 단일소스: 표기(ON/OFF)와 무관하게 스트림 nextSeed로 ui.counter를 항상 동기화
    val (scopeRelativePath, scopePrefix) = scopeKeyInfo

    SyncCounterSeedEffect(
        context = context,
        tableCells = tableCells,
        scopeRelativePath = scopeRelativePath,
        scopePrefix = scopePrefix,
        counterDigits = ui.prefs.counterDigits,
        fnDelim = fnDelim,
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
            counterSeedOverride = ui.counter.scopeNextCounter
        )
        CaptureNamingPolicy.buildDisplayNameForCounter(
            resolvedCells = plan.resolvedCells,
            fnDelim = fnDelim,
            usedCounter = ui.counter.scopeNextCounter,
            now = ui.capture.now,
            includeDate = false,
            includeTime = false
        )
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DDZColor.PrimaryDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.18f)
                    .background(DDZColor.PrimaryDark)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.64f),
                contentAlignment = Alignment.Center
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
                    ui.prefs.wmTableAnchor,
                    ui.prefs.wmTableWidthRatio,
                    ui.prefs.wmTableHeightRatio,
                    ui.prefs.wmOffsetXRatio,
                    ui.prefs.wmOffsetYRatio,
                    ui.prefs.wmBgAlpha,
                    ui.prefs.wmBgStyle,
                    ui.prefs.wmLabelScale,
                    ui.prefs.wmValueScale
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
            }
        }

        Box(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CameraTopBarSection(
                onExitToHome = onExitToHome,
                onOpenSettings = { ui.showWizard = true }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = DDZSpacing.screenPadding * 4),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = topDisplayName,
                color = DDZColor.Surface,
                modifier = Modifier
                    .background(DDZColor.PrimaryDark.copy(alpha = 0.45f))
                    .padding(horizontal = DDZSpacing.cardPadding, vertical = DDZSpacing.itemGap)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = DDZSpacing.screenPadding + DDZSpacing.itemGap),
            contentAlignment = Alignment.Center
        ) {
            val enabledNow =
                (boundImageCapture != null && ui.capture.capturedUri == null && !ui.capture.isCapturing)

            CaptureButtonSection(
                ready = enabledNow,
                onClick = {
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
                        repository = repository,
                        buildWatermarkConfig = ::buildWatermarkConfig,
                        onApplyTemplatePatch = { onTemplateChange(it) },
                        onUpdateScopeNextCounter = { ui.counter.scopeNextCounter = it },
                        onSetCapturedUri = { ui.capture.capturedUri = it },
                        onSetCapturing = { ui.capture.isCapturing = it }
                    )
                }
            )
        }

        if (ui.showWizard) {
            CameraSettingsDialog(
                context = context,
                scope = scope,
                showWmPreview = ui.prefs.showWmPreview,
                onShowWmPreviewChange = { ui.prefs.showWmPreview = it },
                continuousPreviewMode = ui.prefs.continuousPreviewMode,
                onContinuousPreviewModeChange = { ui.prefs.continuousPreviewMode = it },
                onOpenTableEditor = onOpenTableEditor,
                captureAspect = ui.prefs.captureAspect,
                onCaptureAspectChange = { ui.prefs.captureAspect = it },
                saveMode = ui.prefs.saveMode,
                onSaveModeChange = { ui.prefs.saveMode = it },
                counterDigits = ui.prefs.counterDigits,
                onCounterDigitsChange = { ui.prefs.counterDigits = it },
                onDismiss = { ui.showWizard = false }
            )
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
private fun rememberScopeKeyInfo(
    tableResolver: TableResolver,
    tableCells: List<com.example.dzlog.domain.model.TableCellState>,
    counterDigits: Int,
    dateFormat: String,
    timeFormat: String,
    fnDelim: String
): Pair<String, String> {
    return remember(tableCells, counterDigits, dateFormat, timeFormat, fnDelim) {
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
        val prefix = CounterManager.computeCounterStreamPrefix(
            resolvedCells = planForScope.resolvedCells,
            fnDelim = fnDelim
        )
        val g1 = resolveGroupValue(planForScope.resolvedCells, GroupLevel.G1)
        val g2 = resolveGroupValue(planForScope.resolvedCells, GroupLevel.G2)

        val baseRelativePath = buildGalleryRelativePath(g1, g2)
        val hasG2Group = planForScope.resolvedCells.any { it.raw?.groupLevel == GroupLevel.G2 }
        val streamRelativePath = CounterManager.computeCounterStreamRelativePathKey(
            baseRelativePath = baseRelativePath,
            hasG2Group = hasG2Group,
            group2Value = g2
        )

        streamRelativePath to prefix
    }
}

@Composable
private fun SyncCounterSeedEffect(
    context: android.content.Context,
    tableCells: List<com.example.dzlog.domain.model.TableCellState>,
    scopeRelativePath: String,
    scopePrefix: String,
    counterDigits: Int,
    fnDelim: String,
    ui: CameraUiState
) {
    LaunchedEffect(scopeRelativePath, scopePrefix, counterDigits) {
        val scopeKey = "$scopeRelativePath|$scopePrefix"

        val templateCounterSeed = tableCells
            .firstOrNull { it.dataType == TableCellDataType.COUNTER }
            ?.typedValue
            .let { it as? CellValue.CounterSeed }
            ?.start
            ?.coerceAtLeast(1)

        val nextSeedFromStream = CounterManager.getNextCounter(
            context = context,
            relativePath = scopeRelativePath,
            counterPrefix = scopePrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
            .coerceAtLeast(1)

        val isNewStream =
            (ui.counter.lastScopeKey != null && ui.counter.lastScopeKey != scopeKey)

        var resolvedSeed = ui.counter.scopeNextCounter

        // 기본: 스트림 seed를 하한으로 유지
        if (isNewStream || resolvedSeed < nextSeedFromStream) {
            resolvedSeed = nextSeedFromStream
        }

        // TableEditor에서 사용자가 COUNTER를 명시 수정한 경우(>1) camera 진입 시 해당 값을 우선 반영
        // - ON/OFF 토글 부산물로 남은 기본 seed(1)는 무시한다.
        if (templateCounterSeed != null && templateCounterSeed > 1 && templateCounterSeed != resolvedSeed) {
            resolvedSeed = templateCounterSeed
        }

        ui.counter.scopeNextCounter = resolvedSeed

        ui.counter.lastScopeKey = scopeKey
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
    } catch (_: Exception) {
        ui.prefs.captureAspect = CaptureAspect.R3_4
        ui.prefs.saveMode = SaveMode.WATERMARK_ONLY
        ui.prefs.continuousPreviewMode = ContinuousPreviewMode.OFF
        ui.prefs.counterDigits = COUNTER_DIGITS_DEFAULT
        ui.prefs.showWmPreview = true
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
