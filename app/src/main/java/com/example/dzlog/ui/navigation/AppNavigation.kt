package com.example.dzlog.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dzlog.data.preferences.KEY_ORIENTATION_MODE
import com.example.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.example.dzlog.data.preferences.OrientationMode
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.data.template.defaultTableTemplateState
import com.example.dzlog.data.template.tableTemplateStateFromJson
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.data.datastore.AppSettingsStore
import com.example.dzlog.ui.camera.CameraScreen
import com.example.dzlog.ui.home.HomeScreen
import com.example.dzlog.ui.home.SettingsScreen
import com.example.dzlog.ui.settings.CaptureSettingsScreen
import com.example.dzlog.ui.log.LogG1Screen
import com.example.dzlog.ui.log.LogG2Screen
import com.example.dzlog.ui.log.LogGridScreen
import com.example.dzlog.ui.log.LogViewerScreen
import com.example.dzlog.ui.table.TableEditorScreen
import kotlinx.coroutines.flow.first


enum class AppScreen {
    HOME,
    CAMERA,
    TABLE_EDITOR,
    SETTINGS,
    CAPTURE_SETTINGS,
    LOG_G1,
    LOG_G2,
    LOG_GRID,
    LOG_VIEWER
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

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var previousScreen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedG1 by remember { mutableStateOf<String?>(null) }
    var selectedG2 by remember { mutableStateOf<String?>(null) }

    // 로그 화면 상태(그리드/뷰어 공통)
    var logItems by remember { mutableStateOf(emptyList<com.example.dzlog.domain.model.MediaImageItem>()) }
    var logIsSelectionMode by remember { mutableStateOf(false) }
    var logSelectedIds by remember { mutableStateOf(setOf<Long>()) }
    var logViewerStartIndex by remember { mutableStateOf(0) }
    var orientationMode by remember { mutableStateOf(OrientationMode.PORTRAIT_LOCK) }
    val tableTemplateViewModel: TableTemplateViewModel = viewModel()
    val tableTemplateState = tableTemplateViewModel.tableTemplateState

    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val appSettings by AppSettingsStore.flow(context).collectAsState(
        initial = com.example.dzlog.data.datastore.AppSettings(
            saveMode = com.example.dzlog.domain.model.SaveMode.BOTH,
            continuousPreviewMode = com.example.dzlog.domain.model.ContinuousPreviewMode.OFF,
            counterPadding = 0,
            counterSuffixEnabled = true,
            resetCounterOnPathChange = true,
            toastEnabled = true,
            hapticEnabled = true,
            blankWarningEnabled = true,
            usedCounterValuesJson = null
        )
    )

    var lastBackPressedMs by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        orientationMode = try {
            val prefs = context.dataStore.data.first()
            OrientationMode.from(prefs[KEY_ORIENTATION_MODE] ?: OrientationMode.PORTRAIT_LOCK.v)
        } catch (_: Exception) {
            OrientationMode.PORTRAIT_LOCK
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            val prefs = context.dataStore.data.first()
            val json = prefs[KEY_TABLE_TEMPLATE_JSON]
            if (!json.isNullOrBlank()) {
                tableTemplateStateFromJson(json)?.let { loaded ->
                    tableTemplateViewModel.update(loaded)
                }
            }
        }
    }

    LaunchedEffect(orientationMode) {
        val a = activity ?: return@LaunchedEffect
        a.requestedOrientation = when (orientationMode) {
            OrientationMode.PORTRAIT_LOCK ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            OrientationMode.AUTO_ROTATE ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun navigateTo(target: AppScreen) {
        previousScreen = screen
        screen = target
    }

    fun resetLogSelectionState() {
        logIsSelectionMode = false
        logSelectedIds = emptySet()
    }

    BackHandler(enabled = true) {
        // 1) 멀티 선택 모드라면: 먼저 선택 모드 종료
        if (logIsSelectionMode && (screen == AppScreen.LOG_GRID || screen == AppScreen.LOG_VIEWER)) {
            resetLogSelectionState()
            return@BackHandler
        }

        // 2) 기본 내비게이션(화면 기준)
        when (screen) {
            AppScreen.HOME -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedMs < 1500L) {
                    activity?.finish()
                } else {
                    lastBackPressedMs = now
                    if (appSettings.toastEnabled) {
                        Toast.makeText(context, "한 번 더 누르면 종료", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            AppScreen.SETTINGS -> screen = AppScreen.HOME
            AppScreen.CAPTURE_SETTINGS -> screen = AppScreen.SETTINGS
            AppScreen.TABLE_EDITOR -> screen = previousScreen
            AppScreen.CAMERA -> screen = AppScreen.HOME

            AppScreen.LOG_G1 -> screen = AppScreen.HOME
            AppScreen.LOG_G2 -> screen = AppScreen.LOG_G1
            AppScreen.LOG_GRID -> screen = AppScreen.LOG_G2
            AppScreen.LOG_VIEWER -> screen = AppScreen.LOG_GRID
        }
    }

    when (screen) {
        AppScreen.HOME -> HomeScreen(
            tableTemplateState = tableTemplateState,
            onOpenSettings = { navigateTo(AppScreen.SETTINGS) },
            onOpenTableEditor = { navigateTo(AppScreen.TABLE_EDITOR) },
            onStartCamera = { navigateTo(AppScreen.CAMERA) },
            onOpenLog = {
                selectedG1 = null
                selectedG2 = null
                navigateTo(AppScreen.LOG_G1)
            },
            onOpenLogFor = { g1, g2 ->
                selectedG1 = g1
                selectedG2 = g2
                logItems = emptyList()
                resetLogSelectionState()
                navigateTo(AppScreen.LOG_GRID)
            }
        )

        AppScreen.CAMERA -> {
            CameraScreen(
                onExitToHome = { navigateTo(AppScreen.HOME) },
                tableTemplateState = tableTemplateState,
                onTemplateChange = tableTemplateViewModel::update,
                onOpenTableEditor = { navigateTo(AppScreen.TABLE_EDITOR) }
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
            tableTemplateStateProvider = { tableTemplateState },
            onBack = { screen = AppScreen.HOME },
            onOpenTableDetail = { navigateTo(AppScreen.TABLE_EDITOR) },
            onOpenCaptureSettings = { navigateTo(AppScreen.CAPTURE_SETTINGS) }
        )

        AppScreen.CAPTURE_SETTINGS -> {
            CaptureSettingsScreen(onBack = { screen = AppScreen.SETTINGS })
        }

        AppScreen.LOG_G1 -> {
            LogG1Screen(
                onBack = { screen = AppScreen.HOME },
                onSelectG1 = { g1 ->
                    selectedG1 = g1
                    selectedG2 = null
                    navigateTo(AppScreen.LOG_G2)
                }
            )
        }

        AppScreen.LOG_G2 -> {
            val g1 = selectedG1
            if (g1 == null) {
                // 방어: 상태가 없으면 G1 화면으로 복귀
                screen = AppScreen.LOG_G1
            } else {
                LogG2Screen(
                    g1 = g1,
                    onBack = { screen = AppScreen.LOG_G1 },
                    onSelectG2 = { g2 ->
                        selectedG2 = g2
                        logItems = emptyList()
                        resetLogSelectionState()
                        navigateTo(AppScreen.LOG_GRID)
                    }
                )
            }
        }

        AppScreen.LOG_GRID -> {
            val g1 = selectedG1
            val g2 = selectedG2
            if (g1 == null || g2 == null) {
                screen = AppScreen.LOG_G1
            } else {
                LogGridScreen(
                    g1 = g1,
                    g2 = g2,
                    items = logItems,
                    isSelectionMode = logIsSelectionMode,
                    selectedIds = logSelectedIds,
                    onItemsLoaded = { loaded -> logItems = loaded },
                    onBack = {
                        // 그룹을 빠져나갈 때는 선택 상태를 초기화
                        resetLogSelectionState()
                        screen = AppScreen.LOG_G2
                    },
                    onOpenViewer = { startIndex ->
                        logViewerStartIndex = startIndex
                        navigateTo(AppScreen.LOG_VIEWER)
                    },
                    onToggleSelection = { id ->
                        logSelectedIds = if (logSelectedIds.contains(id)) logSelectedIds - id else logSelectedIds + id
                    },
                    onEnterSelectionWith = { id ->
                        logIsSelectionMode = true
                        logSelectedIds = logSelectedIds + id
                    },
                    onExitSelection = { resetLogSelectionState() },
                    onSelectAll = {
                        logIsSelectionMode = true
                        logSelectedIds = logItems.map { it.id }.toSet()
                    }
                )
            }
        }

        AppScreen.LOG_VIEWER -> {
            val g1 = selectedG1
            val g2 = selectedG2
            if (g1 == null || g2 == null) {
                screen = AppScreen.LOG_G1
            } else {
                LogViewerScreen(
                    g1 = g1,
                    g2 = g2,
                    items = logItems,
                    startIndex = logViewerStartIndex,
                    isSelectionMode = logIsSelectionMode,
                    selectedIds = logSelectedIds,
                    onBack = { screen = AppScreen.LOG_GRID },
                    onEnterSelectionWith = { id ->
                        logIsSelectionMode = true
                        logSelectedIds = logSelectedIds + id
                    },
                    onToggleSelection = { id ->
                        logSelectedIds = if (logSelectedIds.contains(id)) logSelectedIds - id else logSelectedIds + id
                    },
                    onExitSelection = { resetLogSelectionState() },
                    onSelectAll = {
                        logIsSelectionMode = true
                        logSelectedIds = logItems.map { it.id }.toSet()
                    }
                )
            }
        }
    }
}
