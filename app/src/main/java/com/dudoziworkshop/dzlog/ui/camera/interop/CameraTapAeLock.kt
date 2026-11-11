package com.dudoziworkshop.dzlog.ui.camera.interop

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.core.content.ContextCompat

/**
 * Locks only auto exposure. CameraX focus metering remains responsible for AF,
 * while CameraControl.setExposureCompensationIndex remains usable under AE lock.
 * Never change AF_MODE or lens distance here: the manual focus tool owns those.
 */
@OptIn(ExperimentalCamera2Interop::class)
internal fun requestTapAeLock(
    context: Context,
    camera: Camera,
    locked: Boolean,
    onApplied: (Boolean) -> Unit,
) {
    val supported = runCatching {
        Camera2CameraInfo.from(camera.cameraInfo)
            .getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) == true
    }.getOrDefault(false)
    if (!supported) {
        onApplied(false)
        return
    }

    val control = runCatching { Camera2CameraControl.from(camera.cameraControl) }.getOrNull()
    if (control == null) {
        onApplied(false)
        return
    }
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, locked)
        .build()
    val future = runCatching { control.addCaptureRequestOptions(options) }.getOrNull()
    if (future == null) {
        onApplied(false)
        return
    }
    future.addListener({
        onApplied(runCatching { future.get(); true }.getOrDefault(false))
    }, ContextCompat.getMainExecutor(context))
}
