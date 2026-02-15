@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog.ui.camera.preview

import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.lifecycle.Observer
import com.example.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.example.dzlog.domain.model.CaptureRequest
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.naming.resolveGroupValue
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.ui.camera.buildWatermarkConfig
import com.example.dzlog.ui.camera.controller.bindCamera
import com.example.dzlog.ui.camera.controller.startTapToFocus
import com.example.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * CameraPreviewArea
 */
@Composable
internal fun CameraPreviewArea(
    args: CameraPreviewAreaArgs,
    boundCamera: Camera?,
    onBoundCameraChange: (Camera?) -> Unit,
    onBoundImageCaptureChange: (ImageCapture?) -> Unit,
    capturedUri: android.net.Uri?,
    onDismissCaptured: () -> Unit,
    tapFocusUi: TapFocusUiState?,
    onTapFocusUiChange: (TapFocusUiState?) -> Unit
) {
    val context = args.context
    val lifecycleOwner = args.lifecycleOwner
    val scope = args.scope
    val captureAspect = args.captureAspect

    var previewLogged by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DDZColor.PrimaryDark.copy(alpha = 0f))
            .clipToBounds()
    ) {
        val previewView = remember(context) {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }

        var captureRect by remember { mutableStateOf(RectF(0f, 0f, 0f, 0f)) }
        var usableTopRatio by remember { mutableStateOf(0f) }
        var usableBottomRatio by remember { mutableStateOf(1f) }
        var watermarkRect by remember { mutableStateOf<RectF?>(null) }
        var watermarkDragActive by remember { mutableStateOf(false) }
        var suppressWatermarkTapUntilMs by remember { mutableStateOf(0L) }
        var isWatermarkArmed by remember { mutableStateOf(false) }
        var previewOffsetX by remember { mutableStateOf(args.watermarkUi.offsetXRatio.coerceIn(0, 100)) }
        var previewOffsetY by remember { mutableStateOf(args.watermarkUi.offsetYRatio.coerceIn(0, 100)) }
        var dragStartLeftPx by remember { mutableStateOf(0f) }
        var dragStartTopPx by remember { mutableStateOf(0f) }
        var dragAccumDx by remember { mutableStateOf(0f) }
        var dragAccumDy by remember { mutableStateOf(0f) }
        var dragStartedAfterSlop by remember { mutableStateOf(false) }
        var watermarkLastInteractionMs by remember { mutableStateOf(0L) }
        val dragTouchSlop = LocalViewConfiguration.current.touchSlop

        fun commitWatermarkOffsetIfNeeded() {
            args.onWatermarkOffsetRatioCommit(previewOffsetX, previewOffsetY)
        }

        fun markWatermarkInteraction() {
            watermarkLastInteractionMs = SystemClock.uptimeMillis()
        }

        LaunchedEffect(args.watermarkUi.offsetXRatio, args.watermarkUi.offsetYRatio) {
            previewOffsetX = args.watermarkUi.offsetXRatio.coerceIn(0, 100)
            previewOffsetY = args.watermarkUi.offsetYRatio.coerceIn(0, 100)
        }

        LaunchedEffect(isWatermarkArmed, watermarkDragActive, watermarkLastInteractionMs) {
            if (!isWatermarkArmed || watermarkDragActive) return@LaunchedEffect
            val timeoutMs = 1_000L
            val waitMs = (watermarkLastInteractionMs + timeoutMs - SystemClock.uptimeMillis()).coerceAtLeast(0L)
            delay(waitMs)
            if (
                isWatermarkArmed &&
                !watermarkDragActive &&
                (SystemClock.uptimeMillis() - watermarkLastInteractionMs) >= timeoutMs
            ) {
                commitWatermarkOffsetIfNeeded()
                isWatermarkArmed = false
            }
        }

        fun updateCaptureRect() {
            val contentRect = resolvePreviewContentRect(
                previewView = previewView,
                overlayWidth = previewView.width.toFloat(),
                overlayHeight = previewView.height.toFloat()
            )
            val usableRect = resolveUsableRect(
                contentRect = contentRect,
                settingsButtonBottomY = args.settingsButtonBottomY,
                shutterButtonTopY = args.shutterButtonTopY,
                safeTopY = args.safeTopY,
                safeBottomY = args.safeBottomY,
                verticalMarginPx = args.usableVerticalMarginPx
            )
            captureRect = computeCaptureAreaRect(
                contentRect = contentRect,
                captureAspectRatio = captureAspect.ratioF,
                usableRect = usableRect
            )
            val (nextTopRatio, nextBottomRatio) = computeUsableVerticalRatios(contentRect, usableRect)
            usableTopRatio = nextTopRatio
            usableBottomRatio = nextBottomRatio
            args.onUsableVerticalRatioChange(nextTopRatio, nextBottomRatio)
            if (!previewLogged) {
                Log.d(
                    "DZlogPreview",
                    "Preview crop=${captureRect.width().toInt()}x${captureRect.height().toInt()} aspect=${captureAspect.label} content=${contentRect.width().toInt()}x${contentRect.height().toInt()} usableTop=${usableRect.top.toInt()} usableBottom=${usableRect.bottom.toInt()}"
                )
                previewLogged = true
            }
        }

        DisposableEffect(previewView, lifecycleOwner, captureAspect) {
            val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                updateCaptureRect()
            }
            val streamObserver = Observer<PreviewView.StreamState> { state ->
                if (state == PreviewView.StreamState.STREAMING) {
                    updateCaptureRect()
                }
            }

            previewView.addOnLayoutChangeListener(layoutListener)
            previewView.previewStreamState.observe(lifecycleOwner, streamObserver)

            onDispose {
                previewView.removeOnLayoutChangeListener(layoutListener)
                previewView.previewStreamState.removeObserver(streamObserver)
            }
        }

        LaunchedEffect(captureAspect, args.settingsButtonBottomY, args.shutterButtonTopY, args.safeTopY, args.safeBottomY, args.usableVerticalMarginPx) {
            updateCaptureRect()
            bindCamera(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                aspect = captureAspect
            ) { cap, camera ->
                onBoundImageCaptureChange(cap)
                onBoundCameraChange(camera)
            }
        }

        DisposableEffect(boundCamera) {
            val activeCamera = boundCamera ?: return@DisposableEffect onDispose { }
            val observer = Observer<androidx.camera.core.ZoomState> { zoomState ->
                val (_, maxZoom) = resolveZoomBounds(
                    minSupported = zoomState.minZoomRatio,
                    maxSupported = zoomState.maxZoomRatio
                )
                val maxTenths = (maxZoom * 10f).roundToInt().coerceAtLeast(10)
                args.onMaxZoomTenthsChange(maxTenths)
                val actual = ((zoomState.zoomRatio * 10f).roundToInt()).coerceIn(10, maxTenths)
                args.onActualZoomTenthsChange(actual)
            }
            activeCamera.cameraInfo.zoomState.observe(lifecycleOwner, observer)
            onDispose {
                activeCamera.cameraInfo.zoomState.removeObserver(observer)
            }
        }

        LaunchedEffect(boundCamera, args.zoomRatioTenths) {
            val activeCamera = boundCamera ?: return@LaunchedEffect
            val requested = args.zoomRatioTenths.coerceIn(10, args.maxZoomTenths.coerceAtLeast(10)) / 10f
            val zoomState = activeCamera.cameraInfo.zoomState.value
            val (minZoom, maxZoom) = resolveZoomBounds(
                minSupported = zoomState?.minZoomRatio ?: 1f,
                maxSupported = zoomState?.maxZoomRatio ?: 1f
            )
            val target = requested.coerceIn(minZoom, maxZoom)
            runCatching { activeCamera.cameraControl.setZoomRatio(target) }
        }

        val gestureModifier = Modifier
            .fillMaxSize()
            .pointerInput(boundCamera, captureRect, tapFocusUi) {
                detectTapGestures { offset ->
                    if (SystemClock.uptimeMillis() < suppressWatermarkTapUntilMs) return@detectTapGestures
                    if (watermarkRect?.contains(offset.x, offset.y) == true) {
                        if (isWatermarkArmed) {
                            commitWatermarkOffsetIfNeeded()
                            args.onOpenTableEditor()
                            isWatermarkArmed = false
                        } else {
                            isWatermarkArmed = true
                            markWatermarkInteraction()
                        }
                        return@detectTapGestures
                    }
                    if (isWatermarkArmed) {
                        commitWatermarkOffsetIfNeeded()
                        isWatermarkArmed = false
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
            .pointerInput(boundCamera, captureRect, watermarkRect, isWatermarkArmed) {
                val pinchScaleDeadZone = 0.01f
                awaitEachGesture {
                    val activeCamera = boundCamera ?: return@awaitEachGesture
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val tableRect = watermarkRect
                    var dragEnabled = isWatermarkArmed && tableRect != null &&
                        tableRect.contains(firstDown.position.x, firstDown.position.y)
                    watermarkDragActive = dragEnabled
                    if (dragEnabled && tableRect != null) {
                        dragStartLeftPx = tableRect.left - captureRect.left
                        dragStartTopPx = tableRect.top - captureRect.top
                        dragAccumDx = 0f
                        dragAccumDy = 0f
                        dragStartedAfterSlop = false
                        markWatermarkInteraction()
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
                            if (dragEnabled) {
                                dragEnabled = false
                                watermarkDragActive = false
                                dragAccumDx = 0f
                                dragAccumDy = 0f
                                dragStartedAfterSlop = false
                            }
                            val zoom = event.calculateZoom()
                            if (abs(zoom - 1f) >= pinchScaleDeadZone) {
                                val centroid = event.calculateCentroid(useCurrent = true)
                                if (shouldHandlePinch(
                                        centroidX = centroid.x,
                                        centroidY = centroid.y,
                                        captureRect = captureRect,
                                        watermarkDragActive = watermarkDragActive
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
                                        args.onMaxZoomTenthsChange(maxTenths)
                                        val tenths = (next * 10f).roundToInt().coerceIn(10, maxTenths)
                                        args.onRequestedZoomTenthsCommit(tenths)
                                        args.onActualZoomTenthsChange(tenths)
                                    }
                                }
                            }
                            event.changes.forEach { change ->
                                if (change.positionChanged()) change.consume()
                            }
                            continue
                        }

                        if (!dragEnabled) continue
                        val change = event.changes.firstOrNull { it.pressed } ?: continue
                        val tableRectNow = watermarkRect ?: continue

                        val contentRect = captureRect
                        val tableW = tableRectNow.width()
                        val tableH = tableRectNow.height()
                        val maxX = (contentRect.width() - tableW).coerceAtLeast(0f)
                        val maxY = (contentRect.height() - tableH).coerceAtLeast(0f)
                        if (maxX <= 0f || maxY <= 0f) continue

                        val delta = change.positionChange()
                        dragAccumDx += delta.x
                        dragAccumDy += delta.y
                        if (change.positionChanged()) change.consume()
                        if (!dragStartedAfterSlop) {
                            val moved = hypot(dragAccumDx.toDouble(), dragAccumDy.toDouble()).toFloat()
                            if (moved < dragTouchSlop) continue
                            dragStartedAfterSlop = true
                        }

                        val nextLeftPx = (dragStartLeftPx + dragAccumDx).coerceIn(0f, maxX)
                        val nextTopPx = (dragStartTopPx + dragAccumDy).coerceIn(0f, maxY)
                        val nextXRatio = ((nextLeftPx / maxX) * 100f).roundToInt().coerceIn(0, 100)
                        val nextYRatio = ((nextTopPx / maxY) * 100f).roundToInt().coerceIn(0, 100)
                        if (nextXRatio != previewOffsetX || nextYRatio != previewOffsetY) {
                            previewOffsetX = nextXRatio
                            previewOffsetY = nextYRatio
                            args.onWatermarkOffsetRatioPreview(nextXRatio, nextYRatio)
                            markWatermarkInteraction()
                        }
                    }

                    if (dragEnabled && dragStartedAfterSlop) {
                        suppressWatermarkTapUntilMs = SystemClock.uptimeMillis() + 180L
                        isWatermarkArmed = true
                        commitWatermarkOffsetIfNeeded()
                    }
                    watermarkDragActive = false
                    dragAccumDx = 0f
                    dragAccumDy = 0f
                    dragStartedAfterSlop = false
                }
            }


        val plan = remember(
            args.tableTemplateState,
            args.scopeNextCounter,
            args.now,
            args.counterDigits,
            args.dateFormat,
            args.timeFormat
        ) {
            args.tableResolver.plan(
                cells = args.tableTemplateState.cells,
                captureNow = args.now,
                config = TableResolver.Config(
                    counterDigits = args.counterDigits,
                    dateFormat = args.dateFormat,
                    timeFormat = args.timeFormat
                ),
                counterSeedOverride = args.scopeNextCounter
            )
        }

        remember(plan) {
            plan.resolvedCells.any { it.type == TableCellDataType.COUNTER }
        }

        val previewRequest = CaptureRequest(
            group1 = resolveGroupValue(plan.resolvedCells, GroupLevel.G1),
            group2 = resolveGroupValue(plan.resolvedCells, GroupLevel.G2),
            displayName = CaptureNamingPolicy.buildDisplayNameForCounter(
                resolvedCells = plan.resolvedCells,
                fnDelim = args.fnDelim,
                counterDigits = args.counterDigits,
                usedCounter = args.scopeNextCounter,
                now = args.now,
                includeDate = false,
                includeTime = false
            ),
            resolvedCells = plan.resolvedCells,
            watermarkCells = WatermarkBuilder.buildTableCells(plan.resolvedCells),
            saveMode = args.saveMode,
            captureAspect = captureAspect,
            tableTemplate = args.tableTemplateState,
            usableTopRatio = usableTopRatio,
            usableBottomRatio = usableBottomRatio,
            watermark = buildWatermarkConfig(
                anchor = args.watermarkUi.anchor,
                offsetXRatio = args.watermarkUi.offsetXRatio,
                offsetYRatio = args.watermarkUi.offsetYRatio,
                tableWidthRatio = args.watermarkUi.tableWidthRatio,
                tableHeightRatio = args.watermarkUi.tableHeightRatio,
                tableBgAlpha = args.watermarkUi.bgAlpha,
                bgStyle = args.watermarkUi.bgStyle,
                labelScale = args.watermarkUi.labelScale,
                valueScale = args.watermarkUi.valueScale
            )
        )

        CameraPreviewHost(
            previewView = previewView,
            previewContentRect = if (captureRect.width() > 0f && captureRect.height() > 0f) captureRect else null,
            previewRequest = previewRequest,
            showWmPreview = args.showWmPreview,
            showGrid = args.showGrid,
            capturedUri = capturedUri,
            continuousPreviewMode = args.continuousPreviewMode,
            aspectRatio = captureAspect.ratioF,
            captureAspectRatio = captureAspect.ratioF,
            onDismissCaptured = onDismissCaptured,
            tapFocusUi = tapFocusUi,
            isWatermarkArmed = isWatermarkArmed,
            onWatermarkRectChange = { watermarkRect = it }
        )

        Box(modifier = gestureModifier)
    }
}

internal fun resolveZoomBounds(minSupported: Float, maxSupported: Float): Pair<Float, Float> {
    val clampedMax = maxSupported.coerceAtLeast(1f)
    val clampedMin = max(1f, minSupported).coerceAtMost(clampedMax)
    return clampedMin to clampedMax
}

internal fun shouldHandlePinch(
    centroidX: Float,
    centroidY: Float,
    captureRect: RectF,
    watermarkDragActive: Boolean
): Boolean {
    if (captureRect.width() <= 0f || captureRect.height() <= 0f) return false
    if (!captureRect.contains(centroidX, centroidY)) return false
    if (watermarkDragActive) return false
    return true
}
