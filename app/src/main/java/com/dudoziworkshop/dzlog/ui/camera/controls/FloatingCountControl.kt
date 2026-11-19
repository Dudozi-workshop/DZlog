package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlin.math.roundToInt

private val BUTTON_SIZE = 48.dp
private val BUTTON_SPACING = 3.dp

/**
 * Three floating controls, one draggable group. Moving any individual button
 * relocates the entire group; a drag never triggers a counter change.
 */
@Composable
internal fun FloatingCountControl(
    visible: Boolean,
    enabled: Boolean,
    xRatio: Float,
    yRatio: Float,
    onPositionRatioChange: (Float, Float) -> Unit,
    onPrevious: () -> Unit,
    onSync: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    // Pointer input handlers outlive recompositions: always dispatch latest callbacks.
    val previousAction = rememberUpdatedState(onPrevious)
    val syncAction = rememberUpdatedState(onSync)
    val nextAction = rememberUpdatedState(onNext)
    val savePosition = rememberUpdatedState(onPositionRatioChange)
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }
    var centerX by remember { mutableFloatStateOf(Float.NaN) }
    var centerY by remember { mutableFloatStateOf(Float.NaN) }
    var dragging by remember { mutableStateOf(false) }

    Box(modifier.onSizeChanged {
        containerWidth = it.width.toFloat()
        containerHeight = it.height.toFloat()
    }) {
        if (containerWidth > 0f && containerHeight > 0f) {
            val buttonWidth = with(density) { BUTTON_SIZE.toPx() }
            val buttonGap = with(density) { BUTTON_SPACING.toPx() }
            val groupWidth = buttonWidth * 3f + buttonGap * 2f
            val groupHeight = buttonWidth
            val minX = groupWidth / 2f
            val maxX = (containerWidth - groupWidth / 2f).coerceAtLeast(minX)
            val minY = groupHeight / 2f
            val maxY = (containerHeight - groupHeight / 2f).coerceAtLeast(minY)
            val targetX = (xRatio * containerWidth).coerceIn(minX, maxX)
            val targetY = (yRatio * containerHeight).coerceIn(minY, maxY)

            LaunchedEffect(containerWidth, containerHeight, xRatio, yRatio) {
                if (!dragging) {
                    centerX = targetX
                    centerY = targetY
                }
            }

            if (centerX.isFinite() && centerY.isFinite()) {
                Row(
                    modifier = Modifier.offset {
                        IntOffset(
                            (centerX - groupWidth / 2f).roundToInt(),
                            (centerY - groupHeight / 2f).roundToInt(),
                        )
                    },
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(BUTTON_SPACING),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val actions = listOf(
                        { previousAction.value() },
                        { syncAction.value() },
                        { nextAction.value() },
                    )
                    val icons = listOf(Icons.Default.ChevronLeft, Icons.Default.Refresh, Icons.Default.ChevronRight)
                    val names = listOf("카운트 이전", "카운트 동기화", "카운트 다음")
                    actions.forEachIndexed { index, action ->
                        Box(
                            modifier = Modifier
                                .size(BUTTON_SIZE)
                                .background(
                                    when {
                                        !enabled -> DDZColor.Surface.copy(alpha = 0.60f)
                                        index == 1 -> DDZColor.SageLight
                                        else -> DDZColor.Surface.copy(alpha = 0.96f)
                                    },
                                    CircleShape,
                                )
                                .border(1.dp, DDZColor.SageDarkStrong.copy(alpha = if (enabled) 0.85f else 0.45f), CircleShape)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = names[index]
                                    if (enabled) onClick(label = names[index]) {
                                        action()
                                        true
                                    } else disabled()
                                }
                                .pointerInput(containerWidth, containerHeight, enabled) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var pointerId = down.id
                                        var moved = false
                                        var total = Offset.Zero
                                        var nextX = centerX
                                        var nextY = centerY
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == pointerId }
                                                ?: event.changes.firstOrNull() ?: break
                                            pointerId = change.id
                                            if (!change.pressed) break
                                            val delta = change.positionChange()
                                            total += delta
                                            if (enabled && !moved && total.getDistance() > viewConfiguration.touchSlop) {
                                                moved = true
                                                dragging = true
                                            }
                                            if (moved) {
                                                nextX = (nextX + delta.x).coerceIn(minX, maxX)
                                                nextY = (nextY + delta.y).coerceIn(minY, maxY)
                                                centerX = nextX
                                                centerY = nextY
                                            }
                                            change.consume()
                                        }
                                        if (moved) {
                                            dragging = false
                                            savePosition.value(nextX / containerWidth, nextY / containerHeight)
                                        } else if (enabled) {
                                            action()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = null,
                                tint = DDZColor.SageDarkStrong.copy(alpha = if (enabled) 1f else 0.45f),
                            )
                        }
                    }
                }
            }
        }
    }
}
