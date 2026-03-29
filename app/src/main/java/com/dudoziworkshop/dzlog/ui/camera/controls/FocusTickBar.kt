package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlin.math.abs
import kotlin.math.roundToInt

private const val FOCUS_STEP_COUNT = 100
private const val FOCUS_HAPTIC_STEP_INTERVAL = 4
private const val FOCUS_HAPTIC_MIN_DRAG_DELTA = 0.02f
private const val FOCUS_MINOR_RENDER_INTERVAL = 2
private val FOCUS_EMPHASIZED_STEPS = (0..FOCUS_STEP_COUNT step 10).toSet()

@Composable
internal fun FocusTickBar(
    value: Float,
    enabled: Boolean,
    hapticEnabled: Boolean,
    onValueChange: (Float) -> Unit,
    onStepHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tickSpacingPx = with(density) { 9.dp.toPx() }
    val tickHeightPx = with(density) { 18.dp.toPx() }
    val pointerHeightPx = with(density) { 30.dp.toPx() }

    val normalizedValue = value.coerceIn(0f, 1f)
    val normalizedStep = (normalizedValue * FOCUS_STEP_COUNT).roundToInt().coerceIn(0, FOCUS_STEP_COUNT)

    val latestStep by rememberUpdatedState(normalizedStep)
    val latestEnabled by rememberUpdatedState(enabled)
    val latestHapticEnabled by rememberUpdatedState(hapticEnabled)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnStepHaptic by rememberUpdatedState(onStepHaptic)

    Canvas(
        modifier = modifier
            .fillMaxWidth(0.78f)
            .pointerInput(Unit, enabled) {
                detectTapGestures { offset ->
                    if (!latestEnabled) return@detectTapGestures
                    val centerX = size.width / 2f
                    val deltaStep = (offset.x - centerX) / tickSpacingPx
                    val mapped = (latestStep + deltaStep)
                        .roundToInt()
                        .coerceIn(0, FOCUS_STEP_COUNT)
                    latestOnValueChange(mapped / FOCUS_STEP_COUNT.toFloat())
                }
            }
            .pointerInput(Unit, enabled) {
                var dragStartStep = latestStep
                var dragStartX = 0f
                var hasDragStart = false
                var lastHapticStep = latestStep

                detectDragGestures(
                    onDragStart = { startOffset ->
                        if (latestEnabled) {
                            dragStartStep = latestStep
                            dragStartX = startOffset.x
                            hasDragStart = true
                            lastHapticStep = latestStep
                        }
                    },
                    onDragEnd = { hasDragStart = false },
                    onDragCancel = { hasDragStart = false },
                ) { change, _ ->
                    if (!latestEnabled) {
                        change.consume()
                        return@detectDragGestures
                    }

                    val startX = if (hasDragStart) dragStartX else change.position.x
                    val deltaStep = (startX - change.position.x) / tickSpacingPx
                    val mapped = (dragStartStep + deltaStep)
                        .roundToInt()
                        .coerceIn(0, FOCUS_STEP_COUNT)
                    latestOnValueChange(mapped / FOCUS_STEP_COUNT.toFloat())

                    val previousHapticStep = lastHapticStep
                    val valueDelta = abs(mapped - previousHapticStep) / FOCUS_STEP_COUNT.toFloat()
                    if (
                        latestHapticEnabled &&
                        mapped != previousHapticStep &&
                        mapped % FOCUS_HAPTIC_STEP_INTERVAL == 0 &&
                        valueDelta >= FOCUS_HAPTIC_MIN_DRAG_DELTA
                    ) {
                        latestOnStepHaptic()
                        lastHapticStep = mapped
                    }
                    change.consume()
                }
            }
    ) {
        val centerX = size.width / 2f
        val verticalCenter = size.height / 2f

        for (step in 0..FOCUS_STEP_COUNT) {
            if (step % FOCUS_MINOR_RENDER_INTERVAL != 0) continue
            val x = centerX + (step - normalizedStep) * tickSpacingPx
            if (x < 0f || x > size.width) continue

            val emphasized = step in FOCUS_EMPHASIZED_STEPS
            val tickColor = if (emphasized) {
                DDZColor.SageDark.copy(alpha = if (enabled) 0.82f else 0.42f)
            } else {
                DDZColor.SageBorder.copy(alpha = if (enabled) 0.68f else 0.3f)
            }

            drawLine(
                color = tickColor,
                start = Offset(x, verticalCenter - tickHeightPx / 2f),
                end = Offset(x, verticalCenter + tickHeightPx / 2f),
                strokeWidth = if (emphasized) 1.9.dp.toPx() else 1.2.dp.toPx()
            )
        }

        drawLine(
            color = if (enabled) DDZColor.SageDarkStrong else DDZColor.SageBorder,
            start = Offset(centerX, verticalCenter - pointerHeightPx / 2f),
            end = Offset(centerX, verticalCenter + pointerHeightPx / 2f),
            strokeWidth = 3.dp.toPx()
        )
    }
}
