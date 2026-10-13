package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * CameraX -> persistent JPEG staging boundary.
 *
 * Keep this object application-scoped: staging continues when the camera Composable
 * leaves composition. Never release the next-shot gate until onStagedReady fires.
 * A crash leaves the incoming JPEG in filesDir for manual recovery inspection.
 */
internal class CaptureStagedCameraGateway(
    context: Context,
    private val pendingStore: CapturePendingStore,
    private val applicationScope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val incomingDir = File(appContext.filesDir, "capture-pending-v2").also {
        check(it.isDirectory || it.mkdirs()) { "Cannot create capture staging directory" }
    }

    internal data class Callbacks(
        val onCaptureStarted: () -> Unit = {},
        val onImageCaptured: () -> Unit = {},
        val onStagedReady: (CapturePendingStore.PendingCapture) -> Unit,
        val onFailure: (Throwable) -> Unit,
    )

    fun capture(
        imageCapture: ImageCapture,
        reserved: CapturePendingStore.PendingCapture,
        callbacks: Callbacks,
    ) {
        require(reserved.status == CapturePendingStore.Status.RESERVED)
        val source = File(incomingDir, "${reserved.reservation.id}.camera.jpg.part")
        if (!source.createNewFile()) {
            callbacks.onFailure(IOException("Already exists: pending camera image"))
            return
        }

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = false
            isReversedVertical = false
        }
        val options = ImageCapture.OutputFileOptions.Builder(source)
            .setMetadata(metadata)
            .build()
        try {
            imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(appContext),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onCaptureStarted() {
                        runCatching { callbacks.onCaptureStarted() }
                            .onFailure { Log.w(TAG, "Capture-start observer failed", it) }
                    }

                    override fun onImageSaved(
                        outputFileResults: ImageCapture.OutputFileResults,
                    ) {
                        runCatching { callbacks.onImageCaptured() }
                            .onFailure { Log.w(TAG, "JPEG observer failed", it) }
                        applicationScope.launch(Dispatchers.IO) {
                            val staged = runCatching {
                                pendingStore.stageJpeg(reserved.reservation.id, source)
                            }
                            if (staged.isSuccess && !source.delete()) {
                                Log.w(TAG, "Incoming JPEG cleanup deferred")
                            }
                            withContext(Dispatchers.Main) {
                                staged
                                    .onSuccess(callbacks.onStagedReady)
                                    .onFailure(callbacks.onFailure)
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        // Keep a partial image for inspection; do not discard the
                        // reserved journal or permit silent counter reuse.
                        callbacks.onFailure(exception)
                    }
                }
            )
        } catch (error: Exception) {
            callbacks.onFailure(error)
        }
    }

    private companion object {
        const val TAG = "DZlogCaptureStage"
    }
}
