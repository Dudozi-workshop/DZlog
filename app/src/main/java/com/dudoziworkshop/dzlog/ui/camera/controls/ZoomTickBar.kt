package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlin.math.roundToInt

private const val MIN_ZOOM_TENTHS = 10
private const val MAX_ZOOM_TENTHS = 100
private val EMPHASIZED_TICKS = setOf(10, 20, 40, 60, 80, 100)

@Composable
internal fun ZoomTickBar(
    zoomTenths: Int,
    maxZoomTenths: Int,
    hapticEnabled: Boolean,
    onZoomTenthsChange: (Int) -> Unit,
    onStepHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tickSpacingPx = with(density) { 8.dp.toPx() }
    val minorTickHeightPx = with(density) { 14.dp.toPx() }
    val majorTickHeightPx = with(density) { 22.dp.toPx() }
    val pointerHeightPx = with(density) { 30.dp.toPx() }

    val normalizedMaxTenths = maxZoomTenths.coerceIn(MIN_ZOOM_TENTHS, MAX_ZOOM_TENTHS)
    val normalizedTenths = zoomTenths.coerceIn(MIN_ZOOM_TENTHS, normalizedMaxTenths)

    val latestZoomTenths by rememberUpdatedState(normalizedTenths)
    val latestHapticEnabled by rememberUpdatedState(hapticEnabled)
    val latestOnZoomTenthsChange by rememberUpdatedState(onZoomTenthsChange)
    val latestOnStepHaptic by rememberUpdatedState(onStepHaptic)

    Canvas(
        modifier = modifier
            .height(44.dp)
            .pointerInput(normalizedMaxTenths) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val deltaTenths = (offset.x - centerX) / tickSpacingPx
                    val mapped = (latestZoomTenths + deltaTenths)
                        .roundToInt()
                        .coerceIn(MIN_ZOOM_TENTHS, normalizedMaxTenths)
                    latestOnZoomTenthsChange(mapped)
                }
            }
            .pointerInput(normalizedMaxTenths) {
                var dragStartZoomTenths = latestZoomTenths
                var accumulatedDragPx = 0f
                var lastHapticTenths = latestZoomTenths

                detectDragGestures(
                    onDragStart = {
                        dragStartZoomTenths = latestZoomTenths
                        accumulatedDragPx = 0f
                        lastHapticTenths = latestZoomTenths
                    },
                    onDragCancel = {
                        accumulatedDragPx = 0f
                    },
                    onDragEnd = {
                        accumulatedDragPx = 0f
                    }
                ) { change, dragAmount ->
                    accumulatedDragPx -= dragAmount.x
                    val deltaTenths = accumulatedDragPx / tickSpacingPx
                    val mapped = (dragStartZoomTenths + deltaTenths)
                        .roundToInt()
                        .coerceIn(MIN_ZOOM_TENTHS, normalizedMaxTenths)
                    latestOnZoomTenthsChange(mapped)
                    if (latestHapticEnabled && mapped != lastHapticTenths) {
                        latestOnStepHaptic()
                        lastHapticTenths = mapped
                    }
                    change.consume()
                }
            }
    ) {
        val centerX = size.width / 2f
        val verticalCenter = size.height / 2f

        for (tick in MIN_ZOOM_TENTHS..normalizedMaxTenths) {
            val x = centerX + (tick - normalizedTenths) * tickSpacingPx
            if (x < 0f || x > size.width) continue

            val emphasized = tick in EMPHASIZED_TICKS
            val tickHeight = if (emphasized) majorTickHeightPx else minorTickHeightPx
            val tickColor = if (emphasized) DDZColor.SageDark.copy(alpha = 0.84f) else DDZColor.SageBorder.copy(alpha = 0.92f)

            drawLine(
                color = tickColor,
                start = Offset(x, verticalCenter - tickHeight / 2f),
                end = Offset(x, verticalCenter + tickHeight / 2f),
                strokeWidth = if (emphasized) 2.2.dp.toPx() else 1.6.dp.toPx()
            )
        }

        drawLine(
            color = DDZColor.SageDarkStrong,
            start = Offset(centerX, verticalCenter - pointerHeightPx / 2f),
            end = Offset(centerX, verticalCenter + pointerHeightPx / 2f),
            strokeWidth = 3.dp.toPx()
        )
    }
}
