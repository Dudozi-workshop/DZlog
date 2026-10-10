package com.dudoziworkshop.dzlog.ui.navigation

import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.preferences.KEY_ORIENTATION_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.dudoziworkshop.dzlog.data.preferences.OrientationMode
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.createSavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.defaultTableTemplateState
import com.dudoziworkshop.dzlog.data.template.duplicateTemplateName
import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.data.template.nextNewTemplateName
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.settings.ui.CreditsScreen
import com.dudoziworkshop.dzlog.feature.settings.ui.SettingsScreen
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.state.TableTemplateCatalogViewModel
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.SaveSessionCoordinator
import com.dudoziworkshop.dzlog.feature.table.policy.TableTemplateCatalogCoordinator
import com.dudoziworkshop.dzlog.feature.table.policy.activeSavedTableStyle
import com.dudoziworkshop.dzlog.feature.table.policy.saveTableTemplate
import com.dudoziworkshop.dzlog.ui.camera.CameraScreen
import com.dudoziworkshop.dzlog.ui.home.HomeScreen
import com.dudoziworkshop.dzlog.ui.log.LogFolderScreen
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.ui.log.LogGridScreen
import com.dudoziworkshop.dzlog.ui.log.LogViewerScreen
import com.dudoziworkshop.dzlog.ui.log.ORIGINAL_PHOTOS_TITLE
import com.dudoziworkshop.dzlog.ui.log.isOriginalRelativePath
import com.dudoziworkshop.dzlog.ui.table.mock.TableEditorV2Screen
import com.dudoziworkshop.dzlog.ui.table.template.TableTemplateListScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val BACK_PRESS_EXIT_INTERVAL_MS = 1_500L

enum class AppScreen {
    HOME,
    CAMERA,
    TABLE_TEMPLATES,
    TABLE_EDITOR,
    SETTINGS,
    CREDITS,
    ALBUM_FOLDER,
    ALBUM_G1,
    ALBUM_G2,
    ALBUM_GRID,
    ALBUM_VIEWER
}

enum class GridEntrySource {
    NORMAL,
    HOME_RECENT,
    CAMERA_RECENT,
}

enum class ViewerEntrySource {
    GRID,
    CAMERA_RECENT,
    FOLDER,
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

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var previousScreen by remember { mutableStateOf(AppScreen.HOME) }
    var templateListEntryScreen by remember { mutableStateOf(AppScreen.HOME) }
    var albumEntryScreen by remember { mutableStateOf(AppScreen.HOME) }
    var gridEntrySource by remember { mutableStateOf(GridEntrySource.NORMAL) }
    var viewerEntrySource by remember { mutableStateOf(ViewerEntrySource.GRID) }

// 앨범(G1/G2/그리드/뷰어) 상태
    var albumLocation by remember { mutableStateOf<AlbumLocation?>(null) }
    var originalNavContext by remember { mutableStateOf<OriginalNavContext?>(null) }
    var albumGridEntryScreen by remember { mutableStateOf(AppScreen.ALBUM_FOLDER) }
    var albumFolderPath by remember { mutableStateOf(GalleryFolderIndexPolicy.ROOT) }
    var gridItems by remember { mutableStateOf<List<com.dudoziworkshop.dzlog.domain.model.MediaImageItem>>(emptyList()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var viewerStartIndex by remember { mutableIntStateOf(0) }
    val cameraSessionCaptureStack = remember { mutableStateListOf<List<Uri>>() }

    // 기존 상태

    var orientationMode by remember { mutableStateOf(OrientationMode.PORTRAIT_LOCK) }
    val tableTemplateViewModel: TableTemplateCatalogViewModel = viewModel()
    val tableTemplateState = tableTemplateViewModel.tableTemplateState

    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? android.app.Activity
    val tableCatalogCoordinator = remember(context) {
        TableTemplateCatalogCoordinator(context)
    }

    val appSettings by AppSettingsStore.flow(context).collectAsState(
        initial = AppSettings.Default,
    )

    var lastBackPressedMs by remember { mutableLongStateOf(0L) }
    var hasRestoredTemplate by remember { mutableStateOf(false) }
    var pendingNewTemplateId by remember { mutableStateOf<String?>(null) }
    var pendingNewTemplatePreviousActiveId by remember { mutableStateOf<String?>(null) }
    var openSaveSettingsInitially by remember { mutableStateOf(false) }
    // An editor-only choice; camera activation is committed by saveV2EditSession.
    var editorReplacementId by remember { mutableStateOf<String?>(null) }
    val appScope = rememberCoroutineScope()

    DisposableEffect(screen, view) {
        val window = activity?.window ?: return@DisposableEffect onDispose { }
        val insetsController = WindowInsetsControllerCompat(window, view)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val darkSurfaceScreen = screen == AppScreen.ALBUM_VIEWER
        insetsController.isAppearanceLightStatusBars = !darkSurfaceScreen
        insetsController.isAppearanceLightNavigationBars = !darkSurfaceScreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        onDispose { }
    }

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
            val catalog = tableCatalogCoordinator.load()
            tableTemplateViewModel.restoreCatalog(
                items = catalog.items,
                activeId = catalog.activeTemplateId,
            )
        }.onFailure {
            tableTemplateViewModel.restoreCatalog(
                items = emptyList(),
                activeId = null,
            )
            Toast.makeText(
                context,
                "저장된 템플릿을 불러오지 못했습니다.",
                Toast.LENGTH_LONG,
            ).show()
        }
        hasRestoredTemplate = true
    }

    fun persistCurrentCatalog() {
        if (!hasRestoredTemplate) return
        appScope.launch {
            tableCatalogCoordinator.persist(
                items = tableTemplateViewModel.templates,
                activeTemplateId = tableTemplateViewModel.activeTemplateId,
            )
        }
    }

    fun updateTemplateState(updated: TableTemplateState) {
        tableTemplateViewModel.updateStateOnly(updated)
        if (pendingNewTemplateId == tableTemplateViewModel.activeTemplateId) {
            pendingNewTemplateId = null
            pendingNewTemplatePreviousActiveId = null
        }
        if (!hasRestoredTemplate) return
        appScope.launch {
            val next = tableCatalogCoordinator.updateActiveTemplate(
                items = tableTemplateViewModel.templates,
                activeTemplateId = tableTemplateViewModel.activeTemplateId,
                templateState = updated,
            )
            tableTemplateViewModel.setCatalog(next, tableTemplateViewModel.activeTemplateId)
        }
    }

    suspend fun saveV2EditSession(
        updatedTemplate: TableTemplateState,
        updatedStyle: TableStyleState,
        includePathInCounterScope: Boolean,
        includeFilenameInCounterScope: Boolean,
        saveMode: SaveMode,
        counterPadding: Int,
        nextCounter: Int?,
        usesAutoNext: Boolean,
    ): Boolean {
        if (!hasRestoredTemplate) return false

        val previousActiveId = tableTemplateViewModel.activeTemplateId
        val activeId = editorReplacementId ?: previousActiveId
        if (activeId == null || tableTemplateViewModel.templates.none { it.id == activeId }) return false

        return runCatching {
            val updatedItems = SaveSessionCoordinator.persist(
                context = context,
                catalog = tableCatalogCoordinator,
                items = tableTemplateViewModel.templates,
                activeId = activeId,
                template = updatedTemplate,
                style = updatedStyle,
                includePath = includePathInCounterScope,
                includeFilename = includeFilenameInCounterScope,
                saveMode = saveMode,
                padding = counterPadding,
                next = nextCounter,
                usesAutoNext = usesAutoNext,
                rollbackActiveId = previousActiveId,
            )

            tableTemplateViewModel.setCatalog(updatedItems, activeId)
            if (pendingNewTemplateId == activeId) {
                pendingNewTemplateId = null
                pendingNewTemplatePreviousActiveId = null
            }
            true
        }.getOrDefault(false)
    }

    fun openSavedTemplate(templateId: String) {
        val target = tableTemplateViewModel.templates.firstOrNull { it.id == templateId } ?: return
        appScope.launch {
            tableCatalogCoordinator.activate(
                items = tableTemplateViewModel.templates,
                activeTemplateId = target.id,
            )
            tableTemplateViewModel.activate(target.id)
            openSaveSettingsInitially = false
            previousScreen = AppScreen.TABLE_TEMPLATES
            screen = AppScreen.TABLE_EDITOR
        }
    }

    fun createNewTemplate() {
        appScope.launch {
            val previousActiveId = tableTemplateViewModel.activeTemplateId
            val item = createSavedTableTemplate(
                name = nextNewTemplateName(tableTemplateViewModel.templates),
                templateState = newBlankTableTemplateState(),
                styleState = activeSavedTableStyle(
                    tableTemplateViewModel.templates,
                    tableTemplateViewModel.activeTemplateId,
                ),
            )
            val next = listOf(item) + tableTemplateViewModel.templates
            pendingNewTemplateId = item.id
            pendingNewTemplatePreviousActiveId = previousActiveId
            tableTemplateViewModel.setCatalog(next, item.id)
            openSaveSettingsInitially = false
            previousScreen = AppScreen.TABLE_TEMPLATES
            screen = AppScreen.TABLE_EDITOR
        }
    }

    fun openActiveSaveSettings() {
        val hasActiveTemplate = tableTemplateViewModel.activeTemplateId != null &&
            tableTemplateViewModel.templates.any { it.id == tableTemplateViewModel.activeTemplateId }

        if (!hasActiveTemplate) {
            templateListEntryScreen = screen
            previousScreen = screen
            screen = AppScreen.TABLE_TEMPLATES
            return
        }

        openSaveSettingsInitially = true
        previousScreen = screen
        screen = AppScreen.TABLE_EDITOR
    }

    fun discardPendingNewTemplate() {
        val pendingId = pendingNewTemplateId ?: return
        val next = tableTemplateViewModel.templates.filterNot { it.id == pendingId }
        val restoredActiveId = pendingNewTemplatePreviousActiveId
            ?.takeIf { previousId -> next.any { it.id == previousId } }
            ?: next.firstOrNull()?.id
        tableTemplateViewModel.setCatalog(next, restoredActiveId)
        pendingNewTemplateId = null
        pendingNewTemplatePreviousActiveId = null
    }

    fun renameTemplate(templateId: String, newName: String) {
        val normalized = newName.trim()
        if (normalized.isBlank()) return
        val next = tableTemplateViewModel.templates.map { item ->
            if (item.id == templateId) {
                item.copy(name = normalized, modifiedAt = System.currentTimeMillis())
            } else {
                item
            }
        }
        tableTemplateViewModel.setCatalog(next, tableTemplateViewModel.activeTemplateId)
        persistCurrentCatalog()
    }

    fun duplicateTemplate(templateId: String) {
        val source = tableTemplateViewModel.templates.firstOrNull { it.id == templateId } ?: return
        val copy = createSavedTableTemplate(
            name = duplicateTemplateName(source.name, tableTemplateViewModel.templates),
            templateState = source.templateState,
            styleState = source.styleState,
        )
        val next = listOf(copy) + tableTemplateViewModel.templates
        tableTemplateViewModel.setCatalog(next, tableTemplateViewModel.activeTemplateId)
        persistCurrentCatalog()
    }

    fun deleteTemplate(templateId: String) {
        val next = tableTemplateViewModel.templates.filterNot { it.id == templateId }
        val nextActiveId = when {
            tableTemplateViewModel.activeTemplateId != templateId ->
                tableTemplateViewModel.activeTemplateId
            next.isNotEmpty() -> next.first().id
            else -> null
        }
        tableTemplateViewModel.setCatalog(next, nextActiveId)
        persistCurrentCatalog()
    }


    LaunchedEffect(orientationMode) {
        activity?.requestedOrientation = when (orientationMode) {
            OrientationMode.PORTRAIT_LOCK ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            OrientationMode.AUTO_ROTATE ->
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun isAlbumScreen(target: AppScreen): Boolean =
        target == AppScreen.ALBUM_FOLDER ||
        target == AppScreen.ALBUM_G1 ||
            target == AppScreen.ALBUM_G2 ||
            target == AppScreen.ALBUM_GRID ||
            target == AppScreen.ALBUM_VIEWER

    fun navigateTo(target: AppScreen) {
        if (screen == AppScreen.CAMERA && target != AppScreen.CAMERA && !isAlbumScreen(target)) {
            cameraSessionCaptureStack.clear()
        }
        if (target == AppScreen.TABLE_TEMPLATES) {
            templateListEntryScreen = screen
        }
        previousScreen = screen
        screen = target
    }

    fun resetGridUiState(clearGridItems: Boolean = true) {
        if (clearGridItems) {
            gridItems = emptyList()
        }
        isSelectionMode = false
        selectedIds = emptySet()
    }

    fun clearOriginalContext() {
        originalNavContext = null
    }

    fun resolveAlbumFallbackScreen(): AppScreen {
        return when {
            viewerEntrySource == ViewerEntrySource.CAMERA_RECENT -> AppScreen.CAMERA
            gridEntrySource == GridEntrySource.HOME_RECENT -> AppScreen.HOME
            gridEntrySource == GridEntrySource.CAMERA_RECENT -> AppScreen.CAMERA
            else -> albumGridEntryScreen
        }
    }

    fun resolveViewerBackTarget(): AppScreen {
        return when (viewerEntrySource) {
            ViewerEntrySource.CAMERA_RECENT -> AppScreen.CAMERA
            ViewerEntrySource.FOLDER -> AppScreen.ALBUM_FOLDER
            ViewerEntrySource.GRID -> AppScreen.ALBUM_GRID
        }
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

    fun openWaterGrid(location: AlbumLocation, clearGridItems: Boolean = true) {
        albumLocation = location
        clearOriginalContext()
        resetGridUiState(clearGridItems = clearGridItems)
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
        albumFolderPath = GalleryFolderIndexPolicy.ROOT
        albumGridEntryScreen = AppScreen.ALBUM_FOLDER
        resetGridUiState()
        albumEntryScreen = screen
        gridEntrySource = GridEntrySource.NORMAL
        viewerEntrySource = ViewerEntrySource.GRID
        navigateTo(AppScreen.ALBUM_FOLDER)
    }

    fun openRecentCaptureGrid(g1: String, g2: String, relativePath: String, startIndex: Int) {
        val location = AlbumLocation(
            g1 = g1,
            g2Label = g2.trim(),
            relativePath = relativePath.trim(),
            originalLinkPath = null
        )

        if (screen == AppScreen.CAMERA) {
            // Recent thumbnail opens the containing album grid, not the single-photo viewer.
            openWaterGrid(location)
            if (isOriginalRelativePath(location.relativePath)) {
                openOriginalGridFrom(
                    parent = OriginalParent.LIST,
                    originalPath = location.relativePath,
                    returnLocationIfWater = null,
                )
            }
            albumGridEntryScreen = AppScreen.ALBUM_FOLDER
            albumEntryScreen = AppScreen.CAMERA
            gridEntrySource = GridEntrySource.CAMERA_RECENT
            viewerEntrySource = ViewerEntrySource.GRID
            navigateTo(AppScreen.ALBUM_GRID)
            return
        }

        openWaterGrid(location)
        if (isOriginalRelativePath(location.relativePath)) {
            // 원본만 존재 → 부모는 리스트 → back은 상위 리스트로.
            openOriginalGridFrom(
                parent = OriginalParent.LIST,
                originalPath = location.relativePath,
                returnLocationIfWater = null
            )
        }
        albumGridEntryScreen = AppScreen.ALBUM_G2
        albumEntryScreen = screen
        // 정책: Home recent에서 진입한 Grid만 별도 출처로 기록해 back target을 Home으로 고정한다.
        gridEntrySource = if (screen == AppScreen.HOME) GridEntrySource.HOME_RECENT else GridEntrySource.NORMAL
        viewerEntrySource = ViewerEntrySource.GRID
        navigateTo(AppScreen.ALBUM_GRID)
    }


    fun buildGridHeaderTitle(location: AlbumLocation): String {
        val g1 = location.g1.trim()
        val rawG2 = location.g2Label.trim()

        val isOriginalLikeLabel = rawG2 == ORIGINAL_PHOTOS_TITLE ||
            rawG2.contains("원본") ||
            rawG2.equals("original", ignoreCase = true)

        val g2LabelToShow = rawG2.takeIf {
            it.isNotBlank() &&
                it != g1 &&
                !isOriginalLikeLabel
        }

        return g2LabelToShow?.let { "$g1 / $it" } ?: g1
    }

    fun handleAlbumGridBack() {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedIds = emptySet()
            return
        }

        val location = albumLocation
        if (location == null) {
            screen = resolveAlbumFallbackScreen()
            return
        }

        if (isOriginalRelativePath(location.relativePath)) {
            val context = originalNavContext
            if (context?.parent == OriginalParent.WATER && context.returnLocation != null) {
                albumLocation = context.returnLocation
                clearOriginalContext()
                resetGridUiState()
            } else {
                clearOriginalContext()
                screen = resolveAlbumFallbackScreen()
            }
            return
        }

        screen = when (gridEntrySource) {
            GridEntrySource.HOME_RECENT -> AppScreen.HOME
            GridEntrySource.CAMERA_RECENT -> AppScreen.CAMERA
            GridEntrySource.NORMAL -> albumGridEntryScreen
        }
    }

    val keepCameraAliveBehindAlbum = albumEntryScreen == AppScreen.CAMERA && isAlbumScreen(screen)

    BackHandler(
        enabled = screen != AppScreen.TABLE_EDITOR,
    ) {
        // 기본 내비게이션(화면 기준)
        when (screen) {
            AppScreen.HOME -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedMs < BACK_PRESS_EXIT_INTERVAL_MS) {
                    activity?.finish()
                } else {
                    lastBackPressedMs = now
                    if (appSettings.toastEnabled) {
                        Toast.makeText(context, "한 번 더 누르면 종료", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            AppScreen.SETTINGS -> screen = AppScreen.HOME
            AppScreen.CREDITS -> screen = AppScreen.SETTINGS
            AppScreen.TABLE_TEMPLATES -> screen = templateListEntryScreen
            AppScreen.TABLE_EDITOR -> screen = previousScreen
            AppScreen.CAMERA -> navigateTo(AppScreen.HOME)

            AppScreen.ALBUM_GRID -> {
                handleAlbumGridBack()
            }
            AppScreen.ALBUM_VIEWER -> screen = resolveViewerBackTarget()
            AppScreen.ALBUM_FOLDER -> {
                val parent = GalleryFolderIndexPolicy.parentOf(albumFolderPath)
                if (parent != null) albumFolderPath = parent
                else screen = if (albumEntryScreen == AppScreen.CAMERA) AppScreen.CAMERA else AppScreen.HOME
            }
            AppScreen.ALBUM_G1 -> screen = AppScreen.ALBUM_FOLDER
            AppScreen.ALBUM_G2 -> screen = AppScreen.ALBUM_FOLDER
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (keepCameraAliveBehindAlbum) {
            Box(modifier = Modifier.alpha(0f)) {
                CameraScreen(
                    tableTemplateState = tableTemplateState,
                    onTemplateChange = ::updateTemplateState,
                    onOpenTableEditor = { navigateTo(AppScreen.TABLE_TEMPLATES) },
                    onOpenSaveSettings = ::openActiveSaveSettings,
                    onOpenAlbum = ::openAlbumRoot,
                    onOpenRecentCaptureGrid = ::openRecentCaptureGrid,
                    sessionCaptureStack = cameraSessionCaptureStack
                )
            }
        }

        when (screen) {
            AppScreen.HOME -> HomeScreen(
                tableTemplateState = tableTemplateState,
                activeTemplateName = tableTemplateViewModel.templates
                    .firstOrNull { it.id == tableTemplateViewModel.activeTemplateId }
                    ?.name
                    .orEmpty(),
                onOpenSettings = { navigateTo(AppScreen.SETTINGS) },
                onStartCamera = { navigateTo(AppScreen.CAMERA) },
                onOpenTableEditor = {
                    openSaveSettingsInitially = false
                    navigateTo(AppScreen.TABLE_TEMPLATES)
                },
                onOpenSaveSettings = ::openActiveSaveSettings,
                onOpenAlbum = ::openAlbumRoot,
                onOpenRecentCaptureGrid = ::openRecentCaptureGrid
            )

            AppScreen.CAMERA -> {
                CameraScreen(
                    tableTemplateState = tableTemplateState,
                    onTemplateChange = ::updateTemplateState,
                    onOpenTableEditor = { navigateTo(AppScreen.TABLE_TEMPLATES) },
                    onOpenSaveSettings = ::openActiveSaveSettings,
                    onOpenAlbum = ::openAlbumRoot,
                    onOpenRecentCaptureGrid = ::openRecentCaptureGrid,
                    sessionCaptureStack = cameraSessionCaptureStack
                )
            }

            AppScreen.TABLE_TEMPLATES -> {
                TableTemplateListScreen(
                    templates = tableTemplateViewModel.templates,
                    activeTemplateId = tableTemplateViewModel.activeTemplateId,
                    onBack = { screen = templateListEntryScreen },
                    onOpenTemplate = ::openSavedTemplate,
                    onCreateTemplate = ::createNewTemplate,
                    onRenameTemplate = ::renameTemplate,
                    onDuplicateTemplate = ::duplicateTemplate,
                    onDeleteTemplate = ::deleteTemplate,
                )
            }

            AppScreen.TABLE_EDITOR -> {
                val editingId = editorReplacementId ?: tableTemplateViewModel.activeTemplateId
                val editingTemplate = tableTemplateViewModel.templates.firstOrNull { it.id == editingId }
                key(editingId) {
                    TableEditorV2Screen(
                        templateState = if (editorReplacementId == null) tableTemplateState
                        else editingTemplate?.templateState ?: tableTemplateState,
                        includePathInCounterScope = appSettings.includePathInCounterScope,
                        includeFilenameInCounterScope = appSettings.includeFilenameInCounterScope,
                        saveMode = appSettings.saveMode,
                        counterPadding = appSettings.counterPadding,
                        styleState = activeSavedTableStyle(
                            tableTemplateViewModel.templates,
                            editingId,
                        ),
                        isUnsavedNewTemplate = pendingNewTemplateId != null && pendingNewTemplateId == editingId,
                        openSaveSettingsInitially = openSaveSettingsInitially,
                        onSave = ::saveV2EditSession,
                        onDiscardUnsavedNewTemplate = ::discardPendingNewTemplate,
                        templateId = editingId,
                        templateName = editingTemplate?.name ?: "새 템플릿",
                        templates = tableTemplateViewModel.templates,
                        hasPendingTemplateSelection = editingId != tableTemplateViewModel.activeTemplateId,
                        onSwitchTemplate = { targetId ->
                            if (tableTemplateViewModel.templates.any { it.id == targetId }) {
                                editorReplacementId = targetId
                            }
                        },
                        onBack = {
                            editorReplacementId = null
                            openSaveSettingsInitially = false
                            screen = previousScreen
                        },
                    )
                }
            }

            AppScreen.SETTINGS -> SettingsScreen(
                onBack = { screen = AppScreen.HOME },
                onOpenCredits = { navigateTo(AppScreen.CREDITS) }
            )
            AppScreen.CREDITS -> CreditsScreen(
                onBack = { screen = AppScreen.SETTINGS }
            )
            AppScreen.ALBUM_FOLDER -> {
                LogFolderScreen(
                    relativePath = albumFolderPath,
                    capturePathDrafts = (tableTemplateViewModel.templates.map { it.templateState.pathSlotDrafts } + listOf(tableTemplateState.pathSlotDrafts)).distinct(),
                    onBack = {
                        val parent = GalleryFolderIndexPolicy.parentOf(albumFolderPath)
                        if (parent != null) albumFolderPath = parent
                        else screen = if (albumEntryScreen == AppScreen.CAMERA) AppScreen.CAMERA else AppScreen.HOME
                    },
                    onOpenFolder = { folder -> albumFolderPath = folder },
                    onOpenPhoto = { photos, index ->
                        val relativePath = photos.getOrNull(index)?.relativePath ?: albumFolderPath
                        albumLocation = AlbumLocation(
                            g1 = "",
                            g2Label = "",
                            relativePath = relativePath,
                            originalLinkPath = if (isOriginalRelativePath(relativePath)) null else relativePath + "original/",
                        )
                        gridItems = photos
                        viewerStartIndex = index
                        viewerEntrySource = ViewerEntrySource.FOLDER
                        gridEntrySource = GridEntrySource.NORMAL
                        albumGridEntryScreen = AppScreen.ALBUM_FOLDER
                        screen = AppScreen.ALBUM_VIEWER
                    },
                    onOpenOriginal = { originalPath ->
                        clearOriginalContext()
                        resetGridUiState()
                        albumFolderPath = originalPath
                    },
                )
            }

            AppScreen.ALBUM_G1, AppScreen.ALBUM_G2 -> {
                // Legacy routes are redirected; new entrypoints only use ALBUM_FOLDER.
                screen = AppScreen.ALBUM_FOLDER
            }
        // 📸 앨범 내 사진 목록 화면
            AppScreen.ALBUM_GRID -> {
            val location = requireValidAlbumLocationOrFallback()
            if (location != null) {
                LogGridScreen(
                    headerTitle = buildGridHeaderTitle(location),
                    isOriginalGrid = isOriginalRelativePath(location.relativePath),
                    relativePath = location.relativePath,
                    items = gridItems,
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedIds,
                    onItemsLoaded = { loaded ->
                        gridItems = loaded
                        selectedIds = selectedIds.intersect(loaded.mapTo(mutableSetOf()) { it.id })
                        if (selectedIds.isEmpty()) isSelectionMode = false
                    },
                    onDeleted = { verifiedIds ->
                        selectedIds = selectedIds - verifiedIds
                        if (selectedIds.isEmpty()) isSelectionMode = false
                    },
                    onOpenViewer = { photos, idx ->
                        gridItems = photos
                        viewerStartIndex = idx
                        viewerEntrySource = ViewerEntrySource.GRID
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
                    onSelectAll = { visibleIds ->
                        selectedIds = visibleIds
                        isSelectionMode = visibleIds.isNotEmpty()
                    },
                    onBack = { handleAlbumGridBack() }
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
                    onBack = {
                        screen = resolveViewerBackTarget()
                    },
                    onItemsReloaded = { gridItems = it },
                    onRequestCloseViewer = {
                        isSelectionMode = false
                        selectedIds = emptySet()
                        screen = resolveViewerBackTarget()
                    }
                )
            }
            }
        }
        GalleryPermissionOnboarding()
    }
}

