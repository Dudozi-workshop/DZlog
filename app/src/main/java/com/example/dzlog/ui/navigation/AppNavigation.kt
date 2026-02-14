@file:Suppress("UNUSED_VALUE", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead",
    "AssignedValueIsNeverRead", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead",
    "AssignedValueIsNeverRead", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead",
    "AssignedValueIsNeverRead", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead",
    "AssignedValueIsNeverRead", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead",
    "AssignedValueIsNeverRead", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead",
    "AssignedValueIsNeverRead", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead",
    "AssignedValueIsNeverRead", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead",
    "AssignedValueIsNeverRead", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead",
    "AssignedValueIsNeverRead", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead",
    "AssignedValueIsNeverRead", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead",
    "AssignedValueIsNeverRead", "AssignedValueIsNeverRead", "AssignedValueIsNeverRead"
)

package com.example.dzlog.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dzlog.data.datastore.AppSettingsStore
import com.example.dzlog.data.preferences.KEY_ORIENTATION_MODE
import com.example.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.example.dzlog.data.preferences.OrientationMode
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.data.template.defaultTableTemplateState
import com.example.dzlog.data.template.tableTemplateStateFromJson
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.feature.settings.ui.SettingsScreen
import com.example.dzlog.ui.camera.CameraScreen
import com.example.dzlog.ui.home.HomeScreen
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
    ALBUM_G1,
    ALBUM_G2,
    ALBUM_GRID,
    ALBUM_VIEWER
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

// 앨범(G1/G2/그리드/뷰어) 상태
    var selectedG1 by remember { mutableStateOf<String?>(null) }
    var selectedG2 by remember { mutableStateOf<String?>(null) }
    var gridItems by remember { mutableStateOf<List<com.example.dzlog.domain.model.MediaImageItem>>(emptyList()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var viewerStartIndex by remember { mutableIntStateOf(0) }

    // 기존 상태

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
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
            toastEnabled = true,
            hapticEnabled = true,
            blankWarningEnabled = true,
        )
    )

    var lastBackPressedMs by remember { mutableLongStateOf(0L) }

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

    BackHandler(enabled = true) {
        // 기본 내비게이션(화면 기준)
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
            AppScreen.TABLE_EDITOR -> screen = previousScreen
            AppScreen.CAMERA -> screen = AppScreen.HOME

            AppScreen.ALBUM_GRID -> {
                if (isSelectionMode) {
                    isSelectionMode = false
                    selectedIds = emptySet()
                } else {
                    screen = AppScreen.ALBUM_G2
                }
            }
            AppScreen.ALBUM_VIEWER -> screen = AppScreen.ALBUM_GRID
            AppScreen.ALBUM_G1 -> screen = AppScreen.HOME
        AppScreen.ALBUM_G2 -> screen = AppScreen.ALBUM_G1
        }
    }

    when (screen) {
        AppScreen.HOME -> HomeScreen(
            tableTemplateState = tableTemplateState,
            onOpenSettings = { navigateTo(AppScreen.SETTINGS) },
            onStartCamera = { navigateTo(AppScreen.CAMERA) },
            onOpenTableEditor = { navigateTo(AppScreen.TABLE_EDITOR) },
            onOpenAlbum = {
                selectedG1 = null
                selectedG2 = null
                gridItems = emptyList()
                isSelectionMode = false
                selectedIds = emptySet()
                viewerStartIndex = 0
                navigateTo(AppScreen.ALBUM_G1)
            },
            onOpenRecentCaptureGrid = { g1, g2, startIndex ->
                selectedG1 = g1
                selectedG2 = g2
                gridItems = emptyList()
                isSelectionMode = false
                selectedIds = emptySet()
                viewerStartIndex = startIndex
                navigateTo(AppScreen.ALBUM_GRID)
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
            onOpenTableDetail = { navigateTo(AppScreen.TABLE_EDITOR) }
        )
        AppScreen.ALBUM_G1 -> {
            LogG1Screen(
                onBack = { screen = AppScreen.HOME },
                onSelectG1 = { g1 ->
                    selectedG1 = g1
                    selectedG2 = null
                    gridItems = emptyList()
                    isSelectionMode = false
                    selectedIds = emptySet()
                    screen = AppScreen.ALBUM_G2
                }
            )
        }

        AppScreen.ALBUM_G2 -> {
            val g1 = selectedG1
            if (g1 == null) {
                screen = AppScreen.ALBUM_G1
            } else {
                LogG2Screen(
                    g1 = g1,
                    onBack = {
                        selectedG2 = null
                        screen = AppScreen.ALBUM_G1
                    },
                    onSelectG2 = { g2 ->
                        selectedG2 = g2
                        gridItems = emptyList()
                        isSelectionMode = false
                        selectedIds = emptySet()
                        screen = AppScreen.ALBUM_GRID
                    }
                )
            }
        }
        // 📸 앨범 내 사진 목록 화면
        AppScreen.ALBUM_GRID -> {
            val g1 = selectedG1
            val g2 = selectedG2
            if (g1 == null || g2 == null) {
                screen = if (g1 == null) AppScreen.ALBUM_G1 else AppScreen.ALBUM_G2
            } else {
                LogGridScreen(
                    g1 = g1,
                    g2 = g2,
                    items = gridItems,
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedIds,
                    onItemsLoaded = { gridItems = it },
                    onOpenViewer = { idx ->
                        viewerStartIndex = idx
                        screen = AppScreen.ALBUM_VIEWER
                    },
                    onToggleSelection = { id ->
                        selectedIds =
                            if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                        if (selectedIds.isEmpty()) isSelectionMode = false
                    },
                    onEnterSelectionWith = { id ->
                        isSelectionMode = true
                        selectedIds = selectedIds + id
                    },
                    onExitSelection = {
                        isSelectionMode = false
                        selectedIds = emptySet()
                    },
                    onSelectAll = {
                        isSelectionMode = true
                        selectedIds = gridItems.map { it.id }.toSet()
                    }
                )
            }
        }

// 📷 앨범 내 개별 사진 뷰어 화면
        AppScreen.ALBUM_VIEWER -> {
            val g1 = selectedG1
            val g2 = selectedG2
            if (g1 == null || g2 == null) {
                screen = if (g1 == null) AppScreen.ALBUM_G1 else AppScreen.ALBUM_G2
            } else {
                LogViewerScreen(
                    g1 = g1,
                    g2 = g2,
                    items = gridItems,
                    startIndex = viewerStartIndex,
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedIds,
                    onBack = { screen = AppScreen.ALBUM_GRID },
                    onEnterSelectionWith = { id ->
                        isSelectionMode = true
                        selectedIds = selectedIds + id
                    },
                    onToggleSelection = { id ->
                        selectedIds =
                            if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                        if (selectedIds.isEmpty()) isSelectionMode = false
                    },
                    onExitSelection = {
                        isSelectionMode = false
                        selectedIds = emptySet()
                    },
                    onSelectAll = {
                        isSelectionMode = true
                        selectedIds = gridItems.map { it.id }.toSet()
                    },
                    onItemsReloaded = { gridItems = it },
                    onRequestCloseViewer = {
                        isSelectionMode = false
                        selectedIds = emptySet()
                        screen = AppScreen.ALBUM_GRID
                    }
                )
            }
        }
    }
}
