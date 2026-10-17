package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
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
import kotlin.math.roundToInt
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
    onZoomTenthsChange: (Int) -> Unit,
    onZoomGestureFinished: (Int) -> Unit = onZoomTenthsChange,
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
            maxZoomTenths = normalizedMaxTenths,
            hapticEnabled = hapticEnabled,
            onZoomTenthsChange = onZoomTenthsChange,
            onZoomGestureFinished = onZoomGestureFinished,
            onToggleExpanded = onToggleExpanded,
        )
    }
}

@Composable
internal fun ZoomQuickPanelCore(
    zoomTenths: Int,
    maxZoomTenths: Int,
    hapticEnabled: Boolean,
    onZoomTenthsChange: (Int) -> Unit,
    onZoomGestureFinished: (Int) -> Unit,
    onToggleExpanded: () -> Unit,
) {
    val presets = rememberZoomPresets(maxZoomTenths)
    val left = presets.lastOrNull { it < zoomTenths }
    val right = presets.firstOrNull { it > zoomTenths }
    val haptic = LocalHapticFeedback.current
    val latestZoom by rememberUpdatedState(zoomTenths)
    val latestChange by rememberUpdatedState(onZoomTenthsChange)
    val latestFinish by rememberUpdatedState(onZoomGestureFinished)
    var dragValue = zoomTenths
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZoomNeighborChip(left, hapticEnabled, onZoomGestureFinished) {
            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 68.dp, minHeight = DDZLayout.Control.Standard)
                .clip(CHIP_SHAPE)
                .background(DDZColor.SelectedSoft, CHIP_SHAPE)
                .border(1.dp, DDZColor.SelectedDark, CHIP_SHAPE)
                .pointerInput(maxZoomTenths) {
                    var fractional = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragValue = latestZoom; fractional = 0f },
                        onDragEnd = { latestFinish(dragValue) },
                        onDragCancel = { latestFinish(dragValue) },
                    ) { change, delta ->
                        fractional += delta / 12.dp.toPx()
                        val steps = fractional.toInt()
                        if (steps != 0) {
                            fractional -= steps
                            dragValue = (dragValue + steps).coerceIn(10, maxZoomTenths)
                            latestChange(dragValue)
                        }
                        change.consume()
                    }
                }
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(formatZoomActualLabel(zoomTenths), color = DDZColor.SelectionEmphasis,
                style = DDZTypography.Caption)
        }
        ZoomNeighborChip(right, hapticEnabled, onZoomGestureFinished) {
            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

/** Only valid preset stops are selectable; never duplicate the device maximum. */
internal fun rememberZoomPresets(maxZoomTenths: Int): List<Int> {
    val end = maxZoomTenths.coerceIn(10, 100)
    return (listOf(10, 20, 40, 100).filter { it <= end } + end).distinct().sorted()
}

@Composable
private fun ZoomNeighborChip(
    value: Int?,
    hapticEnabled: Boolean,
    onSelect: (Int) -> Unit,
    onHaptic: () -> Unit,
) {
    Box(Modifier.defaultMinSize(minWidth = 42.dp, minHeight = DDZLayout.Control.Standard),
        contentAlignment = Alignment.Center) {
        if (value != null) {
            Box(Modifier.clip(CHIP_SHAPE)
                .background(DDZColor.Surface.copy(alpha = 0.95f), CHIP_SHAPE)
                .border(1.dp, DDZColor.SageBorder, CHIP_SHAPE)
                .clickable {
                    if (hapticEnabled) onHaptic()
                    onSelect(value)
                }
                .padding(horizontal = 8.dp, vertical = 7.dp)) {
                Text(formatZoomActualLabel(value), style = DDZTypography.Caption,
                    color = DDZColor.TextStrong)
            }
        }
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
