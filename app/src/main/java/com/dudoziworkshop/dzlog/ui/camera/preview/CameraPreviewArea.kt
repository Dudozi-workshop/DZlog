@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.dudoziworkshop.dzlog.ui.camera.preview

import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialLocation
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialDemoProgress
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialReadOnly
import android.graphics.RectF
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.naming.buildSavePath
import com.dudoziworkshop.dzlog.domain.phrase.PhraseResolver
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.ui.camera.controller.bindCamera
import com.dudoziworkshop.dzlog.ui.camera.controller.startTapToFocus
import com.dudoziworkshop.dzlog.ui.camera.interop.requestTapAeLock
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFocusMode
import kotlinx.coroutines.delay
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlin.math.max
import java.util.concurrent.atomic.AtomicBoolean
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
    onTapFocusUiChange: (TapFocusUiState?) -> Unit,
    isTableSelected: Boolean,
    isTableLocked: Boolean,
    onTableSelectionChange: (Boolean) -> Unit,
    onCornerResizeScale: (Float, Boolean) -> Unit,
    focusControlsCollapseToken: Int = 0,
) {
    val context = args.context
    val lifecycleOwner = args.lifecycleOwner
    val captureAspect = args.captureAspect

    var previewLogged by remember { mutableStateOf(false) }
    var cameraBindingGeneration by remember { mutableIntStateOf(0) }
    var streamingGeneration by remember { mutableIntStateOf(0) }

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
        var previewBoxWidthPx by remember { mutableFloatStateOf(0f) }
        var previewBoxHeightPx by remember { mutableFloatStateOf(0f) }
        var usableTopRatio by remember { mutableFloatStateOf(0f) }
        var usableBottomRatio by remember { mutableFloatStateOf(1f) }
        var watermarkBoundsRect by remember { mutableStateOf<RectF?>(null) }
        var activeHandleCorner by remember { mutableStateOf<ResizeHandleCorner?>(null) }
        var watermarkRawRect by remember { mutableStateOf<RectF?>(null) }
        var watermarkDragActive by remember { mutableStateOf(false) }
        var suppressWatermarkTapUntilMs by remember { mutableLongStateOf(0L) }
        val isWatermarkArmed = isTableSelected
        var previewBoundsOffsetX10000 by remember { mutableIntStateOf(args.watermarkUi.boundsOffsetX10000.coerceIn(0, 10000)) }
        var previewBoundsOffsetY10000 by remember { mutableIntStateOf(args.watermarkUi.boundsOffsetY10000.coerceIn(0, 10000)) }
        var previewOffsetX by remember { mutableIntStateOf((previewBoundsOffsetX10000 / 100f).roundToInt().coerceIn(0, 100)) }
        var previewOffsetY by remember { mutableIntStateOf((previewBoundsOffsetY10000 / 100f).roundToInt().coerceIn(0, 100)) }
        var dragPreviewOffsetPx by remember { mutableStateOf<Offset?>(null) }
        var dragStartLeftPx by remember { mutableFloatStateOf(0f) }
        var dragStartTopPx by remember { mutableFloatStateOf(0f) }
        var dragTableWidthPx by remember { mutableFloatStateOf(0f) }
        var dragTableHeightPx by remember { mutableFloatStateOf(0f) }
        var pendingLocalOffsetSync by remember { mutableStateOf(false) }
        val dragTouchSlop = LocalViewConfiguration.current.touchSlop
        val liveWatermarkBounds = rememberUpdatedState(watermarkBoundsRect)
        val liveWatermarkRawRect = rememberUpdatedState(watermarkRawRect)
        val liveHandleCorner = rememberUpdatedState(activeHandleCorner)
        // Asynchronous CameraX/Camera2 callbacks must not apply to a rebound camera.
        val liveBoundCamera = rememberUpdatedState(boundCamera)
        val liveFocusMode = rememberUpdatedState(args.focusMode)
        val liveTapFocusUi = rememberUpdatedState(tapFocusUi)
        val previewAlive = remember { AtomicBoolean(true) }
        var aeLocked by remember { mutableStateOf(false) }
        var lockRequestInFlight by remember { mutableStateOf(false) }
        var focusControlsExpanded by remember { mutableStateOf(false) }
        var exposureInteracting by remember { mutableStateOf(false) }
        var focusInteractionId by remember { mutableIntStateOf(0) }
        var focusRequestId by remember { mutableIntStateOf(0) }
        var exposureRequestId by remember { mutableIntStateOf(0) }
        var exposureIndex by remember { mutableIntStateOf(0) }

        val exposureState = boundCamera?.cameraInfo?.exposureState
        val exposureSupported = exposureState?.isExposureCompensationSupported == true
        val minExposureIndex = exposureState?.exposureCompensationRange?.lower ?: 0
        val maxExposureIndex = exposureState?.exposureCompensationRange?.upper ?: 0
        val exposureStep = exposureState?.exposureCompensationStep?.toFloat() ?: 1f

        // Release transient Camera2 control when leaving the preview/rebinding.
        // Keep these changes scoped to the old camera rather than a new binding.
        DisposableEffect(boundCamera) {
            val oldCamera = boundCamera
            onDispose {
                if (oldCamera != null) {
                    requestTapAeLock(context, oldCamera, false) { }
                    runCatching { oldCamera.cameraControl.setExposureCompensationIndex(0) }
                    runCatching { oldCamera.cameraControl.cancelFocusAndMetering() }
                }
            }
        }
        DisposableEffect(Unit) {
            onDispose { previewAlive.set(false) }
        }

        // Hardware and transient UI are scoped to the bound camera.
        LaunchedEffect(boundCamera, args.focusMode) {
            aeLocked = false
            lockRequestInFlight = false
            focusControlsExpanded = false
            exposureInteracting = false
            focusInteractionId += 1
            focusRequestId += 1
            exposureRequestId += 1
            exposureIndex = boundCamera?.cameraInfo?.exposureState?.exposureCompensationIndex ?: 0
            onTapFocusUiChange(null)
            if (args.focusMode != CameraFocusMode.AUTO && boundCamera != null) {
                // Switching to the existing manual-focus tool must clear AE lock/EV.
                requestTapAeLock(context, boundCamera, false) { }
                runCatching { boundCamera.cameraControl.setExposureCompensationIndex(0) }
                exposureIndex = 0
            }
        }

        LaunchedEffect(focusControlsCollapseToken) {
            focusControlsExpanded = false
            if (!aeLocked) onTapFocusUiChange(null)
        }

        LaunchedEffect(focusControlsExpanded, focusInteractionId, aeLocked, tapFocusUi, exposureInteracting) {
            if (!focusControlsExpanded || tapFocusUi == null || exposureInteracting) return@LaunchedEffect
            delay(2_500L)
            focusControlsExpanded = false
            if (!aeLocked) onTapFocusUiChange(null)
        }

        fun requestFocusAt(xPx: Float, yPx: Float, locked: Boolean) {
            val activeCamera = boundCamera ?: return
            val id = ++focusRequestId
            focusInteractionId += 1
            focusControlsExpanded = true
            onTapFocusUiChange(TapFocusUiState(xPx, yPx, FocusRingPhase.FOCUSING))
            startTapToFocus(
                context = context,
                camera = activeCamera,
                previewView = previewView,
                xPx = xPx,
                yPx = yPx,
                locked = locked,
                onResult = { success ->
                    if (previewAlive.get() && id == focusRequestId && liveBoundCamera.value === activeCamera && liveFocusMode.value == CameraFocusMode.AUTO) {
                        onTapFocusUiChange(
                            TapFocusUiState(xPx, yPx,
                                if (success) FocusRingPhase.SUCCESS else FocusRingPhase.FAILED)
                        )
                    }
                },
            )
        }

        val focusTapCallbackState = rememberUpdatedState<(Float, Float) -> Unit> { x, y ->
            requestFocusAt(x, y, locked = aeLocked)
        }

        fun toggleFocusExposureLock() {
            val activeCamera = boundCamera ?: return
            if (lockRequestInFlight || args.focusMode != CameraFocusMode.AUTO) return
            val desired = !aeLocked
            lockRequestInFlight = true
            requestTapAeLock(context, activeCamera, desired) { applied ->
                if (!previewAlive.get() ||
                    liveBoundCamera.value !== activeCamera ||
                    liveFocusMode.value != CameraFocusMode.AUTO) {
                    // A stale lock completion must not affect the next camera session.
                    if (applied && desired) requestTapAeLock(context, activeCamera, false) { }
                    return@requestTapAeLock
                }
                lockRequestInFlight = false
                if (!applied) {
                    Toast.makeText(context, "이 기기에서는 노출 잠금을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@requestTapAeLock
                }
                aeLocked = desired
                focusInteractionId += 1
                if (desired) {
                    // Reapply an AF-only metering point without a timeout, while
                    // preserving the exposure frozen by Camera2 AE lock.
                    liveTapFocusUi.value?.let { requestFocusAt(it.xPx, it.yPx, locked = true) }
                } else {
                    focusRequestId += 1
                    runCatching { activeCamera.cameraControl.cancelFocusAndMetering() }
                    focusControlsExpanded = false
                    onTapFocusUiChange(null)
                }
            }
        }

        fun requestExposureIndex(nextIndex: Int) {
            val activeCamera = boundCamera ?: return
            if (!exposureSupported) return
            val next = nextIndex.coerceIn(minExposureIndex, maxExposureIndex)
            if (next == exposureIndex) return
            exposureIndex = next
            focusInteractionId += 1
            val id = ++exposureRequestId
            val future = runCatching { activeCamera.cameraControl.setExposureCompensationIndex(next) }.getOrNull()
            if (future == null) {
                exposureIndex = activeCamera.cameraInfo.exposureState.exposureCompensationIndex
                return
            }
            future.addListener({
                if (previewAlive.get() && id == exposureRequestId && liveBoundCamera.value === activeCamera) {
                    exposureIndex = runCatching { future.get() }
                        .getOrElse { activeCamera.cameraInfo.exposureState.exposureCompensationIndex }
                }
            }, ContextCompat.getMainExecutor(context))
        }

        fun commitWatermarkOffsetIfNeeded() {
            args.onWatermarkOffsetRatioCommit(previewOffsetX, previewOffsetY)
            args.onWatermarkBoundsOffset10000Commit(previewBoundsOffsetX10000, previewBoundsOffsetY10000)
        }

        LaunchedEffect(args.watermarkUi.boundsOffsetX10000, args.watermarkUi.boundsOffsetY10000) {
            if (watermarkDragActive || dragPreviewOffsetPx != null) return@LaunchedEffect
            val nextX10000 = args.watermarkUi.boundsOffsetX10000.coerceIn(0, 10000)
            val nextY10000 = args.watermarkUi.boundsOffsetY10000.coerceIn(0, 10000)
            val nextX = (nextX10000 / 100f).roundToInt().coerceIn(0, 100)
            val nextY = (nextY10000 / 100f).roundToInt().coerceIn(0, 100)
            if (pendingLocalOffsetSync && (nextX10000 != previewBoundsOffsetX10000 || nextY10000 != previewBoundsOffsetY10000)) {
                return@LaunchedEffect
            }
            previewBoundsOffsetX10000 = nextX10000
            previewBoundsOffsetY10000 = nextY10000
            previewOffsetX = nextX
            previewOffsetY = nextY
            pendingLocalOffsetSync = false
            dragPreviewOffsetPx = null
        }

        // Selection stays visible until the user deselects, captures, or navigates away.
        LaunchedEffect(args.showWmPreview) {
            if (!args.showWmPreview) onTableSelectionChange(false)
        }

        fun updateCaptureRect() {
            val previewCaptureLayout = calculatePreviewCaptureLayout(
                previewView = previewView,
                overlayWidth = previewBoxWidthPx.takeIf { it > 0f } ?: previewView.width.toFloat(),
                overlayHeight = previewBoxHeightPx.takeIf { it > 0f } ?: previewView.height.toFloat(),
                captureAspectRatio = captureAspect.ratioF,
            )
            captureRect = previewCaptureLayout.captureRect
            usableTopRatio = 0f
            usableBottomRatio = 1f
            args.onUsableVerticalRatioChange(0f, 1f)
            if (!previewLogged) {
                Log.d(
                    "DZlogPreview",
                    "Preview crop=${captureRect.width().toInt()}x${captureRect.height().toInt()} aspect=${captureAspect.label} content=${previewCaptureLayout.contentRect.width().toInt()}x${previewCaptureLayout.contentRect.height().toInt()} usableTop=0 usableBottom=${previewCaptureLayout.contentRect.bottom.toInt()}"
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
                    streamingGeneration += 1
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
                photoQualityMode = args.photoQualityMode
            ) { cap, camera ->
                onBoundImageCaptureChange(cap)
                onBoundCameraChange(camera)
                cameraBindingGeneration += 1
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

        // Binding/resuming can reset hardware zoom even when the saved request is unchanged.
        LaunchedEffect(boundCamera, args.zoomRatioTenths, args.maxZoomTenths,
            cameraBindingGeneration, streamingGeneration) {
            val activeCamera = boundCamera ?: return@LaunchedEffect
            val zoomState = activeCamera.cameraInfo.zoomState.value ?: return@LaunchedEffect
            val target = restoredZoomRatio(args.zoomRatioTenths,
                zoomState.minZoomRatio, zoomState.maxZoomRatio)
            runCatching { activeCamera.cameraControl.setZoomRatio(target) }
        }

        val gestureModifier = Modifier
            .fillMaxSize()
            .cameraPreviewGestureModifier(
                previewView = previewView,
                boundCamera = boundCamera,
                captureRect = captureRect,
                focusTapCallbackState = focusTapCallbackState,
                focusMode = args.focusMode,
                suppressWatermarkTapUntilMs = suppressWatermarkTapUntilMs,
                watermarkBoundsRectState = liveWatermarkBounds,
                watermarkRawRectState = liveWatermarkRawRect,
                isWatermarkArmed = isWatermarkArmed,
                isTableLocked = isTableLocked,
                dragTouchSlop = dragTouchSlop,
                dragStartLeftPx = dragStartLeftPx,
                dragStartTopPx = dragStartTopPx,
                dragTableWidthPx = dragTableWidthPx,
                dragTableHeightPx = dragTableHeightPx,
                dragPreviewOffsetPx = dragPreviewOffsetPx,
                onCommitWatermarkOffsetIfNeeded = ::commitWatermarkOffsetIfNeeded,
                onWatermarkArmedChange = onTableSelectionChange,
                onWatermarkDragActiveChange = { watermarkDragActive = it },
                onDragStartLeftPxChange = { dragStartLeftPx = it },
                onDragStartTopPxChange = { dragStartTopPx = it },
                onDragTableWidthPxChange = { dragTableWidthPx = it },
                onDragTableHeightPxChange = { dragTableHeightPx = it },
                onDragPreviewOffsetPxChange = { dragPreviewOffsetPx = it },
                onSuppressWatermarkTapUntilMsChange = { suppressWatermarkTapUntilMs = it },
                onPreviewBoundsOffsetX10000Change = { previewBoundsOffsetX10000 = it },
                onPreviewBoundsOffsetY10000Change = { previewBoundsOffsetY10000 = it },
                onPreviewOffsetXChange = { previewOffsetX = it },
                onPreviewOffsetYChange = { previewOffsetY = it },
                onPendingLocalOffsetSyncChange = { pendingLocalOffsetSync = it },
                onWatermarkBoundsOffset10000Preview = args.onWatermarkBoundsOffset10000Preview,
                onWatermarkOffsetRatioPreview = args.onWatermarkOffsetRatioPreview,
                onMaxZoomTenthsChange = args.onMaxZoomTenthsChange,
                onPinchZoomActiveChange = args.onPinchZoomActiveChange,
                onRequestedZoomTenthsCommit = args.onRequestedZoomTenthsCommit,
                onActualZoomTenthsChange = args.onActualZoomTenthsChange,
                onCornerResizeScale = onCornerResizeScale,
                activeHandleCornerState = liveHandleCorner,
                onActiveHandleCornerChange = { activeHandleCorner = it },
            )

        val plan = remember(
            args.tableTemplateState,
            args.scopeNextCounter,
            args.phraseProgressCursor,
            args.now,
            args.counterDigits,
            args.dateFormat,
            args.timeFormat
        ) {
            val selectedPhraseTextByCellId = PhraseResolver.resolveSelectedTextByCellId(
                cells = args.tableTemplateState.cells,
                phraseSets = args.tableTemplateState.phraseSets,
                // 순환문구 선택은 phrase 커서 기준이어야 하며, 카운터 seed와 분리한다.
                progressCursor = args.phraseProgressCursor,
            )
            args.tableResolver.plan(
                cells = args.tableTemplateState.cells,
                captureNow = args.now,
                config = TableResolver.Config(
                    counterDigits = args.counterDigits,
                    dateFormat = args.dateFormat,
                    timeFormat = args.timeFormat
                ),
                counterSeedOverride = args.scopeNextCounter,
                selectedPhraseTextByCellId = selectedPhraseTextByCellId,
            )
        }

        remember(plan) {
            plan.resolvedCells.any { it.type == TableCellDataType.COUNTER }
        }

        // Phrase switching can temporarily invalidate the counter stream.
        // Only reuse a complete preview for the SAME template content and phrase
        // position. Edits to cells, phrase sets, or naming rules must never leave
        // the previous template's values visible while counters resynchronize.
        var lastCompletePreview by remember(
            args.tableTemplateState,
            args.phraseProgressCursor,
        ) {
            mutableStateOf<Pair<Int, List<com.dudoziworkshop.dzlog.domain.table.ResolvedCell>>?>(null)
        }
        val completePreview = args.scopeNextCounter?.let { it to plan.resolvedCells }
        SideEffect {
            if (completePreview != null) lastCompletePreview = completePreview
        }
        val displayedPreview = completePreview ?: lastCompletePreview
        val resolvedCellsForPreview = displayedPreview?.second ?: plan.resolvedCells.map { cell ->
            if (cell.type == TableCellDataType.COUNTER) {
                cell.copy(resolvedText = "", isEmpty = true)
            } else {
                cell
            }
        }
        val displayedCounter = displayedPreview?.first

        val previewRequest = CaptureRequest(
            relativePath = buildSavePath(
                resolvedCells = resolvedCellsForPreview,
                pathSlotDrafts = args.tableTemplateState.pathSlotDrafts,
                now = args.now,
                dateFormat = args.dateFormat,
                timeFormat = args.timeFormat,
            ),
            displayName = CaptureNamingPolicy.buildDisplayNameForCounter(
                resolvedCells = resolvedCellsForPreview,
                fileNameSlotDrafts = args.tableTemplateState.fileNameSlotDrafts,
                fnDelim = args.fnDelim,
                counterDigits = args.counterDigits,
                usedCounter = displayedCounter,
                now = args.now,
                dateFormat = args.dateFormat,
                timeFormat = args.timeFormat,
            ),
            resolvedCells = resolvedCellsForPreview,
            watermarkCells = WatermarkBuilder.buildTableCells(resolvedCellsForPreview),
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
                boundsOffsetX10000 = previewBoundsOffsetX10000,
                boundsOffsetY10000 = previewBoundsOffsetY10000,
                tableWidthRatio = args.watermarkUi.tableWidthRatio,
                tableHeightRatio = args.watermarkUi.tableHeightRatio,
                tableBgAlpha = args.watermarkUi.bgAlpha,
                bgStyle = args.watermarkUi.bgStyle,
                valueScale = args.watermarkUi.valueScale,
                textColorMode = args.watermarkUi.textColorMode,
                manualTextColor = args.watermarkUi.manualTextColor,
                textAlign = args.watermarkUi.textAlign,
                gridEnabled = args.watermarkUi.wmGridEnabled,
                rotationCwDeg = args.watermarkUi.rotationCwDeg
            )
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val density = LocalDensity.current
            val widthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
            val parentHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
            val previewBoxLayout = calculatePreviewBoxLayout(
                parentWidthPx = widthPx,
                parentHeightPx = parentHeightPx,
                captureAspect = captureAspect,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(previewBoxLayout.widthFraction)
                    .aspectRatio(previewBoxLayout.safeAspect)
                    .offset { IntOffset(0, previewBoxLayout.topOffsetPx.roundToInt()) }
                    .onSizeChanged { size ->
                        previewBoxWidthPx = size.width.toFloat()
                        previewBoxHeightPx = size.height.toFloat()
                    }
                    .clipToBounds()
            ) {
                val previewContentRect = captureRect.takeIf { it.width() > 0f && it.height() > 0f }
                val baseBoundsOffsetPx = calculatePreviewWatermarkBaseBoundsOffsetPx(
                    captureRect = captureRect,
                    tableWidthRatio = args.watermarkUi.tableWidthRatio,
                    tableHeightRatio = args.watermarkUi.tableHeightRatio,
                    rotationCwDeg = args.watermarkUi.rotationCwDeg,
                    previewBoundsOffsetX10000 = previewBoundsOffsetX10000,
                    previewBoundsOffsetY10000 = previewBoundsOffsetY10000,
                )
                val effectiveOverrideOffsetPx = dragPreviewOffsetPx ?: baseBoundsOffsetPx

                CameraPreviewHost(
                    previewView = previewView,
                )

                CameraPreviewOverlays(
                    previewContentRect = previewContentRect,
                    previewRequest = previewRequest,
                    showWmPreview = args.showWmPreview,
                    showGrid = args.showGrid,
                    capturedUri = capturedUri,
                    continuousPreviewMode = args.continuousPreviewMode,
                    aspectRatio = captureAspect.ratioF,
                    onDismissCaptured = onDismissCaptured,
                    tapFocusUi = if (LocalTutorialReadOnly.current && LocalTutorialLocation.current?.pageId == "focus") {
                        TapFocusUiState(previewBoxWidthPx * 0.60f, previewBoxHeightPx * 0.60f,
                            if (LocalTutorialDemoProgress.current < 0.5f) FocusRingPhase.FOCUSING else FocusRingPhase.SUCCESS)
                    } else if (capturedUri != null) null else tapFocusUi,
                    tapFocusLocked = aeLocked,
                    tapFocusCompact = !focusControlsExpanded,
                    isWatermarkArmed = isWatermarkArmed,
                    isTableLocked = isTableLocked,
                    watermarkOffsetOverridePx = effectiveOverrideOffsetPx,
                    dragVisibleOffsetPx = dragPreviewOffsetPx,
                    activeHandleCorner = activeHandleCorner,
                    onWatermarkBoundsRectChange = { watermarkBoundsRect = it },
                    onWatermarkRawRectChange = { watermarkRawRect = it }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.TopCenter)
                        .background(DDZColor.SageBorder)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                        .background(DDZColor.SageBorder)
                )

                Box(modifier = gestureModifier)

                // Interactive camera controls sit ABOVE the preview tap layer.
                // They do not cover the rest of the preview or interfere with
                // table dragging, pinch zoom, or the capture button.
                if (args.focusMode == CameraFocusMode.AUTO && capturedUri == null) {
                    FocusRingControlsOverlay(
                        focus = tapFocusUi,
                        locked = aeLocked,
                        expanded = focusControlsExpanded,
                        exposureIndex = exposureIndex,
                        minExposureIndex = minExposureIndex,
                        maxExposureIndex = maxExposureIndex,
                        exposureStep = exposureStep,
                        exposureSupported = exposureSupported,
                        onToggleLock = ::toggleFocusExposureLock,
                        onExposureIndexChange = ::requestExposureIndex,
                        onExposureInteractionChange = { exposureInteracting = it },
                    )
                }
            }
        }
    }
}

internal fun resolveZoomBounds(minSupported: Float, maxSupported: Float): Pair<Float, Float> {
    val clampedMax = maxSupported.coerceAtLeast(1f)
    val clampedMin = max(1f, minSupported).coerceAtMost(clampedMax)
    return clampedMin to clampedMax
}

/** Restore the request against hardware bounds, never a stale UI maximum. */
internal fun restoredZoomRatio(requestedTenths: Int, minSupported: Float, maxSupported: Float): Float {
    val (minZoom, maxZoom) = resolveZoomBounds(minSupported, maxSupported)
    return (requestedTenths.coerceIn(10, 100) / 10f).coerceIn(minZoom, maxZoom)
}
