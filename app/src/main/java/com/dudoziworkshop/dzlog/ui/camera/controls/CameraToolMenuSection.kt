package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.camera.state.CameraOverlayTool
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun CameraToolMenuSection(
    onSelectTool: (CameraOverlayTool) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(DDZColor.Surface.copy(alpha = 0.94f), RoundedCornerShape(DDZLayout.Radius.Full))
            .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(DDZLayout.Radius.Full))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        ToolMenuChip(label = "배율") { onSelectTool(CameraOverlayTool.ZOOM) }
        ToolMenuChip(label = "초점") { onSelectTool(CameraOverlayTool.FOCUS) }
        ToolMenuChip(label = "플래시") { onSelectTool(CameraOverlayTool.FLASH) }
    }
}

@Composable
private fun ToolMenuChip(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = DDZColor.TextStrong, style = DDZTypography.Caption)
    }
}
