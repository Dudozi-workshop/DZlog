@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RectF
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.example.dzlog.data.counter.clampCounterDigits
import com.example.dzlog.data.mediastore.MediaStoreSaverImpl
import com.example.dzlog.data.preferences.KEY_CAPTURE_ASPECT
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
import com.example.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.data.preferences.persistCaptureAspect
import com.example.dzlog.data.repository.DzlogRepositoryImpl
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.naming.buildDisplayNameFromResolvedCells
import com.example.dzlog.domain.naming.resolveGroupValue
import com.example.dzlog.domain.preview.computeNextDelayMillis
import com.example.dzlog.domain.preview.decideTickUnit
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.domain.table.applyPatch
import com.example.dzlog.domain.watermark.WatermarkBuilder
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                color = Color.White
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

    val projectKeyNow = "default"

    val repository = remember {
        val saver = MediaStoreSaverImpl()
        DzlogRepositoryImpl(
            saver = saver,
            watermarkRenderer = WatermarkRendererImpl()
        )
    }

    var boundImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var captureAspect by remember { mutableStateOf(CaptureAspect.R3_4) }
    var saveMode by remember { mutableStateOf(SaveMode.WATERMARK_ONLY) }
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

    var previewLogged by remember { mutableStateOf(false) }

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
            wmLabelScale = (prefs[KEY_WM_LABEL_SCALE] ?: 100).coerceIn(60, 160)
            wmValueScale = (prefs[KEY_WM_VALUE_SCALE] ?: 100).coerceIn(60, 160)

            captureAspect = CaptureAspect.from(
                prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v
            )

            saveMode = when (prefs[KEY_SAVE_MODE] ?: 0) {
                0 -> SaveMode.WATERMARK_ONLY
                1 -> SaveMode.BOTH
                else -> SaveMode.ORIGINAL_ONLY
            }

            counterDigits = clampCounterDigits(prefs[KEY_COUNTER_DIGITS] ?: COUNTER_DIGITS_DEFAULT)
            showWmPreview = (prefs[KEY_SHOW_WM_PREVIEW] ?: 1) == 1
        } catch (_: Exception) {
            captureAspect = CaptureAspect.R3_4
            saveMode = SaveMode.WATERMARK_ONLY
            counterDigits = COUNTER_DIGITS_DEFAULT
            showWmPreview = true
            wmTableAnchor = WatermarkTableAnchor.BOTTOM_RIGHT
            wmTableWidthRatio = 40
            wmTableHeightRatio = 20
            wmOffsetXRatio = 0
            wmOffsetYRatio = 0
            wmBgAlpha = 80
            wmLabelScale = 100
            wmValueScale = 100
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.18f)
                    .background(Color.Black)
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
                        .background(Color.Black)
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
                        projectKey = projectKeyNow,
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
                            labelScale = wmLabelScale,
                            valueScale = wmValueScale
                        )
                    )

                    WatermarkPreviewOverlay(
                        enabled = showWmPreview,
                        request = previewRequest,
                        previewContentRect = previewContentRect
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0x66000000))
                    .clickable { onExitToHome() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("뒤로", color = Color.White)
            }

            Box(
                modifier = Modifier
                    .background(Color(0x66000000))
                    .clickable { showWizard = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("촬영 설정", color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            val enabledNow = boundImageCapture != null

            Box(
                modifier = Modifier
                    .size(78.dp)
                    .background(
                        color = if (enabledNow) Color.White else Color(0xFF777777),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .clickable(enabled = enabledNow) {
                        val cap = boundImageCapture
                        if (cap == null) {
                            Toast.makeText(context, "카메라 준비 중", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }

                        val captureNow = now
                        val planForCapture = tableResolver.plan(
                            cells = tableCells,
                            captureNow = captureNow,
                            config = TableResolver.Config(
                                counterDigits = counterDigits,
                                dateFormat = dateFormat,
                                timeFormat = timeFormat
                            )
                        )

                        val req = com.example.dzlog.domain.model.CaptureRequest(
                            projectKey = projectKeyNow,
                            group1 = resolveGroupValue(planForCapture.resolvedCells, GroupLevel.G1),
                            group2 = resolveGroupValue(planForCapture.resolvedCells, GroupLevel.G2),
                            displayName = buildDisplayNameFromResolvedCells(
                                resolvedCells = planForCapture.resolvedCells,
                                fnDelim = fnDelim,
                                includeDate = false,
                                includeTime = false,
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
                                if (entry.isNameAdjusted) {
                                    Toast.makeText(
                                        context,
                                        "중복 파일명으로 ${entry.displayName} 저장됨",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                Toast.makeText(context, "저장 완료", Toast.LENGTH_SHORT).show()
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
                            color = if (enabledNow) Color(0xFF0B0C0D) else Color(0xFF555555),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "●",
                        color = if (enabledNow) Color.White else Color(0xFFDDDDDD),
                        fontSize = 18.sp
                    )
                }
            }
        }

        if (showWizard) {
            AlertDialog(
                onDismissRequest = { showWizard = false },
                title = { Text("설정", color = Color.White) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)

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
                            Text("촬영 화면에 워터마크 미리보기 표시", color = Color.White)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            showWizard = false
                            onOpenTableEditor()
                        }) {
                            Text("표 편집")
                        }
                        Text(
                            text = "셀 속성/그룹/G1·G2 설정",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(16.dp))

                        Text("표 크기", color = Color.White)
                        Spacer(Modifier.height(8.dp))

                        Text("가로 크기 (${wmTableWidthRatio}%)", color = Color.White)
                        Slider(
                            value = wmTableWidthRatio.toFloat(),
                            onValueChange = { v ->
                                val nv = v.toInt().coerceIn(40, 100)
                                wmTableWidthRatio = nv
                                scope.launch {
                                    context.dataStore.edit { it[KEY_WM_TABLE_WIDTH] = nv }
                                }
                            },
                            valueRange = 40f..100f
                        )

                        Spacer(Modifier.height(6.dp))

                        Text("세로 크기 (${wmTableHeightRatio}%)", color = Color.White)
                        Slider(
                            value = wmTableHeightRatio.toFloat(),
                            onValueChange = { v ->
                                val nv = v.toInt().coerceIn(10, 35)
                                wmTableHeightRatio = nv
                                scope.launch {
                                    context.dataStore.edit { it[KEY_WM_TABLE_HEIGHT] = nv }
                                }
                            },
                            valueRange = 10f..35f
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("배경 투명도 (${wmBgAlpha})", color = Color.White)
                        Text("0=투명, 255=진함", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                        Slider(
                            value = wmBgAlpha.toFloat(),
                            onValueChange = { v ->
                                val nv = v.toInt().coerceIn(0, 255)
                                wmBgAlpha = nv
                                scope.launch {
                                    context.dataStore.edit { it[KEY_WM_BG_ALPHA] = nv }
                                }
                            },
                            valueRange = 0f..255f
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("글자 크기", color = Color.White)
                        Text("라벨/값 크기를 따로 조절", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                        Spacer(Modifier.height(8.dp))
                        Text("라벨 크기 (${wmLabelScale}%)", color = Color.White)
                        Slider(
                            value = wmLabelScale.toFloat(),
                            onValueChange = { v ->
                                val nv = v.toInt().coerceIn(60, 160)
                                wmLabelScale = nv
                                scope.launch { context.dataStore.edit { it[KEY_WM_LABEL_SCALE] = nv } }
                            },
                            valueRange = 60f..160f
                        )

                        Text("값 크기 (${wmValueScale}%)", color = Color.White)
                        Slider(
                            value = wmValueScale.toFloat(),
                            onValueChange = { v ->
                                val nv = v.toInt().coerceIn(60, 160)
                                wmValueScale = nv
                                scope.launch { context.dataStore.edit { it[KEY_WM_VALUE_SCALE] = nv } }
                            },
                            valueRange = 60f..160f
                        )

                        Spacer(Modifier.height(20.dp))
                        Text(
                            "※ 미리보기는 촬영 화면에서 확인됨",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        Text("촬영 비율", color = Color.White)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (captureAspect == CaptureAspect.R3_4),
                                onClick = {
                                    captureAspect = CaptureAspect.R3_4
                                    scope.launch(Dispatchers.IO) { persistCaptureAspect(context, CaptureAspect.R3_4) }
                                }
                            )
                            Text("3:4", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (captureAspect == CaptureAspect.R9_16),
                                onClick = {
                                    captureAspect = CaptureAspect.R9_16
                                    scope.launch(Dispatchers.IO) { persistCaptureAspect(context, CaptureAspect.R9_16) }
                                }
                            )
                            Text("9:16", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (captureAspect == CaptureAspect.R1_1),
                                onClick = {
                                    captureAspect = CaptureAspect.R1_1
                                    scope.launch(Dispatchers.IO) { persistCaptureAspect(context, CaptureAspect.R1_1) }
                                }
                            )
                            Text("1:1", color = Color.White)
                        }
                        Spacer(Modifier.height(16.dp))

                        Text("표 위치", color = Color.White)

                        fun saveAnchor(v: Int, a: WatermarkTableAnchor) {
                            wmTableAnchor = a
                            scope.launch {
                                context.dataStore.edit { prefs -> prefs[KEY_WM_TABLE_ANCHOR] = v }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = wmTableAnchor == WatermarkTableAnchor.TOP_LEFT,
                                onClick = { saveAnchor(0, WatermarkTableAnchor.TOP_LEFT) }
                            )
                            Text("좌상", color = Color.White)
                            Spacer(Modifier.width(12.dp))

                            RadioButton(
                                selected = wmTableAnchor == WatermarkTableAnchor.TOP_RIGHT,
                                onClick = { saveAnchor(1, WatermarkTableAnchor.TOP_RIGHT) }
                            )
                            Text("우상", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = wmTableAnchor == WatermarkTableAnchor.BOTTOM_LEFT,
                                onClick = { saveAnchor(2, WatermarkTableAnchor.BOTTOM_LEFT) }
                            )
                            Text("좌하", color = Color.White)
                            Spacer(Modifier.width(12.dp))

                            RadioButton(
                                selected = wmTableAnchor == WatermarkTableAnchor.BOTTOM_RIGHT,
                                onClick = { saveAnchor(3, WatermarkTableAnchor.BOTTOM_RIGHT) }
                            )
                            Text("우하", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = wmTableAnchor == WatermarkTableAnchor.CUSTOM,
                                onClick = { saveAnchor(4, WatermarkTableAnchor.CUSTOM) }
                            )
                            Text("사용자 지정", color = Color.White)
                        }

                        if (wmTableAnchor == WatermarkTableAnchor.CUSTOM) {
                            Spacer(Modifier.height(12.dp))
                            Text("사용자 지정 위치", color = Color.White)
                            Text("X=좌→우, Y=상→하 (0~100)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))

                            Text("X 위치 (${wmOffsetXRatio}%)", color = Color.White)
                            Slider(
                                value = wmOffsetXRatio.toFloat(),
                                onValueChange = { v ->
                                    val nv = v.toInt().coerceIn(0, 100)
                                    wmOffsetXRatio = nv
                                    scope.launch {
                                        context.dataStore.edit { it[KEY_WM_OFFSET_X] = nv }
                                    }
                                },
                                valueRange = 0f..100f
                            )

                            Text("Y 위치 (${wmOffsetYRatio}%)", color = Color.White)
                            Slider(
                                value = wmOffsetYRatio.toFloat(),
                                onValueChange = { v ->
                                    val nv = v.toInt().coerceIn(0, 100)
                                    wmOffsetYRatio = nv
                                    scope.launch {
                                        context.dataStore.edit { it[KEY_WM_OFFSET_Y] = nv }
                                    }
                                },
                                valueRange = 0f..100f
                            )
                        }

                        Text("저장 모드", color = Color.White)
                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = saveMode == SaveMode.WATERMARK_ONLY,
                                onClick = {
                                    saveMode = SaveMode.WATERMARK_ONLY
                                    scope.launch { context.dataStore.edit { it[KEY_SAVE_MODE] = 0 } }
                                }
                            )
                            Text("워터마크만", color = Color.White)
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
                            Text("원본+워터마크", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = saveMode == SaveMode.ORIGINAL_ONLY,
                                onClick = {
                                    saveMode = SaveMode.ORIGINAL_ONLY
                                    scope.launch { context.dataStore.edit { it[KEY_SAVE_MODE] = 2 } }
                                }
                            )
                            Text("원본만", color = Color.White)
                        }

                        Spacer(Modifier.height(20.dp))

                        Text("카운터 자릿수", color = Color.White)
                        Text("예: 4자리면 0001", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    val next = clampCounterDigits(counterDigits - 1)
                                    counterDigits = next
                                    scope.launch { context.dataStore.edit { it[KEY_COUNTER_DIGITS] = next } }
                                }
                            ) { Text("-", color = Color.White) }

                            Spacer(Modifier.width(12.dp))
                            Text(counterDigits.toString(), color = Color.White, fontSize = 18.sp)
                            Spacer(Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    val next = clampCounterDigits(counterDigits + 1)
                                    counterDigits = next
                                    scope.launch { context.dataStore.edit { it[KEY_COUNTER_DIGITS] = next } }
                                }
                            ) { Text("+", color = Color.White) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showWizard = false }) {
                        Text("닫기")
                    }
                },
                containerColor = Color(0xFF1A1A1A)
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
