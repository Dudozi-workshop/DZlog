package com.example.dzlog.data.repository

import android.content.Context
import androidx.camera.core.ImageCapture
import com.example.dzlog.domain.model.CaptureRequest
import com.example.dzlog.domain.model.LogEntry

interface DzlogRepository {
    fun captureAndSave(
        context: Context,
        imageCapture: ImageCapture,
        request: CaptureRequest,
        onDone: (LogEntry) -> Unit,
        onFail: (String) -> Unit
    )

    fun buildRelativePath(group1: String, group2: String): String

    fun buildOriginalRelativePath(group1: String, group2: String): String
}
