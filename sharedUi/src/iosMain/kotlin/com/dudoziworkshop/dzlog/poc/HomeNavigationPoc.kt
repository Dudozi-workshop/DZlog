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
import com.dudoziworkshop.dzlog.data.backup.LocalTemplateBackupMerge
import com.dudoziworkshop.dzlog.data.project.IosCaptureProjectRegistry
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.feature.settings.ui.SettingsRootContent
import com.dudoziworkshop.dzlog.domain.time.formatCaptureTime
import com.dudoziworkshop.dzlog.ui.table.mock.TableEditorV2Content
import com.dudoziworkshop.dzlog.ui.table.mock.formatMockCounter
import com.dudoziworkshop.dzlog.ui.table.mock.tableEditorNow
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.naming.capturePhysicalSavePaths
import com.dudoziworkshop.dzlog.ui.common.DDZConfirmDialog
import com.dudoziworkshop.dzlog.ui.home.*
import com.dudoziworkshop.dzlog.ui.table.template.TableTemplateListContent
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.cinterop.*
import org.jetbrains.skia.Image
import platform.Foundation.*

internal enum class CameraWorkspacePage { TEMPLATES, TABLE_EDITOR, SAVE_SETTINGS, PROJECTS, CREATE_PROJECT }

/** Home and camera share the same catalog, editor and project management routes. */
@Composable
internal fun HomeNavigationPoc(
    template: SavedTableTemplate,
    editorKey: String = IOS_EDITOR_KEY,
    resetTestFixture: Boolean = false,
    enableOnboarding: Boolean = false,
    cameraPage: CameraWorkspacePage? = null,
    onReturnToCamera: () -> Unit = {},
) {
    val appVersionLabel = remember { iosAppVersionLabel() }
    val store = remember(editorKey) { IosWorkspaceStore(editorKey) }
    val projectStore = remember(editorKey) { IosCaptureProjectStore(editorKey) }
    val initial = remember(store) {
        if (resetTestFixture) store.resetTestFixture()
        runCatching { store.load(template) }
    }
    val initialProjects = remember(projectStore) {
        if (resetTestFixture) projectStore.resetTestFixture()
        runCatching { projectStore.load() }
    }
    var workspace by remember { mutableStateOf(initial.getOrNull() ?: EditorWorkspace(listOf(EditorDocument(template)), null)) }
    var projectRegistry by remember {
        mutableStateOf(initialProjects.getOrNull() ?: IosCaptureProjectRegistry())
    }
    var showOnboarding by remember(store, enableOnboarding) {
        mutableStateOf(enableOnboarding && store.shouldOfferOnboarding())
    }
    var onboardingBusy by remember { mutableStateOf(false) }
    val loadFailed = initial.isFailure
    val projectLoadFailed = initialProjects.isFailure
    var notice by remember {
        mutableStateOf(when {
            loadFailed -> "기존 편집 문서를 읽지 못했습니다. 덮어쓰지 않도록 편집을 잠갔습니다."
            projectLoadFailed -> "프로젝트 목록을 읽지 못했습니다. 기존 기록을 보호하기 위해 프로젝트 변경을 잠갔습니다."
            else -> null
        })
    }
    val editorPlatform = remember { IosTableEditorPlatform { notice = it } }
    var showEditor by remember { mutableStateOf(cameraPage == CameraWorkspacePage.TABLE_EDITOR ||
        cameraPage == CameraWorkspacePage.SAVE_SETTINGS) }
    var showGallery by remember { mutableStateOf(false) }
    var showCaptureSettings by remember { mutableStateOf(false) }
    val photoRepository = remember { PhotoKitCaptureMediaRepository() }
    var recentPhoto by remember { mutableStateOf<ImageBitmap?>(null) }
    var recentPhotoName by remember { mutableStateOf("") }
    var recentPhotoTime by remember { mutableStateOf("-") }
    var recentPhotoLocation by remember { mutableStateOf("") }
    val cameraReturnRevision = IosCameraReturnSignal.revision
    var recoveryRevision by remember { mutableIntStateOf(0) }
    var recoveryPending by remember { mutableStateOf(IosCameraCaptureSession.hasPending()) }
    var recovering by remember { mutableStateOf(false) }
    LaunchedEffect(editorKey, cameraReturnRevision, recoveryRevision) {
        if (cameraPage != null) return@LaunchedEffect
        if (!projectLoadFailed) {
            runCatching { projectStore.load() }
                .onSuccess { projectRegistry = it }
                .onFailure { notice = "프로젝트 촬영 기록을 다시 불러오지 못했습니다." }
        }
        recoveryPending = IosCameraCaptureSession.hasPending()
        if (!loadFailed && IosCameraCaptureSession.hasPending()) {
            recovering = true
            val recovered = runCatching { IosCameraCaptureSession.recover(store, template) }.getOrDefault(false)
            recoveryPending = IosCameraCaptureSession.hasPending()
            recovering = false
            if (recovered) {
                runCatching { store.load(template) }.onSuccess { workspace = it }
            } else {
                notice = "이전 촬영 기록을 복구하지 못했습니다. 추가 촬영은 보류됩니다."
            }
        }
    }
    LaunchedEffect(showGallery, cameraReturnRevision) {
        if (cameraPage != null) return@LaunchedEffect
        if (cameraReturnRevision > 0 && !loadFailed && !showGallery) {
            runCatching { store.load(template) }.onSuccess { workspace = it }
        }
        if (!showGallery) {
            val indexed = runCatching { photoRepository.listSavedAssets() }.getOrDefault(emptyList())
            val newestCapture = indexed.lastOrNull()?.captureId
            val recent = indexed.lastOrNull {
                it.captureId == newestCapture && it.asset.target.role == CaptureMediaRole.RESULT &&
                    it.availability == CaptureMediaAvailability.PRESENT
            } ?: indexed.lastOrNull { it.availability == CaptureMediaAvailability.PRESENT }
            recentPhoto = null
            recentPhotoName = recent?.asset?.target?.displayName.orEmpty()
            recentPhotoTime = recent?.createdAtMillis?.let(::formatIosRecentCaptureTime) ?: "-"
            recentPhotoLocation = recent?.asset?.target?.relativePath
                ?.let(::formatIosRecentCaptureLocation).orEmpty()
            if (recent != null) {
                val bytes = runCatching { photoRepository.readAsset(recent.asset.assetId) }.getOrNull()
                recentPhoto = bytes?.let { runCatching { Image.makeFromEncoded(uprightIosImage(it)).toComposeImageBitmap() }.getOrNull() }
            }
        }
    }
    var showList by remember { mutableStateOf(cameraPage == CameraWorkspacePage.TEMPLATES) }
    var showSettings by remember { mutableStateOf(false) }
    var showCaptureFeedback by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }
    var backupMode by remember { mutableStateOf<IosWorkspaceBackupMode?>(null) }
    var qualityExpanded by remember { mutableStateOf(false) }
    var showSaveInfo by remember { mutableStateOf(false) }
    var editorFromList by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var pendingNew by remember { mutableStateOf<EditorDocument?>(null) }
    var openSaveSettings by remember { mutableStateOf(cameraPage == CameraWorkspacePage.SAVE_SETTINGS) }
    var showProjectPicker by remember { mutableStateOf(false) }
    var showProjectList by remember { mutableStateOf(cameraPage == CameraWorkspacePage.PROJECTS ||
        cameraPage == CameraWorkspacePage.CREATE_PROJECT) }
    var createProjectInitially by remember { mutableStateOf(cameraPage == CameraWorkspacePage.CREATE_PROJECT) }
    var projectDetailId by remember { mutableStateOf<String?>(null) }
    var projectGalleryId by remember { mutableStateOf<String?>(null) }
    val document = pendingNew ?: workspace.documents.firstOrNull { it.template.id == editingId }
        ?: workspace.active ?: workspace.documents.firstOrNull() ?: EditorDocument(template)
    val currentTemplate = document.template
    val selected = workspace.activeTemplateId != null
    val templates = workspace.documents.map { it.template }
    val leaf = rememberHomeLeafPainter()
    fun persist(next: EditorWorkspace): Boolean {
        if (IosCameraCaptureSession.hasPending()) {
            notice = "이전 촬영 기록을 복구한 후 설정을 변경해 주세요."
            return false
        }
        if (!loadFailed && store.save(next)) { workspace = next; return true }
        notice = "변경 내용을 저장하지 못했습니다. 다시 시도해 주세요."
        return false
    }
    fun selectProject(projectId: String?): Boolean {
        if (loadFailed || projectLoadFailed || IosCameraCaptureSession.hasPending()) return false
        val target = projectId?.let(projectRegistry::find)
        if (projectId != null && target?.isOpen != true) return false
        val rememberedTemplate = if (target == null) projectRegistry.generalLastTemplateId else target.lastTemplateId
        val available = workspace.documents.map { it.template.id }
        val selectedTemplateId = rememberedTemplate?.takeIf(available::contains)
            ?: workspace.activeTemplateId?.takeIf(available::contains)
            ?: available.firstOrNull()
        val beforeWorkspace = workspace
        val nextWorkspace = workspace.copy(activeTemplateId = selectedTemplateId)
        var nextProjects = runCatching { projectRegistry.select(projectId) }.getOrNull() ?: return false
        if (selectedTemplateId != null) nextProjects = nextProjects.rememberTemplate(selectedTemplateId)
        if (!store.save(nextWorkspace)) {
            notice = "프로젝트에 사용할 템플릿을 저장하지 못했습니다."
            return false
        }
        if (!projectStore.save(nextProjects)) {
            store.save(beforeWorkspace)
            notice = "프로젝트 선택을 저장하지 못했습니다. 이전 상태로 복구했습니다."
            return false
        }
        workspace = store.load(nextWorkspace.active?.template ?: template)
        projectRegistry = projectStore.load()
        return true
    }
    fun rememberSelectedTemplate(templateId: String) {
        if (projectLoadFailed) return
        val next = runCatching { projectRegistry.rememberTemplate(templateId) }.getOrNull() ?: return
        if (next != projectRegistry) {
            if (projectStore.save(next)) projectRegistry = next
            else notice = "프로젝트의 마지막 템플릿을 저장하지 못했습니다."
        }
    }
    fun startCamera() {
        if (loadFailed || recoveryPending || recovering) return
        if (cameraPage != null) return
        if (editorKey != IOS_PRODUCTION_STARTUP_TEST_KEY &&
            NSProcessInfo.processInfo.arguments.contains("--deny-camera-permission-for-ui-test")) {
            NSNotificationCenter.defaultCenter.postNotificationName("dzlog.openCamera", null)
        } else if (workspace.active == null) {
            showList = true
        } else {
            val issue = IosCameraWorkspaceBridge.prepare(editorKey, workspace)
            if (issue == null) {
                NSNotificationCenter.defaultCenter.postNotificationName("dzlog.openCamera", null)
            } else {
                if (!IosCameraCaptureSession.hasPending()) showCaptureSettings = true
                notice = issue
            }
        }
    }
    fun updateCatalog(items: List<SavedTableTemplate>, activeId: String?) {
        val previous = workspace.documents.associateBy { it.template.id }
        val documents = items.map { item -> previous[item.id]?.copy(template = item) ?: EditorDocument(item) }
        persist(workspace.copy(documents = documents.sortedByDescending { it.template.modifiedAt }, activeTemplateId = activeId))
    }
    fun openEditor(id: String?, fromList: Boolean, saveSettings: Boolean = false) {
        if (loadFailed) return
        val targetId = id?.takeIf { candidate ->
            pendingNew?.template?.id == candidate || workspace.documents.any { it.template.id == candidate }
        }
        if (targetId == null) {
            showEditor = false
            showList = true
            return
        }
        editingId = targetId
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
        pendingNew = document.copy(template = item, nextCounter = null, usesAutoNext = true,
            capturePlacement = null)
        openEditor(item.id, true)
    }
    val savedDocument = workspace.active ?: workspace.documents.firstOrNull() ?: EditorDocument(template)
    val savedPreview = remember(savedDocument) {
        buildPreview(PreviewInput(savedDocument.template.templateState, tableEditorNow(), savedDocument.counterPadding,
            NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            NamingFormatDefaults.FILE_NAME_DELIMITER, savedDocument.includePathInScope, savedDocument.includeFilenameInScope,
            savedDocument.saveMode, savedDocument.nextCounter, 0))
    }
    if (showOnboarding) {
        IosOnboardingScreen(
            busy = onboardingBusy,
            onChoose = { starterId ->
                onboardingBusy = true
                val starter = iosProductionStarterTemplate(starterId)
                val next = EditorWorkspace(listOf(EditorDocument(starter)), starter.id)
                if (store.save(next)) {
                    store.markOnboardingCompleted()
                    workspace = next
                    showOnboarding = false
                } else {
                    notice = "시작 양식을 저장하지 못했습니다. 다시 시도해 주세요."
                }
                onboardingBusy = false
            },
        )
    } else if (projectDetailId != null) {
        val project = requireNotNull(projectRegistry.find(requireNotNull(projectDetailId)))
        IosProjectDetailScreen(
            project = project,
            selected = project.id == projectRegistry.activeId,
            onBack = { projectDetailId = null; showProjectList = true },
            onContinue = {
                if (selectProject(project.id)) {
                    projectDetailId = null
                    showProjectList = false
                    startCamera()
                }
            },
            onShowPhotos = {
                projectDetailId = null
                projectGalleryId = project.id
            },
            onRename = { name ->
                projectStore.rename(project.id, name)?.let { projectRegistry = it }
                    ?: run { notice = "프로젝트 이름을 저장하지 못했습니다." }
            },
            onComplete = {
                val wasSelected = project.id == projectRegistry.activeId
                projectStore.complete(project.id)?.let { completed ->
                    projectRegistry = completed
                    if (wasSelected) selectProject(null)
                } ?: run { notice = "프로젝트 완료 상태를 저장하지 못했습니다." }
            },
            onReopen = {
                projectStore.reopen(project.id)?.let { projectRegistry = it }
                    ?: run { notice = "프로젝트를 재개하지 못했습니다." }
            },
            onDeleteMetadata = {
                projectStore.delete(project.id)?.let {
                    projectRegistry = it
                    projectDetailId = null
                    showProjectList = true
                } ?: run { notice = "프로젝트를 삭제하지 못했습니다." }
            },
        )
    } else if (showProjectList) {
        IosProjectListScreen(
            registry = projectRegistry,
            createInitially = createProjectInitially,
            onBack = { showProjectList = false; createProjectInitially = false },
            onCreate = { title ->
                projectStore.create(title, workspace.activeTemplateId)?.let {
                    projectRegistry = it
                    createProjectInitially = false
                } ?: run { notice = "새 프로젝트를 저장하지 못했습니다." }
            },
            onOpen = { projectDetailId = it; showProjectList = false },
        )
    } else if (showCaptureSettings) {
        IosCaptureSettingsScreen(savedDocument, workspace.settings.continuousPreviewMode,
            workspace.settings.captureHapticEnabled, workspace.settings.captureAspectMode,
            onBack = { showCaptureSettings = false },
            onApply = { updated, previewMode, hapticEnabled, aspectMode ->
            if (persist(workspace.save(updated).copy(settings = workspace.settings.copy(
                    continuousPreviewMode = previewMode,
                    captureHapticEnabled = hapticEnabled,
                    captureAspectMode = aspectMode)))) showCaptureSettings = false
        })
    } else if (projectGalleryId != null) {
        val project = requireNotNull(projectRegistry.find(requireNotNull(projectGalleryId)))
        IosQuickResultsScreen(
            repository = photoRepository,
            onBack = {
                projectDetailId = project.id
                projectGalleryId = null
            },
            captureIds = project.captureIds,
            projectTitle = project.title,
        )
    } else if (showGallery) {
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
                        capturePlacement = document.capturePlacement,
                    )
                    if (persist(workspace.save(updated))) {
                        pendingNew = null
                        rememberSelectedTemplate(updated.template.id)
                        true
                    } else false
                },
                initialNextCounter = document.nextCounter, initialUsesAutoNext = document.usesAutoNext,
            )
        }
    } else if (backupMode != null) {
        IosWorkspaceBackupScreen(
            mode = requireNotNull(backupMode),
            workspace = workspace,
            onBack = { backupMode = null },
            onImportConfirmed = { archive ->
                val disk = runCatching { store.load(template) }.getOrNull()
                if (disk != workspace || IosCameraCaptureSession.hasPending()) {
                    null
                } else {
                    val result = LocalTemplateBackupMerge.merge(
                        workspace.documents.map { it.template }, workspace.activeTemplateId, archive,
                    )
                    val existing = workspace.documents.associateBy { it.template.id }
                    val imported = workspace.copy(
                        documents = result.templates.map { item ->
                            existing[item.id]?.copy(template = item) ?: EditorDocument(item)
                        },
                        activeTemplateId = result.activeTemplateId,
                    )
                    if (persist(imported)) result else null
                }
            },
        )
    } else if (showHelp) {
        IosHelpSettingsScreen(onBack = { showHelp = false })
    } else if (showCredits) {
        IosCreditsSettingsScreen(
            versionLabel = appVersionLabel,
            onBack = { showCredits = false },
        )
    } else if (showPermissions) {
        IosPermissionsSettingsScreen(onBack = { showPermissions = false })
    } else if (showCaptureFeedback) {
        IosCaptureFeedbackSettingsScreen(
            hapticEnabled = workspace.settings.captureHapticEnabled,
            onBack = { showCaptureFeedback = false },
            onChangeHaptic = { enabled ->
                persist(workspace.copy(settings = workspace.settings.copy(captureHapticEnabled = enabled)))
            },
        )
    } else if (showSettings) {
        SettingsRootContent(
            onBack = { showSettings = false }, qualityMode = workspace.settings.qualityMode,
            assistShutterEnabled = workspace.settings.assistShutterEnabled,
            undoSavePathWarningEnabled = workspace.settings.undoSavePathWarningEnabled,
            isQualityExpanded = qualityExpanded, isQualitySaving = false, version = appVersionLabel,
            onToggleQuality = { qualityExpanded = !qualityExpanded },
            onSelectQuality = { mode ->
                if (persist(workspace.copy(settings = workspace.settings.copy(qualityMode = mode)))) qualityExpanded = false
            },
            onChangeAssistShutter = { persist(workspace.copy(settings = workspace.settings.copy(assistShutterEnabled = it))) },
            onChangeUndoWarning = { persist(workspace.copy(settings = workspace.settings.copy(undoSavePathWarningEnabled = it))) },
            onOpenPermissions = { showPermissions = true },
            onOpenCaptureFeedback = { showCaptureFeedback = true },
            onOpenCredits = { showCredits = true }, onOpenGuide = { showHelp = true },
            onOpenExport = { backupMode = IosWorkspaceBackupMode.EXPORT },
            onOpenImport = { backupMode = IosWorkspaceBackupMode.IMPORT },
        )
    } else if (showList) {
        TableTemplateListContent(
            templates = templates, activeTemplateId = workspace.activeTemplateId,
            onBack = { showList = false },
            onSelectTemplate = { id ->
                if (persist(workspace.copy(activeTemplateId = id))) {
                    rememberSelectedTemplate(id)
                    showList = false
                }
            },
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
    } else if (cameraPage != null) {
        LaunchedEffect(Unit) { onReturnToCamera() }
    } else {
        HomeContent(
            templateName = if (selected) savedDocument.template.name else "기본 촬영",
            labels = HomeLabels("설정", "현재 템플릿",
                "다음 촬영 번호 " + if (selected) formatMockCounter(savedDocument.nextCounter ?: 1, savedDocument.counterPadding) else "0001",
                "촬영 시작", "표 편집", "저장 정보"),
            onManageTemplates = { showList = true }, onOpenSettings = { showSettings = true },
            onStartCamera = ::startCamera,
            onOpenTableEditor = {
                openEditor(workspace.activeTemplateId, false)
            }, onOpenSaveInfo = { showSaveInfo = true },
            modifier = Modifier.fillMaxSize().background(DDZColor.Background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(),
            captureEnabled = !loadFailed && !recoveryPending && !recovering,
            settingsEnabled = !loadFailed && !recoveryPending && !recovering,
            editingEnabled = !loadFailed && !recoveryPending && !recovering, saveInfoEnabled = true,
            background = { HomeAmbientContent(painter = leaf) },
            recentCapture = {
                HomeRecentCaptureContent(
                    hasImage = recentPhoto != null, displayName = recentPhotoName,
                    timeText = recentPhotoTime, locationText = recentPhotoLocation,
                    recentLabel = "최근 촬영",
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
            projectName = projectRegistry.active?.title,
            onOpenProjectPicker = if (projectLoadFailed) null else ({ showProjectPicker = true }),
        )
    }
    notice?.let { message ->
        DDZConfirmDialog(title = "안내", message = message,
            confirmText = if (recoveryPending) "복구 재시도" else "확인", dismissText = "닫기",
            onConfirm = { notice = null; if (recoveryPending) recoveryRevision += 1 }, onDismiss = { notice = null })
    }
    if (showSaveInfo) {
        HomeSaveInfoSheet(
            fileName = if (selected) savedPreview.previewNaming.displayName else "템플릿을 선택해 주세요.",
            relativePaths = if (selected) capturePhysicalSavePaths(
                savedPreview.previewNaming.relativePath, savedDocument.saveMode) else emptyList(),
            onOpenSaveSettings = {
                showSaveInfo = false
                openEditor(workspace.activeTemplateId, false, saveSettings = true)
            }, onDismiss = { showSaveInfo = false },
            settingsEnabled = !loadFailed && selected && !recoveryPending && !recovering,
        )
    }
    if (showProjectPicker) {
        IosProjectPickerSheet(
            registry = projectRegistry,
            onSelect = { projectId ->
                if (selectProject(projectId)) showProjectPicker = false
                else notice = notice ?: "프로젝트를 선택하지 못했습니다."
            },
            onCreate = {
                showProjectPicker = false
                createProjectInitially = true
                showProjectList = true
            },
            onManage = {
                showProjectPicker = false
                createProjectInitially = false
                showProjectList = true
            },
            onDismiss = { showProjectPicker = false },
        )
    }
}

private fun formatIosRecentCaptureTime(epochMilliseconds: Long): String {
    val now = tableEditorNow()
    val captured = now.copy(epochMilliseconds = epochMilliseconds)
    return if (formatCaptureTime("yyyyMMdd", captured) == formatCaptureTime("yyyyMMdd", now)) {
        "오늘 ${formatCaptureTime("HH:mm", captured)}"
    } else {
        formatCaptureTime("yyyy.MM.dd HH:mm", captured)
    }
}

private fun formatIosRecentCaptureLocation(relativePath: String): String =
    relativePath.removePrefix("Pictures/").trimEnd('/').ifBlank { "DZlog" }

private fun iosAppVersionLabel(): String {
    val bundle = NSBundle.mainBundle
    val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "-"
    val build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "-"
    return "버전 $version ($build)"
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

