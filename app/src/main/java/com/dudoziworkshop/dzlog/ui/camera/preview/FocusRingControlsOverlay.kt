package com.dudoziworkshop.dzlog.ui.camera.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlin.math.roundToInt

/**
 * No global AE badge, no black capsule. A tiny lock is drawn ON the
 * interrupted ring outline; while locked it remains there after collapse.
 * Tapping the center of the collapsed ring reopens the EV control.
 */
@Composable
internal fun FocusRingControlsOverlay(
    focus: TapFocusUiState?,
    locked: Boolean,
    expanded: Boolean,
    exposureIndex: Int,
    minExposureIndex: Int,
    maxExposureIndex: Int,
    exposureStep: Float,
    exposureSupported: Boolean,
    onToggleLock: () -> Unit,
    onExposureIndexChange: (Int) -> Unit,
    onExposureInteractionChange: (Boolean) -> Unit,
    onRequestExpand: () -> Unit,
) {
    if (focus == null) return
    val lockInteraction = remember { MutableInteractionSource() }
    val expandInteraction = remember { MutableInteractionSource() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val radius = with(density) {
            (if (expanded) FOCUS_RING_RADIUS_DP.dp else FOCUS_RING_COMPACT_RADIUS_DP.dp).toPx()
        }
        val centerX = visibleFocusCenter(focus.xPx, widthPx, radius)
        val centerY = visibleFocusCenter(focus.yPx, heightPx, radius)
        val lockHitPx = with(density) { FOCUS_LOCK_TOUCH_DP.dp.toPx() }
        val evWidthPx = with(density) { FOCUS_EV_WIDTH_DP.dp.toPx() }
        val evHeightPx = with(density) { FOCUS_EV_TOUCH_HEIGHT_DP.dp.toPx() }
        // Combined with the 8dp track inset, the visible ring-to-EV-line gap
        // is about 10dp (formerly 6dp + 23dp = 29dp).
        val gapPx = with(density) { 2.dp.toPx() }
        val placement = placeFocusControlsVertically(
            focusY = focus.yPx,
            viewHeight = heightPx,
            ringRadius = radius,
            lockHeight = lockHitPx,
            exposureHeight = evHeightPx,
            gap = gapPx,
        )

        if (locked && !expanded) {
            // A small central hit area lets the user adjust EV again without
            // moving the locked focus point. The lock itself only unlocks.
            Box(
                modifier = Modifier
                    .offsetPx(
                        centerX - with(density) { 12.dp.toPx() },
                        centerY - with(density) { 12.dp.toPx() },
                    )
                    .size(24.dp)
                    .semantics { contentDescription = "밝기 조절 열기" }
                    .clickable(
                        interactionSource = expandInteraction,
                        indication = null,
                        onClick = onRequestExpand,
                    ),
            )
        }

        if (locked || expanded) {
            val lockX = (centerX - lockHitPx / 2f)
                .coerceIn(0f, (widthPx - lockHitPx).coerceAtLeast(0f))
            Box(
                modifier = Modifier
                    .offsetPx(lockX, placement.lockTop)
                    .size(FOCUS_LOCK_TOUCH_DP.dp)
                    .semantics {
                        contentDescription = if (locked) "초점·노출 잠금 해제" else "초점·노출 잠금"
                    }
                    .clickable(
                        interactionSource = lockInteraction,
                        indication = null,
                        onClick = onToggleLock,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (locked) DDZColor.CameraFocusLocked else Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        if (expanded && exposureSupported && maxExposureIndex > minExposureIndex) {
            val evX = (centerX - evWidthPx / 2f)
                .coerceIn(0f, (widthPx - evWidthPx).coerceAtLeast(0f))
            CompactExposureSlider(
                index = exposureIndex,
                minIndex = minExposureIndex,
                maxIndex = maxExposureIndex,
                exposureStep = exposureStep,
                above = placement.exposureAbove,
                locked = locked,
                onIndexChange = onExposureIndexChange,
                onInteractionChange = onExposureInteractionChange,
                modifier = Modifier.offsetPx(evX, placement.exposureTop),
            )
        }
    }
}

private fun Modifier.offsetPx(x: Float, y: Float): Modifier =
    this.then(Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) })
