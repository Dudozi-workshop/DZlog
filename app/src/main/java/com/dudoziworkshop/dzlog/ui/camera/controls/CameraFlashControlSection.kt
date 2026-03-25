package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFlashMode
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun CameraFlashControlSection(
    mode: CameraFlashMode,
    onModeChange: (CameraFlashMode) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(DDZColor.Surface.copy(alpha = 0.95f), RoundedCornerShape(DDZLayout.Radius.Full))
            .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(DDZLayout.Radius.Full))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        FlashModeChip(label = "OFF", icon = Icons.Default.FlashOff, selected = mode == CameraFlashMode.OFF) {
            onModeChange(CameraFlashMode.OFF)
        }
        FlashModeChip(label = "AUTO", icon = Icons.Default.FlashAuto, selected = mode == CameraFlashMode.AUTO) {
            onModeChange(CameraFlashMode.AUTO)
        }
        FlashModeChip(label = "ON", icon = Icons.Default.FlashOn, selected = mode == CameraFlashMode.ON) {
            onModeChange(CameraFlashMode.ON)
        }
    }
}

@Composable
internal fun CameraFlashCompactSection(
    mode: CameraFlashMode,
    onClick: () -> Unit,
) {
    val label = when (mode) {
        CameraFlashMode.OFF -> "OFF"
        CameraFlashMode.AUTO -> "AUTO"
        CameraFlashMode.ON -> "ON"
    }
    FlashModeChip(
        label = label,
        icon = when (mode) {
            CameraFlashMode.OFF -> Icons.Default.FlashOff
            CameraFlashMode.AUTO -> Icons.Default.FlashAuto
            CameraFlashMode.ON -> Icons.Default.FlashOn
        },
        selected = true,
        onClick = onClick,
    )
}

@Composable
private fun FlashModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) DDZColor.SageLight.copy(alpha = 0.82f) else Color.Transparent
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(DDZLayout.Radius.Full))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DDZColor.SageDarkStrong,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = DDZTypography.Caption,
                color = DDZColor.SageDarkStrong,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
