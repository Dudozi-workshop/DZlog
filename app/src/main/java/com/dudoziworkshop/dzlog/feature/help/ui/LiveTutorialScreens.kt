package com.dudoziworkshop.dzlog.feature.help.ui

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.policy.QuickEditSelectionMemory
import com.dudoziworkshop.dzlog.feature.capture.policy.CapturePhraseUndoHistory
import com.dudoziworkshop.dzlog.feature.help.model.TutorialLocation
import com.dudoziworkshop.dzlog.ui.home.HomeScreen
import com.dudoziworkshop.dzlog.ui.camera.CameraScreen
import com.dudoziworkshop.dzlog.ui.log.LogViewerScreen
import com.dudoziworkshop.dzlog.ui.log.LogFolderScreen
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATES_JSON
import com.dudoziworkshop.dzlog.data.template.*
import kotlinx.coroutines.flow.first
import com.dudoziworkshop.dzlog.ui.home.HomeTemplatePicker
import com.dudoziworkshop.dzlog.ui.table.template.TableTemplateListScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import com.dudoziworkshop.dzlog.ui.table.mock.TableEditorV2Screen
import com.dudoziworkshop.dzlog.feature.settings.ui.SettingsRootScreen
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy

/** Separate view models and draft-only editors cannot change the active work session. */
@Composable
internal fun LiveTutorialScreens(location: TutorialLocation, template: TableTemplateState,
    style: TableStyleState, settings: AppSettings, templateName: String) {
    val app = LocalContext.current.applicationContext as Application
    val owner = remember(location.areaId) {
        object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
            override val viewModelStore = ViewModelStore()
            override val defaultViewModelProviderFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        when (location.areaId) {
            "home" -> when (location.pageId) {
                "templates", "manage", "more" -> LiveHomeTemplates(location, template, style, templateName)
                "settings-screen" -> SettingsRootScreen(onBack = {})
                "table-screen" -> TableEditorV2Screen(templateState = template,
                    includePathInCounterScope = settings.includePathInCounterScope,
                    includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
                    saveMode = settings.saveMode, counterPadding = settings.counterPadding,
                    styleState = style, onSave = { _, _, _, _, _, _, _, _ -> false }, onBack = {}, templateName = templateName)
                "gallery", "recent-folder" -> LiveHomeFolder(location, settings)
                else -> HomeScreen(tableTemplateState = template, activeTemplateName = templateName,
                onOpenSettings = {}, onStartCamera = {}, onOpenTableEditor = {}, onOpenAlbum = {},
                onOpenRecentCaptureGrid = { _, _, _, _ -> })
            }
            "camera" -> if (location.pageId == "result") com.dudoziworkshop.dzlog.ui.log.QuickResultsScreen(
                captures = emptyList(), gridColumns = settings.galleryGridColumns,
                showCounterBadges = settings.showGalleryCounterBadges, onGridColumnsChange = {},
                onOpenCapture = { _, _ -> }, onOpenAlbum = {}, onReturnToCamera = {},
            ) else if (location.pageId == "details") TableEditorV2Screen(templateState = template,
                includePathInCounterScope = settings.includePathInCounterScope,
                includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
                saveMode = settings.saveMode, counterPadding = settings.counterPadding,
                styleState = style, onSave = { _, _, _, _, _, _, _, _ -> false }, onBack = {}, templateName = templateName)
            else CameraScreen(tableTemplateState = template, onTemplateChange = {},
                onOpenTableEditor = {}, onOpenSaveSettings = {}, onOpenAlbum = {},
                onOpenRecentCaptureGrid = { _, _, _, _ -> },
                sessionCaptureStack = remember { mutableStateListOf() },
                phraseUndoHistory = remember { CapturePhraseUndoHistory() },
                quickTemplateName = templateName,
                quickEditSelectionMemory = remember { QuickEditSelectionMemory() })
            "gallery" -> LiveGalleryTutorial(location, settings)
            "settings" -> SettingsRootScreen(onBack = {})
            else -> key(location.areaId, location.sessionId) {
                TableEditorV2Screen(templateState = if (location.areaId == "table-detail" && location.sessionId in setOf("cells", "structure")) guideQuickTemplate() else template,
                    includePathInCounterScope = settings.includePathInCounterScope,
                    includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
                    saveMode = settings.saveMode, counterPadding = settings.counterPadding,
                    styleState = style, openSaveSettingsInitially = location.areaId == "saving",
                    onSave = { _, _, _, _, _, _, _, _ -> false }, onBack = {}, templateName = templateName)
            }
        }
    }
}

@Composable
private fun LiveGalleryTutorial(location: TutorialLocation, settings: AppSettings) {
    val context = LocalContext.current
    val viewerPhotos by produceState<List<MediaImageItem>>(emptyList()) {
        run {
            value = try {
                withContext(Dispatchers.IO) {
                    DzlogMediaStoreReader(context.contentResolver).loadImagesUnderPrefix(GalleryFolderIndexPolicy.ROOT)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (_: Exception) { emptyList() }
        }
    }
    if (location.sessionId == "viewer" && viewerPhotos.isNotEmpty()) {
        Box(Modifier.fillMaxSize().tutorialTarget("gallery.content")) {
            LogViewerScreen(relativePath = viewerPhotos.first().relativePath,
                items = viewerPhotos, startIndex = 0, onBack = {}, onItemsReloaded = {},
                onRequestCloseViewer = {})
        }
    } else {
        val folder = if (location.pageId == "browse") viewerPhotos.firstOrNull()?.relativePath
            ?: GalleryFolderIndexPolicy.ROOT else GalleryFolderIndexPolicy.ROOT
        val target = if (location.pageId in setOf("photos/move", "photos/delete") && viewerPhotos.isEmpty()) "gallery.selection-actions"
            else if (location.sessionId == "viewer" || (location.pageId == "browse" && folder == GalleryFolderIndexPolicy.ROOT))
            "gallery.content" else LocalTutorialTarget.current
        CompositionLocalProvider(LocalTutorialTarget provides target) {
            LogFolderScreen(relativePath = folder,
                onBack = {}, onOpenFolder = {}, onOpenPhoto = { _, _ -> }, onOpenOriginal = { _, _ -> },
                gridColumns = settings.galleryGridColumns, showCounterBadges = settings.showGalleryCounterBadges)
        }
    }
    val selected = viewerPhotos.take(1)
    if (selected.isNotEmpty()) when (location.pageId) {
        "photos/move" -> com.dudoziworkshop.dzlog.ui.log.GalleryPhotoMoveConfirmation(
            selected = selected, allPhotos = viewerPhotos,
            destinationPath = GalleryFolderIndexPolicy.ROOT + "안내 예시/", busy = false,
            onChangeDestination = {}, onCancel = {}, onConfirm = {},
        )
        "photos/delete" -> com.dudoziworkshop.dzlog.ui.log.GalleryPhotoDeleteConfirmation(
            selected = selected, allPhotos = viewerPhotos, busy = false, onCancel = {}, onConfirm = {},
        )
    }
}

@Composable
private fun LiveHomeFolder(location: TutorialLocation, settings: AppSettings) {
    val context = LocalContext.current
    val latest by produceState<MediaImageItem?>(null) {
        value = try { withContext(Dispatchers.IO) { DzlogMediaStoreReader(context.contentResolver).loadLatestImage() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
    }
    val path = if (location.pageId == "recent-folder") latest?.relativePath else GalleryFolderIndexPolicy.ROOT
    if (path == null) androidx.compose.material3.Text("최근 촬영한 사진이 없어요.")
    else LogFolderScreen(relativePath = path, onBack = {}, onOpenFolder = {},
        onOpenPhoto = { _, _ -> }, onOpenOriginal = { _, _ -> },
        gridColumns = settings.galleryGridColumns, showCounterBadges = settings.showGalleryCounterBadges)
}

@Composable
private fun LiveHomeTemplates(location: TutorialLocation, template: TableTemplateState,
    style: TableStyleState, templateName: String) {
    val context = LocalContext.current
    val fallback = remember(template, style, templateName) {
        listOf(createSavedTableTemplate(templateName, template, style, modifiedAt = 0L, id = "tutorial-current"))
    }
    val templates by produceState(fallback) {
        val stored = context.dataStore.data.first()[KEY_TABLE_TEMPLATES_JSON]
        value = stored?.let(::savedTableTemplatesFromJson)?.takeIf { it.isNotEmpty() } ?: fallback
    }
    if (location.pageId == "templates") {
        Box(Modifier.fillMaxSize()) {
            HomeScreen(tableTemplateState = template, activeTemplateName = templateName,
                onOpenSettings = {}, onStartCamera = {}, onOpenTableEditor = {},
                onOpenAlbum = {}, onOpenRecentCaptureGrid = { _, _, _, _ -> })
            HomeTemplatePicker(templates, null, onSelect = {}, onManage = {}, onDismiss = {})
        }
    } else TableTemplateListScreen(templates = templates, activeTemplateId = null,
        onBack = {}, onOpenTemplate = {}, onSelectTemplate = {}, onCreateTemplate = {},
        onRenameTemplate = { _, _ -> }, onDuplicateTemplate = {}, onDeleteTemplate = {})
}
