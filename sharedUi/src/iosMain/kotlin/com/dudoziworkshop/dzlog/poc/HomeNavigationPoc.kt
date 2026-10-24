package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.*
import com.dudoziworkshop.dzlog.data.media.PhotoKitCaptureMediaRepository
import com.dudoziworkshop.dzlog.data.media.CaptureMediaAvailability
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.feature.settings.ui.SettingsRootContent
import com.dudoziworkshop.dzlog.domain.time.formatCaptureTime
import com.dudoziworkshop.dzlog.ui.table.mock.TableEditorV2Content
import com.dudoziworkshop.dzlog.ui.table.mock.formatMockCounter
import com.dudoziworkshop.dzlog.ui.table.mock.tableEditorNow
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.ui.common.DDZConfirmDialog
import com.dudoziworkshop.dzlog.ui.home.*
import com.dudoziworkshop.dzlog.ui.table.template.TableTemplateListContent
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.cinterop.*
import org.jetbrains.skia.Image
import platform.Foundation.*

/** The existing home/list/editor/settings UI with an iOS persistence adapter. */
@Composable
internal fun HomeNavigationPoc(template: SavedTableTemplate, editorKey: String = IOS_EDITOR_KEY, resetTestFixture: Boolean = false) {
    val store = remember(editorKey) { IosWorkspaceStore(editorKey) }
    val initial = remember(store) {
        if (resetTestFixture) store.resetTestFixture()
        runCatching { store.load(template) }
    }
    var workspace by remember { mutableStateOf(initial.getOrNull() ?: EditorWorkspace(listOf(EditorDocument(template)), null)) }
    val loadFailed = initial.isFailure
    var notice by remember { mutableStateOf(if (loadFailed) "기존 편집 문서를 읽지 못했습니다. 덮어쓰지 않도록 편집을 잠갔습니다." else null) }
    val editorPlatform = remember { IosTableEditorPlatform { notice = it } }
    var showEditor by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    val photoRepository = remember { PhotoKitCaptureMediaRepository() }
    var recentPhoto by remember { mutableStateOf<ImageBitmap?>(null) }
    var recentPhotoName by remember { mutableStateOf("") }
    LaunchedEffect(showGallery) {
        if (!showGallery) {
            val indexed = runCatching { photoRepository.listSavedAssets() }.getOrDefault(emptyList())
            val newestCapture = indexed.lastOrNull()?.captureId
            val recent = indexed.lastOrNull {
                it.captureId == newestCapture && it.asset.target.role == CaptureMediaRole.RESULT &&
                    it.availability == CaptureMediaAvailability.PRESENT
            } ?: indexed.lastOrNull { it.availability == CaptureMediaAvailability.PRESENT }
            recentPhoto = null
            recentPhotoName = recent?.asset?.target?.displayName.orEmpty()
            if (recent != null) {
                val bytes = runCatching { photoRepository.readAsset(recent.asset.assetId) }.getOrNull()
                recentPhoto = bytes?.let { runCatching { Image.makeFromEncoded(it).toComposeImageBitmap() }.getOrNull() }
            }
        }
    }
    var showList by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var qualityExpanded by remember { mutableStateOf(false) }
    var showSaveInfo by remember { mutableStateOf(false) }
    var editorFromList by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var pendingNew by remember { mutableStateOf<EditorDocument?>(null) }
    var openSaveSettings by remember { mutableStateOf(false) }
    val document = pendingNew ?: workspace.documents.firstOrNull { it.template.id == editingId }
        ?: workspace.active ?: workspace.documents.firstOrNull() ?: EditorDocument(template)
    val currentTemplate = document.template
    val selected = workspace.activeTemplateId != null
    val templates = workspace.documents.map { it.template }
    val leaf = rememberHomeLeafPainter()
    fun persist(next: EditorWorkspace): Boolean {
        if (!loadFailed && store.save(next)) { workspace = next; return true }
        notice = "변경 내용을 저장하지 못했습니다. 다시 시도해 주세요."
        return false
    }
    fun updateCatalog(items: List<SavedTableTemplate>, activeId: String?) {
        val previous = workspace.documents.associateBy { it.template.id }
        val documents = items.map { item -> previous[item.id]?.copy(template = item) ?: EditorDocument(item) }
        persist(workspace.copy(documents = documents.sortedByDescending { it.template.modifiedAt }, activeTemplateId = activeId))
    }
    fun openEditor(id: String?, fromList: Boolean, saveSettings: Boolean = false) {
        if (loadFailed) return
        editingId = id
        editorFromList = fromList
        openSaveSettings = saveSettings
        showEditor = true
        showList = false
    }
    fun createTemplate() {
        val item = createSavedTableTemplate(nextNewTemplateName(templates), newBlankTableTemplateState(),
            (workspace.active ?: workspace.documents.firstOrNull())?.template?.styleState
                ?: com.dudoziworkshop.dzlog.feature.table.model.TableStyleState())
        // Like Android, a new draft is not persisted until Save; discard removes only this draft.
        pendingNew = document.copy(template = item, nextCounter = null, usesAutoNext = true)
        openEditor(item.id, true)
    }
    fun unavailable() { notice = "이 기능은 아직 사용할 수 없습니다." }
    val savedDocument = workspace.active ?: workspace.documents.firstOrNull() ?: EditorDocument(template)
    val savedPreview = remember(savedDocument) {
        buildPreview(PreviewInput(savedDocument.template.templateState, tableEditorNow(), savedDocument.counterPadding,
            NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            NamingFormatDefaults.FILE_NAME_DELIMITER, savedDocument.includePathInScope, savedDocument.includeFilenameInScope,
            savedDocument.saveMode, savedDocument.nextCounter, 0))
    }
    if (showGallery) {
        IosPhotoKitGalleryScreen(repository = photoRepository, onBack = { showGallery = false })
    } else if (showEditor) {
        key(currentTemplate.id) {
            TableEditorV2Content(
                platform = editorPlatform, templateState = currentTemplate.templateState,
                styleState = currentTemplate.styleState, includePathInCounterScope = document.includePathInScope,
                includeFilenameInCounterScope = document.includeFilenameInScope,
                saveMode = document.saveMode, counterPadding = document.counterPadding,
                templateId = currentTemplate.id, templateName = currentTemplate.name,
                templates = (listOfNotNull(pendingNew?.template) + templates).distinctBy { it.id },
                isUnsavedNewTemplate = pendingNew != null,
                onDiscardUnsavedNewTemplate = { pendingNew = null },
                openSaveSettingsInitially = openSaveSettings,
                onSwitchTemplate = { id -> editingId = id; pendingNew = null },
                onBack = { showEditor = false; showList = editorFromList; pendingNew = null; editingId = null },
                onSave = { table, style, pathScope, filenameScope, mode, padding, next, autoNext ->
                    val updated = EditorDocument(
                        currentTemplate.copy(templateState = table, styleState = style,
                            modifiedAt = tableEditorNow().epochMilliseconds),
                        pathScope, filenameScope, mode, padding, next, autoNext,
                    )
                    if (persist(workspace.save(updated))) { pendingNew = null; true } else false
                },
                initialNextCounter = document.nextCounter, initialUsesAutoNext = document.usesAutoNext,
            )
        }
    } else if (showSettings) {
        SettingsRootContent(
            onBack = { showSettings = false }, qualityMode = workspace.settings.qualityMode,
            assistShutterEnabled = workspace.settings.assistShutterEnabled,
            undoSavePathWarningEnabled = workspace.settings.undoSavePathWarningEnabled,
            isQualityExpanded = qualityExpanded, isQualitySaving = false, version = "iOS 개발 빌드",
            onToggleQuality = { qualityExpanded = !qualityExpanded },
            onSelectQuality = { mode ->
                if (persist(workspace.copy(settings = workspace.settings.copy(qualityMode = mode)))) qualityExpanded = false
            },
            onChangeAssistShutter = { persist(workspace.copy(settings = workspace.settings.copy(assistShutterEnabled = it))) },
            onChangeUndoWarning = { persist(workspace.copy(settings = workspace.settings.copy(undoSavePathWarningEnabled = it))) },
            onOpenPermissions = ::unavailable, onOpenCaptureFeedback = ::unavailable,
            onOpenCredits = ::unavailable, onOpenGuide = ::unavailable,
            onOpenExport = ::unavailable, onOpenImport = ::unavailable,
        )
    } else if (showList) {
        TableTemplateListContent(
            templates = templates, activeTemplateId = workspace.activeTemplateId,
            onBack = { showList = false },
            onSelectTemplate = { id -> if (persist(workspace.copy(activeTemplateId = id))) showList = false },
            allowManagement = !loadFailed,
            onOpenTemplate = { openEditor(it, true) }, onCreateTemplate = ::createTemplate,
            onRenameTemplate = { id, name -> updateCatalog(renameSavedTableTemplate(templates, id, name), workspace.activeTemplateId) },
            onDuplicateTemplate = { id ->
                val next = duplicateSavedTableTemplate(templates, id)
                val copy = next.firstOrNull { item -> templates.none { it.id == item.id } }
                if (copy != null) {
                    // Duplicate the per-template editor metadata along with the reused table/style model.
                    val source = workspace.documents.first { it.template.id == id }
                    persist(workspace.copy(documents = listOf(source.copy(template = copy)) + workspace.documents))
                }
            },
            onDeleteTemplate = { id ->
                val next = deleteSavedTableTemplate(templates, workspace.activeTemplateId, id)
                updateCatalog(next.items, next.activeTemplateId)
            },
            formatDate = { timestamp, used ->
                if (timestamp <= 0) { if (used) "사용 기록 없음" else "수정일 없음" }
                else (if (used) "사용 " else "수정 ") + formatCaptureTime("yyyy.MM.dd", tableEditorNow().copy(epochMilliseconds = timestamp))
            },
        )
    } else {
        HomeContent(
            templateName = if (selected) savedDocument.template.name else "기본 촬영",
            labels = HomeLabels("설정", "현재 템플릿",
                "다음 촬영 번호 " + if (selected) formatMockCounter(savedDocument.nextCounter ?: 1, savedDocument.counterPadding) else "0001",
                "촬영 시작", "표 편집", "저장 정보"),
            onManageTemplates = { showList = true }, onOpenSettings = { showSettings = true }, onStartCamera = {},
            onOpenTableEditor = {
                if (workspace.documents.isEmpty()) showList = true else openEditor(workspace.activeTemplateId, false)
            }, onOpenSaveInfo = { showSaveInfo = true },
            modifier = Modifier.fillMaxSize().background(DDZColor.Background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(),
            captureEnabled = false, settingsEnabled = !loadFailed, editingEnabled = !loadFailed, saveInfoEnabled = true,
            background = { HomeAmbientContent(painter = leaf) },
            recentCapture = {
                HomeRecentCaptureContent(
                    hasImage = recentPhoto != null, displayName = recentPhotoName,
                    timeText = "DZlog 저장 사진", recentLabel = "최근 촬영",
                    emptyTitle = "아직 촬영한 사진이 없어요.", emptyHint = "첫 기록을 남겨보세요.",
                    onOpenAlbum = { showGallery = true }, onOpenImage = { showGallery = true },
                    albumEnabled = true,
                    imageContent = {
                        recentPhoto?.let { photo ->
                            ComposeImage(bitmap = photo, contentDescription = "최근 사진",
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    },
                )
            },
        )
    }
    notice?.let { message ->
        DDZConfirmDialog(title = "안내", message = message, confirmText = "확인", dismissText = "닫기",
            onConfirm = { notice = null }, onDismiss = { notice = null })
    }
    if (showSaveInfo) {
        HomeSaveInfoSheet(
            fileName = if (selected) savedPreview.previewNaming.displayName else "ios-sample_0001.jpg",
            relativePaths = if (selected) listOf("사진 저장 연결 전") else listOf("PoC/현장 기록/", "PoC/보관/"),
            onOpenSaveSettings = {
                showSaveInfo = false
                if (workspace.documents.isEmpty()) showList = true else openEditor(workspace.activeTemplateId, false, true)
            }, onDismiss = { showSaveInfo = false }, settingsEnabled = !loadFailed && selected,
        )
    }
}

/** Same PNG as Android, loaded once from the native bundle; all rendering stays shared. */
@OptIn(ExperimentalForeignApi::class)
@Composable
private fun rememberHomeLeafPainter(): Painter {
    val bitmap = remember {
        val path = requireNotNull(NSBundle.mainBundle.pathForResource("home_leaf_shadow_c", "png"))
        val data = requireNotNull(NSData.dataWithContentsOfFile(path))
        val bytes = requireNotNull(data.bytes).reinterpret<ByteVar>().readBytes(data.length.toInt())
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }
    return remember(bitmap) { BitmapPainter(bitmap) }
}
