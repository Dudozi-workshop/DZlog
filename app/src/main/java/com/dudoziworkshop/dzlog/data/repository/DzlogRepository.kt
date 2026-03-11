package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import androidx.camera.core.ImageCapture
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.LogEntry

interface DzlogRepository {
    fun captureAndSave(
        context: Context,
        imageCapture: ImageCapture,
        request: CaptureRequest,
        onDone: (LogEntry) -> Unit,
        onFail: (String) -> Unit
    )

}
