@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dudoziworkshop.dzlog.ui.camera.controls

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.capture.policy.UndoCapturePolicy
import com.dudoziworkshop.dzlog.ui.camera.settings.CameraSettingsWriter
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFlashMode
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFocusMode
import com.dudoziworkshop.dzlog.ui.camera.state.CameraOverlayTool
import com.dudoziworkshop.dzlog.ui.camera.state.CameraUiState
import com.dudoziworkshop.dzlog.ui.log.DzThumbnail
import com.dudoziworkshop.dzlog.ui.log.parseG1G2FromRelativePath
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val BottomControlsHorizontalPadding = 12.dp
private val ToolOverlayBottomSpacing = 10.dp

internal fun isCaptureReady(
    scopeNextCounter: Int?,
    capturedUri: Uri?,
    isCapturing: Boolean,
    boundImageCaptureAvailable: Boolean,
): Boolean {
    return (
        boundImageCaptureAvailable &&
            capturedUri == null &&
            !isCapturing &&
            scopeNextCounter != null
        )
}

@Composable
internal fun CameraBottomControls(
    scope: CoroutineScope,
    ui: CameraUiState,
    settingsWriter: CameraSettingsWriter,
    captureReady: Boolean,
    latestImage: MediaImageItem?,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
    sessionCaptureStack: MutableList<List<Uri>>,
    undoPending: Boolean,
    onUndoDelete: (targetUris: List<Uri>) -> Unit,
    onTriggerCapture: () -> Unit,
    onOpenQuickValues: () -> Unit,
    onShutterButtonTopYChange: (Float?) -> Unit,
    hapticEnabled: Boolean,
) {
    val density = LocalDensity.current
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }

    val showToolMenu = ui.showToolMenu
    val selectedTool = ui.selectedTool
    val isToolPanelExpanded = ui.isToolPanelExpanded
    val compactTool = if (ui.isPinchZoomActive) null else selectedTool
    val showDismissLayer = showToolMenu || isToolPanelExpanded || ui.isZoomChipExpanded

    Box(modifier = Modifier.fillMaxSize()) {
        if (showDismissLayer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { ui.dismissToolOverlays() }
            )
        }

        CameraBottomBarRow(
            ui = ui,
            latestImage = latestImage,
            enabledNow = captureReady,
            sessionCaptureStack = sessionCaptureStack,
            undoPending = undoPending,
            onOpenAlbum = onOpenAlbum,
            onOpenRecentCaptureGrid = onOpenRecentCaptureGrid,
            onTriggerCapture = onTriggerCapture,
            onUndoDelete = onUndoDelete,
            onShutterButtonTopYChange = onShutterButtonTopYChange,
            onOpenQuickValues = {
                ui.dismissToolOverlays()
                onOpenQuickValues()
            },
            onBottomBarHeightChange = { bottomBarHeightPx = it }
        )

        CameraToolOverlayPanel(
            showToolMenu = showToolMenu,
            compactTool = compactTool,
            selectedTool = selectedTool,
            isToolPanelExpanded = isToolPanelExpanded,
            isPinchZoomActive = ui.isPinchZoomActive,
            zoomRatioTenths = ui.capture.actualZoomTenths,
            maxZoomTenths = ui.capture.maxZoomTenths,
            flashMode = ui.prefs.flashMode,
            focusMode = ui.focusMode,
            focusUiValue = ui.focusUiValue,
            showGrid = ui.prefs.showGrid,
            rotationCwDeg = ui.prefs.wmRotationCwDeg,
            bottomOffset = with(density) { bottomBarHeightPx.toDp() + ToolOverlayBottomSpacing },
            onSelectTool = { selectedTool ->
                ui.isZoomChipExpanded = false
                ui.showToolMenu = false
                ui.selectedTool = selectedTool
                ui.isToolPanelExpanded = false
            },
            onOpenSelectedToolPanel = { if (!ui.isPinchZoomActive) ui.isToolPanelExpanded = true },
            onZoomTenthsChange = { next ->
                val normalized = next.coerceIn(10, ui.capture.maxZoomTenths.coerceAtLeast(10))
                ui.prefs.zoomRatioTenths = normalized
                scope.launch { settingsWriter.setZoomTenths(normalized) }
            },
            onFocusModeChange = { mode ->
                ui.focusMode = mode
            },
            onFocusUiValueChange = {
                ui.focusUiValue = it
                ui.focusMode = CameraFocusMode.MANUAL
            },
            onFlashModeChange = { mode ->
                ui.prefs.flashMode = mode
                scope.launch { settingsWriter.setFlashMode(mode) }
            },
            onToggleGrid = {
                val next = !ui.prefs.showGrid
                ui.prefs.showGrid = next
                scope.launch { settingsWriter.setShowGrid(next) }
            },
            onRotateTable = {
                val next = if (ui.prefs.wmRotationCwDeg == 90) 0 else 90
                ui.prefs.wmRotationCwDeg = next
                scope.launch { settingsWriter.setWmRotationCwDeg(next) }
            },
            assistShutterEnabled = ui.prefs.assistShutterEnabled,
            onToggleAssistShutter = {
                val next = !ui.prefs.assistShutterEnabled
                ui.prefs.assistShutterEnabled = next
                scope.launch { settingsWriter.setAssistShutterEnabled(next) }
            },
            hapticEnabled = hapticEnabled,
        )

        // Camera zoom remains directly available even when the tool menu is closed.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp, end = 10.dp),
            contentAlignment = if (ui.isZoomChipExpanded) Alignment.TopCenter else Alignment.TopEnd,
        ) {
            ZoomControlSection(
                zoomRatioTenths = ui.capture.actualZoomTenths,
                maxZoomTenths = ui.capture.maxZoomTenths,
                expanded = ui.isZoomChipExpanded,
                hapticEnabled = hapticEnabled,
                onToggleExpanded = {
                    val next = !ui.isZoomChipExpanded
                    ui.dismissToolOverlays()
                    ui.isZoomChipExpanded = next
                },
                onZoomTenthsChange = { next ->
                    val normalized = next.coerceIn(10, ui.capture.maxZoomTenths.coerceAtLeast(10))
                    ui.prefs.zoomRatioTenths = normalized
                    scope.launch { settingsWriter.setZoomTenths(normalized) }
                },
            )
        }
    }
}

@Composable
private fun BoxScope.CameraBottomBarRow(
    ui: CameraUiState,
    latestImage: MediaImageItem?,
    enabledNow: Boolean,
    sessionCaptureStack: MutableList<List<Uri>>,
    undoPending: Boolean,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
    onTriggerCapture: () -> Unit,
    onUndoDelete: (List<Uri>) -> Unit,
    onShutterButtonTopYChange: (Float?) -> Unit,
    onOpenQuickValues: () -> Unit,
    onBottomBarHeightChange: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(bottom = DDZSpacing.screenPadding)
            .padding(horizontal = BottomControlsHorizontalPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { onBottomBarHeightChange(it.height) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(18f), contentAlignment = Alignment.Center) {
                RecentCaptureThumbButton(
                    latestImage = latestImage,
                    onClick = {
                        ui.dismissToolOverlays()
                        if (latestImage == null) {
                            onOpenAlbum()
                        } else {
                            val (g1, g2) = parseG1G2FromRelativePath(latestImage.relativePath)
                            onOpenRecentCaptureGrid(g1, g2, latestImage.relativePath, 0)
                        }
                    }
                )
            }

            Box(modifier = Modifier.weight(18f), contentAlignment = Alignment.Center) {
                CameraToolEntryButton(
                    onClick = {
                        if (ui.showToolMenu) {
                            ui.showToolMenu = false
                        } else if (ui.isToolPanelExpanded) {
                            ui.isToolPanelExpanded = false
                            ui.showToolMenu = true
                        } else {
                            ui.showToolMenu = true
                        }
                    }
                )
            }

            Box(modifier = Modifier.weight(28f), contentAlignment = Alignment.Center) {
                CaptureButtonSection(
                    ready = enabledNow,
                    onClick = {
                        onTriggerCapture()
                    },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        onShutterButtonTopYChange(coordinates.positionInRoot().y)
                    }
                )
            }

            Box(modifier = Modifier.weight(18f), contentAlignment = Alignment.Center) {
                QuickValueButton(onClick = onOpenQuickValues)
            }

            Box(modifier = Modifier.weight(18f), contentAlignment = Alignment.Center) {
                UndoCaptureButton(
                    enabled = sessionCaptureStack.isNotEmpty() && !undoPending,
                    onClick = {
                        ui.dismissToolOverlays()
                        if (undoPending) return@UndoCaptureButton
                        val targetUris = UndoCapturePolicy.consumeLatestCapture(stack = sessionCaptureStack)
                        if (targetUris.isEmpty()) return@UndoCaptureButton
                        onUndoDelete(targetUris)
                    }
                )
            }
        }
    }
}

@Composable
private fun BoxScope.CameraToolOverlayPanel(
    showToolMenu: Boolean,
    compactTool: CameraOverlayTool?,
    selectedTool: CameraOverlayTool?,
    isToolPanelExpanded: Boolean,
    isPinchZoomActive: Boolean,
    zoomRatioTenths: Int,
    maxZoomTenths: Int,
    flashMode: CameraFlashMode,
    focusMode: CameraFocusMode,
    focusUiValue: Float,
    showGrid: Boolean,
    rotationCwDeg: Int,
    bottomOffset: androidx.compose.ui.unit.Dp,
    onSelectTool: (CameraOverlayTool) -> Unit,
    onOpenSelectedToolPanel: () -> Unit,
    onZoomTenthsChange: (Int) -> Unit,
    onFocusModeChange: (CameraFocusMode) -> Unit,
    onFocusUiValueChange: (Float) -> Unit,
    onFlashModeChange: (CameraFlashMode) -> Unit,
    onToggleGrid: () -> Unit,
    onRotateTable: () -> Unit,
    assistShutterEnabled: Boolean,
    onToggleAssistShutter: () -> Unit,
    hapticEnabled: Boolean,
) {
    if (!showToolMenu && compactTool == null) return

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = DDZSpacing.screenPadding + bottomOffset),
        contentAlignment = Alignment.BottomCenter
    ) {
        when {
            showToolMenu -> CameraToolMenuSection(
                selectedTool = selectedTool,
                flashMode = flashMode,
                focusMode = focusMode,
                showGrid = showGrid,
                rotationCwDeg = rotationCwDeg,
                assistShutterEnabled = assistShutterEnabled,
                onSelectTool = onSelectTool,
                onToggleGrid = onToggleGrid,
                onRotateTable = onRotateTable,
                onToggleAssistShutter = onToggleAssistShutter,
            )
            compactTool == CameraOverlayTool.ZOOM && !isToolPanelExpanded -> ZoomControlSection(
                zoomRatioTenths = zoomRatioTenths,
                maxZoomTenths = maxZoomTenths,
                expanded = false,
                hapticEnabled = hapticEnabled,
                onToggleExpanded = onOpenSelectedToolPanel,
                onZoomTenthsChange = onZoomTenthsChange
            )
            compactTool == CameraOverlayTool.FLASH && !isToolPanelExpanded -> CameraFlashCompactSection(
                mode = flashMode,
                onClick = onOpenSelectedToolPanel
            )
            compactTool == CameraOverlayTool.FOCUS && !isToolPanelExpanded -> CameraFocusControlSection(
                mode = focusMode,
                focusUiValue = focusUiValue,
                expanded = false,
                hapticEnabled = hapticEnabled,
                onToggleExpanded = onOpenSelectedToolPanel,
                onModeChange = onFocusModeChange,
                onValueChange = onFocusUiValueChange
            )
            selectedTool == CameraOverlayTool.ZOOM && isToolPanelExpanded && !isPinchZoomActive -> ZoomControlSection(
                zoomRatioTenths = zoomRatioTenths,
                maxZoomTenths = maxZoomTenths,
                expanded = true,
                hapticEnabled = hapticEnabled,
                onToggleExpanded = {},
                onZoomTenthsChange = onZoomTenthsChange
            )
            selectedTool == CameraOverlayTool.FOCUS && isToolPanelExpanded && !isPinchZoomActive -> CameraFocusControlSection(
                mode = focusMode,
                focusUiValue = focusUiValue,
                expanded = true,
                hapticEnabled = hapticEnabled,
                onToggleExpanded = {},
                onModeChange = onFocusModeChange,
                onValueChange = onFocusUiValueChange
            )
            selectedTool == CameraOverlayTool.FLASH && isToolPanelExpanded && !isPinchZoomActive -> CameraFlashControlSection(
                mode = flashMode,
                onModeChange = onFlashModeChange
            )
        }
    }
}

@Composable
private fun CameraToolEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CameraControlButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "촬영 도구",
            tint = DDZColor.SageDarkStrong
        )
    }
}

// 2단계 라운딩 토큰: 카메라 하단의 소형 컨트롤은 Small로 통일한다.
private val CameraCompactControlShape = RoundedCornerShape(DDZLayout.Radius.Small)

@Composable
private fun CameraControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = DDZColor.SageBorder,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CameraCompactControlShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(DDZLayout.Control.CameraSmall)
                .clip(CameraCompactControlShape)
                .border(1.dp, borderColor, CameraCompactControlShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
private fun QuickValueButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CameraControlButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "빠른 값 변경",
            tint = DDZColor.SageDarkStrong,
        )
    }
}

@Composable
private fun UndoCaptureButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CameraControlButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = if (enabled) DDZColor.SagePrimary else Color.Transparent
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            tint = if (enabled) Color.White else DDZColor.SageDark
        )
    }
}

@Composable
private fun RecentCaptureThumbButton(
    latestImage: MediaImageItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CameraControlButton(onClick = onClick, modifier = modifier) {
        if (latestImage != null) {
            DzThumbnail(latestImage.uri.toString())
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "사진 목록",
                tint = DDZColor.SageDarkStrong,
            )
        }
    }
}
