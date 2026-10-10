package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dudoziworkshop.dzlog.feature.log.policy.isPhotoInfoSwipe
import kotlin.math.abs

private const val MinScale = 1f
private const val MaxScale = 4f
private const val PanGain = 4.0f

@Composable
fun DzThumbnail(uriString: String) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(uriString)
        .crossfade(false)
        .allowHardware(true)
        .size(128)
        .build()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DzFullImage(
    uriString: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onGoPrevious: () -> Unit,
    onGoNext: () -> Unit,
    onZoomedStateChange: (Boolean) -> Unit,
    onSingleTap: () -> Unit,
    modifier: Modifier = Modifier,
    swipeUpEnabled: Boolean = false,
    onSwipeUp: () -> Unit = {},
) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(uriString)
        .crossfade(false)
        .allowHardware(true)
        .size(2048)
        .build()

    val painter = rememberAsyncImagePainter(model = request)
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(uriString) { mutableFloatStateOf(MinScale) }
    var offset by remember(uriString) { mutableStateOf(Offset.Zero) }
    var isAtLeftEdge by remember(uriString) { mutableStateOf(false) }
    var isAtRightEdge by remember(uriString) { mutableStateOf(false) }
    val latestSwipeUp by rememberUpdatedState(onSwipeUp)
    val infoSwipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }

    LaunchedEffect(uriString) {
        // 정책: 사진 전환(URI 변경) 시 1x 초기 상태를 부모에 즉시 반영해 pager 잠금을 해제한다.
        onZoomedStateChange(false)
    }

    LaunchedEffect(scale) {
        // 정책: scale>1f 동안만 부모 pager 스크롤을 잠근다.
        onZoomedStateChange(scale > MinScale)
    }

    fun displayedImageSize(): Offset {
        val intrinsic = painter.intrinsicSize
        if (
            containerSize.width == 0 ||
            containerSize.height == 0 ||
            !intrinsic.isSpecified ||
            intrinsic.width <= 0f ||
            intrinsic.height <= 0f
        ) {
            return Offset(containerSize.width.toFloat(), containerSize.height.toFloat())
        }

        val containerW = containerSize.width.toFloat()
        val containerH = containerSize.height.toFloat()
        val imageRatio = intrinsic.width / intrinsic.height
        val containerRatio = containerW / containerH

        return if (imageRatio > containerRatio) {
            Offset(containerW, containerW / imageRatio)
        } else {
            Offset(containerH * imageRatio, containerH)
        }
    }

    fun maxOffsets(currentScale: Float): Offset {
        val base = displayedImageSize()
        val scaledW = base.x * currentScale
        val scaledH = base.y * currentScale
        val maxX = ((scaledW - containerSize.width) / 2f).coerceAtLeast(0f)
        val maxY = ((scaledH - containerSize.height) / 2f).coerceAtLeast(0f)
        return Offset(maxX, maxY)
    }

    fun clampOffset(raw: Offset, currentScale: Float): Offset {
        if (currentScale <= MinScale) return Offset.Zero
        val max = maxOffsets(currentScale)
        return Offset(
            x = raw.x.coerceIn(-max.x, max.x),
            y = raw.y.coerceIn(-max.y, max.y)
        )
    }

    fun updateHorizontalEdgeState() {
        if (scale <= MinScale) {
            isAtLeftEdge = false
            isAtRightEdge = false
            return
        }
        val maxX = maxOffsets(scale).x
        // 정책: tolerance 없이 실제 끝 도달 기준으로 버튼 노출 상태를 판단한다.
        isAtLeftEdge = offset.x >= maxX
        isAtRightEdge = offset.x <= -maxX
    }

    // 정책: transformable은 pinch zoom 전용. clamp/1x 판정/edge 갱신은 이번 이벤트의 새 scale 기준으로 처리한다.
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        val nextScale = (scale * zoomChange).coerceIn(MinScale, MaxScale)
        if (nextScale != scale) {
            val isReturningToBaseScale = nextScale <= MinScale
            scale = if (isReturningToBaseScale) MinScale else nextScale
            offset = if (isReturningToBaseScale) {
                Offset.Zero
            } else {
                clampOffset(offset, nextScale)
            }
            updateHorizontalEdgeState()
        }
    }

    val doubleTapModifier = Modifier.pointerInput(uriString, scale, offset, containerSize) {
        detectTapGestures(
            // 정책: 단일 탭/더블탭을 같은 계층에서 처리해 overlay 토글과 zoom 제스처 충돌을 방지한다.
            onTap = { onSingleTap() },
            onDoubleTap = { tapOffset ->
                val targetScale = when {
                    scale < 2f -> 2f
                    scale < 4f -> 4f
                    else -> MinScale
                }

                if (targetScale <= MinScale) {
                    // 정책: 4x -> 1x는 기존과 동일하게 중앙 복귀.
                    scale = MinScale
                    offset = Offset.Zero
                    updateHorizontalEdgeState()
                    return@detectTapGestures
                }

                val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                val scaleRatio = targetScale / scale
                val centeredTap = tapOffset - center

                // 정책: 2x/4x는 탭 좌표 기준 확대가 되도록 offset을 재계산한다.
                val nextOffset = centeredTap - ((centeredTap - offset) * scaleRatio)

                scale = targetScale
                offset = clampOffset(nextOffset, targetScale)
                updateHorizontalEdgeState()
            }
        )
    }

    val dragModifier = if (scale > MinScale) {
        Modifier.pointerInput(uriString, scale) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    if (scale <= MinScale) return@detectDragGestures
                    // 정책: 확대 상태 one-finger drag는 이미지 pan이 우선이며, pan gain으로 체감 이동량을 보정한다.
                    offset = clampOffset(offset + (Offset(dragAmount.x, dragAmount.y) * PanGain), scale)
                    updateHorizontalEdgeState()
                    change.consume()
                }
            )
        }
    } else {
        Modifier
    }

    val infoSwipeModifier = if (swipeUpEnabled && scale <= MinScale) {
        Modifier.pointerInput(uriString, swipeUpEnabled, scale <= MinScale, infoSwipeThreshold) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var total = Offset.Zero
                var claimed = false
                while (true) {
                    val event = awaitPointerEvent()
                    // Pinch and an already claimed horizontal pager/pan take priority.
                    if (event.changes.count { it.pressed } > 1) break
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.isConsumed) break
                    total += change.positionChange()
                    if (!change.pressed) {
                        if (claimed && isPhotoInfoSwipe(total.x, total.y, infoSwipeThreshold)) {
                            change.consume()
                            latestSwipeUp()
                        }
                        break
                    }
                    if (!claimed) {
                        val slop = viewConfiguration.touchSlop
                        if (abs(total.x) > slop && abs(total.x) >= abs(total.y)) break
                        if (total.y > slop) break
                        claimed = total.y < -slop && abs(total.y) > abs(total.x) * 1.25f
                    }
                    if (claimed) change.consume()
                }
            }
        }
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(infoSwipeModifier)
            .onSizeChanged {
                containerSize = it
                offset = clampOffset(offset, scale)
                updateHorizontalEdgeState()
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .then(doubleTapModifier)
                .then(dragModifier)
                .transformable(
                    state = transformState,
                    canPan = { false }
                )
        )

        if (scale > MinScale && isAtLeftEdge && canGoPrevious) {
            IconButton(
                onClick = onGoPrevious,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(44.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .graphicsLayer { alpha = 0.96f }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous photo",
                    tint = Color.White
                )
            }
        }

        if (scale > MinScale && isAtRightEdge && canGoNext) {
            IconButton(
                onClick = onGoNext,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(44.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .graphicsLayer { alpha = 0.96f }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next photo",
                    tint = Color.White
                )
            }
        }
    }

}
