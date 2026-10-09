package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFocusMode
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private val FocusChipShape = RoundedCornerShape(DDZLayout.Radius.Full)

@Composable
internal fun CameraFocusControlSection(
    mode: CameraFocusMode,
    focusUiValue: Float,
    expanded: Boolean,
    hapticEnabled: Boolean,
    onToggleExpanded: () -> Unit,
    onModeChange: (CameraFocusMode) -> Unit,
    onValueChange: (Float) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sliderEnabled = mode == CameraFocusMode.MANUAL
    val normalizedFocusValue = focusUiValue.coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (expanded) {
            FocusDetailExtraSection(
                normalizedFocusValue = normalizedFocusValue,
                sliderEnabled = sliderEnabled,
                hapticEnabled = hapticEnabled,
                onValueChange = onValueChange,
                onStepHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
            )
        }

        FocusQuickPanelCore(
            mode = mode,
            expanded = expanded,
            onToggleExpanded = onToggleExpanded,
            onModeChange = onModeChange,
        )
    }
}

@Composable
internal fun FocusQuickPanelCore(
    mode: CameraFocusMode,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onModeChange: (CameraFocusMode) -> Unit,
) {
    if (!expanded) {
        val modeLabel = if (mode == CameraFocusMode.AUTO) "자동" else "수동"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(FocusChipShape)
                .background(DDZColor.Surface.copy(alpha = 0.95f), FocusChipShape)
                .border(1.dp, DDZColor.SageBorder, FocusChipShape)
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "초점",
                tint = DDZColor.SageDarkStrong,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = modeLabel,
                style = DDZTypography.Caption,
                color = DDZColor.SageDarkStrong
            )
        }
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(FocusChipShape)
            .background(DDZColor.Surface.copy(alpha = 0.95f), FocusChipShape)
            .border(1.dp, DDZColor.SageBorder, FocusChipShape)
            .clickable {
                val toggled = if (mode == CameraFocusMode.AUTO) CameraFocusMode.MANUAL else CameraFocusMode.AUTO
                onModeChange(toggled)
            }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CenterFocusStrong,
            contentDescription = "초점",
            tint = DDZColor.SageDarkStrong,
            modifier = Modifier.size(14.dp),
        )
        FocusModeLabel(label = "자동", selected = mode == CameraFocusMode.AUTO)
        FocusModeLabel(label = "수동", selected = mode == CameraFocusMode.MANUAL)
    }
}

@Composable
internal fun FocusDetailExtraSection(
    normalizedFocusValue: Float,
    sliderEnabled: Boolean,
    hapticEnabled: Boolean,
    onValueChange: (Float) -> Unit,
    onStepHaptic: () -> Unit,
) {
    if (!sliderEnabled) return

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = DDZLayout.Control.Standard, minHeight = DDZLayout.Control.Standard)
            .clip(FocusChipShape)
            .background(DDZColor.Surface.copy(alpha = 0.96f), FocusChipShape)
            .border(1.dp, DDZColor.SageBorder, FocusChipShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatFocusUiValue(normalizedFocusValue),
            style = DDZTypography.Caption,
            color = DDZColor.SageDarkStrong
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .background(DDZColor.Surface.copy(alpha = 0.96f), RoundedCornerShape(DDZLayout.Radius.Medium))
            .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(DDZLayout.Radius.Medium))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        FocusTickBar(
            value = normalizedFocusValue,
            enabled = sliderEnabled,
            hapticEnabled = hapticEnabled,
            onValueChange = onValueChange,
            onStepHaptic = onStepHaptic,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )
    }
}

@Composable
private fun FocusModeLabel(
    label: String,
    selected: Boolean,
) {
    val background = if (selected) DDZColor.SageLight.copy(alpha = 0.90f) else Color.Transparent
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(FocusChipShape)
            .background(background, FocusChipShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = DDZTypography.Caption,
            color = DDZColor.SageDarkStrong
        )
    }
}
