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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

// 2단계 라운딩 토큰: 줌 칩/프리셋은 pill 계열로 Full 고정한다.
private val CHIP_SHAPE = RoundedCornerShape(DDZLayout.Radius.Full)
private val PRESET_VALUES_TENTHS = listOf(10, 20, 40, 100)

@Composable
internal fun ZoomControlSection(
    zoomRatioTenths: Int,
    maxZoomTenths: Int,
    expanded: Boolean,
    hapticEnabled: Boolean,
    onToggleExpanded: () -> Unit,
    onZoomTenthsChange: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val normalizedMaxTenths = maxZoomTenths.coerceIn(10, 100)
    val normalizedTenths = zoomRatioTenths.coerceIn(10, normalizedMaxTenths)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        if (expanded) {
            ZoomDetailExtraSection(
                zoomTenths = normalizedTenths,
                maxZoomTenths = normalizedMaxTenths,
                hapticEnabled = hapticEnabled,
                onZoomTenthsChange = onZoomTenthsChange,
                onStepHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
            )
        }

        ZoomQuickPanelCore(
            zoomTenths = normalizedTenths,
            onToggleExpanded = onToggleExpanded,
        )
    }
}

@Composable
internal fun ZoomQuickPanelCore(
    zoomTenths: Int,
    onToggleExpanded: () -> Unit,
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = DDZLayout.Control.Standard, minHeight = DDZLayout.Control.Standard)
            .clip(CHIP_SHAPE)
            .background(DDZColor.Surface.copy(alpha = 0.95f), CHIP_SHAPE)
            .border(1.dp, DDZColor.SageBorder, CHIP_SHAPE)
            .clickable(onClick = onToggleExpanded)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = formatZoomActualLabel(zoomTenths), color = DDZColor.TextStrong, style = DDZTypography.Caption)
    }
}

@Composable
internal fun ZoomDetailExtraSection(
    zoomTenths: Int,
    maxZoomTenths: Int,
    hapticEnabled: Boolean,
    onZoomTenthsChange: (Int) -> Unit,
    onStepHaptic: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PRESET_VALUES_TENTHS.forEach { presetTenths ->
            val actualPreset = presetTenths.coerceIn(10, maxZoomTenths)
            val selected = zoomTenths == actualPreset
            val presetBackground: Color
            val presetBorder: Color
            val presetText: Color
            if (selected) {
                presetBackground = DDZColor.SageLight.copy(alpha = 0.92f)
                presetBorder = DDZColor.SageBorder
                presetText = DDZColor.SageDarkStrong
            } else {
                presetBackground = DDZColor.Surface.copy(alpha = 0.96f)
                presetBorder = DDZColor.SageBorder
                presetText = DDZColor.TextStrong
            }

            Box(
                modifier = Modifier
                    .clip(CHIP_SHAPE)
                    .background(color = presetBackground, shape = CHIP_SHAPE)
                    .border(1.dp, presetBorder, CHIP_SHAPE)
                    .clickable {
                        if (hapticEnabled) onStepHaptic()
                        onZoomTenthsChange(actualPreset)
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatZoomActualLabel(presetTenths),
                    style = DDZTypography.Caption,
                    color = presetText
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .background(DDZColor.Surface.copy(alpha = 0.96f), RoundedCornerShape(DDZLayout.Radius.Medium))
            .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(DDZLayout.Radius.Medium))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        ZoomTickBar(
            zoomTenths = zoomTenths,
            maxZoomTenths = maxZoomTenths,
            hapticEnabled = hapticEnabled,
            onZoomTenthsChange = onZoomTenthsChange,
            onStepHaptic = onStepHaptic,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
