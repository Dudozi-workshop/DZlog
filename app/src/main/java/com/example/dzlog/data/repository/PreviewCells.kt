package com.example.dzlog.data.repository

import com.example.dzlog.domain.watermark.CellDef
import com.example.dzlog.domain.watermark.resolveCellsFromTemplate

import com.example.dzlog.domain.model.CaptureRequest
import java.util.Date
import com.example.dzlog.domain.model.ResolvedCell
import com.example.dzlog.domain.model.EmptyValuePolicy





fun buildResolvedCellsForPreview(
    request: CaptureRequest,
    template: List<CellDef>
): List<ResolvedCell> {
    val counterText = String.format("%0${request.counterDigits}d", request.counter)

    return resolveCellsFromTemplate(
        template = template,
        treatment = request.watermark.treatment,
        strain = request.watermark.strain,
        folder2Text = request.watermark.folder2Text,
        counterText = counterText,
        capturedAt = Date(),
        showDate = request.watermark.showDate,
        showTime = request.watermark.showTime,
        datePattern = request.watermark.datePattern,
        timePattern = request.watermark.timePattern,
        emptyPolicy = request.watermark.emptyPolicy,
        emptyCustomText = request.watermark.emptyCustomText
    )
}