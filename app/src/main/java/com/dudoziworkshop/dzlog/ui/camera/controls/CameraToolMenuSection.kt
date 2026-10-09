package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFlashMode
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFocusMode
import com.dudoziworkshop.dzlog.ui.camera.state.CameraOverlayTool
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout

private val ToolMenuChipShape = RoundedCornerShape(DDZLayout.Radius.Full)

@Composable
internal fun CameraToolMenuSection(
    selectedTool: CameraOverlayTool?,
    flashMode: CameraFlashMode,
    focusMode: CameraFocusMode,
    showGrid: Boolean,
    assistShutterEnabled: Boolean,
    onSelectTool: (CameraOverlayTool) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleAssistShutter: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(ToolMenuChipShape)
            .background(DDZColor.Surface.copy(alpha = 0.94f), ToolMenuChipShape)
            .border(1.dp, DDZColor.SageBorder, ToolMenuChipShape)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        ToolMenuChip(
            selected = selectedTool == CameraOverlayTool.FOCUS,
            onClick = { onSelectTool(CameraOverlayTool.FOCUS) }
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = when (focusMode) {
                    CameraFocusMode.AUTO -> "초점 자동"
                    CameraFocusMode.MANUAL -> "초점 수동"
                },
                modifier = Modifier.size(16.dp),
            )
        }
        ToolMenuChip(
            selected = selectedTool == CameraOverlayTool.FLASH,
            onClick = { onSelectTool(CameraOverlayTool.FLASH) }
        ) {
            Icon(
                imageVector = flashMode.toToolMenuIcon(),
                contentDescription = "플래시",
                modifier = Modifier.size(16.dp),
            )
        }
        ToolMenuChip(
            selected = showGrid,
            onClick = onToggleGrid
        ) {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = if (showGrid) "격자 숨기기" else "격자 표시",
                modifier = Modifier.size(16.dp),
            )
        }
        ToolMenuChip(
            selected = assistShutterEnabled,
            onClick = onToggleAssistShutter
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = if (assistShutterEnabled) "보조 셔터 끄기" else "보조 셔터 켜기",
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ToolMenuChip(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val chipBg = if (selected) DDZColor.SageLight.copy(alpha = 0.74f) else Color.Transparent
    val iconTint = if (selected) DDZColor.SageDarkStrong else DDZColor.SageDark
    Box(
        modifier = Modifier
            .clip(ToolMenuChipShape)
            .background(chipBg, ToolMenuChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides iconTint
        ) {
            content()
        }
    }
}

private fun CameraFlashMode.toToolMenuIcon(): ImageVector = when (this) {
    CameraFlashMode.OFF -> Icons.Default.FlashOff
    CameraFlashMode.AUTO -> Icons.Default.FlashAuto
    CameraFlashMode.ON -> Icons.Default.FlashOn
}
