package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private val ToolbarShape = RoundedCornerShape(16.dp)

/**
 * Contextual actions for the selected camera table. Size and lock are displayed
 * but remain disabled until their persisted behaviors ship in Phase 2.
 */
@Composable
internal fun CameraTableSelectionToolbar(
    onOpenDetails: () -> Unit,
    onRotate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(ToolbarShape)
            .background(DDZColor.Surface.copy(alpha = 0.96f))
            .border(1.dp, DDZColor.SageBorder, ToolbarShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CameraTableAction("상세", Icons.Default.Edit, true, onOpenDetails)
        CameraTableAction("크기", Icons.Default.AspectRatio, false, {})
        CameraTableAction("회전", Icons.AutoMirrored.Filled.RotateRight, true, onRotate)
        CameraTableAction("잠금", Icons.Default.Lock, false, {})
    }
}

@Composable
private fun CameraTableAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) DDZColor.SageDarkStrong else DDZColor.TextMuted
    Column(
        modifier = Modifier
            .width(66.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint)
        Text(text = label, style = DDZTypography.Caption, color = tint)
    }
}
