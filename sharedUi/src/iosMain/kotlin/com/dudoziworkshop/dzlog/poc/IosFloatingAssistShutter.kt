package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlin.math.roundToInt

private val IosAssistShutterSize = 46.dp
private const val IosAssistMinimumVisibleFraction = 0.35f

/** Camera-local floating shutter with the same movable normalized position contract as Android. */
@Composable
internal fun IosFloatingAssistShutter(
    visible: Boolean,
    captureEnabled: Boolean,
    xRatio: Float,
    yRatio: Float,
    onPositionRatioChange: (Float, Float) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val viewConfiguration = LocalViewConfiguration.current
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }
    var localCenterX by remember { mutableFloatStateOf(Float.NaN) }
    var localCenterY by remember { mutableFloatStateOf(Float.NaN) }
    var dragging by remember { mutableStateOf(false) }

    Box(modifier.onSizeChanged { size ->
        containerWidthPx = size.width.toFloat()
        containerHeightPx = size.height.toFloat()
    }) {
        if (containerWidthPx <= 0f || containerHeightPx <= 0f) return@Box
        val buttonSizePx = with(density) { IosAssistShutterSize.toPx() }
        val radius = buttonSizePx / 2f
        val minX = radius * IosAssistMinimumVisibleFraction
        val maxX = containerWidthPx - minX
        val minY = radius * IosAssistMinimumVisibleFraction
        val maxY = containerHeightPx - minY
        val seededX = (xRatio.coerceIn(0f, 1f) * containerWidthPx).coerceIn(minX, maxX)
        val seededY = (yRatio.coerceIn(0f, 1f) * containerHeightPx).coerceIn(minY, maxY)

        LaunchedEffect(containerWidthPx, containerHeightPx, xRatio, yRatio) {
            if (!dragging) {
                localCenterX = seededX
                localCenterY = seededY
            }
        }
        if (!localCenterX.isFinite() || !localCenterY.isFinite()) return@Box

        Box(
            Modifier
                .offset {
                    IntOffset((localCenterX - radius).roundToInt(), (localCenterY - radius).roundToInt())
                }
                .size(IosAssistShutterSize)
                .background(DDZColor.Surface.copy(alpha = 0.92f), CircleShape)
                .border(1.dp, DDZColor.SageDarkStrong, CircleShape)
                .semantics {
                    contentDescription = "보조 셔터"
                    if (!captureEnabled) disabled()
                    onClick {
                        if (captureEnabled) onCapture()
                        captureEnabled
                    }
                }
                .pointerInput(containerWidthPx, containerHeightPx, captureEnabled) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var pointerId = down.id
                        var moved = false
                        var total = Offset.Zero
                        var currentX = localCenterX
                        var currentY = localCenterY
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull() ?: break
                            pointerId = change.id
                            if (!change.pressed) break
                            val delta = change.positionChange()
                            total += delta
                            if (!moved && total.getDistance() > viewConfiguration.touchSlop) {
                                moved = true
                                dragging = true
                            }
                            if (moved) {
                                currentX = (currentX + delta.x).coerceIn(minX, maxX)
                                currentY = (currentY + delta.y).coerceIn(minY, maxY)
                                localCenterX = currentX
                                localCenterY = currentY
                                change.consume()
                            }
                        }
                        if (moved) {
                            dragging = false
                            onPositionRatioChange(currentX / containerWidthPx, currentY / containerHeightPx)
                        } else if (captureEnabled) {
                            onCapture()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(34.dp).background(
                if (captureEnabled) DDZColor.SagePrimary else DDZColor.IconMuted,
                CircleShape,
            ))
        }
    }
}
