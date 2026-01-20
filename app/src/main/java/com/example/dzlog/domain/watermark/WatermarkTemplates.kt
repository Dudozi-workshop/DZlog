package com.example.dzlog.domain.watermark

import com.example.dzlog.domain.model.EmptyValuePolicy
import com.example.dzlog.domain.model.ResolvedCell
import com.example.dzlog.domain.model.WatermarkGridPreset
import com.example.dzlog.domain.model.WatermarkTemplatePreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ===== Cell & Source (domain/watermark 단일 정의) =====
data class CellDef(
    val label: String,
    val source: ValueSource
)

sealed class ValueSource {
    object Treatment : ValueSource()
    object Strain : ValueSource()
    object Folder2 : ValueSource()
    object Counter : ValueSource()
    object Date : ValueSource()
    object Time : ValueSource()
    data class Memo(val index: Int) : ValueSource()
    data class Custom(val text: String) : ValueSource()
}

// ===== Template builder =====
fun templateForPreset(
    preset: WatermarkTemplatePreset,
    grid: WatermarkGridPreset,
    memo1: String,
    memo2: String,
    memo3: String
): List<CellDef> {

    val base: List<CellDef> = when (preset) {
        WatermarkTemplatePreset.BASIC -> listOf(
            CellDef("처리구", ValueSource.Treatment),
            CellDef("계통", ValueSource.Strain),
            CellDef("번호", ValueSource.Counter),
            CellDef("날짜", ValueSource.Date),
            CellDef("시간", ValueSource.Time),
            CellDef("폴더2", ValueSource.Folder2)
        )

        WatermarkTemplatePreset.FIELD -> listOf(
            CellDef("처리구", ValueSource.Treatment),
            CellDef("폴더2", ValueSource.Folder2),
            CellDef("계통", ValueSource.Strain),
            CellDef("번호", ValueSource.Counter),
            CellDef("날짜", ValueSource.Date),
            CellDef("시간", ValueSource.Time)
        )

        WatermarkTemplatePreset.META -> listOf(
            CellDef("계통", ValueSource.Strain),
            CellDef("번호", ValueSource.Counter),
            CellDef("처리구", ValueSource.Treatment),
            CellDef("폴더2", ValueSource.Folder2),
            CellDef("날짜", ValueSource.Date),
            CellDef("시간", ValueSource.Time)
        )
    }

    val memoTexts = listOf(memo1, memo2, memo3)
    val total = grid.rows * grid.cols
    val extraCount = (total - base.size).coerceAtLeast(0)

    val extras: List<CellDef> = (0 until extraCount).map { i ->
        val idx = i.coerceIn(0, 2)
        val text = memoTexts[idx]
        if (text.isBlank()) {
            CellDef("메모${idx + 1}", ValueSource.Memo(idx))
        } else {
            CellDef("메모${idx + 1}", ValueSource.Custom(text))
        }
    }

    return (base + extras).take(total)
}

// ===== Resolver =====
fun resolveCellsFromTemplate(
    template: List<CellDef>,
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
): List<ResolvedCell> {

    fun applyEmpty(raw: String): String {
        val v = raw.trim()
        if (v.isNotEmpty()) return v
        return when (emptyPolicy) {
            EmptyValuePolicy.BLANK -> ""
            EmptyValuePolicy.DASH -> "-"
            EmptyValuePolicy.CUSTOM -> emptyCustomText
        }
    }

    val dateText =
        if (showDate) SimpleDateFormat(datePattern, Locale.getDefault()).format(capturedAt) else ""
    val timeText =
        if (showTime) SimpleDateFormat(timePattern, Locale.getDefault()).format(capturedAt) else ""

    return template.map { def ->
        val raw = when (val src = def.source) {
            ValueSource.Treatment -> treatment
            ValueSource.Strain -> strain
            ValueSource.Folder2 -> folder2Text
            ValueSource.Counter -> counterText
            ValueSource.Date -> dateText
            ValueSource.Time -> timeText
            is ValueSource.Memo -> ""              // 비어있으면 emptyPolicy가 처리
            is ValueSource.Custom -> src.text
        }
        ResolvedCell(def.label, applyEmpty(raw))
    }
}
