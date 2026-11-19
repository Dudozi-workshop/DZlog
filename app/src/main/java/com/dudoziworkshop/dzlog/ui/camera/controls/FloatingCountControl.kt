package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

// Smaller visible buttons with independent, accessible touch targets.
private val TOUCH_SIZE = 44.dp
private val VISIBLE_BUTTON_SIZE = 34.dp
private val ICON_SIZE = 20.dp
private val BUTTON_SPACING = 2.dp
private val DOCK_PADDING = 4.dp
private val DOCK_SHAPE = RoundedCornerShape(28.dp)

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
            val buttonWidth = with(density) { TOUCH_SIZE.toPx() }
            val buttonGap = with(density) { BUTTON_SPACING.toPx() }
            val dockPadding = with(density) { DOCK_PADDING.toPx() }
            // Keep the entire pill inside the viewfinder when dragging / rotating.
            val groupWidth = buttonWidth * 3f + buttonGap * 2f + dockPadding * 2f
            val groupHeight = buttonWidth + dockPadding * 2f
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
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (centerX - groupWidth / 2f).roundToInt(),
                                (centerY - groupHeight / 2f).roundToInt(),
                            )
                        }
                        .shadow(5.dp, DOCK_SHAPE)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    DDZColor.Surface.copy(alpha = if (enabled) 0.94f else 0.65f),
                                    DDZColor.Surface.copy(alpha = if (enabled) 0.76f else 0.52f),
                                )
                            ),
                            shape = DOCK_SHAPE,
                        )
                        .border(
                            1.dp,
                            DDZColor.SageBorder.copy(alpha = if (enabled) 0.78f else 0.40f),
                            DOCK_SHAPE,
                        )
                        .padding(DOCK_PADDING),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
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
                                .size(TOUCH_SIZE)
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
                            Box(
                                modifier = Modifier
                                    .size(VISIBLE_BUTTON_SIZE)
                                    .shadow(if (enabled) 1.dp else 0.dp, CircleShape)
                                    .background(
                                        when {
                                            !enabled -> DDZColor.Surface.copy(alpha = 0.55f)
                                            index == 1 -> DDZColor.SageLight.copy(alpha = 0.94f)
                                            else -> DDZColor.Surface.copy(alpha = 0.92f)
                                        },
                                        CircleShape,
                                    )
                                    .then(
                                        if (index == 1) Modifier.border(
                                            1.dp,
                                            DDZColor.SageDarkStrong.copy(alpha = if (enabled) 0.38f else 0.20f),
                                            CircleShape,
                                        ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = icons[index],
                                    contentDescription = null,
                                    modifier = Modifier.size(ICON_SIZE),
                                    tint = DDZColor.SageDarkStrong.copy(alpha = if (enabled) 1f else 0.45f),
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}
