@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog.ui.camera.preview

import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import com.example.dzlog.ui.camera.controller.resolvePreviewContentRect
import com.example.dzlog.ui.camera.controller.startTapToFocus
import com.example.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * CameraPreviewArea
 * - 목적: CameraScreen에서 "프리뷰 영역 덩어리"를 캡슐화해 조립자 역할을 강화함
 * - 포함: PreviewView(host), previewContentRect 계산, tap-to-focus UX, 워터마크/결과 오버레이 조립
 * - 제외: 촬영 저장 로직(파일 저장/카운터 증가 등)은 상위에서 유지
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
            .fillMaxWidth()
            .aspectRatio(captureAspect.ratioF)
            .background(DDZColor.PrimaryDark)
            .clipToBounds()
    ) {
        val previewView = remember(context) {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }

        var previewContentRect by remember { mutableStateOf<RectF?>(null) }

        fun updatePreviewContentRect() {
            previewContentRect = resolvePreviewContentRect(previewView)
            if (!previewLogged) {
                val rect = previewContentRect
                if (rect != null) {
                    Log.d(
                        "DZlogPreview",
                        "Preview size=${rect.width().toInt()}x${rect.height().toInt()} aspect=${captureAspect.label} overlay-only (no bitmap)"
                    )
                    previewLogged = true
                }
            }
        }

        DisposableEffect(previewView, lifecycleOwner) {
            val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                updatePreviewContentRect()
            }
            val streamObserver = Observer<PreviewView.StreamState> { state ->
                if (state == PreviewView.StreamState.STREAMING) {
                    updatePreviewContentRect()
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

        DisposableEffect(previewView, boundCamera) {
            val camera = boundCamera

            // Tap-to-focus (AF/AE) on PreviewView
            val listener = View.OnTouchListener { v, event ->
                if (event.action != MotionEvent.ACTION_UP) return@OnTouchListener true
                if (camera == null) return@OnTouchListener true

                // Accessibility / lint: onTouch consumes click -> performClick required
                v?.performClick()

                val x = event.x
                val y = event.y
                onTapFocusUiChange(
                    TapFocusUiState(
                        xPx = x,
                        yPx = y,
                        phase = FocusRingPhase.FOCUSING
                    )
                )

                startTapToFocus(
                    context = context,
                    camera = camera,
                    previewView = previewView,
                    xPx = x,
                    yPx = y,
                    onResult = { success ->
                        scope.launch {
                            if (success) {
                                onTapFocusUiChange(tapFocusUi?.copy(phase = FocusRingPhase.SUCCESS))
                                delay(350)
                            } else {
                                // 실패 UX는 표시하지 않음(요청 사항): 짧게 사라짐
                                delay(200)
                            }
                            onTapFocusUiChange(null)
                        }
                    }
                )
                true
            }

            previewView.setOnTouchListener(listener)
            onDispose {
                previewView.setOnTouchListener(null)
            }
        }

        val plan = remember(args.tableTemplateState, args.now, args.counterDigits, args.dateFormat, args.timeFormat) {
            args.tableResolver.plan(
                cells = args.tableTemplateState.cells,
                captureNow = args.now,
                config = TableResolver.Config(
                    counterDigits = args.counterDigits,
                    dateFormat = args.dateFormat,
                    timeFormat = args.timeFormat
                )
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
                // ✅ 프리뷰도 "단일 소스 카운터"를 표시한다.
                // - COUNTER 셀 ON/OFF, seed 수정 등 UI 상태에 의해 프리뷰 카운터가 흔들리지 않도록 한다.
                // - 실제 증가 트리거는 captureAndSave에서만 발생한다.
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
            previewContentRect = previewContentRect,
            previewRequest = previewRequest,
            showWmPreview = args.showWmPreview,
            capturedUri = capturedUri,
            continuousPreviewMode = args.continuousPreviewMode,
            aspectRatio = captureAspect.ratioF,
            onDismissCaptured = onDismissCaptured,
            tapFocusUi = tapFocusUi
        )
    }
}
