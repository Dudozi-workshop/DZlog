@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import com.example.dzlog.domain.table.TablePatch
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.domain.table.applyPatch
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.watermark.WatermarkRendererImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

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
    var captureAspect by remember { mutableStateOf(CaptureAspect.R3_4) }
    var saveMode by remember { mutableStateOf(SaveMode.BOTH) }
    var continuousPreviewMode by remember { mutableStateOf(ContinuousPreviewMode.OFF) }
    var counterDigits by remember { mutableIntStateOf(COUNTER_DIGITS_DEFAULT) }

    val tableResolver = remember { TableResolver() }
    var showWizard by remember { mutableStateOf(false) }

    var showWmPreview by remember { mutableStateOf(true) }
    var wmTableAnchor by remember { mutableStateOf(WatermarkTableAnchor.BOTTOM_RIGHT) }
    var wmTableWidthRatio by remember { mutableIntStateOf(40) }
    var wmTableHeightRatio by remember { mutableIntStateOf(20) }
    var wmOffsetXRatio by remember { mutableIntStateOf(0) }
    var wmOffsetYRatio by remember { mutableIntStateOf(0) }
    var wmBgAlpha by remember { mutableIntStateOf(80) }
    // 0=BLACK, 1=WHITE, 2=TRANSPARENT
    var wmBgStyle by remember { mutableIntStateOf(0) }
    var wmLabelScale by remember { mutableIntStateOf(100) }
    var wmValueScale by remember { mutableIntStateOf(100) }
    val fnDelim = "_"

    val tableCells = tableTemplateState.cells

    val dateFormat = "yyyy.MM.dd"
    val timeFormat = "HH.mm.ss"

    var tapFocusUi by remember { mutableStateOf<TapFocusUiState?>(null) }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(dateFormat, timeFormat) {
        val unit = decideTickUnit(dateFormat, timeFormat)
        while (true) {
            val delayMs = computeNextDelayMillis(unit)
            delay(delayMs)
            now = Date()
        }
    }

    val scopeKeyInfo = remember(tableTemplateState, counterDigits) {
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
        // ✅ counter 스트림 prefix: 날짜/시간 제외(파일명에는 붙어도 카운터에는 영향 없음)
        val prefix = CounterManager.computeCounterPrefix(
            resolvedCells = planForScope.resolvedCells,
            fnDelim = fnDelim
        )

        // ✅ 카운터 스트림키의 relativePath는 "실제 저장 경로"와 동일한 기준으로 계산해야 함
        // (저장은 CaptureRequest.group1/group2 = resolvedCells 기반)
        val g1 = resolveGroupValue(planForScope.resolvedCells, GroupLevel.G1)
        val g2 = resolveGroupValue(planForScope.resolvedCells, GroupLevel.G2)
        val relativePath = buildGalleryRelativePath(g1, g2)
        relativePath to prefix
    }

    val scopeRelativePath = scopeKeyInfo.first
    val scopePrefix = scopeKeyInfo.second
    var scopeNextCounter by remember { mutableIntStateOf(1) }
    // ✅ 카운터 스트림 변경 감지용
    var lastScopeKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scopeRelativePath, scopePrefix, counterDigits) {
        val scopeKey = "$scopeRelativePath|$scopePrefix"
        val counterCell = tableCells.firstOrNull { it.dataType == TableCellDataType.COUNTER }
        val currentSeed = (counterCell?.typedValue as? CellValue.CounterSeed)?.start
        // ✅ 정책(스트림키=relativePathPrefix) 기준 next counter 계산
        val nextSeed = CounterManager.getNextCounter(
            context = context,
            relativePath = scopeRelativePath,
            counterPrefix = scopePrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
        val normalizedSeed = if (nextSeed < 1) 1 else nextSeed
        val isNewStream = (lastScopeKey != null && lastScopeKey != scopeKey)
        val desiredSeed = when {
            counterCell == null -> normalizedSeed
            currentSeed == null -> normalizedSeed
            isNewStream -> normalizedSeed
            else -> maxOf(normalizedSeed, currentSeed)
        }
        scopeNextCounter = desiredSeed
        // 다음 실행에서 이전 스트림 비교에 사용됨
        lastScopeKey = scopeKey
        // IDE inspection용 read (동작 영향 없음)
        lastScopeKey
        if (counterCell != null && (currentSeed == null || currentSeed != desiredSeed)) {
            val patch = TablePatch(mapOf(counterCell.cellId to desiredSeed.toString()))
            onTemplateChange(tableTemplateState.applyPatch(patch))
        }
    }

    // ✅ 결과 미리보기용 상태
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    // ✅ 촬영 중(in-flight) 상태: 중복 촬영 방지용
    var isCapturing by remember { mutableStateOf(false) }
    // ✅ 연타/동시 호출 방지 게이트(로직 레벨). UI 상태보다 우선함.
    val captureGate = remember { AtomicBoolean(false) }

    LaunchedEffect(capturedUri, continuousPreviewMode) {
        if (capturedUri != null && continuousPreviewMode == ContinuousPreviewMode.SHORT) {
            delay(1500)
            capturedUri = null
        }
    }

    LaunchedEffect(Unit) {
        try {
            val prefs = context.dataStore.data.first()
            wmTableAnchor = when (prefs[KEY_WM_TABLE_ANCHOR] ?: 3) {
                0 -> WatermarkTableAnchor.TOP_LEFT
                1 -> WatermarkTableAnchor.TOP_RIGHT
                2 -> WatermarkTableAnchor.BOTTOM_LEFT
                3 -> WatermarkTableAnchor.BOTTOM_RIGHT
                else -> WatermarkTableAnchor.CUSTOM
            }

            wmTableWidthRatio = (prefs[KEY_WM_TABLE_WIDTH] ?: 40).coerceIn(40, 100)
            wmTableHeightRatio = (prefs[KEY_WM_TABLE_HEIGHT] ?: 20).coerceIn(10, 35)
            wmOffsetXRatio = (prefs[KEY_WM_OFFSET_X] ?: 0).coerceIn(0, 100)
            wmOffsetYRatio = (prefs[KEY_WM_OFFSET_Y] ?: 0).coerceIn(0, 100)

            wmBgAlpha = (prefs[KEY_WM_BG_ALPHA] ?: 80).coerceIn(0, 255)
            wmBgStyle = (prefs[KEY_WM_TABLE_BG_STYLE] ?: 0).coerceIn(0, 2)
            wmLabelScale = (prefs[KEY_WM_LABEL_SCALE] ?: 100).coerceIn(60, 160)
            wmValueScale = (prefs[KEY_WM_VALUE_SCALE] ?: 100).coerceIn(60, 160)

            captureAspect = CaptureAspect.from(
                prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v
            )

            // 표준: 0=원본, 1=워터마크, 2=원본+워터마크
            saveMode = SaveMode.from(prefs[KEY_SAVE_MODE] ?: SaveMode.BOTH.v)

            continuousPreviewMode = ContinuousPreviewMode.from(
                prefs[KEY_CONTINUOUS_PREVIEW_MODE] ?: ContinuousPreviewMode.OFF.v
            )

            counterDigits = clampCounterDigits(prefs[KEY_COUNTER_DIGITS] ?: COUNTER_DIGITS_DEFAULT)
            showWmPreview = (prefs[KEY_SHOW_WM_PREVIEW] ?: 1) == 1
        } catch (_: Exception) {
            captureAspect = CaptureAspect.R3_4
            saveMode = SaveMode.WATERMARK_ONLY
            continuousPreviewMode = ContinuousPreviewMode.OFF
            counterDigits = COUNTER_DIGITS_DEFAULT
            showWmPreview = true
            wmTableAnchor = WatermarkTableAnchor.BOTTOM_RIGHT
            wmTableWidthRatio = 40
            wmTableHeightRatio = 20
            wmOffsetXRatio = 0
            wmOffsetYRatio = 0
            wmBgAlpha = 80
            wmBgStyle = 0
            wmLabelScale = 100
            wmValueScale = 100
        }
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
                CameraPreviewArea(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    scope = scope,
                    captureAspect = captureAspect,
                    saveMode = saveMode,
                    continuousPreviewMode = continuousPreviewMode,
                    counterDigits = counterDigits,
                    dateFormat = dateFormat,
                    timeFormat = timeFormat,
                    fnDelim = fnDelim,
                    tableTemplateState = tableTemplateState,
                    tableResolver = tableResolver,
                    now = now,
                    showWmPreview = showWmPreview,
                    wmTableAnchor = wmTableAnchor,
                    wmTableWidthRatio = wmTableWidthRatio,
                    wmTableHeightRatio = wmTableHeightRatio,
                    wmOffsetXRatio = wmOffsetXRatio,
                    wmOffsetYRatio = wmOffsetYRatio,
                    wmBgAlpha = wmBgAlpha,
                    wmBgStyle = wmBgStyle,
                    wmLabelScale = wmLabelScale,
                    wmValueScale = wmValueScale,
                    boundCamera = boundCamera,
                    onBoundCameraChange = { boundCamera = it },
                    onBoundImageCaptureChange = { boundImageCapture = it },
                    capturedUri = capturedUri,
                    onDismissCaptured = { capturedUri = null },
                    tapFocusUi = tapFocusUi,
                    onTapFocusUiChange = { tapFocusUi = it }
                )
            }
        }

        Box(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CameraTopBarSection(
                onExitToHome = onExitToHome,
                onOpenSettings = { showWizard = true }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = DDZSpacing.screenPadding + DDZSpacing.itemGap),
            contentAlignment = Alignment.Center
        ) {
            val enabledNow =
                (boundImageCapture != null && capturedUri == null && !isCapturing)

            CaptureButtonSection(
                ready = enabledNow,
                onClick = {
                    handleCaptureClick(
                        context = context,
                        gate = captureGate,
                        imageCapture = boundImageCapture,
                        capturedUriPresent = (capturedUri != null),
                        continuousPreviewMode = continuousPreviewMode,
                        tableResolver = tableResolver,
                        tableTemplateState = tableTemplateState,
                        counterDigits = counterDigits,
                        dateFormat = dateFormat,
                        timeFormat = timeFormat,
                        fnDelim = fnDelim,
                        scopeNextCounter = scopeNextCounter,
                        captureAspect = captureAspect,
                        saveMode = saveMode,
                        wmTableAnchor = wmTableAnchor,
                        wmOffsetXRatio = wmOffsetXRatio,
                        wmOffsetYRatio = wmOffsetYRatio,
                        wmTableWidthRatio = wmTableWidthRatio,
                        wmTableHeightRatio = wmTableHeightRatio,
                        wmBgAlpha = wmBgAlpha,
                        wmBgStyle = wmBgStyle,
                        wmLabelScale = wmLabelScale,
                        wmValueScale = wmValueScale,
                        repository = repository,
                        buildWatermarkConfig = ::buildWatermarkConfig,
                        onApplyTemplatePatch = { onTemplateChange(it) },
                        onUpdateScopeNextCounter = { scopeNextCounter = it },
                        onSetCapturedUri = { capturedUri = it },
                        onSetCapturing = { isCapturing = it }
                    )
                }
            )
        }

        if (showWizard) {
            CameraSettingsDialog(
                context = context,
                scope = scope,
                showWmPreview = showWmPreview,
                onShowWmPreviewChange = { showWmPreview = it },
                continuousPreviewMode = continuousPreviewMode,
                onContinuousPreviewModeChange = { continuousPreviewMode = it },
                onOpenTableEditor = onOpenTableEditor,
                captureAspect = captureAspect,
                onCaptureAspectChange = { captureAspect = it },
                saveMode = saveMode,
                onSaveModeChange = { saveMode = it },
                counterDigits = counterDigits,
                onCounterDigitsChange = { counterDigits = it },
                onDismiss = { showWizard = false }
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

