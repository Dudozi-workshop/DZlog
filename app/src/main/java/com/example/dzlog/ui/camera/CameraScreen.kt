@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import android.util.Rational
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import coil.compose.AsyncImage
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
import com.example.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.example.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.example.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.example.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.data.preferences.persistCaptureAspect
import com.example.dzlog.data.repository.DzlogRepositoryImpl
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.counter.CounterManager
import com.example.dzlog.domain.naming.buildDisplayNameFromResolvedCells
import com.example.dzlog.domain.naming.buildFileNamePrefixFromResolvedCells
import com.example.dzlog.domain.naming.buildGalleryRelativePath
import com.example.dzlog.domain.naming.resolveGroupValue
import com.example.dzlog.domain.preview.computeNextDelayMillis
import com.example.dzlog.domain.preview.decideTickUnit
import com.example.dzlog.domain.table.TablePatch
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.domain.table.applyPatch
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.ui.common.DDZButton
import com.example.dzlog.ui.common.DDZButtonStyle
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography
import com.example.dzlog.watermark.WatermarkRendererImpl
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

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

        // ✅ 정책(스트림키=relativePathprefix) 기준 next counter 계산
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
        lastScopeKey = scopeKey
        if (counterCell != null && (currentSeed == null || currentSeed != desiredSeed)) {
            val patch = TablePatch(mapOf(counterCell.cellId to desiredSeed.toString()))
            onTemplateChange(tableTemplateState.applyPatch(patch))
        }
    }

    var previewLogged by remember { mutableStateOf(false) }

    // ✅ 결과 미리보기용 상태
    var capturedUri by remember { mutableStateOf<Uri?>(null) }

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(captureAspect.ratioF)
                        .background(DDZColor.PrimaryDark)
                        .clipToBounds()
                ) {
                    val previewView = remember(context) {
                        PreviewView(context).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    }
                    var previewContentRect by remember { mutableStateOf<RectF?>(null) }

                    fun updatePreviewContentRect() {
                        previewContentRect = resolvePreviewContentRect(previewView)
                        if (!previewLogged) {
                            val rect = previewContentRect
                            if (rect != null) {
                                Log.d(
                                    "DZlogPreview",
                                    "Preview size=${rect.width().toInt()}x${rect.height().toInt()} aspect=${captureAspect.label} overlay-only (no bitmap)"
                                )
                                previewLogged = true
                            }
                        }
                    }

                    DisposableEffect(previewView, lifecycleOwner) {
                        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                            updatePreviewContentRect()
                        }
                        val streamObserver = Observer<PreviewView.StreamState> { state ->
                            if (state == PreviewView.StreamState.STREAMING) {
                                updatePreviewContentRect()
                            }
                        }
                        previewView.addOnLayoutChangeListener(layoutListener)
                        previewView.previewStreamState.observe(lifecycleOwner, streamObserver)
                        onDispose {
                            previewView.removeOnLayoutChangeListener(layoutListener)
                            previewView.previewStreamState.removeObserver(streamObserver)
                        }
                    }

                    LaunchedEffect(captureAspect) {
                        bindCamera(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            aspect = captureAspect
                        ) { cap ->
                            boundImageCapture = cap
                        }
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { _: Context -> previewView },
                        update = { it.scaleType = PreviewView.ScaleType.FILL_CENTER }
                    )
                    val plan = remember(tableCells, now, counterDigits, dateFormat, timeFormat) {
                        tableResolver.plan(
                            cells = tableCells,
                            captureNow = now,
                            config = TableResolver.Config(
                                counterDigits = counterDigits,
                                dateFormat = dateFormat,
                                timeFormat = timeFormat
                            )
                        )
                    }

                    val previewRequest = com.example.dzlog.domain.model.CaptureRequest(
                        group1 = resolveGroupValue(plan.resolvedCells, GroupLevel.G1),
                        group2 = resolveGroupValue(plan.resolvedCells, GroupLevel.G2),
                        displayName = buildDisplayNameFromResolvedCells(
                            resolvedCells = plan.resolvedCells,
                            fnDelim = fnDelim,
                            includeDate = false,
                            includeTime = false,
                            now = now
                        ),
                        resolvedCells = plan.resolvedCells,
                        watermarkCells = WatermarkBuilder.buildTableCells(plan.resolvedCells),
                        saveMode = saveMode,
                        captureAspect = captureAspect,
                        tableTemplate = tableTemplateState,
                        watermark = com.example.dzlog.domain.model.WatermarkConfig(
                            showLabel = false,
                            anchor = wmTableAnchor,
                            offsetXRatio = wmOffsetXRatio,
                            offsetYRatio = wmOffsetYRatio,
                            tableWidthRatio = wmTableWidthRatio,
                            tableHeightRatio = wmTableHeightRatio,
                            tableBgAlpha = wmBgAlpha,
                            bgStyle = wmBgStyle,
                            labelScale = wmLabelScale,
                            valueScale = wmValueScale
                        )
                    )

                    WatermarkPreviewOverlay(
                        enabled = showWmPreview,
                        request = previewRequest,
                        previewContentRect = previewContentRect
                    )

                    // ✅ 촬영 결과물 오버레이 (Continuous Preview - 팝업 축소 버전)
                    if (capturedUri != null && continuousPreviewMode != ContinuousPreviewMode.OFF) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)) // 약한 반투명 검은 배경
                                .clickable { capturedUri = null }
                                .zIndex(10f),
                            contentAlignment = Alignment.TopCenter // 조금 더 위쪽으로 배치
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 60.dp) // 상단 여백 조절
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.72f) // 72% 크기 (중앙 위치 통일용)
                                        .aspectRatio(captureAspect.ratioF)
                                        .clip(RoundedCornerShape(8.dp)) // 테두리 안쪽 클리핑 (사진 잘림 방지)
                                        .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                ) {
                                    AsyncImage(
                                        model = capturedUri,
                                        contentDescription = "Captured result",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit // 사진이 잘리지 않도록 Fit 유지
                                    )
                                }

                                Spacer(Modifier.height(DDZSpacing.itemGap))
                                
                                val hintText = if (continuousPreviewMode == ContinuousPreviewMode.HOLD) {
                                    "화면을 터치하면 닫힙니다"
                                } else {
                                    "저장 완료"
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(DDZColor.PrimaryDark.copy(alpha = 0.8f), shape = RoundedCornerShape(20.dp))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        hintText,
                                        color = DDZColor.Surface,
                                        style = DDZTypography.Caption
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(
                    top = DDZSpacing.screenPadding + DDZSpacing.sectionGap + DDZSpacing.itemGap,
                    start = DDZSpacing.screenPadding,
                    end = DDZSpacing.screenPadding
                )
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(DDZColor.PrimaryDark.copy(alpha = 0.4f))
                    .clickable { onExitToHome() }
                    .padding(horizontal = DDZSpacing.cardPadding, vertical = DDZSpacing.itemGap)
            ) {
                Text("뒤로", style = DDZTypography.ButtonText, color = DDZColor.Surface)
            }

            Box(
                modifier = Modifier
                    .background(DDZColor.PrimaryDark.copy(alpha = 0.4f))
                    .clickable { showWizard = true }
                    .padding(horizontal = DDZSpacing.cardPadding, vertical = DDZSpacing.itemGap)
            ) {
                Text("촬영 설정", style = DDZTypography.ButtonText, color = DDZColor.Surface)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = DDZSpacing.screenPadding + DDZSpacing.itemGap),
            contentAlignment = Alignment.Center
        ) {
            val enabledNow = (boundImageCapture != null && capturedUri == null)

            Box(
                modifier = Modifier
                    .size(78.dp)
                    .background(
                        color = if (enabledNow) DDZColor.Surface else DDZColor.IconMuted,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .clickable(enabled = enabledNow) {
                        val cap = boundImageCapture
                        if (cap == null) {
                            Toast.makeText(context, "카메라 준비 중", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }

                        // Use a single timestamp for this capture so watermark / file name are consistent.
                        val captureNow = Date()
                        val planForCapture = tableResolver.plan(
                            cells = tableCells,
                            captureNow = captureNow,
                            config = TableResolver.Config(
                                counterDigits = counterDigits,
                                dateFormat = dateFormat,
                                timeFormat = timeFormat
                            )
                        )

                        val hasCounterCell = planForCapture.resolvedCells.any { it.type == TableCellDataType.COUNTER }
                        val req = com.example.dzlog.domain.model.CaptureRequest(
                            group1 = resolveGroupValue(planForCapture.resolvedCells, GroupLevel.G1),
                            group2 = resolveGroupValue(planForCapture.resolvedCells, GroupLevel.G2),
                            displayName = buildDisplayNameFromResolvedCells(
                                resolvedCells = planForCapture.resolvedCells,
                                fnDelim = fnDelim,
                                includeDate = false,
                                includeTime = false,
                                counterOverride = if (hasCounterCell) null else scopeNextCounter,
                                now = captureNow
                            ),
                            resolvedCells = planForCapture.resolvedCells,
                            watermarkCells = WatermarkBuilder.buildTableCells(planForCapture.resolvedCells),
                            saveMode = saveMode,
                            captureAspect = captureAspect,
                            tableTemplate = tableTemplateState,
                            watermark = com.example.dzlog.domain.model.WatermarkConfig(
                                showLabel = false,
                                anchor = wmTableAnchor,
                                offsetXRatio = wmOffsetXRatio,
                                offsetYRatio = wmOffsetYRatio,
                                tableWidthRatio = wmTableWidthRatio,
                                tableHeightRatio = wmTableHeightRatio,
                                tableBgAlpha = wmBgAlpha,
                                bgStyle = wmBgStyle,
                                labelScale = wmLabelScale,
                                valueScale = wmValueScale
                            )
                        )

                        repository.captureAndSave(
                            context = context,
                            imageCapture = cap,
                            request = req,
                            onDone = { entry ->
                                onTemplateChange(tableTemplateState.applyPatch(planForCapture.patch))
                                if (!hasCounterCell) {
                                    val base = entry.displayName.substringBeforeLast('.', entry.displayName)
                                    val token = base.substringAfterLast('_', missingDelimiterValue = "").trim()
                                    val parsed = if (token.all { it.isDigit() }) token.toIntOrNull() else null
                                    scopeNextCounter = ((parsed ?: scopeNextCounter) + 1).coerceAtLeast(1)
                                }
                                if (entry.isNameAdjusted) {
                                    Toast.makeText(
                                        context,
                                        "중복 파일명으로 ${entry.displayName} 저장됨",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                
                                // ✅ 촬영 후 미리보기 설정 적용
                                if (continuousPreviewMode != ContinuousPreviewMode.OFF) {
                                    capturedUri = entry.contentUri
                                } else {
                                    Toast.makeText(context, "저장 완료", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onFail = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = if (enabledNow) DDZColor.PrimaryDark else DDZColor.Border,
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "●",
                        color = if (enabledNow) DDZColor.Surface else DDZColor.TextMuted,
                        style = DDZTypography.CardTitle
                    )
                }
            }
        }

        if (showWizard) {
            AlertDialog(
                onDismissRequest = { showWizard = false },
                title = { Text("설정", style = DDZTypography.ScreenTitle, color = DDZColor.Surface) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = showWmPreview,
                                onCheckedChange = { checked ->
                                    showWmPreview = checked
                                    scope.launch {
                                        context.dataStore.edit { prefs: MutablePreferences ->
                                            prefs[KEY_SHOW_WM_PREVIEW] = if (checked) 1 else 0
                                        }
                                    }
                                }
                            )
                            Text(
                                "촬영 화면에 워터마크 미리보기 표시",
                                style = DDZTypography.Body,
                                color = DDZColor.Surface
                            )
                        }
                        Spacer(Modifier.height(DDZSpacing.sectionGap))
                        
                        Text("연속 촬영 미리보기", style = DDZTypography.Body, color = DDZColor.Surface)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ContinuousPreviewMode.entries.forEach { mode ->
                                RadioButton(
                                    selected = continuousPreviewMode == mode,
                                    onClick = {
                                        continuousPreviewMode = mode
                                        scope.launch {
                                            context.dataStore.edit { it[KEY_CONTINUOUS_PREVIEW_MODE] = mode.v }
                                        }
                                    }
                                )
                                Text(mode.name, style = DDZTypography.Body, color = DDZColor.Surface)
                                Spacer(Modifier.width(8.dp))
                            }
                        }

                        Spacer(Modifier.height(DDZSpacing.sectionGap))
                        DDZButton(
                            text = "표 편집",
                            onClick = {
                                showWizard = false
                                onOpenTableEditor()
                            },
                            style = DDZButtonStyle.Primary
                        )
                        Text(
                            text = "셀 속성/그룹/G1·G2 설정",
                            color = DDZColor.Surface.copy(alpha = 0.7f),
                            style = DDZTypography.Caption
                        )
                        Spacer(Modifier.height(DDZSpacing.screenPadding))

                        Text(
                            "표 위치/크기/스타일은 표 상세설정에서 변경",
                            color = DDZColor.Surface.copy(alpha = 0.7f),
                            style = DDZTypography.Caption
                        )

                        Spacer(Modifier.height(DDZSpacing.sectionGap + DDZSpacing.itemGap))

                        Text("촬영 비율", style = DDZTypography.Body, color = DDZColor.Surface)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (captureAspect == CaptureAspect.R3_4),
                                onClick = {
                                    captureAspect = CaptureAspect.R3_4
                                    scope.launch(Dispatchers.IO) { persistCaptureAspect(context, CaptureAspect.R3_4) }
                                }
                            )
                            Text("3:4", style = DDZTypography.Body, color = DDZColor.Surface)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (captureAspect == CaptureAspect.R9_16),
                                onClick = {
                                    captureAspect = CaptureAspect.R9_16
                                    scope.launch(Dispatchers.IO) { persistCaptureAspect(context, CaptureAspect.R9_16) }
                                }
                            )
                            Text("9:16", style = DDZTypography.Body, color = DDZColor.Surface)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (captureAspect == CaptureAspect.R1_1),
                                onClick = {
                                    captureAspect = CaptureAspect.R1_1
                                    scope.launch(Dispatchers.IO) { persistCaptureAspect(context, CaptureAspect.R1_1) }
                                }
                            )
                            Text("1:1", style = DDZTypography.Body, color = DDZColor.Surface)
                        }
                        Spacer(Modifier.height(DDZSpacing.screenPadding))

                        Text("저장 모드", style = DDZTypography.Body, color = DDZColor.Surface)
                        Spacer(Modifier.height(DDZSpacing.itemGap))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = saveMode == SaveMode.WATERMARK_ONLY,
                                onClick = {
                                    saveMode = SaveMode.WATERMARK_ONLY
                                    scope.launch { context.dataStore.edit { it[KEY_SAVE_MODE] = 0 } }
                                }
                            )
                            Text("워터마크", style = DDZTypography.Body, color = DDZColor.Surface)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = saveMode == SaveMode.ORIGINAL_ONLY,
                                onClick = {
                                    saveMode = SaveMode.ORIGINAL_ONLY
                                    scope.launch { context.dataStore.edit { it[KEY_SAVE_MODE] = 2 } }
                                }
                            )
                            Text("원본", style = DDZTypography.Body, color = DDZColor.Surface)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = saveMode == SaveMode.BOTH,
                                onClick = {
                                    saveMode = SaveMode.BOTH
                                    scope.launch {
                                        context.dataStore.edit { prefs: MutablePreferences ->
                                            prefs[KEY_SAVE_MODE] = 1
                                        }
                                    }
                                }
                            )
                            Text("원본+워터마크", style = DDZTypography.Body, color = DDZColor.Surface)
                        }

                        Spacer(Modifier.height(DDZSpacing.sectionGap + DDZSpacing.itemGap))

                        Text("카운터 자릿수", style = DDZTypography.Body, color = DDZColor.Surface)
                        Text(
                            "예: 4자리면 0001",
                            color = DDZColor.Surface.copy(alpha = 0.7f),
                            style = DDZTypography.Caption
                        )
                        Spacer(Modifier.height(DDZSpacing.itemGap))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    val next = clampCounterDigits(counterDigits - 1)
                                    counterDigits = next
                                    scope.launch { context.dataStore.edit { it[KEY_COUNTER_DIGITS] = next } }
                                }
                            ) { Text("-", style = DDZTypography.ButtonText, color = DDZColor.Surface) }

                            Spacer(Modifier.width(DDZSpacing.sectionGap))
                            Text(counterDigits.toString(), style = DDZTypography.CardTitle, color = DDZColor.Surface)
                            Spacer(Modifier.width(DDZSpacing.sectionGap))

                            Button(
                                onClick = {
                                    val next = clampCounterDigits(counterDigits + 1)
                                    counterDigits = next
                                    scope.launch { context.dataStore.edit { it[KEY_COUNTER_DIGITS] = next } }
                                }
                            ) { Text("+", style = DDZTypography.ButtonText, color = DDZColor.Surface) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showWizard = false }) {
                        Text("닫기", style = DDZTypography.ButtonText)
                    }
                },
                containerColor = DDZColor.PrimaryDark
            )
        }
    }
}

@Composable
private fun WatermarkPreviewOverlay(
    enabled: Boolean,
    request: com.example.dzlog.domain.model.CaptureRequest,
    previewContentRect: RectF?
) {
    if (!enabled || previewContentRect == null) return

    val cells = request.watermarkCells

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
    ) {
        drawIntoCanvas { canvas ->
            drawWatermarkTableOnCanvas(
                canvas = canvas.nativeCanvas,
                bounds = previewContentRect,
                cells = cells,
                rows = request.tableTemplate.rows,
                cols = request.tableTemplate.cols,
                showLabel = request.watermark.showLabel,
                anchor = request.watermark.anchor,
                offsetXRatio = request.watermark.offsetXRatio,
                offsetYRatio = request.watermark.offsetYRatio,
                tableHeightRatio = request.watermark.tableHeightRatio,
                tableWidthRatio = request.watermark.tableWidthRatio,
                bgAlpha = request.watermark.tableBgAlpha,
                labelScale = request.watermark.labelScale,
                valueScale = request.watermark.valueScale
            )
        }
    }
}

private fun bindCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    aspect: CaptureAspect,
    onBound: (ImageCapture?) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val rotation = previewView.display.rotation
        Log.d("DZlog", "BIND aspect=${aspect.label} w/h=${aspect.w}/${aspect.h}")

        val cameraAspectRatio = aspect.toCameraXAspectRatio()

        val previewBuilder = Preview.Builder()
            .setTargetRotation(rotation)
        if (cameraAspectRatio != null) {
            previewBuilder.setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            cameraAspectRatio,
                            AspectRatioStrategy.FALLBACK_RULE_AUTO
                        )
                    )
                    .build()
            )
        }
        val preview = previewBuilder.build()
            .apply { surfaceProvider = previewView.surfaceProvider }

        val imageCaptureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
        if (cameraAspectRatio != null) {
            imageCaptureBuilder.setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            cameraAspectRatio,
                            AspectRatioStrategy.FALLBACK_RULE_AUTO
                        )
                    )
                    .build()
            )
        }
        val imageCapture = imageCaptureBuilder.build()

        val viewPort = ViewPort.Builder(
            Rational(aspect.w, aspect.h),
            rotation
        )
            .setScaleType(ViewPort.FILL_CENTER)
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .setViewPort(viewPort)
            .addUseCase(preview)
            .addUseCase(imageCapture)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCaseGroup
            )
            onBound(imageCapture)
        } catch (_: Exception) {
            onBound(null)
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun resolvePreviewContentRect(previewView: PreviewView): RectF? {
    val width = previewView.width
    val height = previewView.height
    if (width <= 0 || height <= 0) return null
    return RectF(0f, 0f, width.toFloat(), height.toFloat())
}
