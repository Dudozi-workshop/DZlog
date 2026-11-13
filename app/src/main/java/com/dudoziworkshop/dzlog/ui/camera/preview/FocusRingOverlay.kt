package com.dudoziworkshop.dzlog.ui.camera.preview

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.delay

/**
 * [FocusRingOverlay]
 * - 목적: 탭 포커스 링을 렌더링함(포커싱 펄스 + 성공 시 정지/잠깐 유지)
 * - 입력: TapFocusUiState (탭 좌표/phase)
 * - 제외: 터치 처리/CameraControl 호출/상태 생성 로직 금지(표시 전용)
 */
@Composable
internal fun FocusRingOverlay(
    ui: TapFocusUiState,
    locked: Boolean = false,
    compact: Boolean = false,
    showLock: Boolean = false,
) {
    val density = LocalDensity.current
    val isFocusing = (ui.phase == FocusRingPhase.FOCUSING)
    var flashFailure by remember { mutableStateOf(false) }
    LaunchedEffect(ui.phase, ui.xPx, ui.yPx) {
        flashFailure = ui.phase == FocusRingPhase.FAILED
        if (flashFailure) {
            delay(FOCUS_FAILURE_FLASH_MS)
            flashFailure = false
        }
    }

    val infinite = rememberInfiniteTransition(label = "focusRing")
    val pulseAlpha = infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 280),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    ).value

    val pulseScale = infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 280),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    ).value

    val targetScale = if (isFocusing) pulseScale else 1.0f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 120),
        label = "scale"
    )

    val alpha = if (isFocusing) pulseAlpha else 1.0f

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(5f)
    ) {
        val radiusBase = with(density) {
            (if (compact) FOCUS_RING_COMPACT_RADIUS_DP.dp else FOCUS_RING_RADIUS_DP.dp).toPx()
        }
        val stroke = with(density) { 1.25.dp.toPx() }
        val radius = radiusBase * scale
        val center = Offset(
            visibleFocusCenter(ui.xPx, size.width, radiusBase),
            visibleFocusCenter(ui.yPx, size.height, radiusBase),
        )
        // Failure is a brief signal even when AE/AF lock is active;
        // afterwards the locked ring returns to the sage accent.
        val ringColor = (if (flashFailure) DDZColor.Destructive
            else if (locked) DDZColor.CameraFocusLocked else Color.White).copy(alpha = alpha)

        if (showLock) {
            // Leave a small symmetrical gap either side of the lock on the
            // circular outline. The lock remains upright at both 12 and 6.
            val lockAtBottom = center.y - radiusBase -
                with(density) { FOCUS_LOCK_TOUCH_DP.dp.toPx() } / 2f < 0f
            val gapCenterAngle = if (lockAtBottom) 90f else 270f
            val halfGapDegrees = 24f
            if (locked) {
                // A subtle dark keyline makes the sage ring legible over
                // bright specimens without adding a filled background.
                drawArc(
                    color = Color.Black.copy(alpha = 0.65f * alpha),
                    startAngle = gapCenterAngle + halfGapDegrees,
                    sweepAngle = 360f - 2f * halfGapDegrees,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = with(density) { 3.5.dp.toPx() }),
                )
            }
            drawArc(
                color = ringColor,
                startAngle = gapCenterAngle + halfGapDegrees,
                sweepAngle = 360f - 2f * halfGapDegrees,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = if (locked) with(density) { 1.75.dp.toPx() } else stroke),
            )
        } else {
            if (locked) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.65f * alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = with(density) { 3.5.dp.toPx() }),
                )
            }
            drawCircle(
                color = ringColor,
                radius = radius,
                center = center,
                style = Stroke(width = if (locked) with(density) { 1.75.dp.toPx() } else stroke),
            )
        }
    }
}
