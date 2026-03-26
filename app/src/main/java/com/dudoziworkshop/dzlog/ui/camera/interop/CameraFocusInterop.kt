package com.dudoziworkshop.dzlog.ui.camera.interop

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import com.dudoziworkshop.dzlog.ui.camera.state.CameraFocusMode

@OptIn(ExperimentalCamera2Interop::class)
internal fun applyFocusModeToBoundCamera(
    camera: Camera,
    mode: CameraFocusMode,
    focusUiValue: Float,
) {
    val camera2Control = runCatching { Camera2CameraControl.from(camera.cameraControl) }.getOrNull() ?: return

    if (mode == CameraFocusMode.AUTO) {
        runCatching { camera2Control.clearCaptureRequestOptions() }
        runCatching { camera.cameraControl.cancelFocusAndMetering() }
        return
    }

    val minimumFocusDistance = resolveMinimumFocusDistance(camera) ?: return
    if (minimumFocusDistance <= 0f) return

    val mappedDistance = mapUiFocusValueToLensDistance(
        focusUiValue = focusUiValue,
        minimumFocusDistance = minimumFocusDistance,
    )
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, mappedDistance)
        .build()
    runCatching { camera2Control.setCaptureRequestOptions(options) }
}

@OptIn(ExperimentalCamera2Interop::class)
private fun resolveMinimumFocusDistance(camera: Camera): Float? {
    val camera2Info = runCatching { Camera2CameraInfo.from(camera.cameraInfo) }.getOrNull() ?: return null
    return runCatching {
        camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
    }.getOrNull()
}

internal fun mapUiFocusValueToLensDistance(
    focusUiValue: Float,
    minimumFocusDistance: Float,
): Float {
    val normalizedUiValue = focusUiValue.coerceIn(0f, 1f)
    return (1f - normalizedUiValue) * minimumFocusDistance
}
