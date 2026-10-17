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
import com.dudoziworkshop.dzlog.ui.log.LogFolderScreen
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
            "home" -> HomeScreen(tableTemplateState = template, activeTemplateName = templateName,
                onOpenSettings = {}, onStartCamera = {}, onOpenTableEditor = {}, onOpenAlbum = {},
                onOpenRecentCaptureGrid = { _, _, _, _ -> })
            "camera" -> CameraScreen(tableTemplateState = template, onTemplateChange = {},
                onOpenTableEditor = {}, onOpenSaveSettings = {}, onOpenAlbum = {},
                onOpenRecentCaptureGrid = { _, _, _, _ -> },
                sessionCaptureStack = remember { mutableStateListOf() },
                phraseUndoHistory = remember { CapturePhraseUndoHistory() },
                quickTemplateName = templateName,
                quickEditSelectionMemory = remember { QuickEditSelectionMemory() })
            "gallery" -> LogFolderScreen(relativePath = GalleryFolderIndexPolicy.ROOT,
                onBack = {}, onOpenFolder = {}, onOpenPhoto = { _, _ -> }, onOpenOriginal = { _, _ -> },
                gridColumns = settings.galleryGridColumns, showCounterBadges = settings.showGalleryCounterBadges)
            "settings" -> SettingsRootScreen(onBack = {})
            else -> key(location.areaId, location.sessionId) {
                TableEditorV2Screen(templateState = template,
                    includePathInCounterScope = settings.includePathInCounterScope,
                    includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
                    saveMode = settings.saveMode, counterPadding = settings.counterPadding,
                    styleState = style, openSaveSettingsInitially = location.areaId == "saving",
                    onSave = { _, _, _, _, _, _, _, _ -> false }, onBack = {}, templateName = templateName)
            }
        }
    }
}
