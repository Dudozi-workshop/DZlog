package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlin.math.roundToInt

private val ASSIST_SHUTTER_BUTTON_SIZE: Dp = 46.dp
private const val MIN_VISIBLE_FRACTION = 0.35f

@Composable
internal fun FloatingAssistShutterButton(
    enabled: Boolean,
    xRatio: Float,
    yRatio: Float,
    onPositionRatioChange: (Float, Float) -> Unit,
    onTapCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    val viewConfiguration = LocalViewConfiguration.current
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }
    var localCenterX by remember { mutableFloatStateOf(Float.NaN) }
    var localCenterY by remember { mutableFloatStateOf(Float.NaN) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.onSizeChanged { size ->
            containerWidthPx = size.width.toFloat()
            containerHeightPx = size.height.toFloat()
        }
    ) {
        if (containerWidthPx > 0f && containerHeightPx > 0f) {
            val buttonSizePx = with(density) { ASSIST_SHUTTER_BUTTON_SIZE.toPx() }
            val buttonRadiusPx = buttonSizePx / 2f
            val minCenterX = buttonRadiusPx * MIN_VISIBLE_FRACTION
            val maxCenterX = containerWidthPx - minCenterX
            val minCenterY = buttonRadiusPx * MIN_VISIBLE_FRACTION
            val maxCenterY = containerHeightPx - minCenterY

            fun normalizeCenter(ratio: Float, max: Float): Float {
                if (!ratio.isFinite()) return 0f
                return ratio * max
            }

            val seededCenterX = normalizeCenter(xRatio, containerWidthPx).coerceIn(minCenterX, maxCenterX)
            val seededCenterY = normalizeCenter(yRatio, containerHeightPx).coerceIn(minCenterY, maxCenterY)

            LaunchedEffect(containerWidthPx, containerHeightPx, xRatio, yRatio) {
                if (!isDragging) {
                    localCenterX = seededCenterX
                    localCenterY = seededCenterY
                }
            }

            if (localCenterX.isFinite() && localCenterY.isFinite()) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (localCenterX - buttonRadiusPx).roundToInt(),
                                y = (localCenterY - buttonRadiusPx).roundToInt(),
                            )
                        }
                        .size(ASSIST_SHUTTER_BUTTON_SIZE)
                        .background(
                            color = DDZColor.Surface,
                            shape = CircleShape,
                        )
                        .border(
                            width = 1.dp,
                            color = DDZColor.SageDarkStrong,
                            shape = CircleShape,
                        )
                        .pointerInput(containerWidthPx, containerHeightPx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var pointerId = down.id
                                var dragged = false
                                var totalDelta = Offset.Zero
                                var currentCenterX = localCenterX
                                var currentCenterY = localCenterY

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                                        ?: break
                                    pointerId = change.id

                                    if (!change.pressed) break

                                    val delta = change.positionChange()
                                    totalDelta += delta
                                    if (!dragged && totalDelta.getDistance() > viewConfiguration.touchSlop) {
                                        dragged = true
                                        isDragging = true
                                    }

                                    if (dragged) {
                                        currentCenterX = (currentCenterX + delta.x).coerceIn(minCenterX, maxCenterX)
                                        currentCenterY = (currentCenterY + delta.y).coerceIn(minCenterY, maxCenterY)
                                        localCenterX = currentCenterX
                                        localCenterY = currentCenterY
                                    }
                                    change.consume()
                                }

                                if (dragged) {
                                    isDragging = false
                                    onPositionRatioChange(
                                        currentCenterX / containerWidthPx,
                                        currentCenterY / containerHeightPx,
                                    )
                                } else {
                                    onTapCapture()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = DDZColor.SagePrimary,
                                shape = CircleShape,
                            )
                    )
                }
            }
        }
    }
}
