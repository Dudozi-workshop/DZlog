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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

// 2단계 라운딩 토큰: 줌 칩/프리셋은 pill 계열로 Full 고정한다.
private val CHIP_SHAPE = RoundedCornerShape(DDZLayout.Radius.Full)
private val BASE_PRESET_VALUES_TENTHS = listOf(10, 20, 40)

private fun buildVisiblePresetTenths(normalizedMaxTenths: Int): List<Int> {
    return (BASE_PRESET_VALUES_TENTHS + normalizedMaxTenths)
        .filter { it <= normalizedMaxTenths }
        .distinct()
        .sorted()
}

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
    val zoomLabel = formatZoomActualLabel(normalizedTenths)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(DDZColor.Card.copy(alpha = 0f), RoundedCornerShape(DDZLayout.Radius.Medium))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        if (expanded) {
            ZoomPresetShortcutRow(
                normalizedTenths = normalizedTenths,
                presetTenthsList = buildVisiblePresetTenths(normalizedMaxTenths),
                hapticEnabled = hapticEnabled,
                onStepHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                onZoomTenthsChange = onZoomTenthsChange,
            )

            ZoomTickBar(
                zoomTenths = normalizedTenths,
                maxZoomTenths = normalizedMaxTenths,
                hapticEnabled = hapticEnabled,
                onZoomTenthsChange = onZoomTenthsChange,
                onStepHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                modifier = Modifier.fillMaxWidth(0.76f)
            )
        }

        ZoomCompactValueChip(
            zoomLabel = zoomLabel,
            onClick = onToggleExpanded,
        )
    }
}

@Composable
private fun ZoomCompactValueChip(
    zoomLabel: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = DDZLayout.Control.Standard, minHeight = DDZLayout.Control.Standard)
            .background(DDZColor.Surface.copy(alpha = 0.95f), CHIP_SHAPE)
            .border(1.dp, DDZColor.SageBorder, CHIP_SHAPE)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = zoomLabel, color = DDZColor.TextStrong, style = DDZTypography.Caption)
    }
}

@Composable
private fun ZoomPresetShortcutRow(
    normalizedTenths: Int,
    presetTenthsList: List<Int>,
    hapticEnabled: Boolean,
    onStepHaptic: () -> Unit,
    onZoomTenthsChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presetTenthsList.forEach { presetTenths ->
            val selected = normalizedTenths == presetTenths
            val presetBackground: Color
            val presetBorder: Color
            val presetText: Color
            if (selected) {
                presetBackground = DDZColor.SageLight.copy(alpha = 0.82f)
                presetBorder = DDZColor.SageDarkStrong
                presetText = DDZColor.SageDarkStrong
            } else {
                presetBackground = DDZColor.Primary.copy(alpha = 0.14f)
                presetBorder = DDZColor.Primary.copy(alpha = 0.30f)
                presetText = DDZColor.PrimaryElevated
            }

            Box(
                modifier = Modifier
                    .background(color = presetBackground, shape = CHIP_SHAPE)
                    .border(1.dp, presetBorder, CHIP_SHAPE)
                    .clickable {
                        if (hapticEnabled) onStepHaptic()
                        onZoomTenthsChange(presetTenths)
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
}
