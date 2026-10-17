package com.dudoziworkshop.dzlog.ui.camera.controls

import android.graphics.Paint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlin.math.roundToInt

private const val MIN_ZOOM_TENTHS = 10
private const val MAX_ZOOM_TENTHS = 100
/** Mockup 2: horizontal bottom baseline, long upright major marks. */
internal enum class ZoomTickWeight { MINOR, HALF, MAJOR }
internal fun zoomTickWeight(tenths: Int): ZoomTickWeight = when {
    tenths % 10 == 0 -> ZoomTickWeight.MAJOR
    tenths % 5 == 0 -> ZoomTickWeight.HALF
    else -> ZoomTickWeight.MINOR
}
internal fun zoomTickLabel(tenths: Int): String? =
    if (tenths >= MIN_ZOOM_TENTHS && tenths % 10 == 0) (tenths / 10).toString() else null

private data class ZoomDragSession(
    val startZoomTenths: Int,
    val startX: Float,
    val lastHapticTenths: Int,
)

@Composable
internal fun ZoomTickBar(
    zoomTenths: Int,
    maxZoomTenths: Int,
    hapticEnabled: Boolean,
    onZoomTenthsChange: (Int) -> Unit,
    onZoomGestureFinished: (Int) -> Unit = onZoomTenthsChange,
    onStepHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tickSpacingPx = with(density) { 8.dp.toPx() }
    val minorTickHeightPx = with(density) { 9.dp.toPx() }
    val halfTickHeightPx = with(density) { 15.dp.toPx() }
    val majorTickHeightPx = with(density) { 25.dp.toPx() }
    val baselineBottomInsetPx = with(density) { 18.dp.toPx() }
    val pointerHeightPx = with(density) { 21.dp.toPx() }
    val pointerTipHeightPx = with(density) { 7.dp.toPx() }
    val pointerHalfWidthPx = with(density) { 5.dp.toPx() }
    val zoomLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DDZColor.OverlayText.copy(alpha = 0.94f).toArgb()
        textAlign = Paint.Align.CENTER
        textSize = with(density) { 11.sp.toPx() }
    }

    val normalizedMaxTenths = maxZoomTenths.coerceIn(MIN_ZOOM_TENTHS, MAX_ZOOM_TENTHS)
    val normalizedTenths = zoomTenths.coerceIn(MIN_ZOOM_TENTHS, normalizedMaxTenths)

    val latestZoomTenths by rememberUpdatedState(normalizedTenths)
    val latestHapticEnabled by rememberUpdatedState(hapticEnabled)
    val latestOnZoomTenthsChange by rememberUpdatedState(onZoomTenthsChange)
    val latestOnZoomGestureFinished by rememberUpdatedState(onZoomGestureFinished)
    val latestOnStepHaptic by rememberUpdatedState(onStepHaptic)

    Canvas(
        modifier = modifier
            .height(50.dp)
            .pointerInput(normalizedMaxTenths) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val deltaTenths = (offset.x - centerX) / tickSpacingPx
                    val mapped = (latestZoomTenths + deltaTenths)
                        .roundToInt()
                        .coerceIn(MIN_ZOOM_TENTHS, normalizedMaxTenths)
                    latestOnZoomTenthsChange(mapped)
                    latestOnZoomGestureFinished(mapped)
                }
            }
            .pointerInput(normalizedMaxTenths) {
                var dragSession: ZoomDragSession? = null
                var lastGestureValue = latestZoomTenths

                detectDragGestures(
                    onDragStart = { startOffset ->
                        lastGestureValue = latestZoomTenths
                        dragSession = ZoomDragSession(
                            startZoomTenths = latestZoomTenths,
                            startX = startOffset.x,
                            lastHapticTenths = latestZoomTenths,
                        )
                    },
                    onDragEnd = {
                        dragSession = null
                        latestOnZoomGestureFinished(lastGestureValue)
                    },
                    onDragCancel = {
                        dragSession = null
                        latestOnZoomGestureFinished(lastGestureValue)
                    },
                ) { change, _ ->
                    val session = dragSession ?: ZoomDragSession(
                        startZoomTenths = latestZoomTenths,
                        startX = change.position.x,
                        lastHapticTenths = latestZoomTenths,
                    ).also { dragSession = it }

                    // Match the main chip gesture: swipe right increases magnification.
                    val deltaTenths = (change.position.x - session.startX) / tickSpacingPx
                    val mapped = (session.startZoomTenths + deltaTenths)
                        .roundToInt()
                        .coerceIn(MIN_ZOOM_TENTHS, normalizedMaxTenths)
                    latestOnZoomTenthsChange(mapped)
                    lastGestureValue = mapped
                    val previousHapticTenths = session.lastHapticTenths
                    // One gentle cue per full step avoids vibrating every 0.1x.
                    if (latestHapticEnabled && mapped / 10 != previousHapticTenths / 10) {
                        latestOnStepHaptic()
                        dragSession = session.copy(lastHapticTenths = mapped)
                    }
                    change.consume()
                }
            }
    ) {
        val centerX = size.width / 2f
        val baselineY = size.height - baselineBottomInsetPx

        // Every tick rises from the same horizontal baseline.
        drawLine(
            color = DDZColor.OverlayText.copy(alpha = 0.30f),
            start = Offset(0f, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 1.dp.toPx()
        )
        for (tick in MIN_ZOOM_TENTHS..normalizedMaxTenths) {
            val x = centerX + (tick - normalizedTenths) * tickSpacingPx
            if (x < 0f || x > size.width || kotlin.math.abs(x - centerX) < 2.dp.toPx()) continue

            val weight = zoomTickWeight(tick)
            val height = when (weight) {
                ZoomTickWeight.MAJOR -> majorTickHeightPx
                ZoomTickWeight.HALF -> halfTickHeightPx
                ZoomTickWeight.MINOR -> minorTickHeightPx
            }
            val opacity = when (weight) {
                ZoomTickWeight.MAJOR -> 0.94f
                ZoomTickWeight.HALF -> 0.74f
                ZoomTickWeight.MINOR -> 0.42f
            }
            drawLine(
                color = DDZColor.OverlayText.copy(alpha = opacity),
                start = Offset(x, baselineY - height),
                end = Offset(x, baselineY),
                strokeWidth = if (weight == ZoomTickWeight.MAJOR) 2.dp.toPx() else 1.dp.toPx()
            )
            val label = zoomTickLabel(tick)
            if (label != null && x > 12.dp.toPx() && x < size.width - 12.dp.toPx()) {
                drawContext.canvas.nativeCanvas.drawText(
                    label, x, baselineY + 12.dp.toPx(), zoomLabelPaint
                )
            }
        }

        // Fixed pointer: inverted triangle + sage line, as in mockup 2.
        val pointerTop = baselineY - pointerHeightPx
        val triangle = Path().apply {
            moveTo(centerX - pointerHalfWidthPx, pointerTop - pointerTipHeightPx)
            lineTo(centerX + pointerHalfWidthPx, pointerTop - pointerTipHeightPx)
            lineTo(centerX, pointerTop - 1.dp.toPx())
            close()
        }
        drawPath(triangle, DDZColor.SelectedSoft)
        drawLine(
            color = DDZColor.SageBorder,
            start = Offset(centerX, pointerTop),
            end = Offset(centerX, baselineY),
            strokeWidth = 2.7.dp.toPx()
        )
    }
}
