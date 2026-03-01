package com.dudoziworkshop.dzlog.ui.navigation

import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.dudoziworkshop.dzlog.ui.log.isOriginalRelativePath
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

enum class OriginalParent {
    WATER,
    LIST
}

data class AlbumLocation(
    val g1: String,
    val g2Label: String,
    val relativePath: String,
    val originalLinkPath: String?
)

data class OriginalNavContext(
    val parent: OriginalParent,
    val returnLocation: AlbumLocation?
)

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
    var albumLocation by remember { mutableStateOf<AlbumLocation?>(null) }
    var originalNavContext by remember { mutableStateOf<OriginalNavContext?>(null) }
    var albumGridEntryScreen by remember { mutableStateOf(AppScreen.ALBUM_G2) }
    var gridItems by remember { mutableStateOf<List<com.dudoziworkshop.dzlog.domain.model.MediaImageItem>>(emptyList()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var viewerStartIndex by remember { mutableIntStateOf(0) }
    val cameraSessionCaptureStack = remember { mutableStateListOf<List<Uri>>() }

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

    fun isAlbumScreen(target: AppScreen): Boolean {
        return target == AppScreen.ALBUM_G1 ||
            target == AppScreen.ALBUM_G2 ||
            target == AppScreen.ALBUM_GRID ||
            target == AppScreen.ALBUM_VIEWER
    }

    fun navigateTo(target: AppScreen) {
        if (screen == AppScreen.CAMERA && target != AppScreen.CAMERA && !isAlbumScreen(target)) {
            cameraSessionCaptureStack.clear()
        }
        previousScreen = screen
        screen = target
    }

    fun resetGridUiState() {
        gridItems = emptyList()
        isSelectionMode = false
        selectedIds = emptySet()
        viewerStartIndex = 0
    }

    fun clearOriginalContext() {
        originalNavContext = null
    }

    fun resolveAlbumFallbackScreen(): AppScreen {
        return if (directReturnToCameraFromAlbumGrid) AppScreen.CAMERA else albumGridEntryScreen
    }

    fun requireValidAlbumLocationOrFallback(): AlbumLocation? {
        val location = albumLocation
        if (location == null || location.relativePath.isBlank()) {
            // 앨범 그리드/뷰어 상위 복귀는 albumGridEntryScreen을 SSOT로 사용한다.
            screen = resolveAlbumFallbackScreen()
            return null
        }
        return location
    }

    fun openWaterGrid(location: AlbumLocation) {
        albumLocation = location
        clearOriginalContext()
        resetGridUiState()
    }

    // 원본 진입은 이 함수로만 처리해 "진입 문맥(부모)"을 단일 경로로 보존한다.
    // 경로 추측이 아닌 문맥 저장으로 back 복귀를 안정화한다.
    fun openOriginalGridFrom(
        parent: OriginalParent,
        originalPath: String,
        returnLocationIfWater: AlbumLocation?
    ) {
        val current = albumLocation
        val g1 = current?.g1 ?: return
        val g2Label = current.g2Label
        val normalizedOriginalPath = originalPath.trim()
        albumLocation = AlbumLocation(
            g1 = g1,
            g2Label = g2Label,
            relativePath = normalizedOriginalPath,
            originalLinkPath = null
        )
        originalNavContext = when (parent) {
            OriginalParent.WATER -> {
                // WATER 진입인데 복귀 위치가 없으면 안전하게 LIST 복귀로 강등한다.
                if (returnLocationIfWater != null) {
                    OriginalNavContext(parent = OriginalParent.WATER, returnLocation = returnLocationIfWater)
                } else {
                    OriginalNavContext(parent = OriginalParent.LIST, returnLocation = null)
                }
            }
            OriginalParent.LIST -> OriginalNavContext(parent = OriginalParent.LIST, returnLocation = null)
        }
        resetGridUiState()
    }

    fun openAlbumRoot() {
        albumLocation = null
        clearOriginalContext()
        albumGridEntryScreen = AppScreen.ALBUM_G1
        resetGridUiState()
        albumEntryScreen = screen
        directReturnToCameraFromAlbumGrid = false
        navigateTo(AppScreen.ALBUM_G1)
    }

    fun openRecentCaptureGrid(g1: String, g2: String, relativePath: String, startIndex: Int) {
        val location = AlbumLocation(
            g1 = g1,
            g2Label = g2.trim(),
            relativePath = relativePath.trim(),
            originalLinkPath = null
        )
        openWaterGrid(location)
        if (isOriginalRelativePath(location.relativePath)) {
            // 원본만 존재 → 부모는 리스트 → back은 상위 리스트로.
            openOriginalGridFrom(
                parent = OriginalParent.LIST,
                originalPath = location.relativePath,
                returnLocationIfWater = null
            )
        }
        viewerStartIndex = startIndex
        albumGridEntryScreen = AppScreen.ALBUM_G2
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
            AppScreen.CAMERA -> navigateTo(AppScreen.HOME)

            AppScreen.ALBUM_GRID -> {
                if (isSelectionMode) {
                    isSelectionMode = false
                    selectedIds = emptySet()
                } else {
                    val location = albumLocation
                    if (location == null) {
                        screen = resolveAlbumFallbackScreen()
                    } else if (isOriginalRelativePath(location.relativePath)) {
                        // 원본 그리드 back은 경로 추측이 아니라 original 문맥 슬롯으로만 결정한다.
                        // WATER인데 returnLocation이 비면 LIST 복귀로 처리해 비정상 상태를 막는다.
                        val context = originalNavContext
                        if (context?.parent == OriginalParent.WATER && context.returnLocation != null) {
                            albumLocation = context.returnLocation
                            clearOriginalContext()
                            resetGridUiState()
                        } else {
                            clearOriginalContext()
                            screen = resolveAlbumFallbackScreen()
                        }
                    } else if (directReturnToCameraFromAlbumGrid) {
                        screen = AppScreen.CAMERA
                    } else {
                        screen = albumGridEntryScreen
                    }
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
                onOpenRecentCaptureGrid = ::openRecentCaptureGrid,
                sessionCaptureStack = cameraSessionCaptureStack
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
            CameraScreen(
                tableTemplateState = tableTemplateState,
                onTemplateChange = ::updateTemplateState,
                onOpenTableEditor = { navigateTo(AppScreen.TABLE_EDITOR) },
                onOpenAlbum = ::openAlbumRoot,
                onOpenRecentCaptureGrid = ::openRecentCaptureGrid,
                sessionCaptureStack = cameraSessionCaptureStack
            )
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
                        val location = AlbumLocation(
                            g1 = title,
                            g2Label = if (title == DzlogMediaStoreReader.ROOT_G1) "" else title,
                            relativePath = waterRel,
                            originalLinkPath = originalRel
                        )
                        openWaterGrid(location)
                        albumGridEntryScreen = AppScreen.ALBUM_G1
                        screen = AppScreen.ALBUM_GRID
                    }

                    originalCount > 0 -> {
                        val baseLocation = AlbumLocation(
                            g1 = title,
                            g2Label = ORIGINAL_PHOTOS_TITLE,
                            relativePath = originalRel,
                            originalLinkPath = null
                        )
                        openWaterGrid(baseLocation)
                        // 원본만 존재 → 부모는 리스트 → back은 상위 리스트로.
                        openOriginalGridFrom(
                            parent = OriginalParent.LIST,
                            originalPath = originalRel,
                            returnLocationIfWater = null
                        )
                        albumGridEntryScreen = AppScreen.ALBUM_G1
                        screen = AppScreen.ALBUM_GRID
                    }

                    else -> {
                        Toast.makeText(context, "사진이 없습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            LogG1Screen(
                onGoHome = { screen = AppScreen.HOME },
                onOpenG2 = { g1 ->
                    albumLocation = AlbumLocation(
                        g1 = g1,
                        g2Label = "",
                        relativePath = "",
                        originalLinkPath = null
                    )
                    clearOriginalContext()
                    resetGridUiState()
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
            val g1 = albumLocation?.g1
            if (g1 == null) {
                screen = AppScreen.ALBUM_G1
            } else {
                LogG2Screen(
                    g1 = g1,
                    onBack = {
                        albumLocation = null
                        clearOriginalContext()
                        resetGridUiState()
                        screen = AppScreen.ALBUM_G1
                    },
                    onOpenGridForRelativePath = { g2Label, relativePath, originalRelativePath ->
                        val location = AlbumLocation(
                            g1 = g1,
                            g2Label = g2Label,
                            relativePath = relativePath,
                            originalLinkPath = originalRelativePath
                        )
                        openWaterGrid(location)
                        if (isOriginalRelativePath(relativePath) && originalRelativePath.isNullOrBlank()) {
                            // 원본만 존재 → 부모는 리스트 → back은 상위 리스트로.
                            openOriginalGridFrom(
                                parent = OriginalParent.LIST,
                                originalPath = relativePath,
                                returnLocationIfWater = null
                            )
                        }
                        albumGridEntryScreen = AppScreen.ALBUM_G2
                        screen = AppScreen.ALBUM_GRID
                    }
                )
            }
        }
        // 📸 앨범 내 사진 목록 화면
        AppScreen.ALBUM_GRID -> {
            val location = requireValidAlbumLocationOrFallback()
            if (location != null) {
                LogGridScreen(
                    relativePath = location.relativePath,
                    items = gridItems,
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedIds,
                    onItemsLoaded = { gridItems = it },
                    onOpenViewer = { idx ->
                        viewerStartIndex = idx
                        screen = AppScreen.ALBUM_VIEWER
                    },
                    originalRelativePath = location.originalLinkPath,
                    onOpenOriginalFolder = { _ ->
                        val current = albumLocation
                        val originalPath = current?.originalLinkPath
                        if (current != null && !originalPath.isNullOrBlank()) {
                            // 원본사진을 워터 하위로 들어왔으므로 back은 워터로 복귀해야 한다.
                            openOriginalGridFrom(
                                parent = OriginalParent.WATER,
                                originalPath = originalPath,
                                returnLocationIfWater = current
                            )
                        }
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
            val location = requireValidAlbumLocationOrFallback()
            if (location != null) {
                LogViewerScreen(
                    relativePath = location.relativePath,
                    items = gridItems,
                    startIndex = viewerStartIndex,
                    onBack = { screen = if (directReturnToCameraFromAlbumGrid) AppScreen.CAMERA else AppScreen.ALBUM_GRID },
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
