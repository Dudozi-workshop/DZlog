@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import kotlinx.cinterop.readBytes
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import platform.AVFoundation.*
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVCaptureFocusModeContinuousAutoFocus
import platform.AVFoundation.AVCaptureFocusModeAutoFocus
import platform.AVFoundation.AVCaptureExposureModeContinuousAutoExposure
import platform.AVFoundation.AVCaptureExposureModeLocked
import platform.AVFoundation.AVCaptureFlashModeOff
import platform.AVFoundation.AVCaptureFlashModeAuto
import platform.AVFoundation.AVCaptureFlashModeOn
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSSelectorFromString
import platform.CoreGraphics.CGPointMake
import platform.darwin.NSObject

/**
 * Native iPhone-only camera engine, kept out of Android and common contracts.
 * A single in-flight photo delegate is retained until its callback finishes.
 *
 * M3-A PoC: lifecycle, focus, zoom, flash and overlay parity are separate gates.
 */
internal class IosAvFoundationCamera {
    val session = AVCaptureSession()
    val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspect
    }
    private val photoOutput = AVCapturePhotoOutput()
    private var configured = false
    private var device: AVCaptureDevice? = null
    private var flashMode = AVCaptureFlashModeOff
    var maximumZoom: Float = 1f
        private set
    var flashAvailable: Boolean = false
        private set
    var minimumExposureBias: Float = 0f
        private set
    var maximumExposureBias: Float = 0f
        private set
    private var onPhoto: ((Result<ByteArray>) -> Unit)? = null

    private val photoDelegate = object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
        override fun captureOutput(
            output: AVCapturePhotoOutput,
            didFinishProcessingPhoto: AVCapturePhoto,
            error: NSError?,
        ) {
            val result = runCatching {
                check(error == null) { "촬영 실패: ${error?.localizedDescription}" }
                // Kotlin 2.0.21's bundled AVFoundation klib does not expose this
                // zero-argument Objective-C selector as a typed Kotlin member.
                // Call the documented selector and fail if the bridge gives no NSData.
                val data = didFinishProcessingPhoto.performSelector(
                    NSSelectorFromString("fileDataRepresentation")
                )
                val jpeg = checkNotNull(data as? NSData) {
                    "촬영 이미지 데이터를 받지 못했습니다."
                }
                check(jpeg.length > 0uL) { "빈 이미지 데이터입니다." }
                jpeg.bytes!!.readBytes(jpeg.length.toInt())
            }
            val callback = onPhoto
            onPhoto = null
            callback?.invoke(result)
        }
    }

    suspend fun requestCameraPermission(): Boolean = false // Fail closed until native permission bridge is verified.

    /** A simulator can compile this camera but normally has no camera device. */
    fun configure(): Boolean {
        if (configured) return true
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return false
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null) ?: return false
        session.beginConfiguration()
        try {
            session.sessionPreset = AVCaptureSessionPresetPhoto
            check(session.canAddInput(input) && session.canAddOutput(photoOutput)) {
                "카메라 입력 또는 사진 출력을 구성할 수 없습니다."
            }
            session.addInput(input)
            session.addOutput(photoOutput)
            this.device = device
            maximumZoom = minOf(6.0, device.activeFormat.videoMaxZoomFactor).toFloat().coerceAtLeast(1f)
            flashAvailable = device.hasFlash
            minimumExposureBias = device.minExposureTargetBias
            maximumExposureBias = device.maxExposureTargetBias
            configured = true
        } finally {
            session.commitConfiguration()
        }
        // The host is portrait-only; keep the sensor/preview connection in the same orientation.
        photoOutput.connectionWithMediaType(AVMediaTypeVideo)?.videoOrientation = AVCaptureVideoOrientationPortrait
        previewLayer.connection?.videoOrientation = AVCaptureVideoOrientationPortrait
        return configured
    }

    fun start() {
        check(configured) { "촬영 세션이 준비되지 않았습니다." }
        if (!session.running) session.startRunning()
    }

    fun stop() {
        resetFocusExposure()
        if (session.running) session.stopRunning()
        onPhoto = null
    }

    fun setZoom(factor: Float): Boolean = configureDevice { device ->
        device.videoZoomFactor = factor.coerceIn(1f, maximumZoom).toDouble()
    }

    fun autoFocus(): Boolean = configureDevice { device ->
        check(device.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus))
        device.focusMode = AVCaptureFocusModeContinuousAutoFocus
    }

    /** Device coordinates must come from AVCaptureVideoPreviewLayer conversion. */
    fun focusAt(deviceX: Double, deviceY: Double, keepExposureLocked: Boolean): Boolean =
        configureDevice { device ->
            val point = CGPointMake(deviceX.coerceIn(0.0, 1.0), deviceY.coerceIn(0.0, 1.0))
            check(device.focusPointOfInterestSupported)
            check(device.isFocusModeSupported(AVCaptureFocusModeAutoFocus))
            device.focusPointOfInterest = point
            device.focusMode = AVCaptureFocusModeAutoFocus
            if (!keepExposureLocked) {
                if (device.exposurePointOfInterestSupported) device.exposurePointOfInterest = point
                check(device.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure))
                device.exposureMode = AVCaptureExposureModeContinuousAutoExposure
            }
        }

    fun setExposureLocked(locked: Boolean): Boolean = configureDevice { device ->
        val mode = if (locked) AVCaptureExposureModeLocked else AVCaptureExposureModeContinuousAutoExposure
        check(device.isExposureModeSupported(mode))
        device.exposureMode = mode
    }

    fun setExposureBias(value: Float): Boolean = configureDevice { device ->
        val bias = value.coerceIn(device.minExposureTargetBias, device.maxExposureTargetBias)
        device.setExposureTargetBias(bias, completionHandler = null)
    }

    private fun resetFocusExposure() {
        if (!configured) return
        configureDevice { device ->
            if (device.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure)) {
                device.exposureMode = AVCaptureExposureModeContinuousAutoExposure
            }
            device.setExposureTargetBias(0f.coerceIn(device.minExposureTargetBias,
                device.maxExposureTargetBias), completionHandler = null)
            if (device.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) {
                device.focusMode = AVCaptureFocusModeContinuousAutoFocus
            }
        }
    }

    fun setFlash(mode: Int): Boolean {
        if (!configured || !flashAvailable || mode !in 0..2) return false
        flashMode = when (mode) { 1 -> AVCaptureFlashModeAuto; 2 -> AVCaptureFlashModeOn; else -> AVCaptureFlashModeOff }
        return true
    }

    private fun configureDevice(block: (AVCaptureDevice) -> Unit): Boolean = runCatching {
        val active = checkNotNull(device)
        check(active.lockForConfiguration(outError = null))
        try { block(active) } finally { active.unlockForConfiguration() }
    }.isSuccess

    fun captureJpeg(callback: (Result<ByteArray>) -> Unit) {
        check(configured && session.running) { "카메라가 실행 중이지 않습니다." }
        check(onPhoto == null) { "이미 촬영 중입니다." }
        onPhoto = callback
        val settings = AVCapturePhotoSettings.photoSettingsWithFormat(
            format = mapOf(
                platform.AVFoundation.AVVideoCodecKey to platform.AVFoundation.AVVideoCodecTypeJPEG,
            ),
        )
        if (flashAvailable) settings.flashMode = flashMode
        photoOutput.capturePhotoWithSettings(settings, delegate = photoDelegate)
    }
}

