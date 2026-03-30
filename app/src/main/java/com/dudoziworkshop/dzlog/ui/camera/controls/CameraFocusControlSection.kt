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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFocusMode
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun CameraFocusControlSection(
    mode: CameraFocusMode,
    focusUiValue: Float,
    hapticEnabled: Boolean,
    onModeChange: (CameraFocusMode) -> Unit,
    onValueChange: (Float) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val normalizedFocusValue = focusUiValue.coerceIn(0f, 1f)
    val isManual = mode == CameraFocusMode.MANUAL

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (isManual) {
            Text(
                text = formatFocusUiValue(normalizedFocusValue),
                style = DDZTypography.Body,
                color = DDZColor.TextStrong,
            )

            Box(
                modifier = Modifier
                    .background(DDZColor.PrimaryDark.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                    .border(1.dp, DDZColor.Border.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(vertical = 5.dp)
            ) {
                FocusTickBar(
                    value = normalizedFocusValue,
                    enabled = true,
                    hapticEnabled = hapticEnabled,
                    onValueChange = onValueChange,
                    onStepHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    modifier = Modifier.height(44.dp)
                )
            }
        }

        FocusModeInlineControl(
            mode = mode,
            onToggleMode = {
                onModeChange(
                    if (mode == CameraFocusMode.AUTO) CameraFocusMode.MANUAL
                    else CameraFocusMode.AUTO
                )
            },
        )
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

@Composable
private fun FocusModeInlineControl(
    mode: CameraFocusMode,
    onToggleMode: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(DDZColor.Surface.copy(alpha = 0.95f), RoundedCornerShape(DDZLayout.Radius.Full))
            .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(DDZLayout.Radius.Full))
            .clickable(onClick = onToggleMode)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CenterFocusStrong,
            contentDescription = "초점 모드",
            tint = DDZColor.SageDarkStrong,
            modifier = Modifier.size(14.dp),
        )

        FocusModeInlineItem(
            label = "자동",
            selected = mode == CameraFocusMode.AUTO,
        )
        FocusModeInlineItem(
            label = "수동",
            selected = mode == CameraFocusMode.MANUAL,
        )
    }
}

@Composable
private fun FocusModeInlineItem(
    label: String,
    selected: Boolean,
) {
    val background = if (selected) DDZColor.SageLight.copy(alpha = 0.92f) else Color.Transparent
    Text(
        text = label,
        style = DDZTypography.Caption.copy(
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        ),
        color = DDZColor.SageDarkStrong,
        modifier = Modifier
            .background(background, RoundedCornerShape(DDZLayout.Radius.Full))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
