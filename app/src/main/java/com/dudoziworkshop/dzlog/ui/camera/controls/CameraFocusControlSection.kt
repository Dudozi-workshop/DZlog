package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
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
    val sliderEnabled = mode == CameraFocusMode.MANUAL
    val normalizedFocusValue = focusUiValue.coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        FocusModeSegmentedControl(
            mode = mode,
            onModeChange = onModeChange,
        )

        FocusTickBar(
            value = normalizedFocusValue,
            enabled = sliderEnabled,
            hapticEnabled = hapticEnabled,
            onValueChange = onValueChange,
            onStepHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
            modifier = Modifier.height(44.dp)
        )

        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = DDZLayout.Control.Standard, minHeight = DDZLayout.Control.Standard)
                .background(DDZColor.Surface.copy(alpha = 0.9f), RoundedCornerShape(DDZLayout.Radius.Full))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatFocusUiValue(normalizedFocusValue),
                style = DDZTypography.Caption,
                color = DDZColor.SageDarkStrong
            )
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

@Composable
private fun FocusModeSegmentedControl(
    mode: CameraFocusMode,
    onModeChange: (CameraFocusMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(DDZColor.Card.copy(alpha = 0.95f), RoundedCornerShape(DDZLayout.Radius.Full))
            .padding(horizontal = 3.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FocusModeSegmentButton(
            label = "자동",
            selected = mode == CameraFocusMode.AUTO,
            onClick = { onModeChange(CameraFocusMode.AUTO) }
        )
        FocusModeSegmentButton(
            label = "수동",
            selected = mode == CameraFocusMode.MANUAL,
            onClick = { onModeChange(CameraFocusMode.MANUAL) }
        )
    }
}

@Composable
private fun FocusModeSegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) DDZColor.SageLight.copy(alpha = 0.82f) else Color.Transparent
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(32.dp)
            .background(background, RoundedCornerShape(DDZLayout.Radius.Full))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp)
    ) {
        Text(
            text = label,
            style = DDZTypography.Caption,
            color = DDZColor.SageDarkStrong
        )
    }
}
