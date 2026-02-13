package com.example.dzlog.feature.capture.io

import com.example.dzlog.data.mediastore.MediaStoreSaverImpl
import com.example.dzlog.data.repository.DzlogRepositoryImpl
import com.example.dzlog.watermark.WatermarkRendererImpl

fun createCaptureRepository(): DzlogRepositoryImpl {
    val saver = MediaStoreSaverImpl()
    return DzlogRepositoryImpl(
        saver = saver,
        watermarkRenderer = WatermarkRendererImpl()
    )
}
