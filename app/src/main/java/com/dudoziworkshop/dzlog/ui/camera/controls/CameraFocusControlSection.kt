package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFocusMode
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun CameraFocusControlSection(
    focusUiValue: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(DDZColor.Surface.copy(alpha = 0.95f), RoundedCornerShape(DDZLayout.Radius.Full))
            .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(DDZLayout.Radius.Full))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = "초점", style = DDZTypography.Caption, color = DDZColor.TextStrong)
        Slider(
            modifier = Modifier.width(200.dp),
            value = focusUiValue.coerceIn(0f, 1f),
            // TODO: UI-only focus scaffold. Real camera focus control will be wired in a later phase.
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = DDZColor.SageDarkStrong,
                activeTrackColor = DDZColor.SagePrimary,
                inactiveTrackColor = DDZColor.Card.copy(alpha = 0.95f)
            )
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "근거리", style = DDZTypography.Caption, color = DDZColor.SageDarkStrong)
            Box(modifier = Modifier.weight(1f))
            Text(text = "원거리", style = DDZTypography.Caption, color = DDZColor.SageDarkStrong)
        }
    }
}

@Composable
internal fun CameraFocusCompactSection(
    mode: CameraFocusMode,
    onClick: () -> Unit,
) {
    val statusLabel = when (mode) {
        CameraFocusMode.AUTO -> "자동"
        CameraFocusMode.MANUAL -> "수동"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(DDZColor.Surface.copy(alpha = 0.95f), RoundedCornerShape(DDZLayout.Radius.Full))
            .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(DDZLayout.Radius.Full))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CenterFocusStrong,
            contentDescription = "초점",
            tint = DDZColor.SageDarkStrong,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = statusLabel,
            style = DDZTypography.Caption,
            color = DDZColor.SageDarkStrong,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
