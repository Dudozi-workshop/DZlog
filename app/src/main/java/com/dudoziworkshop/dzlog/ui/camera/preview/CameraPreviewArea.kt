@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import android.os.SystemClock
import androidx.compose.ui.unit.dp
import android.util.Log
import android.view.View
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.lifecycle.Observer
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.dudoziworkshop.dzlog.domain.naming.buildGalleryRelativePathFromSlotDrafts
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.naming.resolveGroupValue
import com.dudoziworkshop.dzlog.domain.phrase.PhraseResolver
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.ui.camera.controller.bindCamera
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.watermark.computeBoundsSize
import com.dudoziworkshop.dzlog.feature.table.render.computeRatioOnlyTableShape
import kotlinx.coroutines.delay
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
        var watermarkBoundsRect by remember { mutableStateOf<RectF?>(null) }
        var watermarkRawRect by remember { mutableStateOf<RectF?>(null) }
        var watermarkDragActive by remember { mutableStateOf(false) }
        var suppressWatermarkTapUntilMs by remember { mutableStateOf(0L) }
        var isWatermarkArmed by remember { mutableStateOf(false) }
        var previewBoundsOffsetX10000 by remember { mutableStateOf(args.watermarkUi.boundsOffsetX10000.coerceIn(0, 10000)) }
        var previewBoundsOffsetY10000 by remember { mutableStateOf(args.watermarkUi.boundsOffsetY10000.coerceIn(0, 10000)) }
        var previewOffsetX by remember { mutableStateOf((previewBoundsOffsetX10000 / 100f).roundToInt().coerceIn(0, 100)) }
        var previewOffsetY by remember { mutableStateOf((previewBoundsOffsetY10000 / 100f).roundToInt().coerceIn(0, 100)) }
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
            args.onWatermarkBoundsOffset10000Commit(previewBoundsOffsetX10000, previewBoundsOffsetY10000)
        }

        fun markWatermarkInteraction() {
            watermarkLastInteractionMs = SystemClock.uptimeMillis()
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
            .cameraPreviewGestureModifier(
                context = context,
                scope = scope,
                previewView = previewView,
                boundCamera = boundCamera,
                captureRect = captureRect,
                tapFocusUi = tapFocusUi,
                suppressWatermarkTapUntilMs = suppressWatermarkTapUntilMs,
                watermarkBoundsRect = watermarkBoundsRect,
                watermarkRawRect = watermarkRawRect,
                isWatermarkArmed = isWatermarkArmed,
                watermarkDragActive = watermarkDragActive,
                dragTouchSlop = dragTouchSlop,
                dragStartLeftPx = dragStartLeftPx,
                dragStartTopPx = dragStartTopPx,
                dragTableWidthPx = dragTableWidthPx,
                dragTableHeightPx = dragTableHeightPx,
                dragPreviewOffsetPx = dragPreviewOffsetPx,
                dragAccumDx = dragAccumDx,
                dragAccumDy = dragAccumDy,
                dragStartedAfterSlop = dragStartedAfterSlop,
                rotationCwDeg = args.watermarkUi.rotationCwDeg,
                onTapFocusUiChange = onTapFocusUiChange,
                onOpenTableEditor = args.onOpenTableEditor,
                onCommitWatermarkOffsetIfNeeded = ::commitWatermarkOffsetIfNeeded,
                onMarkWatermarkInteraction = ::markWatermarkInteraction,
                onWatermarkArmedChange = { isWatermarkArmed = it },
                onWatermarkDragActiveChange = { watermarkDragActive = it },
                onDragStartLeftPxChange = { dragStartLeftPx = it },
                onDragStartTopPxChange = { dragStartTopPx = it },
                onDragTableWidthPxChange = { dragTableWidthPx = it },
                onDragTableHeightPxChange = { dragTableHeightPx = it },
                onDragPreviewOffsetPxChange = { dragPreviewOffsetPx = it },
                onDragAccumDxChange = { dragAccumDx = it },
                onDragAccumDyChange = { dragAccumDy = it },
                onDragStartedAfterSlopChange = { dragStartedAfterSlop = it },
                onSuppressWatermarkTapUntilMsChange = { suppressWatermarkTapUntilMs = it },
                onPreviewBoundsOffsetX10000Change = { previewBoundsOffsetX10000 = it },
                onPreviewBoundsOffsetY10000Change = { previewBoundsOffsetY10000 = it },
                onPreviewOffsetXChange = { previewOffsetX = it },
                onPreviewOffsetYChange = { previewOffsetY = it },
                onPendingLocalOffsetSyncChange = { pendingLocalOffsetSync = it },
                onWatermarkBoundsOffset10000Preview = args.onWatermarkBoundsOffset10000Preview,
                onWatermarkOffsetRatioPreview = args.onWatermarkOffsetRatioPreview,
                onMaxZoomTenthsChange = args.onMaxZoomTenthsChange,
                onRequestedZoomTenthsCommit = args.onRequestedZoomTenthsCommit,
                onActualZoomTenthsChange = args.onActualZoomTenthsChange,
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

        // 미동기화(null) 상태에서는 COUNTER 값을 임의 숫자로 확정하지 않고 표시를 보류한다.
        val resolvedCellsForPreview = remember(plan, args.scopeNextCounter) {
            if (args.scopeNextCounter != null) {
                plan.resolvedCells
            } else {
                plan.resolvedCells.map { cell ->
                    if (cell.type == TableCellDataType.COUNTER) {
                        cell.copy(resolvedText = "", isEmpty = true)
                    } else {
                        cell
                    }
                }
            }
        }

        val previewRequest = CaptureRequest(
            relativePath = buildGalleryRelativePathFromSlotDrafts(
                resolvedCells = resolvedCellsForPreview,
                pathSlotDrafts = args.tableTemplateState.pathSlotDrafts,
                now = args.now,
                dateFormat = args.dateFormat,
                timeFormat = args.timeFormat,
            ),
            group1 = resolveGroupValue(resolvedCellsForPreview, GroupLevel.G1),
            group2 = resolveGroupValue(resolvedCellsForPreview, GroupLevel.G2),
            displayName = CaptureNamingPolicy.buildDisplayNameForCounter(
                resolvedCells = resolvedCellsForPreview,
                fileNameSlotDrafts = args.tableTemplateState.fileNameSlotDrafts,
                fnDelim = args.fnDelim,
                counterDigits = args.counterDigits,
                usedCounter = args.scopeNextCounter,
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

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val parentHeightPx = with(density) { maxHeight.toPx() }
            val safeAspect = captureAspect.ratioF.coerceAtLeast(0.01f)
            // Camera Preview: 표 자체 외곽 비율(SSOT) + 촬영 배치(위치/회전)는 분리한다.
            val cameraPreviewShape = computeRatioOnlyTableShape(
                tableWidthRatio = args.watermarkUi.tableWidthRatio,
                tableHeightRatio = args.watermarkUi.tableHeightRatio,
                maxWidthRatio = args.watermarkUi.tableWidthRatio,
                maxHeightRatio = args.watermarkUi.tableHeightRatio,
            )

            // ===== Preview Layout Anchor Rule =====
            // 9:16 프리뷰는 PreviewArea의 top(=상단바 바로 아래)에 붙인다.
            // 3:4, 1:1 프리뷰는 9:16의 centerY를 기준으로 중앙 정렬한다.
            // 이 규칙은 “9:16 최대 세로 확보 + 비율 변경 시 중심 흔들림 최소화”를 위한 고정 설계다.
            val previewWidthPx = widthPx
            val h916 = if (previewWidthPx > 0f) previewWidthPx * 16f / 9f else 0f
            val top916 = 0f
            val centerY = top916 + (h916 / 2f)
            val h34 = if (previewWidthPx > 0f) previewWidthPx * 4f / 3f else 0f
            val top34 = centerY - (h34 / 2f)
            val h11 = previewWidthPx
            val top11 = centerY - (h11 / 2f)

            val rawTopCurrentPx = when (captureAspect) {
                CaptureAspect.R9_16 -> top916
                CaptureAspect.R3_4 -> top34
                CaptureAspect.R1_1 -> top11
            }
            val heightCurrentPx = if (previewWidthPx > 0f) previewWidthPx / safeAspect else 0f
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
                val baseBoundsOffsetPx = if (captureRect.width() > 0f && captureRect.height() > 0f) {
                    val baseW = captureRect.width()
                    val rawW = baseW * (cameraPreviewShape.tableWidthRatio.coerceIn(10, 100) / 100f)
                    val rawH = baseW * (cameraPreviewShape.tableHeightRatio.coerceIn(10, 100) / 100f)
                    val (boundsW, boundsH) = computeBoundsSize(rawW, rawH, args.watermarkUi.rotationCwDeg)
                    val boundsMaxX = (captureRect.width() - boundsW).coerceAtLeast(0f)
                    val boundsMaxY = (captureRect.height() - boundsH).coerceAtLeast(0f)
                    val baseBoundsLeftPx = boundsMaxX * (previewBoundsOffsetX10000 / 10000f)
                    val baseBoundsTopPx = boundsMaxY * (previewBoundsOffsetY10000 / 10000f)
                    Offset(baseBoundsLeftPx, baseBoundsTopPx)
                } else {
                    null
                }
                val effectiveOverrideOffsetPx = dragPreviewOffsetPx ?: baseBoundsOffsetPx

                val previewContentRect = if (captureRect.width() > 0f && captureRect.height() > 0f) captureRect else null

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
                    tapFocusUi = tapFocusUi,
                    isWatermarkArmed = isWatermarkArmed,
                    watermarkOffsetOverridePx = effectiveOverrideOffsetPx,
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
            }
        }
    }
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

