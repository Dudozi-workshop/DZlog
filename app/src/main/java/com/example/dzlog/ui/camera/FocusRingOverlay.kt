package com.example.dzlog.ui.camera

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * [FocusRingOverlay]
 * - 목적: 탭 포커스 링을 렌더링함(포커싱 펄스 + 성공 시 정지/잠깐 유지)
 * - 입력: TapFocusUiState (탭 좌표/phase)
 * - 제외: 터치 처리/CameraControl 호출/상태 생성 로직 금지(표시 전용)
 */
@Composable
internal fun FocusRingOverlay(ui: TapFocusUiState) {
    val density = LocalDensity.current
    val isFocusing = (ui.phase == FocusRingPhase.FOCUSING)

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
        val center = Offset(ui.xPx, ui.yPx)
        val radiusBase = with(density) { 28.dp.toPx() }
        val stroke = with(density) { 1.25.dp.toPx() }
        val radius = radiusBase * scale

        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )
    }
}
