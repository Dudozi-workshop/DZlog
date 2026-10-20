@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.capturePhysicalSavePaths
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.common.rememberThreeButtonNavEquivalentBottomPadding

@Composable
fun HomeScreen(
    tableTemplateState: TableTemplateState,
    activeTemplateName: String = "",
    onManageTemplates: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenSaveSettings: () -> Unit = onOpenTableEditor,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
) {
    var showSaveInfo by remember { mutableStateOf(false) }
    val homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val settings = uiState.settings
    val nextCounterPreview = uiState.nextCounterPreview
    val latestImage = uiState.latestImage

    androidx.compose.runtime.DisposableEffect(homeViewModel) {
        homeViewModel.activate(tableTemplateState)
        onDispose { homeViewModel.deactivate() }
    }

    LaunchedEffect(tableTemplateState) {
        homeViewModel.updateTemplate(tableTemplateState)
    }

    val defaultCaptureName = stringResource(com.dudoziworkshop.dzlog.R.string.home_default_capture)
    val normalizedTemplateName = activeTemplateName.trim().ifBlank { defaultCaptureName }
    val nextCounterText = remember(nextCounterPreview, settings.counterPadding) {
        val raw = nextCounterPreview.toString()
        if (settings.counterPadding > 0) raw.padStart(settings.counterPadding, '0') else raw
    }

    HomeContent(
        templateName = normalizedTemplateName,
        labels = HomeLabels(
            settings = stringResource(com.dudoziworkshop.dzlog.R.string.home_settings_content_description),
            currentTemplate = stringResource(com.dudoziworkshop.dzlog.R.string.home_current_capture),
            nextCounter = stringResource(com.dudoziworkshop.dzlog.R.string.home_next_capture, nextCounterText),
            startCapture = stringResource(com.dudoziworkshop.dzlog.R.string.home_start_capture),
            tableSettings = stringResource(com.dudoziworkshop.dzlog.R.string.home_table_settings),
            saveInfo = stringResource(com.dudoziworkshop.dzlog.R.string.home_save_settings),
        ),
        onManageTemplates = onManageTemplates, onOpenSettings = onOpenSettings,
        onStartCamera = onStartCamera, onOpenTableEditor = onOpenTableEditor,
        onOpenSaveInfo = { homeViewModel.refreshSavePreview(); showSaveInfo = true },
        modifier = Modifier.dzScreen(), bottomInset = rememberThreeButtonNavEquivalentBottomPadding(),
        background = { HomeAmbientBackground() },
        recentCapture = {
            HomeRecentCaptureSection(image = latestImage, timeText = uiState.latestImageTimeText,
                onOpenAlbum = onOpenAlbum, onOpenRecentCaptureGrid = onOpenRecentCaptureGrid)
        },
    )
    if (showSaveInfo) {
        HomeSaveInfoSheet(
            fileName = uiState.nextFilenamePreview,
            relativePaths = uiState.nextRelativePathPreview?.let {
                capturePhysicalSavePaths(it, settings.saveMode)
            }.orEmpty(),
            onOpenSaveSettings = {
                showSaveInfo = false
                onOpenSaveSettings()
            },
            onDismiss = { showSaveInfo = false },
        )
    }

}
