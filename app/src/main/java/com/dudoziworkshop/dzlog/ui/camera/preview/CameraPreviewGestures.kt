package com.dudoziworkshop.dzlog.ui.camera.preview

import android.content.Context
import android.graphics.RectF
import android.os.SystemClock
import androidx.camera.core.Camera
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import com.dudoziworkshop.dzlog.ui.camera.controller.startTapToFocus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

internal fun Modifier.cameraPreviewGestureModifier(
    context: Context,
    scope: CoroutineScope,
    previewView: PreviewView,
    boundCamera: Camera?,
    captureRect: RectF,
    tapFocusUi: TapFocusUiState?,
    suppressWatermarkTapUntilMs: Long,
    watermarkBoundsRect: RectF?,
    watermarkRawRect: RectF?,
    isWatermarkArmed: Boolean,
    dragTouchSlop: Float,
    dragStartLeftPx: Float,
    dragStartTopPx: Float,
    dragTableWidthPx: Float,
    dragTableHeightPx: Float,
    dragPreviewOffsetPx: Offset?,
    onTapFocusUiChange: (TapFocusUiState?) -> Unit,
    onOpenTableEditor: () -> Unit,
    onCommitWatermarkOffsetIfNeeded: () -> Unit,
    onMarkWatermarkInteraction: () -> Unit,
    onWatermarkArmedChange: (Boolean) -> Unit,
    onWatermarkDragActiveChange: (Boolean) -> Unit,
    onDragStartLeftPxChange: (Float) -> Unit,
    onDragStartTopPxChange: (Float) -> Unit,
    onDragTableWidthPxChange: (Float) -> Unit,
    onDragTableHeightPxChange: (Float) -> Unit,
    onDragPreviewOffsetPxChange: (Offset?) -> Unit,
    onDragAccumDxChange: (Float) -> Unit,
    onDragAccumDyChange: (Float) -> Unit,
    onDragStartedAfterSlopChange: (Boolean) -> Unit,
    onSuppressWatermarkTapUntilMsChange: (Long) -> Unit,
    onPreviewBoundsOffsetX10000Change: (Int) -> Unit,
    onPreviewBoundsOffsetY10000Change: (Int) -> Unit,
    onPreviewOffsetXChange: (Int) -> Unit,
    onPreviewOffsetYChange: (Int) -> Unit,
    onPendingLocalOffsetSyncChange: (Boolean) -> Unit,
    onWatermarkBoundsOffset10000Preview: (Int, Int) -> Unit,
    onWatermarkOffsetRatioPreview: (Int, Int) -> Unit,
    onMaxZoomTenthsChange: (Int) -> Unit,
    onRequestedZoomTenthsCommit: (Int) -> Unit,
    onActualZoomTenthsChange: (Int) -> Unit,
): Modifier = this
    .pointerInput(boundCamera, captureRect, tapFocusUi) {
        detectTapGestures { offset ->
            if (SystemClock.uptimeMillis() < suppressWatermarkTapUntilMs) return@detectTapGestures
            if (watermarkBoundsRect?.contains(offset.x, offset.y) == true) {
                if (isWatermarkArmed) {
                    onCommitWatermarkOffsetIfNeeded()
                    onOpenTableEditor()
                    onWatermarkArmedChange(false)
                } else {
                    onWatermarkArmedChange(true)
                    onMarkWatermarkInteraction()
                }
                return@detectTapGestures
            }
            if (isWatermarkArmed) {
                onCommitWatermarkOffsetIfNeeded()
                onWatermarkArmedChange(false)
                return@detectTapGestures
            }
            val activeCamera = boundCamera ?: return@detectTapGestures
            if (!captureRect.contains(offset.x, offset.y)) return@detectTapGestures

            onTapFocusUiChange(
                TapFocusUiState(
                    xPx = offset.x,
                    yPx = offset.y,
                    phase = FocusRingPhase.FOCUSING
                )
            )
            startTapToFocus(
                context = context,
                camera = activeCamera,
                previewView = previewView,
                xPx = offset.x,
                yPx = offset.y,
                onResult = { success ->
                    scope.launch {
                        if (success) {
                            onTapFocusUiChange(TapFocusUiState(offset.x, offset.y, FocusRingPhase.SUCCESS))
                            delay(350)
                        } else {
                            delay(200)
                        }
                        onTapFocusUiChange(null)
                    }
                }
            )
        }
    }
    .pointerInput(boundCamera, captureRect, isWatermarkArmed) {
        val pinchScaleDeadZone = 0.01f
        awaitEachGesture {
            val activeCamera = boundCamera ?: return@awaitEachGesture
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            var localDragEnabled = isWatermarkArmed && watermarkBoundsRect != null && watermarkRawRect != null &&
                watermarkBoundsRect.contains(firstDown.position.x, firstDown.position.y)
            var localDragStartLeftPx = dragStartLeftPx
            var localDragStartTopPx = dragStartTopPx
            var localDragTableWidthPx = dragTableWidthPx
            var localDragTableHeightPx = dragTableHeightPx
            var localDragPreviewOffsetPx = dragPreviewOffsetPx
            var localDragAccumDx = 0f
            var localDragAccumDy = 0f
            var localDragStartedAfterSlop = false

            onWatermarkDragActiveChange(false)
            if (localDragEnabled) {
                val br = watermarkBoundsRect!!
                localDragStartLeftPx = br.left - captureRect.left
                localDragStartTopPx = br.top - captureRect.top
                localDragTableWidthPx = br.width()
                localDragTableHeightPx = br.height()
                localDragAccumDx = 0f
                localDragAccumDy = 0f
                localDragStartedAfterSlop = false
                localDragPreviewOffsetPx = Offset(localDragStartLeftPx, localDragStartTopPx)
                onDragStartLeftPxChange(localDragStartLeftPx)
                onDragStartTopPxChange(localDragStartTopPx)
                onDragTableWidthPxChange(localDragTableWidthPx)
                onDragTableHeightPxChange(localDragTableHeightPx)
                onDragAccumDxChange(localDragAccumDx)
                onDragAccumDyChange(localDragAccumDy)
                onDragStartedAfterSlopChange(false)
                onDragPreviewOffsetPxChange(localDragPreviewOffsetPx)
                onMarkWatermarkInteraction()
            }

            while (true) {
                val event = awaitPointerEvent()
                val activePointers = event.changes.count { it.pressed }
                if (activePointers == 0) break

                if (activePointers > 1) {
                    if (isWatermarkArmed) {
                        event.changes.forEach { change ->
                            if (change.positionChanged()) change.consume()
                        }
                        continue
                    }
                    val zoom = event.calculateZoom()
                    if (abs(zoom - 1f) >= pinchScaleDeadZone) {
                        val centroid = event.calculateCentroid(useCurrent = true)
                        if (shouldHandlePreviewPinch(
                                centroidX = centroid.x,
                                centroidY = centroid.y,
                                captureRect = captureRect,
                            )
                        ) {
                            val zoomState = activeCamera.cameraInfo.zoomState.value
                            if (zoomState != null) {
                                val (minZoom, maxZoom) = resolveZoomBounds(
                                    minSupported = zoomState.minZoomRatio,
                                    maxSupported = zoomState.maxZoomRatio
                                )
                                val next = (zoomState.zoomRatio * zoom).coerceIn(minZoom, maxZoom)
                                runCatching { activeCamera.cameraControl.setZoomRatio(next) }
                                val maxTenths = (maxZoom * 10f).roundToInt().coerceAtLeast(10)
                                onMaxZoomTenthsChange(maxTenths)
                                val tenths = (next * 10f).roundToInt().coerceIn(10, maxTenths)
                                onRequestedZoomTenthsCommit(tenths)
                                onActualZoomTenthsChange(tenths)
                            }
                        }
                    }
                    event.changes.forEach { change ->
                        if (change.positionChanged()) change.consume()
                    }
                    continue
                }

                if (!localDragEnabled) continue
                val change = event.changes.firstOrNull { it.pressed } ?: continue

                val delta = change.positionChange()
                localDragAccumDx += delta.x
                localDragAccumDy += delta.y
                onDragAccumDxChange(localDragAccumDx)
                onDragAccumDyChange(localDragAccumDy)
                if (!localDragStartedAfterSlop) {
                    val moved = hypot(localDragAccumDx.toDouble(), localDragAccumDy.toDouble()).toFloat()
                    if (moved < dragTouchSlop) continue
                    localDragStartedAfterSlop = true
                    onDragStartedAfterSlopChange(true)
                    onWatermarkDragActiveChange(true)
                }
                if (change.positionChanged()) change.consume()

                val nextOffsetPx = computeClampedDragOffsetPx(
                    contentRect = captureRect,
                    dragStartLeftPx = localDragStartLeftPx,
                    dragStartTopPx = localDragStartTopPx,
                    dragAccumDx = localDragAccumDx,
                    dragAccumDy = localDragAccumDy,
                    rawW = localDragTableWidthPx,
                    rawH = localDragTableHeightPx
                )

                localDragPreviewOffsetPx = nextOffsetPx
                onDragPreviewOffsetPxChange(nextOffsetPx)
                onMarkWatermarkInteraction()
            }

            if (localDragEnabled && localDragStartedAfterSlop) {
                onSuppressWatermarkTapUntilMsChange(SystemClock.uptimeMillis() + 180L)
                onWatermarkArmedChange(true)

                val committedOffset = localDragPreviewOffsetPx ?: Offset(localDragStartLeftPx, localDragStartTopPx)
                val committedLeftPx = committedOffset.x
                val committedTopPx = committedOffset.y
                val boundsMaxX = (captureRect.width() - localDragTableWidthPx).coerceAtLeast(0f)
                val boundsMaxY = (captureRect.height() - localDragTableHeightPx).coerceAtLeast(0f)
                val committedX10000 = if (boundsMaxX <= 0f) {
                    0
                } else {
                    ((committedLeftPx / boundsMaxX) * 10000f).roundToInt().coerceIn(0, 10000)
                }
                val committedY10000 = if (boundsMaxY <= 0f) {
                    0
                } else {
                    ((committedTopPx / boundsMaxY) * 10000f).roundToInt().coerceIn(0, 10000)
                }
                val committedXRatio = (committedX10000 / 100f).roundToInt().coerceIn(0, 100)
                val committedYRatio = (committedY10000 / 100f).roundToInt().coerceIn(0, 100)

                onPreviewBoundsOffsetX10000Change(committedX10000)
                onPreviewBoundsOffsetY10000Change(committedY10000)
                onPreviewOffsetXChange(committedXRatio)
                onPreviewOffsetYChange(committedYRatio)
                onPendingLocalOffsetSyncChange(true)
                onWatermarkBoundsOffset10000Preview(committedX10000, committedY10000)
                onWatermarkOffsetRatioPreview(committedXRatio, committedYRatio)
                onMarkWatermarkInteraction()
                onCommitWatermarkOffsetIfNeeded()
            }
            onWatermarkDragActiveChange(false)
            onDragPreviewOffsetPxChange(null)
            onDragTableWidthPxChange(0f)
            onDragTableHeightPxChange(0f)
            onDragAccumDxChange(0f)
            onDragAccumDyChange(0f)
            onDragStartedAfterSlopChange(false)
        }
    }

private fun computeClampedDragOffsetPx(    contentRect: RectF,
    dragStartLeftPx: Float,
    dragStartTopPx: Float,
    dragAccumDx: Float,
    dragAccumDy: Float,
    rawW: Float,
    rawH: Float
): Offset {
    val candidateLeft = dragStartLeftPx + dragAccumDx
    val candidateTop = dragStartTopPx + dragAccumDy
    val maxX = (contentRect.width() - rawW).coerceAtLeast(0f)
    val maxY = (contentRect.height() - rawH).coerceAtLeast(0f)
    return Offset(
        x = candidateLeft.coerceIn(0f, maxX),
        y = candidateTop.coerceIn(0f, maxY)
    )
}

private fun shouldHandlePreviewPinch(
    centroidX: Float,
    centroidY: Float,
    captureRect: RectF
): Boolean {
    if (captureRect.width() <= 0f || captureRect.height() <= 0f) return false
    if (!captureRect.contains(centroidX, centroidY)) return false
    return true
}
