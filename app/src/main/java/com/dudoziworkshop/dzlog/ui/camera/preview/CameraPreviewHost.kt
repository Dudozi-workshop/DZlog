package com.dudoziworkshop.dzlog.ui.camera.preview

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * [CameraPreviewHost]
 * - 목적: PreviewView(카메라 프리뷰) 본체만 렌더링함
 * - 포함: AndroidView(PreviewView)
 * - 제외: CameraX 바인딩/촬영 저장/터치 처리/CameraControl 호출 금지(표시 전용)
 */
@Composable
internal fun CameraPreviewHost(
    previewView: PreviewView,
) {
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { _: Context -> previewView },
        update = { _ -> }
    )
}
