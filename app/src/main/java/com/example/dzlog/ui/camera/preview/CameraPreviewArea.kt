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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.lifecycle.Observer
import com.example.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.example.dzlog.domain.model.CaptureAspect
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
import androidx.compose.ui.unit.IntOffset

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
            .background(DDZColor.Background)
            .clipToBounds()
    ) {
        val previewView = remember(context) {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }

        var captureRect by remember { mutableStateOf(RectF(0f, 0f, 0f, 0f)) }
        var previewBoxWidthPx by remember { mutableStateOf(0f) }
        var previewBoxHeightPx by remember { mutableStateOf(0f) }
        var usableTopRatio by remember { mutableStateOf(0f) }
        var usableBottomRatio by remember { mutableStateOf(1f) }
        var watermarkRect by remember { mutableStateOf<RectF?>(null) }
        var watermarkDragActive by remember { mutableStateOf(false) }
        var suppressWatermarkTapUntilMs by remember { mutableStateOf(0L) }
        var isWatermarkArmed by remember { mutableStateOf(false) }
        var previewOffsetX by remember { mutableStateOf(args.watermarkUi.offsetXRatio.coerceIn(0, 100)) }
        var previewOffsetY by remember { mutableStateOf(args.watermarkUi.offsetYRatio.coerceIn(0, 100)) }
        var dragPreviewOffsetPx by remember { mutableStateOf<Offset?>(null) }
        var dragStartLeftPx by remember { mutableStateOf(0f) }
        var dragStartTopPx by remember { mutableStateOf(0f) }
        var dragTableWidthPx by remember { mutableStateOf(0f) }
        var dragTableHeightPx by remember { mutableStateOf(0f) }
        var pendingLocalOffsetSync by remember { mutableStateOf(false) }
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
            if (watermarkDragActive || dragPreviewOffsetPx != null) return@LaunchedEffect
            val nextX = args.watermarkUi.offsetXRatio.coerceIn(0, 100)
            val nextY = args.watermarkUi.offsetYRatio.coerceIn(0, 100)
            if (pendingLocalOffsetSync && (nextX != previewOffsetX || nextY != previewOffsetY)) {
                return@LaunchedEffect
            }
            previewOffsetX = nextX
            previewOffsetY = nextY
            pendingLocalOffsetSync = false
            dragPreviewOffsetPx = null
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
                overlayWidth = previewBoxWidthPx.takeIf { it > 0f } ?: previewView.width.toFloat(),
                overlayHeight = previewBoxHeightPx.takeIf { it > 0f } ?: previewView.height.toFloat()
            )
            captureRect = computeCaptureAreaRect(
                contentRect = contentRect,
                captureAspectRatio = captureAspect.ratioF,
                usableRect = contentRect
            )
            usableTopRatio = 0f
            usableBottomRatio = 1f
            args.onUsableVerticalRatioChange(0f, 1f)
            if (!previewLogged) {
                Log.d(
                    "DZlogPreview",
                    "Preview crop=${captureRect.width().toInt()}x${captureRect.height().toInt()} aspect=${captureAspect.label} content=${contentRect.width().toInt()}x${contentRect.height().toInt()} usableTop=0 usableBottom=${contentRect.bottom.toInt()}"
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

        LaunchedEffect(captureAspect) {
            previewView.scaleType = when (captureAspect) {
                CaptureAspect.R3_4 -> PreviewView.ScaleType.FIT_CENTER
                CaptureAspect.R1_1 -> PreviewView.ScaleType.FILL_CENTER
                CaptureAspect.R9_16 -> PreviewView.ScaleType.FILL_CENTER
            }
            updateCaptureRect()
        }

        LaunchedEffect(previewView, lifecycleOwner, args.photoQualityMode) {
            bindCamera(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                aspect = CaptureAspect.R3_4,
                photoQualityMode = args.photoQualityMode
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
            .pointerInput(boundCamera, captureRect, isWatermarkArmed) {
                val pinchScaleDeadZone = 0.01f
                awaitEachGesture {
                    val activeCamera = boundCamera ?: return@awaitEachGesture
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val tableRect = watermarkRect
                    var dragEnabled = isWatermarkArmed && tableRect != null &&
                        tableRect.contains(firstDown.position.x, firstDown.position.y)
                    watermarkDragActive = false
                    if (dragEnabled && tableRect != null) {
                        dragStartLeftPx = tableRect.left - captureRect.left
                        dragStartTopPx = tableRect.top - captureRect.top
                        dragTableWidthPx = tableRect.width()
                        dragTableHeightPx = tableRect.height()
                        dragAccumDx = 0f
                        dragAccumDy = 0f
                        dragStartedAfterSlop = false
                        dragPreviewOffsetPx = Offset(dragStartLeftPx, dragStartTopPx)
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
                                dragPreviewOffsetPx = null
                                dragTableWidthPx = 0f
                                dragTableHeightPx = 0f
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

                        val contentRect = captureRect
                        val tableW = dragTableWidthPx
                        val tableH = dragTableHeightPx
                        val maxX = (contentRect.width() - tableW).coerceAtLeast(0f)
                        val maxY = (contentRect.height() - tableH).coerceAtLeast(0f)

                        val delta = change.positionChange()
                        dragAccumDx += delta.x
                        dragAccumDy += delta.y
                        if (!dragStartedAfterSlop) {
                            val moved = hypot(dragAccumDx.toDouble(), dragAccumDy.toDouble()).toFloat()
                            if (moved < dragTouchSlop) continue
                            dragStartedAfterSlop = true
                            watermarkDragActive = true
                        }
                        if (change.positionChanged()) change.consume()

                        val nextOffsetPx = computeClampedDragOffsetPx(
                            dragStartLeftPx = dragStartLeftPx,
                            dragStartTopPx = dragStartTopPx,
                            dragAccumDx = dragAccumDx,
                            dragAccumDy = dragAccumDy,
                            maxX = maxX,
                            maxY = maxY
                        )

                        dragPreviewOffsetPx = nextOffsetPx
                        markWatermarkInteraction()
                    }

                    if (dragEnabled && dragStartedAfterSlop) {
                        suppressWatermarkTapUntilMs = SystemClock.uptimeMillis() + 180L
                        isWatermarkArmed = true

                        val tableW = dragTableWidthPx
                        val tableH = dragTableHeightPx
                        val maxX = (captureRect.width() - tableW).coerceAtLeast(0f)
                        val maxY = (captureRect.height() - tableH).coerceAtLeast(0f)
                        val committedOffset = dragPreviewOffsetPx ?: Offset(dragStartLeftPx, dragStartTopPx)
                        val (committedXRatio, committedYRatio) = computeOffsetRatioFromPx(
                            committedOffsetPx = committedOffset,
                            maxX = maxX,
                            maxY = maxY
                        )

                        previewOffsetX = committedXRatio
                        previewOffsetY = committedYRatio
                        pendingLocalOffsetSync = true
                        args.onWatermarkOffsetRatioPreview(committedXRatio, committedYRatio)
                        markWatermarkInteraction()
                        commitWatermarkOffsetIfNeeded()
                    }
                    watermarkDragActive = false
                    dragPreviewOffsetPx = null
                    dragTableWidthPx = 0f
                    dragTableHeightPx = 0f
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
                counterSeedOverride = args.scopeNextCounter,
                phraseSets = args.tableTemplateState.phraseSets
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
                fileNameSlots = args.tableTemplateState.fileNameSlots,
                includeDate = false,
                includeTime = false
            ),
            resolvedCells = plan.resolvedCells,
            watermarkCells = WatermarkBuilder.buildTableCells(plan.resolvedCells),
            saveMode = args.saveMode,
            captureAspect = captureAspect,
            photoQualityMode = args.photoQualityMode,
            tableTemplate = args.tableTemplateState,
            usableTopRatio = usableTopRatio,
            usableBottomRatio = usableBottomRatio,
            watermark = buildWatermarkConfig(
                anchor = args.watermarkUi.anchor,
                offsetXRatio = previewOffsetX,
                offsetYRatio = previewOffsetY,
                tableWidthRatio = args.watermarkUi.tableWidthRatio,
                tableHeightRatio = args.watermarkUi.tableHeightRatio,
                tableBgAlpha = args.watermarkUi.bgAlpha,
                bgStyle = args.watermarkUi.bgStyle,
                valueScale = args.watermarkUi.valueScale,
                textColorMode = args.watermarkUi.textColorMode,
                manualTextColor = args.watermarkUi.manualTextColor,
                textAlign = args.watermarkUi.textAlign,
                gridEnabled = args.watermarkUi.wmGridEnabled
            )
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val parentHeightPx = with(density) { maxHeight.toPx() }
            val safeAspect = captureAspect.ratioF.coerceAtLeast(0.01f)
            val top9By16Px: Float? = args.settingsButtonBottomY
            val height9By16Px = if (widthPx > 0f) widthPx / (9f / 16f) else 0f
            val anchorCenterYPx = if (top9By16Px != null && height9By16Px > 0f) {
                top9By16Px + (height9By16Px / 2f)
            } else {
                parentHeightPx / 2f
            }
            val heightCurrentPx = if (widthPx > 0f) widthPx / safeAspect else 0f
            val rawTopCurrentPx = anchorCenterYPx - (heightCurrentPx / 2f)
            val minTopPx = 0f
            val maxTopPx = (parentHeightPx - heightCurrentPx).coerceAtLeast(0f)
            val topCurrentPx = rawTopCurrentPx.coerceIn(minTopPx, maxTopPx)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(safeAspect)
                    .offset { IntOffset(0, topCurrentPx.roundToInt()) }
                    .onSizeChanged { size ->
                        previewBoxWidthPx = size.width.toFloat()
                        previewBoxHeightPx = size.height.toFloat()
                    }
                    .clipToBounds()
            ) {
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
                    watermarkOffsetOverridePx = dragPreviewOffsetPx,
                    onWatermarkRectChange = { watermarkRect = it }
                )

                Box(modifier = gestureModifier)
            }
        }
    }
}

internal fun computeClampedDragOffsetPx(
    dragStartLeftPx: Float,
    dragStartTopPx: Float,
    dragAccumDx: Float,
    dragAccumDy: Float,
    maxX: Float,
    maxY: Float
): Offset {
    val nextLeftPx = if (maxX > 0f) {
        (dragStartLeftPx + dragAccumDx).coerceIn(0f, maxX)
    } else {
        0f
    }
    val nextTopPx = if (maxY > 0f) {
        (dragStartTopPx + dragAccumDy).coerceIn(0f, maxY)
    } else {
        0f
    }
    return Offset(nextLeftPx, nextTopPx)
}

internal fun computeOffsetRatioFromPx(
    committedOffsetPx: Offset,
    maxX: Float,
    maxY: Float
): Pair<Int, Int> {
    val committedXRatio = if (maxX > 0f) {
        ((committedOffsetPx.x / maxX) * 100f).roundToInt().coerceIn(0, 100)
    } else {
        0
    }
    val committedYRatio = if (maxY > 0f) {
        ((committedOffsetPx.y / maxY) * 100f).roundToInt().coerceIn(0, 100)
    } else {
        0
    }
    return committedXRatio to committedYRatio
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
