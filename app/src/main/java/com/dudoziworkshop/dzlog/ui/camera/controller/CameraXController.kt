package com.dudoziworkshop.dzlog.ui.camera.controller

import android.content.Context
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import java.util.concurrent.TimeUnit

/**
 * CameraXController
 * - UI에서 직접 다루기 번거로운 CameraX 바인딩/포커스 유틸을 한 파일로 모음
 * - 리팩터링 중 동작 불변을 위해 기존 구현을 기계적으로 이동함
 */
internal fun bindCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    aspect: CaptureAspect,
    photoQualityMode: PhotoQualityMode,
    onBound: (imageCapture: ImageCapture?, camera: Camera?) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val rotation = previewView.display.rotation
        Log.d("DZlog", "BIND requested=${aspect.label} fixed=3:4 quality=${photoQualityMode.name}")

        val previewBuilder = Preview.Builder()
            .setTargetRotation(rotation)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        val preview = previewBuilder.build()
            .apply { surfaceProvider = previewView.surfaceProvider }

        val imageCaptureBuilder = ImageCapture.Builder()
            .setCaptureMode(photoQualityMode.toCaptureMode())
            .setTargetRotation(rotation)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        val imageCapture = imageCaptureBuilder.build()

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            onBound(imageCapture, camera)
        } catch (_: Exception) {
            onBound(null, null)
        }
    }, ContextCompat.getMainExecutor(context))
}

internal fun startTapToFocus(
    context: Context,
    camera: Camera,
    previewView: PreviewView,
    xPx: Float,
    yPx: Float,
    onResult: (Boolean) -> Unit
) {
    val factory = previewView.meteringPointFactory
    val point: MeteringPoint = factory.createPoint(xPx, yPx)

    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
        .setAutoCancelDuration(3, TimeUnit.SECONDS)
        .build()

    val future = camera.cameraControl.startFocusAndMetering(action)
    future.addListener(
        {
            try {
                val result = future.get()
                onResult(result.isFocusSuccessful)
            } catch (_: Exception) {
                onResult(false)
            }
        },
        ContextCompat.getMainExecutor(context)
    )
}

internal fun resolvePreviewContentRect(previewView: PreviewView): RectF? {
    val width = previewView.width
    val height = previewView.height
    if (width <= 0 || height <= 0) return null
    return RectF(0f, 0f, width.toFloat(), height.toFloat())
}
