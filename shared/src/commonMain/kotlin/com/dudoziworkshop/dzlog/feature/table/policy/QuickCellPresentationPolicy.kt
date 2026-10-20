package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.deriveFileNameCellSlotsFromDrafts
import com.dudoziworkshop.dzlog.domain.model.derivePathCellSlotsFromDrafts
import com.dudoziworkshop.dzlog.domain.naming.NamingCellPreview

data class QuickCellLinks(val fileName: Boolean, val path: Boolean) {
    val count: Int get() = (if (fileName) 1 else 0) + (if (path) 1 else 0)
    val description: String get() = when {
        fileName && path -> "파일명과 저장경로에 사용되는 셀이에요."
        fileName -> "파일명에 사용되는 셀이에요."
        path -> "저장경로에 사용되는 셀이에요."
        else -> ""
    }
}

fun quickCellLinks(template: TableTemplateState, cellId: String): QuickCellLinks {
    return QuickCellLinks(
        cellId in deriveFileNameCellSlotsFromDrafts(template.fileNameSlotDrafts),
        cellId in derivePathCellSlotsFromDrafts(template.pathSlotDrafts),
    )
}

/** Keep the first selected contribution visible within a bounded line viewport. Offsets are
 * measured line starts from the complete annotated text, never substring matching. */
fun quickNamingWindow(text: String, highlights: List<IntRange>, lineStarts: List<Int>, visibleLines: Int = 2): NamingCellPreview {
    val limit = visibleLines.coerceAtLeast(1)
    if (lineStarts.size <= limit) return NamingCellPreview(text, highlights)
    val focus = highlights.firstOrNull { it.first in text.indices }?.first ?: 0
    val focusLine = lineStarts.indexOfLast { it <= focus }.coerceAtLeast(0)
    val startLine = if (focusLine < limit) 0 else focusLine.coerceAtMost(lineStarts.size - limit)
    val start = lineStarts[startLine]
    val end = lineStarts.getOrNull(startLine + limit) ?: text.length
    val prefix = if (start > 0) "…" else ""
    val value = text.substring(start, end).trimEnd('\n')
    val visibleEnd = start + value.length
    val clipped = highlights.mapNotNull { range ->
        val left = maxOf(range.first, start)
        val right = minOf(range.last + 1, visibleEnd)
        if (left < right) (left - start + prefix.length) until (right - start + prefix.length) else null
    }
    return NamingCellPreview(prefix + value + if (visibleEnd < text.length) "…" else "", clipped)
}

fun quickMarkersFit(width: Float, valueTop: Float, locked: Boolean, links: Int,
    markerSize: Float, gap: Float): Boolean {
    val count = links + if (locked) 1 else 0
    if (count == 0) return false
    val requiredWidth = markerSize * count + gap * (count - 1) + gap * 2
    return width >= requiredWidth && valueTop >= markerSize + gap * 2
}
