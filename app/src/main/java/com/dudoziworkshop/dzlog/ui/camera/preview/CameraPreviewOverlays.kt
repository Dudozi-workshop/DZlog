package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode

/**
 * [CameraPreviewOverlays]
 * - 목적: CameraPreviewArea 위에 덧그리는 오버레이 UI만 조립함
 * - 포함: 마스크, 포커스 링, 그리드, 워터마크 프리뷰, 캡처 결과 오버레이
 */
@Composable
internal fun CameraPreviewOverlays(
    previewContentRect: RectF?,
    previewRequest: CaptureRequest,
    showWmPreview: Boolean,
    showGrid: Boolean,
    capturedUri: Uri?,
    continuousPreviewMode: ContinuousPreviewMode,
    aspectRatio: Float,
    onDismissCaptured: () -> Unit,
    tapFocusUi: TapFocusUiState?,
    isWatermarkArmed: Boolean,
    isTableLocked: Boolean,
    watermarkOffsetOverridePx: Offset?,
    dragVisibleOffsetPx: Offset?,
    activeHandleCorner: ResizeHandleCorner?,
    onWatermarkBoundsRectChange: (RectF?) -> Unit,
    onWatermarkRawRectChange: (RectF?) -> Unit,
) {
    CaptureAreaMaskOverlay(captureRect = previewContentRect)

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
        dragVisibleOffsetPx = dragVisibleOffsetPx,
        activeHandleCorner = activeHandleCorner,
        isArmed = isWatermarkArmed,
        isTableLocked = isTableLocked,
        onBoundsRectChange = onWatermarkBoundsRectChange,
        onRawRectChange = onWatermarkRawRectChange
    )

    CaptureResultOverlay(
        capturedUri = capturedUri,
        continuousPreviewMode = continuousPreviewMode,
        aspectRatio = aspectRatio,
        onDismiss = onDismissCaptured
    )
}
