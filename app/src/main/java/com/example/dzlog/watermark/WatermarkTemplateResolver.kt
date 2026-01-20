package com.example.dzlog.watermark

import com.example.dzlog.domain.model.EmptyValuePolicy
import com.example.dzlog.domain.model.ResolvedCell
import com.example.dzlog.domain.model.WatermarkGridPreset
import com.example.dzlog.domain.model.WatermarkTemplatePreset
import java.util.Date

interface WatermarkTemplateResolver {
    fun resolve(
        preset: WatermarkTemplatePreset,
        grid: WatermarkGridPreset,
        memo1: String,
        memo2: String,
        memo3: String,
        treatment: String,
        strain: String,
        folder2Text: String,
        counterText: String,
        capturedAt: Date,
        showDate: Boolean,
        showTime: Boolean,
        datePattern: String,
        timePattern: String,
        emptyPolicy: EmptyValuePolicy,
        emptyCustomText: String
    ): List<ResolvedCell>
}
