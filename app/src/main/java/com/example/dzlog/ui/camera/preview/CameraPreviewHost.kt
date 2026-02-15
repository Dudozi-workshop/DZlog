package com.example.dzlog.ui.camera.preview

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.dzlog.domain.model.CaptureRequest
import com.example.dzlog.domain.model.ContinuousPreviewMode

/**
 * [CameraPreviewHost]
 * - 목적: PreviewView(카메라 프리뷰) + 오버레이(UI 레이어)를 조립해 렌더링함
 * - 포함: AndroidView(PreviewView), FocusRingOverlay, WatermarkPreviewOverlay, CaptureResultOverlay
 * - 제외: CameraX 바인딩/촬영 저장/터치 처리/CameraControl 호출 금지(표시 전용)
 */
@Composable
internal fun CameraPreviewHost(
    previewView: PreviewView,
    previewContentRect: RectF?,
    previewRequest: CaptureRequest,
    showWmPreview: Boolean,
    showGrid: Boolean,
    capturedUri: Uri?,
    continuousPreviewMode: ContinuousPreviewMode,
    aspectRatio: Float,
    captureAspectRatio: Float,
    onDismissCaptured: () -> Unit,
    tapFocusUi: TapFocusUiState?,
    isWatermarkArmed: Boolean,
    watermarkOffsetOverridePx: Offset?,
    onWatermarkRectChange: (RectF?) -> Unit
) {
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { _: Context -> previewView },
        update = { it.scaleType = PreviewView.ScaleType.FILL_CENTER }
    )


    CaptureAreaMaskOverlay(captureRect = previewContentRect)

    // Tap-to-focus UI (ring)
    if (tapFocusUi != null) {
        FocusRingOverlay(tapFocusUi)
    }


    if (showGrid) {
        CameraGridOverlay(captureRect = previewContentRect)
    }

    WatermarkPreviewOverlay(
        enabled = showWmPreview,
        request = previewRequest,
        previewContentRect = previewContentRect,
        overrideOffsetPx = watermarkOffsetOverridePx,
        isArmed = isWatermarkArmed,
        onTableRectChange = onWatermarkRectChange
    )

    CaptureResultOverlay(
        capturedUri = capturedUri,
        continuousPreviewMode = continuousPreviewMode,
        aspectRatio = aspectRatio,
        onDismiss = onDismissCaptured
    )
}
