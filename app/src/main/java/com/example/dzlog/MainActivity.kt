@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog

// =========================================================
// Imports (FOUNDATION ONLY)
// =========================================================

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dzlog.data.counter.CounterSyncImpl
import com.example.dzlog.data.mediastore.MediaStoreSaverImpl
import com.example.dzlog.data.repository.DzlogRepositoryImpl
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellKind
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.naming.buildGalleryRelativePath
import com.example.dzlog.domain.naming.buildDisplayName
import com.example.dzlog.domain.naming.resolveGroupValue
import com.example.dzlog.domain.watermark.resolveCellsFromTableTemplate
import com.example.dzlog.domain.preview.TimeSystem
import com.example.dzlog.domain.preview.decideTickUnit
import com.example.dzlog.domain.preview.computeNextDelayMillis
import com.example.dzlog.domain.preview.resolvePreviewCellText
import com.example.dzlog.watermark.WatermarkRendererImpl
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Date

// =========================================================
// App Navigation
// =========================================================

enum class AppScreen {
    HOME,
    CAMERA,
    TABLE_EDITOR,
    SETTINGS
}

class TableTemplateViewModel : ViewModel() {
    var tableTemplateState by mutableStateOf(defaultTableTemplateState())
        private set

    fun update(state: TableTemplateState) {
        tableTemplateState = state
    }

    fun reset() {
        tableTemplateState = defaultTableTemplateState()
    }
}

// =========================================================
// DataStore
// =========================================================

private val Context.dataStore by preferencesDataStore(name = "dzlog_prefs")

// =========================================================
// Basic Keys
// =========================================================

val KEY_COUNTER_DIGITS = intPreferencesKey("counter_digits")
val KEY_CAPTURE_ASPECT = intPreferencesKey("capture_aspect")
val KEY_SAVE_MODE = intPreferencesKey("save_mode")
val KEY_ORIENTATION_MODE = intPreferencesKey("orientation_mode")
val KEY_SHOW_WM_PREVIEW = intPreferencesKey("show_wm_preview") // 0/1
val KEY_WM_TABLE_ANCHOR = intPreferencesKey("wm_table_anchor") // 0~4
val KEY_WM_TABLE_WIDTH = intPreferencesKey("wm_table_width_ratio")   // 40~100
val KEY_WM_TABLE_HEIGHT = intPreferencesKey("wm_table_height_ratio") // 10~35
val KEY_WM_OFFSET_X = intPreferencesKey("wm_offset_x_ratio") // 0~100
val KEY_WM_OFFSET_Y = intPreferencesKey("wm_offset_y_ratio") // 0~100
val KEY_WM_BG_ALPHA = intPreferencesKey("wm_bg_alpha") // 0~255
val KEY_WM_LABEL_SCALE = intPreferencesKey("wm_label_scale") // 60~160
val KEY_WM_VALUE_SCALE = intPreferencesKey("wm_value_scale") // 60~160



// =========================================================
// Folder / Counter
// =========================================================

const val COUNTER_DIGITS_DEFAULT = 4

fun clampCounterDigits(v: Int) = v.coerceIn(1, 6) // [각주 1]

// =========================================================
// Save / Orientation
// =========================================================

enum class OrientationMode(val v: Int) {
    PORTRAIT_LOCK(0),
    AUTO_ROTATE(1);

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.v == v } ?: PORTRAIT_LOCK
    }
}


// =========================================================
// Watermark Core
// =========================================================



// =========================================================
// PART 2 — MainActivity / AppRoot / HomeScreen (Navigation)
// =========================================================

// =========================================================
// Part 2-A. MainActivity
// =========================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }
}

// =========================================================
// Part 2-B. AppRoot
// =========================================================

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var previousScreen by remember { mutableStateOf(AppScreen.HOME) }
    var orientationMode by remember { mutableStateOf(OrientationMode.PORTRAIT_LOCK) }
    val tableTemplateViewModel: TableTemplateViewModel = viewModel()
    val tableTemplateState = tableTemplateViewModel.tableTemplateState

    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // ✅ Load orientation mode from DataStore
    LaunchedEffect(Unit) {
        orientationMode = try {
            val prefs = context.dataStore.data.first()
            OrientationMode.from(prefs[KEY_ORIENTATION_MODE] ?: OrientationMode.PORTRAIT_LOCK.v)
        } catch (_: Exception) {
            OrientationMode.PORTRAIT_LOCK
        }
    }

    // ✅ Apply orientation mode
    LaunchedEffect(orientationMode) {
        val a = activity ?: return@LaunchedEffect
        a.requestedOrientation = when (orientationMode) {
            OrientationMode.PORTRAIT_LOCK ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            OrientationMode.AUTO_ROTATE ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
// 화면 전환을 단일 진입점으로 통일하여 lint 경고(Assigned value is never read) 제거
    fun navigateTo(target: AppScreen) {
     previousScreen = screen
      screen = target
       }

    when (screen) {
        AppScreen.HOME -> HomeScreen(
            onOpenSettings = {
                navigateTo(AppScreen.SETTINGS)
            },
            onOpenTableEditor = {
                navigateTo(AppScreen.TABLE_EDITOR)
            },
            onStartCamera = {
                navigateTo(AppScreen.CAMERA)
            }
        )

        AppScreen.CAMERA -> {
            // ✅ CameraScreen은 Part 3에서 제공됨
            CameraScreen(
                onExitToHome = { navigateTo(AppScreen.HOME) },
                tableTemplateState = tableTemplateState,
                onOpenTableEditor = {
                    navigateTo(AppScreen.TABLE_EDITOR)
                }
            )
        }

        AppScreen.TABLE_EDITOR -> {
            TableEditorScreen(
                templateState = tableTemplateState,
                onTemplateChange = tableTemplateViewModel::update,
                onReset = tableTemplateViewModel::reset,
                onBack = { screen = previousScreen }
            )
        }
        AppScreen.SETTINGS -> SettingsScreen(
            onBack = { screen = AppScreen.HOME }
        )
    }
}


// =========================================================
// Part 2-C. HomeScreen
// =========================================================

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onOpenTableEditor: () -> Unit
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Text("DZlog", fontSize = 24.sp, color = Color.White)
            Spacer(Modifier.height(20.dp))

            Button(onClick = onStartCamera, modifier = Modifier.fillMaxWidth()) {
                Text("촬영 시작")
            }

            Spacer(Modifier.height(12.dp))

            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("설정")
            }
            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenTableEditor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("표 상세설정", fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Table Editor", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("설정 화면(준비중)")
        Button(onClick = onBack) { Text("Back") }
    }
}


// =========================================================
// PART 3 — CameraScreen (Permission Gate) / CameraPreview (CameraX Bind)
// =========================================================

// ---------------------------------------------------------
// CameraScreen (Permission Gate)
// ---------------------------------------------------------

@Composable
fun CameraScreen(
    onExitToHome: () -> Unit,                // [수정됨-뒤로-2]
    tableTemplateState: TableTemplateState,
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
                onExitToHome = onExitToHome,             // [수정됨-뒤로-3]
                tableTemplateState = tableTemplateState,
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

// =========================================================
// CameraPreview 함수 (CameraX Bind Core)
// =========================================================

@SuppressLint("AutoboxingStateCreation")
@Composable
fun CameraPreview(
    onExitToHome: () -> Unit,                // [수정됨-뒤로-4]
    tableTemplateState: TableTemplateState,
    onOpenTableEditor: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as? LifecycleOwner ?: return
    val scope = rememberCoroutineScope()

    // ✅ 임시(나중에 Wizard 설정값으로 대체)
    val projectKeyNow = "default"


    val repository = remember {
        val saver = MediaStoreSaverImpl()
        DzlogRepositoryImpl(
            saver = saver,
            counterSync = CounterSyncImpl(saver),
            watermarkRenderer = WatermarkRendererImpl()
        )
    }



    // ✅ CameraX bind 결과(촬영에 사용할 ImageCapture) — 단일 소스
    var boundImageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // ✅ 선택 비율(단일 소스)
    var captureAspect by remember { mutableStateOf(CaptureAspect.R3_4) }

    // ✅ 추가: 저장모드/자릿수 (기본값은 안전하게)
    var saveMode by remember { mutableStateOf(SaveMode.WATERMARK_ONLY) }
    var counterDigits by remember { mutableIntStateOf(4) } // 기본 0001 형태

    // ✅ 추가: 촬영 카운터(파일명/표에 들어갈 숫자)
    var counter by remember { mutableIntStateOf(1) }

    // ✅ 추가: 워터마크에 들어갈 값

    // ✅ Wizard 시작 여부
    var showWizard by remember { mutableStateOf(false) }

    //프리뷰 표시 설정
    var showWmPreview by remember { mutableStateOf(true) } // 기본 켜짐(원하면 false)
    //표 위치 변경
    var wmTableAnchor by remember {
        mutableStateOf(WatermarkTableAnchor.BOTTOM_RIGHT)
    }    // 표 크기 변경
    var wmTableWidthRatio by remember { mutableIntStateOf(40) }
    var wmTableHeightRatio by remember { mutableIntStateOf(20) }
    var wmOffsetXRatio by remember { mutableIntStateOf(0) } // 0~100
    var wmOffsetYRatio by remember { mutableIntStateOf(0) } // 0~100
// 표 투명도
    var wmBgAlpha by remember { mutableIntStateOf(80) } // 0~255 (기본 80 추천)
    // 글씨 크기 변경
    var wmLabelScale by remember { mutableIntStateOf(100) } // 60~160
    var wmValueScale by remember { mutableIntStateOf(100) } // 60~160
    val fnDelim = "_"

    val tableCells = tableTemplateState.cells

    // [Phase2] Camera에서도 저장 경로 기준 nextCounter 계산(폴더 스캔)
    val savePathNow = remember(tableCells) { buildGalleryRelativePath(tableCells) }
    val cameraCounterSync = remember { CounterSyncImpl(MediaStoreSaverImpl()) }
    var folderNextCounter by remember { mutableIntStateOf(1) }

    LaunchedEffect(savePathNow, counterDigits) {
        cameraCounterSync.syncNextCounter(
            context = context,
            relativePath = savePathNow,
            counterDigits = counterDigits,
            fnDelim = "_",
            onResult = { next -> folderNextCounter = next },
            onFail = { _ -> folderNextCounter = 1 }
        )
    }

    // COUNTER 오버라이드(있으면 우선)
    val counterOverride: Int? = remember(tableCells) {
        tableCells
            .firstOrNull { it.dataType == TableCellDataType.COUNTER }
            ?.valueText
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.toIntOrNull()
            ?.takeIf { it >= 0 }
    }

    // 저장 카운터 초기화(경로/자릿수/오버라이드가 바뀔 때만 동기화)
    LaunchedEffect(savePathNow, counterDigits, counterOverride, folderNextCounter) {
        counter = counterOverride ?: folderNextCounter
    }

    var previewLogged by remember { mutableStateOf(false) }

    // ---------- Load captureAspect from DataStore ----------
    LaunchedEffect(Unit) {
        try {
            val prefs = context.dataStore.data.first()
// 표 위치 수정
            wmTableAnchor = when (prefs[KEY_WM_TABLE_ANCHOR] ?: 3) {
                0 -> WatermarkTableAnchor.TOP_LEFT
                1 -> WatermarkTableAnchor.TOP_RIGHT
                2 -> WatermarkTableAnchor.BOTTOM_LEFT
                3 -> WatermarkTableAnchor.BOTTOM_RIGHT
                else -> WatermarkTableAnchor.CUSTOM
            }

            //표 크기 수정
            wmTableWidthRatio = (prefs[KEY_WM_TABLE_WIDTH] ?: 40).coerceIn(40, 100)
            wmTableHeightRatio = (prefs[KEY_WM_TABLE_HEIGHT] ?: 20).coerceIn(10, 35)
            wmOffsetXRatio = (prefs[KEY_WM_OFFSET_X] ?: 0).coerceIn(0, 100)
            wmOffsetYRatio = (prefs[KEY_WM_OFFSET_Y] ?: 0).coerceIn(0, 100)

            // 표 투명도
            wmBgAlpha = (prefs[KEY_WM_BG_ALPHA] ?: 80).coerceIn(0, 255)

            // 글씨 크기
            wmLabelScale = (prefs[KEY_WM_LABEL_SCALE] ?: 100).coerceIn(60, 160)
            wmValueScale = (prefs[KEY_WM_VALUE_SCALE] ?: 100).coerceIn(60, 160)


            captureAspect = CaptureAspect.from(
                prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v
            )

            // ✅ SaveMode는 Int로 저장(0/1/2)
            saveMode = when (prefs[KEY_SAVE_MODE] ?: 0) {
                0 -> SaveMode.WATERMARK_ONLY
                1 -> SaveMode.BOTH
                else -> SaveMode.ORIGINAL_ONLY
            }

            // ✅ 자릿수는 1~6 사이로 클램프
            counterDigits = clampCounterDigits(prefs[KEY_COUNTER_DIGITS] ?: COUNTER_DIGITS_DEFAULT)
            showWmPreview = (prefs[KEY_SHOW_WM_PREVIEW] ?: 1) == 1

        } catch (_: Exception) {
            captureAspect = CaptureAspect.R3_4
            saveMode = SaveMode.WATERMARK_ONLY
            counterDigits = 4
            showWmPreview = true
            wmTableAnchor = WatermarkTableAnchor.BOTTOM_RIGHT
            //표 크기 수정
            wmTableWidthRatio = 40
            wmTableHeightRatio = 20
            wmOffsetXRatio = 0
            wmOffsetYRatio = 0
            //표 투명도
            wmBgAlpha = 80
            // 글씨 크기
            wmLabelScale = 100
            wmValueScale = 100




        }
    }

    // =====================================================
    // UI Frame: 선택 비율의 "액자" (what-you-see-is-what-you-get)
    // =====================================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // ✅ 레이아웃은 Column 1개만 쓴다 (중첩 Column 금지)
        Column(modifier = Modifier.fillMaxSize()) {

            // 위 블랙바
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.18f)
                    .background(Color.Black)
            )

            // ✅ 가운데 프리뷰 "액자"
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
                        .clipToBounds() // ✅ 액자 밖은 잘라냄(크롭 강제)
                ) {
                    // =================================================
                    // AndroidView(PreviewView) + CameraX bind (UseCaseGroup)
                    // =================================================

// PreviewView는 1회 생성 + context 변경 시만 재생성
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

// captureAspect 변경 시에만 CameraX 재바인딩
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

// AndroidView는 고정
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { _: Context -> previewView },
                        update = { it.scaleType = PreviewView.ScaleType.FILL_CENTER }
                    )
                    val previewRequest = com.example.dzlog.domain.model.CaptureRequest(
                        projectKey = projectKeyNow,
                        group1 = resolveGroupValue(tableCells, GroupLevel.G1),
                        group2 = resolveGroupValue(tableCells, GroupLevel.G2),
                        displayName = buildDisplayName(
                            cells = tableCells,
                            counter = counter,
                            counterDigits = counterDigits,
                            fnDelim = fnDelim,
                            includeDate = false,
                            includeTime = false
                        ),
                        counter = counter,
                        counterDigits = counterDigits,
                        saveMode = saveMode,
                        captureAspect = captureAspect,
                        tableTemplate = tableTemplateState,
                        watermark = com.example.dzlog.domain.model.WatermarkConfig(
                            showLabel = true,
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
                        previewContentRect = previewContentRect,
                        dateFormat = "yyyy.MM.dd",
                        timeFormat = "HH.mm.ss",
                        timeSystem = TimeSystem.H24
                    )
                }
            }
        }

        // =====================================================
        // 상단 UI(테스트용) - 지금은 설정만
        // =====================================================
        Row(
            modifier = Modifier
                .padding(top = 40.dp, start = 16.dp, end = 16.dp) // [수정됨-UI-1] 버튼 위치 아래로
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0x66000000))
                    .clickable { onExitToHome() }                  // [수정됨-UI-2] 뒤로가기
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("뒤로", color = Color.White)
            }

            Box(
                modifier = Modifier
                    .background(Color(0x66000000))
                    .clickable { showWizard = true }               // [수정됨-UI-3] 촬영 설정 열기
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("촬영 설정", color = Color.White)
            }
        }

        // =====================================================
        // 촬영 버튼(FAB) - 화면 하단 중앙
        // =====================================================
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

                        val req = com.example.dzlog.domain.model.CaptureRequest(
                            projectKey = projectKeyNow,
                            group1 = resolveGroupValue(tableCells, GroupLevel.G1),
                            group2 = resolveGroupValue(tableCells, GroupLevel.G2),
                            displayName = buildDisplayName(
                                cells = tableCells,
                                counter = counter,
                                counterDigits = counterDigits,
                                fnDelim = fnDelim,
                                includeDate = false,
                                includeTime = false
                            ),
                            counter = counter,
                            counterDigits = counterDigits,
                            saveMode = saveMode,
                            captureAspect = captureAspect,
                            tableTemplate = tableTemplateState,
                            watermark = com.example.dzlog.domain.model.WatermarkConfig(
                                showLabel = true,
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
                                counter += 1   // ✅ 이 줄이 없으면 평생 001
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
                    }
                ,
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
        // =====================================================
        // 설정 다이얼로그(Wizard UI)
        // =====================================================
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
                                        context.dataStore.edit { prefs ->
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

                        //표 크기 설정
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

                        // 표 투명도
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
// 글씨 크기
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

// 사진 비율
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "※ 미리보기는 촬영 화면에서 확인됨",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        Text("촬영 비율", color = Color.White)

                        // 3:4
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

                        // 9:16
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

                        // 1:1
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

                        //표 위치
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
// 사용자 지정 시 위치 조절 슬라이드
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


// =============================
// ✅ 저장 모드 설정
// =============================
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
                                        context.dataStore.edit { prefs: androidx.datastore.preferences.core.MutablePreferences ->
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

// =============================
// ✅ 카운터 자릿수 설정
// =============================
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

        // ✅ 중요: boundImageCapture는 "촬영 버튼 onClick"에서 사용해야 함
        // (촬영 버튼 코드는 다음 단계에서 붙이면 됨)
    }
} //CameraPreview 함수 끝
@Composable
private fun WatermarkPreviewOverlay(
    enabled: Boolean,
    request: com.example.dzlog.domain.model.CaptureRequest,
    previewContentRect: RectF?,
    dateFormat: String,
    timeFormat: String,
    timeSystem: TimeSystem
    ) {
    if (!enabled || previewContentRect == null) return

    val cells = resolveCellsFromTableTemplate(
        request.tableTemplate.cells,
        request.tableTemplate.rows,
        request.tableTemplate.cols
    )

    // TIME/DATE가 “현재 시각” 기준으로 보이도록 tick
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(dateFormat, timeFormat) {
        val unit = decideTickUnit(dateFormat, timeFormat)
        while (true) {
            val delayMs = computeNextDelayMillis(unit)
            delay(delayMs)
            now = Date()
        }
    }

    // 워터마크에 실제로 그릴 “표시용” 셀 값 치환
    val resolvedCells = remember(
        cells,
        now,
        request.counter,
        request.counterDigits,
        dateFormat,
        timeFormat,
        timeSystem
    ) {
        cells.map { cell ->
            val display = resolvePreviewCellText(
                cell = cell,
                now = now,
                nextCounter = request.counter,
                counterDigits = request.counterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
                timeSystem = timeSystem
            )
            // 표에는 value 영역에 표시(라벨은 기존 label 유지)
            cell.copy(valueText = display)
        }
    }


    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
    ) {
        drawIntoCanvas { canvas ->
            drawWatermarkTableOnCanvas(
                canvas = canvas.nativeCanvas,
                bounds = previewContentRect,
                cells = resolvedCells,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableEditorScreen(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
    ) {
    val context = LocalContext.current
    var selectedCellId by remember { mutableStateOf(templateState.cells.firstOrNull()?.cellId) }
    val scrollState = rememberScrollState()

    // =========================
    // Phase 1: 인플레이스 편집 상태
    // =========================
    var editingCellId by remember { mutableStateOf<String?>(null) }
    var editingValue by remember { mutableStateOf("") }
    var editingOriginalValue by remember { mutableStateOf("") }

    // 포커스/키보드 제어
    val keyboardController = LocalSoftwareKeyboardController.current
    val inlineFocusRequester = remember { FocusRequester() }

    // 안전장치
    var inlineHasFocusedOnce by remember { mutableStateOf(false) }
    var suppressNextCommit by remember { mutableStateOf(false) }

    // 편집 모드 진입 시 포커스 강제 요청
    LaunchedEffect(editingCellId) {
        inlineHasFocusedOnce = false
        if (editingCellId != null) {
            delay(30) // 레이아웃 안정화
            inlineFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // [Phase1-A1] 포커스 아웃/외부 클릭 저장을 위한 커밋 헬퍼
    fun commitInlineEditIfNeeded() {
        if (suppressNextCommit) {
            suppressNextCommit = false
            editingCellId = null   // ← 이 줄이 핵심
            return
        }

        val id = editingCellId ?: return
        val updated = updateCell(templateState, id) { c ->
            c.copy(valueText = editingValue)
        }
        onTemplateChange(updated)
        editingCellId = null
    }

    if (selectedCellId == null && templateState.cells.isNotEmpty()) {
        selectedCellId = templateState.cells.first().cellId
    }


    val hasUnassignedCells = templateState.cells.any { isCellUnassigned(it) }
    val selectedCell = templateState.cells.firstOrNull { it.cellId == selectedCellId }
    val hasGroup1 = templateState.cells.any { it.groupLevel == GroupLevel.G1 }
    // TableEditor용 저장 경로 미리보기
    val savePathPreview = remember(templateState.cells) {
        buildGalleryRelativePath(templateState.cells)
    }

    // --- Preview settings (1차: 하드코딩, 추후 프리셋 전역 설정으로 이관) ---
        val dateFormat = "yyyy.MM.dd"
        val timeFormat = "HH.mm.ss"
        val timeSystem = TimeSystem.H24

        // [각주 6] counterDigits는 촬영설정(DataStore) 값을 읽어 미리보기/실저장 규칙과 맞춘다.
        var previewCounterDigits by remember { mutableIntStateOf(COUNTER_DIGITS_DEFAULT) }
        LaunchedEffect(Unit) {
                runCatching {
                        val prefs = context.dataStore.data.first()
                        previewCounterDigits = clampCounterDigits(prefs[KEY_COUNTER_DIGITS] ?: COUNTER_DIGITS_DEFAULT)
                    }.onFailure {
                        previewCounterDigits = COUNTER_DIGITS_DEFAULT
                    }
            }

        // [각주 7] 폴더 기준 nextCounter 계산 (Camera에서 쓰는 CounterSync 재사용)
        var previewNextCounter by remember { mutableIntStateOf(1) }
        val counterSync = remember { CounterSyncImpl(MediaStoreSaverImpl()) }
        LaunchedEffect(savePathPreview, previewCounterDigits) {
                counterSync.syncNextCounter(
                        context = context,
                        relativePath = savePathPreview,
                        counterDigits = previewCounterDigits,
                        fnDelim = "_",
                        onResult = { next -> previewNextCounter = next },
                        onFail = { _ -> previewNextCounter = 1 }
                            )
            }
    // [Phase2] COUNTER 셀에 입력된 시작값이 있으면(정수) 그것을 우선 적용
        val counterOverride: Int? = remember(templateState.cells) {
            templateState.cells
                .firstOrNull { it.dataType == TableCellDataType.COUNTER }
                ?.valueText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.toIntOrNull()
                ?.takeIf { it >= 0 }
        }
        val effectiveNextCounter = counterOverride ?: previewNextCounter

        // [각주 8] timeFormat/dateFormat에 따라 갱신 단위를 자동 결정
        var previewNow by remember { mutableStateOf(Date()) }
        LaunchedEffect(dateFormat, timeFormat) {
                val unit = decideTickUnit(dateFormat, timeFormat)
                while (true) {
                        val delayMs = computeNextDelayMillis(unit)
                        delay(delayMs)
                        previewNow = Date()
                    }
            }

        // 파일명 미리보기: nextCounter + counterDigits 반영(우선 카운터만 정확히)
        val filenamePreview = buildDisplayName(
            cells = templateState.cells,
            counter = effectiveNextCounter,
            counterDigits = previewCounterDigits,
            fnDelim = "_",
            includeDate = false,
            includeTime = false
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("표 상세설정") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                     }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1EDE3))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFEAE0))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Save Path Preview", fontSize = 12.sp, color = Color.DarkGray)
                    Text(savePathPreview, color = Color.Black)
                    Spacer(Modifier.height(6.dp))
                    Text("Filename Preview", fontSize = 12.sp, color = Color.DarkGray)
                    Text(filenamePreview, color = Color.Black)
                }

                repeat(templateState.rows) { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(templateState.cols) { col ->
                            val cell = templateState.cells.firstOrNull {
                                it.rowIndex == row && it.colIndex == col
                            }
                            val cellUnassigned = cell?.let { isCellUnassigned(it) } ?: false
                            val isSelected = cell?.cellId == selectedCellId
                            val cellBackground = if (cellUnassigned) {
                                Color(0xFFFFE1E1)
                            } else {
                                Color(0xFFF7F4EE)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .padding(2.dp)
                                    .background(cellBackground)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF5B7F60) else Color(
                                            0xFFB5B0A8
                                        )
                                    )
                                    .clickable(enabled = cell != null) {
                                        if (cell == null) return@clickable

                                        // 다른 셀로 이동하면(편집 중이면) 자동 저장 후 이동
                                        if (selectedCellId != cell.cellId) {
                                            commitInlineEditIfNeeded()
                                            selectedCellId = cell.cellId
                                            return@clickable
                                        }

                                        // 같은 셀을 다시 클릭하면(TEXT/NUMBER + INPUT) 인플레이스 편집 진입
                                        val canInlineEdit =
                                            cell.kind == TableCellKind.INPUT &&
                                            (cell.dataType == TableCellDataType.TEXT ||
                                             cell.dataType == TableCellDataType.NUMBER ||
                                             cell.dataType == TableCellDataType.COUNTER)

                                        if (canInlineEdit) {
                                            editingCellId = cell.cellId
                                            editingValue = cell.valueText
                                            editingOriginalValue = cell.valueText
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell != null) {
                                    val display = resolvePreviewCellText(
                                        cell = cell,
                                        now = previewNow,
                                        nextCounter = effectiveNextCounter,
                                        counterDigits = previewCounterDigits,
                                        dateFormat = dateFormat,
                                        timeFormat = timeFormat,
                                        timeSystem = timeSystem
                                    )
                                    val isEditing = (editingCellId == cell.cellId)
                                    val canInlineEdit =
                                        cell.kind == TableCellKind.INPUT &&
                                        (cell.dataType == TableCellDataType.TEXT ||
                                         cell.dataType == TableCellDataType.NUMBER ||
                                         cell.dataType == TableCellDataType.COUNTER)

                                    if (isEditing && canInlineEdit) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val keyboardType = when (cell.dataType) {
                                                TableCellDataType.NUMBER -> KeyboardType.Decimal // 음수/소수 허용(필터링 없음)
                                                TableCellDataType.COUNTER -> KeyboardType.Number // counter는 정수
                                                else -> KeyboardType.Text
                                            }

                                            TextField(
                                                value = editingValue,
                                                onValueChange = { editingValue = it }, // 음수/소수 허용: 필터링하지 않음
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = keyboardType,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        commitInlineEditIfNeeded()
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(inlineFocusRequester)
                                                    .onFocusChanged { state ->
                                                        if (state.isFocused) {
                                                            inlineHasFocusedOnce = true
                                                        } else {
                                                            // "진짜로 포커스를 받았다가 잃을 때만" 자동 저장
                                                            if (inlineHasFocusedOnce) {
                                                                commitInlineEditIfNeeded()
                                                            }
                                                        }
                                                    }
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        // Cancel = 되돌림(변경 적용 전이므로 값 복구만 하고 종료)
                                                        suppressNextCommit = true

                                                        editingValue = editingOriginalValue
                                                        editingCellId = null
                                                    }
                                                ) { Text("Cancel") }

                                                TextButton(
                                                    onClick = {
                                                        val v = editingValue.trim()
                                                        if (cell.dataType == TableCellDataType.COUNTER) {
                                                            if (v.isNotEmpty() && v.toIntOrNull()?.let { it >= 0 } != true) {
                                                                // counter는 0 이상 정수만 허용 (원하면 Toast로 안내 가능)
                                                                return@TextButton
                                                            }
                                                        }
                                                        val updated = updateCell(templateState, cell.cellId) { c ->
                                                            c.copy(valueText = v)
                                                        }
                                                        onTemplateChange(updated)
                                                        editingCellId = null
                                                    }
                                                ) { Text("OK") }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = display.ifBlank { cell.label.ifBlank { "R${row + 1}C${col + 1}" } },
                                            color = Color.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val updated = addRow(templateState)
                        onTemplateChange(updated)
                    }
                ) { Text("+Row") }
                Button(
                    onClick = {
                        val updated = removeRow(templateState)
                        onTemplateChange(updated)
                        if (updated.cells.none { it.cellId == selectedCellId }) {
                            selectedCellId = updated.cells.firstOrNull()?.cellId
                        }
                    },
                    enabled = templateState.rows > 1
                ) { Text("-Row") }
                Button(
                    onClick = {
                        val updated = addColumn(templateState)
                        onTemplateChange(updated)
                    }
                ) { Text("+Col") }
                Button(
                    onClick = {
                        val updated = removeColumn(templateState)
                        onTemplateChange(updated)
                        if (updated.cells.none { it.cellId == selectedCellId }) {
                            selectedCellId = updated.cells.firstOrNull()?.cellId
                        }
                    },
                    enabled = templateState.cols > 1
                ) { Text("-Col") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Saved (stub)", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !hasUnassignedCells
                ) { Text("Save") }
                Button(onClick = onReset) { Text("Reset") }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F4EE))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Cell Detail Panel", fontSize = 16.sp, color = Color.Black)
                if (selectedCell == null) {
                    Text("셀을 선택하세요.", color = Color.DarkGray)
                } else {
                    Text("Kind", color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedCell.kind == TableCellKind.BASE,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.copy(kind = TableCellKind.BASE, fileNameInclude = false)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("BASE", color = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        RadioButton(
                            selected = selectedCell.kind == TableCellKind.INPUT,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.copy(kind = TableCellKind.INPUT)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("INPUT", color = Color.Black)
                    }

                    Text("Data Type", color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedCell.dataType == TableCellDataType.TEXT,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.copy(dataType = TableCellDataType.TEXT)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("TEXT", color = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        RadioButton(
                            selected = selectedCell.dataType == TableCellDataType.NUMBER,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.copy(dataType = TableCellDataType.NUMBER)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("NUMBER", color = Color.Black)
                    }

                    // [각주 9] 1차 확장: DATE/TIME/COUNTER 선택 UI 추가(값 입력은 아직 TEXT 필드로 남겨둠)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedCell.dataType == TableCellDataType.DATE,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.copy(dataType = TableCellDataType.DATE)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("DATE", color = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        RadioButton(
                            selected = selectedCell.dataType == TableCellDataType.TIME,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell -> cell.copy(dataType = TableCellDataType.TIME)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("TIME", color = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        RadioButton(
                            selected = selectedCell.dataType == TableCellDataType.COUNTER,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.copy(dataType = TableCellDataType.COUNTER)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("COUNTER", color = Color.Black)
                    }

                    Text("Group", color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GroupLevel.entries.forEach { level ->
                            val enabled = level != GroupLevel.G2 || hasGroup1
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedCell.groupLevel == level,
                                    enabled = enabled,
                                    onClick = {
                                        if (enabled) {
                                            val hadExisting = templateState.cells.any {
                                                it.cellId != selectedCell.cellId && it.groupLevel == level
                                            }
                                            val updated = updateGroupLevel(
                                                templateState = templateState,
                                                cellId = selectedCell.cellId,
                                                level = level
                                            )
                                            onTemplateChange(updated)
                                            if (hadExisting && level != GroupLevel.NONE) {
                                                Toast.makeText(
                                                    context,
                                                    "${level.name} moved to selected cell",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                )
                                Text(level.name, color = if (enabled) Color.Black else Color.LightGray)
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }

                    if (selectedCell.kind == TableCellKind.INPUT) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedCell.fileNameInclude,
                                onCheckedChange = { checked ->
                                    val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                        cell.copy(fileNameInclude = checked)
                                    }
                                    onTemplateChange(updated)
                                }
                            )
                            Text("Filename include", color = Color.Black)
                        }
                    }

                    val canInlineEditSelected =
                        selectedCell.kind == TableCellKind.INPUT &&
                                (selectedCell.dataType == TableCellDataType.TEXT || selectedCell.dataType == TableCellDataType.NUMBER)

                    if (!canInlineEditSelected) {
                        TextField(
                            value = selectedCell.valueText,
                            onValueChange = { value ->
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.copy(valueText = value)
                                }
                                onTemplateChange(updated)
                            },
                            singleLine = true,
                            label = { Text("Value") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "TEXT/NUMBER는 셀을 다시 눌러 바로 입력하세요.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }

                }
            }
        }
    }
}

private fun updateCell(
    templateState: TableTemplateState,
    cellId: String,
    transform: (TableCellState) -> TableCellState
): TableTemplateState {
    return templateState.copy(
        cells = templateState.cells.map { cell ->
            if (cell.cellId == cellId) {
                transform(cell)
            } else {
                cell
            }
        }
    )
}

private fun updateGroupLevel(
    templateState: TableTemplateState,
    cellId: String,
    level: GroupLevel
): TableTemplateState {
    return templateState.copy(
        cells = templateState.cells.map { cell ->
            when {
                cell.cellId == cellId -> cell.copy(groupLevel = level)
                level == GroupLevel.G1 && cell.groupLevel == GroupLevel.G1 -> cell.copy(groupLevel = GroupLevel.NONE)
                level == GroupLevel.G2 && cell.groupLevel == GroupLevel.G2 -> cell.copy(groupLevel = GroupLevel.NONE)
                else -> cell
            }
        }
    )
}

private fun addRow(templateState: TableTemplateState): TableTemplateState {
    val newRowIndex = templateState.rows
    val newCells = (0 until templateState.cols).map { col ->
        TableCellState(rowIndex = newRowIndex, colIndex = col)
    }
    return templateState.copy(
        rows = templateState.rows + 1,
        cells = templateState.cells + newCells
    )
}

private fun removeRow(templateState: TableTemplateState): TableTemplateState {
    val lastRowIndex = templateState.rows - 1
    return templateState.copy(
        rows = templateState.rows - 1,
        cells = templateState.cells.filterNot { it.rowIndex == lastRowIndex }
    )
}

private fun addColumn(templateState: TableTemplateState): TableTemplateState {
    val newColIndex = templateState.cols
    val newCells = (0 until templateState.rows).map { row ->
        TableCellState(rowIndex = row, colIndex = newColIndex)
    }
    return templateState.copy(
        cols = templateState.cols + 1,
        cells = templateState.cells + newCells
    )
}

private fun removeColumn(templateState: TableTemplateState): TableTemplateState {
    val lastColIndex = templateState.cols - 1
    return templateState.copy(
        cols = templateState.cols - 1,
        cells = templateState.cells.filterNot { it.colIndex == lastColIndex }
    )
}

private fun isCellUnassigned(cell: TableCellState): Boolean {
    return cell.label.isBlank()
}

private fun defaultTableTemplateState(): TableTemplateState {
    val rows = 2
    val cols = 4
    return TableTemplateState(
        rows = rows,
        cols = cols,
        cells = listOf(
            TableCellState(
                rowIndex = 0,
                colIndex = 0,
                kind = TableCellKind.INPUT,
                valueText = "T1",
                fileNameInclude = true,
                groupLevel = GroupLevel.G1,
                label = "Treatment"
            ),
            TableCellState(
                rowIndex = 0,
                colIndex = 1,
                kind = TableCellKind.INPUT,
                valueText = "S1",
                fileNameInclude = true,
                groupLevel = GroupLevel.G2,
                label = "Strain"
            ),
            TableCellState(
                rowIndex = 0,
                colIndex = 2,
                kind = TableCellKind.INPUT,
                valueText = "DZlog",
                fileNameInclude = true,
                label = "Prefix"
            ),
            TableCellState(
                rowIndex = 0,
                colIndex = 3,
                kind = TableCellKind.INPUT,
                valueText = "B3",
                fileNameInclude = false,
                label = "Batch"
            ),
            TableCellState(
                rowIndex = 1,
                colIndex = 0,
                kind = TableCellKind.INPUT,
                valueText = "",
                fileNameInclude = false,
                label = "Note"
            ),
            TableCellState(
                rowIndex = 1,
                colIndex = 1,
                kind = TableCellKind.INPUT,
                valueText = "",
                fileNameInclude = false,
                label = "Sample"
            ),
            TableCellState(
                rowIndex = 1,
                colIndex = 2,
                kind = TableCellKind.INPUT,
                valueText = "",
                fileNameInclude = false,
                label = "Memo 1"
            ),
            TableCellState(
                rowIndex = 1,
                colIndex = 3,
                kind = TableCellKind.INPUT,
                valueText = "",
                fileNameInclude = false,
                label = "Memo 2"

            )
        )
    )
}

// =========================================================
// PART 4 — Setup Wizard Overlay (Settings UI) + Aspect(4:3/16:9/1:1) 저장
// =========================================================

private suspend fun persistCaptureAspect(context: Context, aspect: CaptureAspect) {
    runCatching {
        context.dataStore.edit { prefs ->
            prefs[KEY_CAPTURE_ASPECT] = aspect.v
        }
    }
}