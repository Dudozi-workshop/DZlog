package com.dudoziworkshop.dzlog.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.preferences.KEY_ORIENTATION_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.dudoziworkshop.dzlog.data.preferences.OrientationMode
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.template.defaultTableTemplateState
import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.buildGalleryRelativePath
import com.dudoziworkshop.dzlog.feature.settings.ui.SettingsScreen
import com.dudoziworkshop.dzlog.feature.table.policy.saveTableTemplate
import com.dudoziworkshop.dzlog.ui.camera.CameraScreen
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.ui.home.HomeScreen
import com.dudoziworkshop.dzlog.ui.log.LogG1Screen
import com.dudoziworkshop.dzlog.ui.log.LogG2Screen
import com.dudoziworkshop.dzlog.ui.log.LogGridScreen
import com.dudoziworkshop.dzlog.ui.log.LogViewerScreen
import com.dudoziworkshop.dzlog.ui.log.ORIGINAL_PHOTOS_TITLE
import com.dudoziworkshop.dzlog.ui.table.TableEditorScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


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

}

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var previousScreen by remember { mutableStateOf(AppScreen.HOME) }
    var albumEntryScreen by remember { mutableStateOf(AppScreen.HOME) }
    var directReturnToCameraFromAlbumGrid by remember { mutableStateOf(false) }

// 앨범(G1/G2/그리드/뷰어) 상태
    var selectedG1 by remember { mutableStateOf<String?>(null) }
    var selectedG2 by remember { mutableStateOf<String?>(null) }
    var selectedRelativePath by remember { mutableStateOf<String?>(null) }
    var selectedAlbumTitle by remember { mutableStateOf<String?>(null) }
    var albumOriginalReturnRelativePath by remember { mutableStateOf<String?>(null) }
    var albumOriginalReturnTitle by remember { mutableStateOf<String?>(null) }
    var albumGridEntryScreen by remember { mutableStateOf(AppScreen.ALBUM_G2) }
    var gridOriginalRelativePath by remember { mutableStateOf<String?>(null) }
    var gridItems by remember { mutableStateOf<List<com.dudoziworkshop.dzlog.domain.model.MediaImageItem>>(emptyList()) }
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
        initial = com.dudoziworkshop.dzlog.data.datastore.AppSettings(
            saveMode = com.dudoziworkshop.dzlog.domain.model.SaveMode.BOTH,
            continuousPreviewMode = com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode.OFF,
            photoQualityMode = PhotoQualityMode.BALANCED,
            counterPadding = 0,
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
            toastEnabled = true,
            hapticEnabled = true,
            blankWarningEnabled = true,
        )
    )

    var lastBackPressedMs by remember { mutableLongStateOf(0L) }
    var hasRestoredTemplate by remember { mutableStateOf(false) }
    val appScope = rememberCoroutineScope()

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
                val loaded = tableTemplateStateFromJson(json)
                if (loaded != null) {
                    tableTemplateViewModel.update(loaded)
                } else {
                    val reset = defaultTableTemplateState()
                    tableTemplateViewModel.update(reset)
                    saveTableTemplate(context, reset)
                    Toast.makeText(
                        context,
                        "저장된 템플릿을 불러올 수 없어 기본값으로 초기화했습니다.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.onFailure {
            val reset = defaultTableTemplateState()
            tableTemplateViewModel.update(reset)
            runCatching { saveTableTemplate(context, reset) }
        }
        hasRestoredTemplate = true
    }


    fun updateTemplateState(updated: TableTemplateState) {
        tableTemplateViewModel.update(updated)
        if (!hasRestoredTemplate) return
        appScope.launch {
            saveTableTemplate(context, updated)
        }
    }

    fun resetTemplateState() {
        val reset = defaultTableTemplateState()
        tableTemplateViewModel.update(reset)
        if (!hasRestoredTemplate) return
        appScope.launch {
            saveTableTemplate(context, reset)
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

    fun computeAlbumTitle(selectedTitle: String?, g1: String, g2: String?): String {
        selectedTitle?.let { return it }
        val g2Norm = g2?.trim().orEmpty()
        return when {
            g1 == DzlogMediaStoreReader.ROOT_G1 && g2Norm.isBlank() -> DzlogMediaStoreReader.ROOT_G1
            g2Norm.isBlank() -> "DZlog / $g1"
            else -> "DZlog / $g1 / $g2Norm"
        }
    }

    fun isAlbumScreen(target: AppScreen): Boolean {
        return target == AppScreen.ALBUM_G1 ||
            target == AppScreen.ALBUM_G2 ||
            target == AppScreen.ALBUM_GRID ||
            target == AppScreen.ALBUM_VIEWER
    }

    fun openAlbumRoot() {
        selectedG1 = null
        selectedG2 = null
        selectedRelativePath = null
        selectedAlbumTitle = null
        albumOriginalReturnRelativePath = null
        albumOriginalReturnTitle = null
        albumGridEntryScreen = AppScreen.ALBUM_G1
        gridOriginalRelativePath = null
        gridItems = emptyList()
        isSelectionMode = false
        selectedIds = emptySet()
        viewerStartIndex = 0
        albumEntryScreen = screen
        directReturnToCameraFromAlbumGrid = false
        navigateTo(AppScreen.ALBUM_G1)
    }

    fun openRecentCaptureGrid(g1: String, g2: String, startIndex: Int) {
        val g2Value = g2.trim()
        selectedG1 = g1
        selectedG2 = g2Value
        if (g2Value == ORIGINAL_PHOTOS_TITLE) {
            val base = buildGalleryRelativePath(g1, "")
            selectedRelativePath = "${base}original/"
            selectedAlbumTitle = ORIGINAL_PHOTOS_TITLE
            gridOriginalRelativePath = null
        } else {
            selectedRelativePath = buildGalleryRelativePath(g1, g2Value)
            selectedAlbumTitle = null
            gridOriginalRelativePath = null
        }
        albumOriginalReturnRelativePath = null
        albumOriginalReturnTitle = null
        albumGridEntryScreen = AppScreen.ALBUM_G2
        gridItems = emptyList()
        isSelectionMode = false
        selectedIds = emptySet()
        viewerStartIndex = startIndex
        albumEntryScreen = screen
        directReturnToCameraFromAlbumGrid = (screen == AppScreen.CAMERA)
        navigateTo(AppScreen.ALBUM_GRID)
    }

    val keepCameraAliveBehindAlbum = albumEntryScreen == AppScreen.CAMERA && isAlbumScreen(screen)

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
                } else if (selectedAlbumTitle == ORIGINAL_PHOTOS_TITLE && albumOriginalReturnRelativePath != null) {
                    selectedRelativePath = albumOriginalReturnRelativePath
                    selectedAlbumTitle = albumOriginalReturnTitle
                    albumOriginalReturnRelativePath = null
                    albumOriginalReturnTitle = null
                } else if (directReturnToCameraFromAlbumGrid) {
                    screen = AppScreen.CAMERA
                } else {
                    screen = albumGridEntryScreen
                }
            }
            AppScreen.ALBUM_VIEWER -> screen = if (directReturnToCameraFromAlbumGrid) AppScreen.CAMERA else AppScreen.ALBUM_GRID
            AppScreen.ALBUM_G1 -> screen = if (albumEntryScreen == AppScreen.CAMERA) AppScreen.CAMERA else AppScreen.HOME
            AppScreen.ALBUM_G2 -> screen = AppScreen.ALBUM_G1
        }
    }

    if (keepCameraAliveBehindAlbum) {
        Box(modifier = Modifier.alpha(0f)) {
            CameraScreen(
                tableTemplateState = tableTemplateState,
                onTemplateChange = ::updateTemplateState,
                onOpenTableEditor = { navigateTo(AppScreen.TABLE_EDITOR) },
                onOpenAlbum = ::openAlbumRoot,
                onOpenRecentCaptureGrid = ::openRecentCaptureGrid
            )
        }
    }

    when (screen) {
        AppScreen.HOME -> HomeScreen(
            tableTemplateState = tableTemplateState,
            onOpenSettings = { navigateTo(AppScreen.SETTINGS) },
            onStartCamera = { navigateTo(AppScreen.CAMERA) },
            onOpenTableEditor = { navigateTo(AppScreen.TABLE_EDITOR) },
            onOpenAlbum = ::openAlbumRoot,
            onOpenRecentCaptureGrid = ::openRecentCaptureGrid
        )

        AppScreen.CAMERA -> {
            if (!keepCameraAliveBehindAlbum) {
                CameraScreen(
                    tableTemplateState = tableTemplateState,
                    onTemplateChange = ::updateTemplateState,
                    onOpenTableEditor = { navigateTo(AppScreen.TABLE_EDITOR) },
                    onOpenAlbum = ::openAlbumRoot,
                    onOpenRecentCaptureGrid = ::openRecentCaptureGrid
                )
            }
        }

        AppScreen.TABLE_EDITOR -> {
            TableEditorScreen(
                templateState = tableTemplateState,
                onTemplateChange = ::updateTemplateState,
                onReset = ::resetTemplateState,
                onBack = { screen = previousScreen }
            )
        }

        AppScreen.SETTINGS -> SettingsScreen(
            tableTemplateStateProvider = { tableTemplateState },
            onBack = { screen = AppScreen.HOME },
            onOpenTableDetail = { navigateTo(AppScreen.TABLE_EDITOR) }
        )
        AppScreen.ALBUM_G1 -> {
            fun openGridByCounts(
                title: String,
                waterRel: String,
                originalRel: String,
                waterCount: Int,
                originalCount: Int,
            ) {
                when {
                    waterCount > 0 -> {
                        selectedG1 = title
                        selectedG2 = if (title == DzlogMediaStoreReader.ROOT_G1) "" else title
                        selectedRelativePath = waterRel
                        selectedAlbumTitle = if (title == DzlogMediaStoreReader.ROOT_G1) DzlogMediaStoreReader.ROOT_G1 else null
                        gridOriginalRelativePath = originalRel
                        albumGridEntryScreen = AppScreen.ALBUM_G1
                        gridItems = emptyList()
                        isSelectionMode = false
                        selectedIds = emptySet()
                        viewerStartIndex = 0
                        screen = AppScreen.ALBUM_GRID
                    }

                    originalCount > 0 -> {
                        selectedG1 = title
                        selectedG2 = ORIGINAL_PHOTOS_TITLE
                        selectedRelativePath = originalRel
                        selectedAlbumTitle = ORIGINAL_PHOTOS_TITLE
                        gridOriginalRelativePath = null
                        albumGridEntryScreen = AppScreen.ALBUM_G1
                        gridItems = emptyList()
                        isSelectionMode = false
                        selectedIds = emptySet()
                        viewerStartIndex = 0
                        screen = AppScreen.ALBUM_GRID
                    }

                    else -> {
                        Toast.makeText(context, "사진이 없습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            LogG1Screen(
                onBack = { screen = if (albumEntryScreen == AppScreen.CAMERA) AppScreen.CAMERA else AppScreen.HOME },
                onOpenG2 = { g1 ->
                    selectedG1 = g1
                    selectedG2 = null
                    selectedRelativePath = null
                    selectedAlbumTitle = null
                    gridOriginalRelativePath = null
                    gridItems = emptyList()
                    isSelectionMode = false
                    selectedIds = emptySet()
                    screen = AppScreen.ALBUM_G2
                },
                onOpenGridByRuleC = { title, waterRel, originalRel, waterCount, originalCount ->
                    openGridByCounts(
                        title = title,
                        waterRel = waterRel,
                        originalRel = originalRel,
                        waterCount = waterCount,
                        originalCount = originalCount,
                    )
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
                        selectedRelativePath = null
                        selectedAlbumTitle = null
                        gridOriginalRelativePath = null
                        screen = AppScreen.ALBUM_G1
                    },
                    onOpenGridForRelativePath = { g2Label, relativePath, originalRelativePath ->
                        selectedG2 = g2Label
                        selectedRelativePath = relativePath
                        selectedAlbumTitle = if (relativePath.contains("/original/")) ORIGINAL_PHOTOS_TITLE else null
                        albumGridEntryScreen = AppScreen.ALBUM_G2
                        gridOriginalRelativePath = originalRelativePath
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
            val relativePath = selectedRelativePath
            if (g1 == null || g2 == null || relativePath == null) {
                screen = if (g1 == null) AppScreen.ALBUM_G1 else AppScreen.ALBUM_G2
            } else {
                val titleLabel = computeAlbumTitle(selectedAlbumTitle, g1, g2)
                LogGridScreen(
                    g1 = g1,
                    g2 = g2,
                    titleLabel = titleLabel,
                    relativePath = relativePath,
                    items = gridItems,
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedIds,
                    onItemsLoaded = { gridItems = it },
                    onOpenViewer = { idx ->
                        viewerStartIndex = idx
                        screen = AppScreen.ALBUM_VIEWER
                    },
                    originalRelativePath = gridOriginalRelativePath,
                    onOpenOriginalFolder = { originalPath ->
                        albumOriginalReturnRelativePath = selectedRelativePath
                        albumOriginalReturnTitle = selectedAlbumTitle
                        selectedRelativePath = originalPath
                        selectedAlbumTitle = ORIGINAL_PHOTOS_TITLE
                        gridOriginalRelativePath = null
                        gridItems = emptyList()
                        viewerStartIndex = 0
                        isSelectionMode = false
                        selectedIds = emptySet()
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
            val relativePath = selectedRelativePath
            if (g1 == null || g2 == null || relativePath == null) {
                screen = if (g1 == null) AppScreen.ALBUM_G1 else AppScreen.ALBUM_G2
            } else {
                val titleLabel = computeAlbumTitle(selectedAlbumTitle, g1, g2)
                LogViewerScreen(
                    g1 = g1,
                    g2 = g2,
                    titleLabel = titleLabel,
                    relativePath = relativePath,
                    items = gridItems,
                    startIndex = viewerStartIndex,
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedIds,
                    onBack = { screen = if (directReturnToCameraFromAlbumGrid) AppScreen.CAMERA else AppScreen.ALBUM_GRID },
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
