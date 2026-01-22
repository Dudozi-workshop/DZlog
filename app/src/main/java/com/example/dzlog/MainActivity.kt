@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog

// =========================================================
// Imports (FOUNDATION ONLY)
// =========================================================

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Rational
import android.view.Surface
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LifecycleOwner
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
import com.example.dzlog.domain.naming.buildDisplayName
import com.example.dzlog.domain.naming.resolveGroupValue
import com.example.dzlog.watermark.WatermarkRendererImpl
import com.example.dzlog.watermark.renderWatermarkForRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// =========================================================
// App Navigation
// =========================================================

enum class AppScreen {
    HOME,
    CAMERA,
    TABLE_EDITOR
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
val KEY_HIDE_WATERMARK_GALLERY = booleanPreferencesKey("hide_watermark_gallery")
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

fun clampCounterDigits(v: Int) = v.coerceIn(2, 6)

fun formatCounter(counter: Int, digits: Int): String {
    val d = clampCounterDigits(digits)
    return counter.toString().padStart(d, '0')
}

// =========================================================
// Save / Orientation
// =========================================================

enum class OrientationMode(val v: Int, val label: String) {
    PORTRAIT_LOCK(0, "세로 고정"),
    AUTO_ROTATE(1, "자동 회전");

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
    var startWithWizard by remember { mutableStateOf(false) }
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

    when (screen) {
        AppScreen.HOME -> HomeScreen(
            onOpenSettings = {
                startWithWizard = true
                screen = AppScreen.CAMERA
            },
            onOpenTableEditor = {
                previousScreen = screen
                screen = AppScreen.TABLE_EDITOR
            },
            onStartCamera = {
                startWithWizard = false
                screen = AppScreen.CAMERA
            }
        )

        AppScreen.CAMERA -> {
            // ✅ CameraScreen은 Part 3에서 제공됨
            CameraScreen(
                startWithWizard = startWithWizard,
                onExitToHome = { screen = AppScreen.HOME },
                orientationMode = orientationMode,
                setOrientationMode = { orientationMode = it },
                tableTemplateState = tableTemplateState,
                onTableTemplateChange = tableTemplateViewModel::update,
                onOpenTableEditor = {
                    previousScreen = screen
                    screen = AppScreen.TABLE_EDITOR
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


// =========================================================
// PART 3 — CameraScreen (Permission Gate) / CameraPreview (CameraX Bind)
// =========================================================

// ---------------------------------------------------------
// CameraScreen (Permission Gate)
// ---------------------------------------------------------

@Composable
fun CameraScreen(
    startWithWizard: Boolean,
    onExitToHome: () -> Unit,
    orientationMode: OrientationMode,
    setOrientationMode: (OrientationMode) -> Unit,
    tableTemplateState: TableTemplateState,
    onTableTemplateChange: (TableTemplateState) -> Unit,
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
                startWithWizard = startWithWizard,
                onExitToHome = onExitToHome,
                orientationMode = orientationMode,
                setOrientationMode = setOrientationMode,
                tableTemplateState = tableTemplateState,
                onTableTemplateChange = onTableTemplateChange,
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

@Composable
fun CameraPreview(
    startWithWizard: Boolean,
    onExitToHome: () -> Unit,
    orientationMode: OrientationMode,
    setOrientationMode: (OrientationMode) -> Unit,
    tableTemplateState: TableTemplateState,
    onTableTemplateChange: (TableTemplateState) -> Unit,
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
    var counterDigits by remember { mutableStateOf(4) } // 기본 0001 형태

    // ✅ 추가: 촬영 카운터(파일명/표에 들어갈 숫자)
    var counter by remember { mutableStateOf(1) }

    // ✅ 추가: 워터마크에 들어갈 값

    // ✅ Wizard 시작 여부
    var showWizard by remember { mutableStateOf(startWithWizard) }

    //프리뷰 표시 설정
    var showWmPreview by remember { mutableStateOf(true) } // 기본 켜짐(원하면 false)
    //표 위치 변경
    var wmTableAnchor by remember {
        mutableStateOf(com.example.dzlog.domain.model.WatermarkTableAnchor.BOTTOM_RIGHT)
    }    // 표 크기 변경
    var wmTableWidthRatio by remember { mutableStateOf(40) }
    var wmTableHeightRatio by remember { mutableStateOf(20) }
    var wmOffsetXRatio by remember { mutableStateOf(0) } // 0~100
    var wmOffsetYRatio by remember { mutableStateOf(0) } // 0~100
// 표 투명도
    var wmBgAlpha by remember { mutableIntStateOf(80) } // 0~255 (기본 80 추천)
    // 글씨 크기 변경
    var wmLabelScale by remember { mutableStateOf(100) } // 60~160
    var wmValueScale by remember { mutableStateOf(100) } // 60~160
    val fnDelim = "_"

    val tableCells = tableTemplateState.cells

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
            counterDigits = (prefs[KEY_COUNTER_DIGITS] ?: 4).coerceIn(1, 6)

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

                    WatermarkPreviewBitmapOverlay(
                        enabled = showWmPreview,
                        request = previewRequest,
                        imageCapture = boundImageCapture
                    )
                }
            }
        }

        // =====================================================
        // 상단 UI(테스트용) - 지금은 설정만
        // =====================================================
        Box(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .background(Color(0x66000000))
                .clickable {
                    showWizard = true
                    Toast.makeText(context, "설정 열기", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("설정", color = Color.White)
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
                                counter = counter + 1   // ✅ 이 줄이 없으면 평생 001
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
                                onClick = { captureAspect = CaptureAspect.R3_4 }
                            )
                            Text("3:4", color = Color.White)
                        }

                        // 9:16
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (captureAspect == CaptureAspect.R9_16),
                                onClick = { captureAspect = CaptureAspect.R9_16 }
                            )
                            Text("9:16", color = Color.White)
                        }

                        // 1:1
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (captureAspect == CaptureAspect.R1_1),
                                onClick = { captureAspect = CaptureAspect.R1_1 }
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
                                    val next = (counterDigits - 1).coerceIn(1, 6)
                                    counterDigits = next
                                    scope.launch { context.dataStore.edit { it[KEY_COUNTER_DIGITS] = next } }
                                }
                            ) { Text("-", color = Color.White) }

                            Spacer(Modifier.width(12.dp))
                            Text(counterDigits.toString(), color = Color.White, fontSize = 18.sp)
                            Spacer(Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    val next = (counterDigits + 1).coerceIn(1, 6)
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
private fun WatermarkPreviewBitmapOverlay(
    enabled: Boolean,
    request: com.example.dzlog.domain.model.CaptureRequest,
    imageCapture: ImageCapture?
) {
    if (!enabled) return

    val watermarkRenderer = remember { WatermarkRendererImpl() }
    var captureResolution by remember { mutableStateOf<IntSize?>(null) }
    LaunchedEffect(imageCapture) {
        captureResolution = resolveRenderSize(imageCapture)
        if (captureResolution == null && imageCapture != null) {
            repeat(3) {
                kotlinx.coroutines.delay(120)
                captureResolution = resolveRenderSize(imageCapture)
                if (captureResolution != null) return@LaunchedEffect
            }
        }
    }
    // ✅ 프리뷰용 비트맵 캐시
    var previewBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // ✅ 설정값이 바뀔 때만 다시 렌더 (너무 자주면 렉 -> 살짝 딜레이)
    LaunchedEffect(
        captureResolution?.width,
        captureResolution?.height,
        request.watermark.anchor,
        request.watermark.offsetXRatio,
        request.watermark.offsetYRatio,
        request.watermark.tableWidthRatio,
        request.watermark.tableHeightRatio,
        request.watermark.tableBgAlpha,
        request.watermark.labelScale,
        request.watermark.valueScale,
        request.watermark.showLabel,
        request.tableTemplate,
        request.counterDigits,
        request.counter
    ) {
        kotlinx.coroutines.delay(120)
        // ✅ 슬라이더 드래그 시 과도 렌더 방지
        val resolution = captureResolution ?: return@LaunchedEffect

        // ✅ 프리뷰는 "빈 원본 이미지" 위에 실제 워터마크 렌더를 그대로 올림
        // (실제 저장과 동일한 renderWatermarkForRequest 사용)
        val src = android.graphics.Bitmap.createBitmap(
            resolution.width,
            resolution.height,
            android.graphics.Bitmap.Config.ARGB_8888
        ).apply {
            eraseColor(android.graphics.Color.TRANSPARENT)
        }



        previewBmp = renderWatermarkForRequest(
            renderer = watermarkRenderer,
            originalBmp = src,
            request = request
        )
    }

    val bmp = previewBmp ?: return

    // ✅ 화면 전체를 덮지 말고 "표 영역만" 보이게: 투명 배경 비트맵 그대로 overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
    ) {
        androidx.compose.foundation.Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            alignment = Alignment.Center
        )
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
        android.util.Log.d("DZlog", "BIND aspect=${aspect.label} w/h=${aspect.w}/${aspect.h}")

        val cameraAspectRatio = aspect.toCameraXAspectRatio()

        val previewBuilder = Preview.Builder()
            .setTargetRotation(rotation)
        if (cameraAspectRatio != null) {
            previewBuilder.setTargetAspectRatio(cameraAspectRatio)
        }
        val preview = previewBuilder.build()
            .apply { setSurfaceProvider(previewView.surfaceProvider) }

        val imageCaptureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
        if (cameraAspectRatio != null) {
            imageCaptureBuilder.setTargetAspectRatio(cameraAspectRatio)
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

private fun resolveRenderSize(imageCapture: ImageCapture?): IntSize? {
    val resolution = imageCapture?.resolutionInfo?.resolution ?: return null
    val degrees = surfaceRotationToDegrees(imageCapture.targetRotation)
    return if (degrees % 180 == 0) {
        IntSize(resolution.width, resolution.height)
    } else {
        IntSize(resolution.height, resolution.width)
    }
}

private fun surfaceRotationToDegrees(rotation: Int): Int {
    return when (rotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
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

    if (selectedCellId == null && templateState.cells.isNotEmpty()) {
        selectedCellId = templateState.cells.first().cellId
    }


    val hasUnassignedCells = templateState.cells.any { isCellUnassigned(it) }
    val selectedCell = templateState.cells.firstOrNull { it.cellId == selectedCellId }

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
                                        color = if (isSelected) Color(0xFF5B7F60) else Color(0xFFB5B0A8)
                                    )
                                    .clickable(enabled = cell != null) {
                                        selectedCellId = cell?.cellId
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (cell.label.isNotBlank()) {
                                            Text(
                                                text = cell.label,
                                                color = Color.Black,
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (cell.valueText.isNotBlank()) {
                                            Text(
                                                text = cell.valueText,
                                                color = Color.DarkGray,
                                                fontSize = 11.sp
                                            )
                                        }
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

                    Text("Group", color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GroupLevel.entries.forEach { level ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedCell.groupLevel == level,
                                    onClick = {
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
                                )
                                Text(level.name, color = Color.Black)
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedCell.fileNameInclude,
                            enabled = selectedCell.kind == TableCellKind.INPUT,
                            onCheckedChange = { checked ->
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.copy(
                                        fileNameInclude = if (cell.kind == TableCellKind.INPUT) {
                                            checked
                                        } else {
                                            false
                                        }
                                    )
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("Filename include", color = Color.Black)
                    }

                    TextField(
                        value = selectedCell.label,
                        onValueChange = { value ->
                            val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                cell.copy(label = value)
                            }
                            onTemplateChange(updated)
                        },
                        singleLine = true,
                        label = { Text("Label") },
                        modifier = Modifier.fillMaxWidth()
                    )

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

            )
        )
    )
}

// =========================================================
// PART 4 — Setup Wizard Overlay (Settings UI) + Aspect(4:3/16:9/1:1) 저장
// =========================================================

@Composable
fun SetupWizardOverlay(
    context: Context,
    showWizard: Boolean,
    onClose: () -> Unit,

    // --- Aspect (what-you-see-is-what-you-get 핵심) ---
    captureAspect: CaptureAspect,
    setCaptureAspect: (CaptureAspect) -> Unit,

    // --- Guide toggle (표 위치 가이드 on/off) ---
    wmGuideEnabled: Boolean,
    setWmGuideEnabled: (Boolean) -> Unit,

    // --- Orientation mode (이미 AppRoot에서 적용 중이지만 설정 UI는 여기서) ---
    orientationMode: OrientationMode,
    setOrientationMode: (OrientationMode) -> Unit
) {
    if (!showWizard) return

    val tabTitles = listOf("필수", "촬영비율", "가이드/고급")
    var tabIndex by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .background(Color(0xFFF7F7F7))
                .padding(12.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DZlog 설정", fontSize = 18.sp, color = Color.Black)
                    TextButton(onClick = onClose) { Text("닫기") }
                }

                Spacer(Modifier.height(8.dp))

                TabRow(selectedTabIndex = tabIndex) {
                    tabTitles.forEachIndexed { idx, title ->
                        Tab(
                            selected = tabIndex == idx,
                            onClick = { tabIndex = idx },
                            text = { Text(title, fontSize = 12.sp, color = Color.Black) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scroll)
                        .padding(bottom = 10.dp)
                ) {
                    when (tabIndex) {
                        0 -> {
                            Text("기본", color = Color.Black)
                            Spacer(Modifier.height(8.dp))

                            Text("화면 방향", color = Color.Black)
                            Spacer(Modifier.height(6.dp))

                            listOf(
                                OrientationMode.PORTRAIT_LOCK to "세로 고정(권장)",
                                OrientationMode.AUTO_ROTATE to "자동 회전 허용"
                            ).forEach { (mode, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { setOrientationMode(mode) }
                                        .padding(vertical = 6.dp)
                                ) {
                                    RadioButton(
                                        selected = orientationMode == mode,
                                        onClick = { setOrientationMode(mode) }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, color = Color.Black)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "※ 비율/가이드는 다음 탭에서 설정합니다.",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }

                        1 -> {
                            Text("촬영 비율", color = Color.Black)
                            Spacer(Modifier.height(8.dp))

                            Text(
                                "what-you-see-is-what-you-get",
                                color = Color.DarkGray,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "이 설정은 '프리뷰 액자 비율'과 '실제 저장(크롭) 비율'을 동일하게 맞추기 위한 기준입니다.",
                                color = Color.Black,
                                fontSize = 13.sp
                            )

                            Spacer(Modifier.height(12.dp))

                            listOf(
                                CaptureAspect.R3_4 to "3:4 (기본)",
                                CaptureAspect.R9_16 to "9:16",
                                CaptureAspect.R1_1 to "1:1"
                            ).forEach { (aspect, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            setCaptureAspect(aspect)
                                            persistCaptureAspect(context, aspect)
                                        }
                                        .padding(vertical = 6.dp)
                                ) {
                                    RadioButton(
                                        selected = captureAspect == aspect,
                                        onClick = {
                                            setCaptureAspect(aspect)
                                            persistCaptureAspect(context, aspect)
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, color = Color.Black)
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                "※ 비율을 바꾸면 프리뷰가 즉시 바뀌며, 저장되는 사진도 같은 규칙으로 크롭됩니다.",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }

                        2 -> {
                            Text("가이드/고급", color = Color.Black)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { setWmGuideEnabled(!wmGuideEnabled) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = wmGuideEnabled,
                                    onCheckedChange = { setWmGuideEnabled(it) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("프리뷰에 표 위치 가이드 표시", color = Color.Black)
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "※ 가이드의 실제 크기/위치는 Part 5에서 '단일 가이드'로 정리해 맞춥니다.",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                // 하단 저장/닫기
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onClose) { Text("닫기") }
                }
            }
        }
    }
}

private fun persistCaptureAspect(context: Context, aspect: CaptureAspect) {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    scope.launch {
        try {
            context.dataStore.edit { prefs ->
                prefs[KEY_CAPTURE_ASPECT] = aspect.v
            }
        } catch (_: Exception) {
        }
    }
}