@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog.ui.camera.preview

import android.graphics.RectF
import android.util.Log
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.min
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
        var watermarkRect by remember { mutableStateOf<RectF?>(null) }

        fun updateCaptureRect() {
            val contentRect = resolvePreviewContentRect(
                previewView = previewView,
                overlayWidth = previewView.width.toFloat(),
                overlayHeight = previewView.height.toFloat()
            )
            captureRect = computeCaptureAreaRect(
                contentRect = contentRect,
                captureAspectRatio = captureAspect.ratioF
            )
            if (!previewLogged) {
                Log.d(
                    "DZlogPreview",
                    "Preview crop=${captureRect.width().toInt()}x${captureRect.height().toInt()} aspect=${captureAspect.label} content=${contentRect.width().toInt()}x${contentRect.height().toInt()}"
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
                val actual = ((zoomState.zoomRatio * 10f).roundToInt()).coerceIn(10, 20)
                args.onActualZoomTenthsChange(actual)
            }
            activeCamera.cameraInfo.zoomState.observe(lifecycleOwner, observer)
            onDispose {
                activeCamera.cameraInfo.zoomState.removeObserver(observer)
            }
        }

        LaunchedEffect(boundCamera, args.zoomRatioTenths) {
            val activeCamera = boundCamera ?: return@LaunchedEffect
            val requested = args.zoomRatioTenths.coerceIn(10, 20) / 10f
            val zoomState = activeCamera.cameraInfo.zoomState.value
            val maxSupported = zoomState?.maxZoomRatio ?: 1f
            val minSupported = zoomState?.minZoomRatio ?: 1f
            val target = min(requested, min(2f, maxSupported)).coerceAtLeast(minSupported)
            runCatching { activeCamera.cameraControl.setZoomRatio(target) }
        }

        val gestureModifier = Modifier
            .fillMaxSize()
            .pointerInput(watermarkRect, captureRect) {
                detectDragGestures { change, dragAmount ->
                    val tableRect = watermarkRect ?: return@detectDragGestures
                    if (!tableRect.contains(change.position.x, change.position.y)) return@detectDragGestures
                    change.consume()

                    val contentRect = captureRect
                    val tableW = tableRect.width()
                    val tableH = tableRect.height()
                    val maxX = (contentRect.width() - tableW).coerceAtLeast(0f)
                    val maxY = (contentRect.height() - tableH).coerceAtLeast(0f)
                    if (maxX <= 0f || maxY <= 0f) return@detectDragGestures

                    val currentX = (tableRect.left - contentRect.left).coerceIn(0f, maxX)
                    val currentY = (tableRect.top - contentRect.top).coerceIn(0f, maxY)
                    val nextXRatio = (((currentX + dragAmount.x).coerceIn(0f, maxX) / maxX) * 100f).toInt().coerceIn(0, 100)
                    val nextYRatio = (((currentY + dragAmount.y).coerceIn(0f, maxY) / maxY) * 100f).toInt().coerceIn(0, 100)
                    args.onWatermarkOffsetRatioChange(nextXRatio, nextYRatio)
                }
            }
            .pointerInput(boundCamera, captureRect, tapFocusUi, watermarkRect) {
                detectTapGestures { offset ->
                    if (watermarkRect?.contains(offset.x, offset.y) == true) {
                        args.onOpenTableEditor()
                        return@detectTapGestures
                    }
                    val activeCamera = boundCamera ?: return@detectTapGestures
                    if (!captureRect.contains(offset.x, offset.y)) return@detectTapGestures
                    if (watermarkRect?.contains(offset.x, offset.y) == true) return@detectTapGestures

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
            .pointerInput(boundCamera, captureRect, watermarkRect) {
                detectTransformGestures { centroid, _, zoom, _ ->
                    val activeCamera = boundCamera ?: return@detectTransformGestures
                    if (!captureRect.contains(centroid.x, centroid.y)) return@detectTransformGestures
                    if (watermarkRect?.contains(centroid.x, centroid.y) == true) return@detectTransformGestures

                    val zoomState = activeCamera.cameraInfo.zoomState.value ?: return@detectTransformGestures
                    val maxSupported = min(2f, zoomState.maxZoomRatio)
                    val minSupported = zoomState.minZoomRatio
                    val next = (zoomState.zoomRatio * zoom).coerceIn(minSupported, maxSupported)

                    runCatching { activeCamera.cameraControl.setZoomRatio(next) }
                    val tenths = (next * 10f).roundToInt().coerceIn(10, 20)
                    args.onRequestedZoomTenthsCommit(tenths)
                    args.onActualZoomTenthsChange(tenths)
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
            onWatermarkRectChange = { watermarkRect = it }
        )

        Box(modifier = gestureModifier)
    }
}
