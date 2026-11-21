package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.media.SavedGallerySwipeAction
import com.dudoziworkshop.dzlog.data.media.savedGallerySwipeAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Keyed by asset by the caller, so zoom never leaks into the next photo. */
@Composable
internal fun IosGalleryImageViewer(
    image: ImageBitmap,
    enabled: Boolean,
    showControls: Boolean = true,
    onToggleControls: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onShowInfo: () -> Unit = {},
    pagerOwnsHorizontalSwipe: Boolean = false,
    onZoomedStateChange: (Boolean) -> Unit = {},
) {
    val currentToggle by rememberUpdatedState(onToggleControls)
    val currentPrevious by rememberUpdatedState(onPrevious)
    val currentNext by rememberUpdatedState(onNext)
    val currentInfo by rememberUpdatedState(onShowInfo)
    val currentZoomState by rememberUpdatedState(onZoomedStateChange)
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(scale) { currentZoomState(scale > 1f) }
    DisposableEffect(Unit) { onDispose { currentZoomState(false) } }

    fun constrained(value: Offset, zoom: Float): Offset {
        if (viewport.width == 0 || viewport.height == 0) return Offset.Zero
        val fit = minOf(viewport.width.toFloat() / image.width, viewport.height.toFloat() / image.height)
        val maxX = ((image.width * fit * zoom - viewport.width) / 2f).coerceAtLeast(0f)
        val maxY = ((image.height * fit * zoom - viewport.height) / 2f).coerceAtLeast(0f)
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }
    fun setZoom(value: Float) {
        val next = value.coerceIn(1f, 4f)
        offset = constrained(offset * (next / scale), next)
        scale = next
    }

    Column(Modifier.fillMaxSize()) {
        if (showControls) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = enabled && scale > 1f, onClick = { setZoom(scale - 0.5f) }) { Text("축소") }
            Text("배율 ${(scale * 100).roundToInt()}%")
            TextButton(enabled = enabled && scale < 4f, onClick = { setZoom(scale + 0.5f) }) { Text("확대") }
            TextButton(enabled = enabled && scale > 1f, onClick = { setZoom(1f) }) { Text("화면 맞춤") }
        }
        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()
            .onSizeChanged {
                if (viewport != it) { viewport = it; offset = constrained(offset, scale) }
            }
            .pointerInput(image, enabled) {
                if (!enabled) return@pointerInput
                // One recognizer owns tap, swipe, pan and pinch. Once a second
                // finger participates, that gesture can never navigate/open info.
                coroutineScope {
                    val jobs = this
                    var tapJob: Job? = null
                    var previousTap: Pair<Long, Offset>? = null
                    try {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val prior = previousTap
                            val doubleTap = prior != null &&
                                down.uptimeMillis - prior.first in 0..viewConfiguration.doubleTapTimeoutMillis &&
                                (down.position - prior.second).getDistance() <= 48.dp.toPx()
                            if (doubleTap) {
                                tapJob?.cancel()
                                previousTap = null
                            }
                            val startedScale = scale
                            var multiplePointers = false
                            var moved = false
                            var cancelled = down.isConsumed
                            var total = Offset.Zero
                            var endedAt = down.uptimeMillis
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.isConsumed }) cancelled = true
                                val pressed = event.changes.count { it.pressed }
                                if (pressed > 1) multiplePointers = true
                                val primary = event.changes.firstOrNull { it.id == down.id }
                                if (primary != null) {
                                    total += primary.positionChange()
                                    endedAt = primary.uptimeMillis
                                }
                                if (multiplePointers || total.getDistance() > viewConfiguration.touchSlop) {
                                    moved = true
                                    tapJob?.cancel()
                                    previousTap = null
                                }
                                val pagerGesture = pagerOwnsHorizontalSwipe && !multiplePointers &&
                                    startedScale <= 1f && abs(total.x) > abs(total.y)
                                if (!cancelled && moved && !pagerGesture) {
                                    if (multiplePointers || startedScale > 1f) {
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        val next = (scale * zoom).coerceIn(1f, 4f)
                                        val centroid = event.calculateCentroid(useCurrent = false)
                                        if (centroid != Offset.Unspecified) {
                                            val anchor = centroid - Offset(viewport.width / 2f, viewport.height / 2f)
                                            offset = constrained((offset - anchor) * (next / scale) + anchor + pan, next)
                                            scale = next
                                        }
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                            if (!cancelled) {
                                if (moved) {
                                    if (!(pagerOwnsHorizontalSwipe && !multiplePointers &&
                                            startedScale <= 1f && abs(total.x) > abs(total.y))) {
                                        when (savedGallerySwipeAction(total.x, total.y, startedScale,
                                            multiplePointers, 64.dp.toPx())) {
                                            SavedGallerySwipeAction.PREVIOUS -> currentPrevious()
                                            SavedGallerySwipeAction.NEXT -> currentNext()
                                            SavedGallerySwipeAction.INFO -> currentInfo()
                                            SavedGallerySwipeAction.NONE -> Unit
                                        }
                                    }
                                } else if (endedAt - down.uptimeMillis <= viewConfiguration.longPressTimeoutMillis) {
                                    if (doubleTap) setZoom(if (scale > 1f) 1f else 2f)
                                    else {
                                        previousTap = endedAt to down.position
                                        tapJob?.cancel()
                                        tapJob = jobs.launch {
                                            delay(viewConfiguration.doubleTapTimeoutMillis)
                                            previousTap = null
                                            currentToggle()
                                        }
                                    }
                                }
                            }
                        }
                    } finally { tapJob?.cancel() }
                }
            }, contentAlignment = Alignment.Center) {
            Image(image, "사진 확대 보기", Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }, contentScale = ContentScale.Fit)
        }
    }
}
