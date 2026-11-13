package com.dudoziworkshop.dzlog.ui.camera.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlin.math.roundToInt

/**
 * A tiny background-free exposure line. The sun IS the slider thumb;
 * it is not a separate icon beside a Material slider.
 *
 * The touch surface is 116x36dp despite the 2dp visual track.
 */
@Composable
internal fun CompactExposureSlider(
    index: Int,
    minIndex: Int,
    maxIndex: Int,
    above: Boolean,
    locked: Boolean,
    onIndexChange: (Int) -> Unit,
    onInteractionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (maxIndex <= minIndex) return
    val density = LocalDensity.current
    val trackInsetPx = with(density) { 9.dp.toPx() }
    val sunSizePx = with(density) { 16.dp.toPx() }
    // Put the visible line next to the ring, leaving the rest of the 36dp
    // hit area on the side away from it. Keep a full-size touch surface.
    val trackYpx = with(density) {
        (if (above) FOCUS_EV_TOUCH_HEIGHT_DP - FOCUS_EV_TRACK_INSET_DP
         else FOCUS_EV_TRACK_INSET_DP).dp.toPx()
    }
    val trackWidthPx = with(density) { FOCUS_EV_WIDTH_DP.dp.toPx() } - trackInsetPx * 2f
    val fraction = ((index.coerceIn(minIndex, maxIndex) - minIndex).toFloat() /
        (maxIndex - minIndex).toFloat()).coerceIn(0f, 1f)
    val thumbCenterX = trackInsetPx + fraction * trackWidthPx
    val accent = if (locked) DDZColor.CameraFocusLocked else DDZColor.SelectedSoft
    val latestIndexChange by rememberUpdatedState(onIndexChange)
    val latestInteractionChange by rememberUpdatedState(onInteractionChange)

    DisposableEffect(Unit) {
        onDispose { latestInteractionChange(false) }
    }

    Box(
        modifier = modifier
            .width(FOCUS_EV_WIDTH_DP.dp)
            .height(FOCUS_EV_TOUCH_HEIGHT_DP.dp)
            .semantics { contentDescription = "밝기 조절" }
            .pointerInput(minIndex, maxIndex) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    latestInteractionChange(true)
                    fun changeAt(x: Float) {
                        val available = (size.width - 2f * trackInsetPx).coerceAtLeast(1f)
                        val ratio = ((x - trackInsetPx) / available).coerceIn(0f, 1f)
                        val value = minIndex + ((maxIndex - minIndex) * ratio).roundToInt()
                        latestIndexChange(value)
                    }
                    changeAt(firstDown.position.x)
                    firstDown.consume()
                    try {
                        var pressed = true
                        while (pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == firstDown.id }
                            pressed = change?.pressed == true
                            if (pressed && change != null) {
                                changeAt(change.position.x)
                                change.consume()
                            }
                        }
                    } finally {
                        latestInteractionChange(false)
                    }
                }
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val start = Offset(trackInsetPx, trackYpx)
            val end = Offset(size.width - trackInsetPx, trackYpx)
            val thickness = 2.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = start,
                end = end,
                strokeWidth = thickness,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = accent,
                start = start,
                end = Offset(thumbCenterX, trackYpx),
                strokeWidth = thickness,
                cap = StrokeCap.Round,
            )
        }
        Icon(
            imageVector = Icons.Default.WbSunny,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (thumbCenterX - sunSizePx / 2f).roundToInt(),
                        (trackYpx - sunSizePx / 2f).roundToInt(),
                    )
                }
                .size(16.dp),
        )
    }
}
