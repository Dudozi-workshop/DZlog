package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFlashMode
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFocusMode
import com.dudoziworkshop.dzlog.ui.camera.state.CameraOverlayTool
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun CameraToolMenuSection(
    selectedTool: CameraOverlayTool?,
    zoomRatioTenths: Int,
    flashMode: CameraFlashMode,
    focusMode: CameraFocusMode,
    onSelectTool: (CameraOverlayTool) -> Unit,
) {
    val zoomLabel = "${(zoomRatioTenths.coerceIn(10, 100) / 10)}x"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(DDZColor.Surface.copy(alpha = 0.94f), RoundedCornerShape(DDZLayout.Radius.Full))
            .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(DDZLayout.Radius.Full))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        ToolMenuChip(
            selected = selectedTool == CameraOverlayTool.ZOOM,
            onClick = { onSelectTool(CameraOverlayTool.ZOOM) }
        ) {
            Text(text = zoomLabel, style = DDZTypography.Caption)
        }
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
            .background(chipBg, RoundedCornerShape(DDZLayout.Radius.Full))
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
