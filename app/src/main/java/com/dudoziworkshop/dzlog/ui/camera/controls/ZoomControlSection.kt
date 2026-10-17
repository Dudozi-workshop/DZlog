package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

// 2단계 라운딩 토큰: 줌 칩/프리셋은 pill 계열로 Full 고정한다.
private val CHIP_SHAPE = RoundedCornerShape(DDZLayout.Radius.Full)

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
    var isScrubbing by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // S1: the moving ruler is shown for a manual expansion or during a chip drag.
        // The bottom three chips remain in place while the ruler grows upwards.
        if (expanded || isScrubbing) {
            ZoomDetailExtraSection(
                zoomTenths = normalizedTenths,
                maxZoomTenths = normalizedMaxTenths,
                hapticEnabled = hapticEnabled,
                onZoomTenthsChange = onZoomTenthsChange,
                onZoomGestureFinished = onZoomGestureFinished,
                onStepHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
            )
        }

        ZoomQuickPanelCore(
            zoomTenths = normalizedTenths,
            maxZoomTenths = normalizedMaxTenths,
            hapticEnabled = hapticEnabled,
            onZoomTenthsChange = onZoomTenthsChange,
            onZoomGestureFinished = onZoomGestureFinished,
            onDragActiveChange = { isScrubbing = it },
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
    onDragActiveChange: (Boolean) -> Unit,
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
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZoomNeighborChip(left, hapticEnabled, onZoomGestureFinished) {
            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 52.dp, minHeight = 30.dp)
                .clip(CHIP_SHAPE)
                .background(DDZColor.SelectedSoft.copy(alpha = 0.76f), CHIP_SHAPE)
                .border(1.dp, DDZColor.SelectedDark.copy(alpha = 0.75f), CHIP_SHAPE)
                .pointerInput(maxZoomTenths) {
                    var fractional = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragValue = latestZoom
                            fractional = 0f
                            onDragActiveChange(true)
                        },
                        onDragEnd = {
                            onDragActiveChange(false)
                            latestFinish(dragValue)
                        },
                        onDragCancel = {
                            onDragActiveChange(false)
                            latestFinish(dragValue)
                        },
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
                .padding(horizontal = 8.dp, vertical = 5.dp),
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
    Box(Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 30.dp),
        contentAlignment = Alignment.Center) {
        if (value != null) {
            Box(Modifier.clip(CHIP_SHAPE)
                .background(DDZColor.Surface.copy(alpha = 0.72f), CHIP_SHAPE)
                .border(1.dp, DDZColor.SageBorder, CHIP_SHAPE)
                .clickable {
                    if (hapticEnabled) onHaptic()
                    onSelect(value)
                }
                .padding(horizontal = 7.dp, vertical = 5.dp)) {
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
    onZoomGestureFinished: (Int) -> Unit = onZoomTenthsChange,
    onStepHaptic: () -> Unit,
) {
    // S1 iPhone-style moving scale: the tick marks move while the pointer stays
    // centered. The 3 always-visible chips below own the quick preset actions.
    val rulerShape = RoundedCornerShape(DDZLayout.Radius.Medium)
    Column(
        modifier = Modifier
            .width(280.dp)
            .clip(rulerShape)
            .background(DDZColor.AmbientWarm.copy(alpha = 0.82f), rulerShape)
            .border(1.dp, DDZColor.BorderStrong, rulerShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = formatZoomActualLabel(zoomTenths),
            color = DDZColor.SelectionEmphasis,
            style = DDZTypography.Body,
        )
        ZoomTickBar(
            zoomTenths = zoomTenths,
            maxZoomTenths = maxZoomTenths,
            hapticEnabled = hapticEnabled,
            onZoomTenthsChange = onZoomTenthsChange,
            onZoomGestureFinished = onZoomGestureFinished,
            onStepHaptic = onStepHaptic,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
