package com.example.dzlog.domain.naming

import com.example.dzlog.domain.model.CellKey
import com.example.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.table.ResolvedCell
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun sanitizeFolderName(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""
    val illegal = Regex("[\\\\/:*?\"<>|]")
    val cleaned = trimmed.replace(illegal, "_")
    return cleaned.trim().trim('.')
}


/**
 * File-name sanitizer for generic (non date/time) parts.
 *
 * Policy:
 * - Replace illegal characters with '_'.
 * - Trim leading/trailing dots.
 */

fun sanitizeFilePart(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""

    // Replace illegal characters for file names across major platforms.
    val illegal = Regex("[\\\\/:*?\"<>|]")
    val cleaned = trimmed.replace(illegal, "_")

    return cleaned.trim().trim('.')
}


private fun digitsOnly(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""
    return trimmed.filter { it.isDigit() }
}

@JvmName("resolveGroupValueFromStates")
fun resolveGroupValue(cells: List<TableCellState>, level: GroupLevel): String {
    return cells
        .asSequence()
        .filter { it.groupLevel == level }
        .sortedWith(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex })
        .map { sanitizeFolderName(it.rawText) }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

fun resolveGroupValue(resolvedCells: List<ResolvedCell>, level: GroupLevel): String {
    return resolvedCells
        .asSequence()
        .filter { (it.raw?.groupLevel ?: GroupLevel.NONE) == level }
        .sortedWith(compareBy<ResolvedCell> { it.raw?.rowIndex ?: 0 }.thenBy { it.raw?.colIndex ?: 0 })
        .map { sanitizeFolderName(it.resolvedText) }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

fun buildGalleryRelativePath(cells: List<TableCellState>): String {
    val g1 = resolveGroupValue(cells, GroupLevel.G1)
    val g2 = resolveGroupValue(cells, GroupLevel.G2)
    return buildGalleryRelativePath(g1, g2)
}

fun buildGalleryRelativePath(group1: String, group2: String): String {
    val g1 = sanitizeFolderName(group1)
    val g2 = sanitizeFolderName(group2)
    return when {
        g1.isBlank() -> "Pictures/DZlog/"
        g2.isBlank() -> "Pictures/DZlog/$g1/"
        else -> "Pictures/DZlog/$g1/$g2/"
    }
}

fun buildDisplayNameFromResolvedCells(
    resolvedCells: List<ResolvedCell>,
    fnDelim: String,
    includeDate: Boolean,
    includeTime: Boolean,
    fileNameSlots: List<CellKey?>? = null,
    counterDigits: Int = 0,
    counterOverride: Int? = null,
    now: Date = Date()
): String {
    val ordered = resolvedCells
        .sortedWith(compareBy<ResolvedCell> { it.raw?.rowIndex ?: 0 }.thenBy { it.raw?.colIndex ?: 0 })

    val prefix = if (fileNameSlots != null) {
        buildFileNamePrefixFromSlots(
            resolvedCells = resolvedCells,
            slots = fileNameSlots,
            fnDelim = fnDelim,
            includeDate = includeDate,
            includeTime = includeTime,
            now = now
        )
    } else {
        buildFileNamePrefixFromResolvedCells(
            resolvedCells = resolvedCells,
            fnDelim = fnDelim,
            includeDate = includeDate,
            includeTime = includeTime,
            now = now
        )
    }

    // COUNTER는 resolver가 padding까지 완료한 값을 제공한다.
    val counterText = ordered.firstOrNull { it.type == TableCellDataType.COUNTER }?.resolvedText
        ?.takeIf { it.isNotBlank() }
        .orEmpty()

    // MVP 정책: 파일명 suffix 카운터는 "단일 소스"(counterOverride) 우선.
    // - COUNTER 셀 표기 ON/OFF, seed 변경 등 UI 상태에 의해 파일명 카운터가 흔들리지 않도록 한다.
    // - counterOverride가 있으면 항상 그것을 사용하고, padding은 기존 counterText 길이를 따르도록 시도한다.
    val counterFinal = counterOverride
        ?.takeIf { it >= 0 }
        ?.let { ov ->
            val resolvedWidth = counterText.takeIf { it.all { ch -> ch.isDigit() } }?.length ?: 0
            val width = maxOf(resolvedWidth, counterDigits.coerceAtLeast(0))
            if (width > 0) ov.toString().padStart(width, '0') else ov.toString()
        }
        ?: counterText.ifBlank { "1" }

    val base = "${prefix}_${counterFinal}"
    val withExt = if (base.endsWith(".jpg", true) || base.endsWith(".jpeg", true)) base else "$base.jpg"
    return sanitizeFilePart(withExt)
}

fun buildFileNamePrefixFromSlots(
    resolvedCells: List<ResolvedCell>,
    slots: List<CellKey?>,
    fnDelim: String,
    includeDate: Boolean,
    includeTime: Boolean,
    now: Date = Date()
): String {
    val delim = fnDelim.ifBlank { "_" }
    val normalizedSlots = slots.take(FILE_NAME_SLOT_COUNT) +
        List((FILE_NAME_SLOT_COUNT - slots.size).coerceAtLeast(0)) { null }

    val parts = normalizedSlots
        .asSequence()
        .mapNotNull { slot ->
            slot?.let { key -> resolvedCells.firstOrNull { rc -> rc.id == key } }
        }
        .mapNotNull { rc ->
            when (rc.type) {
                TableCellDataType.COUNTER -> null
                TableCellDataType.DATE,
                TableCellDataType.TIME -> digitsOnly(rc.resolvedText)
                else -> sanitizeFilePart(rc.resolvedText)
            }.takeIf { !it.isNullOrBlank() }
        }
        .toMutableList()

    if (includeDate || includeTime) {
        if (includeDate) parts.add(SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now))
        if (includeTime) parts.add(SimpleDateFormat("HHmmss", Locale.getDefault()).format(now))
    }

    if (parts.isEmpty()) parts.add("DZlog")

    return parts.joinToString(delim)
}

fun buildFileNamePrefixFromResolvedCells(
    resolvedCells: List<ResolvedCell>,
    fnDelim: String,
    includeDate: Boolean,
    includeTime: Boolean,
    now: Date = Date()
): String {
    val delim = fnDelim.ifBlank { "_" }
    val ordered = resolvedCells
        .sortedWith(compareBy<ResolvedCell> { it.raw?.rowIndex ?: 0 }.thenBy { it.raw?.colIndex ?: 0 })

    val parts = ordered
        .asSequence()
        .filter { rc ->
            val raw = rc.raw
            // COUNTER는 항상 suffix로만 붙인다(중복 방지)
            raw != null && raw.fileNameInclude && raw.dataType != TableCellDataType.COUNTER
        }
        .map { rc ->
            val raw = rc.raw
            if (raw != null && (raw.dataType == TableCellDataType.DATE || raw.dataType == TableCellDataType.TIME)) {
                digitsOnly(rc.resolvedText)
            } else {
                sanitizeFilePart(rc.resolvedText)
            }
        }
        .filter { it.isNotBlank() }
        .toMutableList()

    if (includeDate || includeTime) {
        if (includeDate) parts.add(SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now))
        if (includeTime) parts.add(SimpleDateFormat("HHmmss", Locale.getDefault()).format(now))
    }

    // prefix(카운터 제외)가 공백이면 "DZlog"를 붙인다.
    if (parts.isEmpty()) parts.add("DZlog")

    return parts.joinToString(delim)
}
