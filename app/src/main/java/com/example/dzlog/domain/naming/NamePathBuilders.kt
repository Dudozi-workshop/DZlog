package com.example.dzlog.domain.naming

import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellKind
import com.example.dzlog.domain.model.TableCellState
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

fun sanitizeFilePart(input: String): String = sanitizeFolderName(input)

fun resolveGroupValue(cells: List<TableCellState>, level: GroupLevel): String {
    return cells
        .asSequence()
        .filter { it.groupLevel == level }
        .sortedWith(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex })
        .map { sanitizeFolderName(it.valueText) }
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
    return if (g1.isNotBlank() && g2.isNotBlank()) {
        "Pictures/DZlog/$g1/$g2/"
    } else {
        "Pictures/DZlog/"
    }
}

fun buildDisplayName(
    cells: List<TableCellState>,
    counter: Int,
    counterDigits: Int,
    fnDelim: String,
    includeDate: Boolean,
    includeTime: Boolean,
    now: Date = Date()
): String {
    val delim = fnDelim.ifBlank { "_" }.take(3)
    val counterText = counter.toString().padStart(counterDigits, '0')
    val parts = cells
        .asSequence()
        .filter { it.kind == TableCellKind.INPUT && it.fileNameInclude }
        .sortedWith(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex })
        .map { sanitizeFilePart(it.valueText) }
        .filter { it.isNotBlank() }
        .toMutableList()

    parts.add(counterText)

    if (includeDate || includeTime) {
        if (includeDate) parts.add(SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now))
        if (includeTime) parts.add(SimpleDateFormat("HHmmss", Locale.getDefault()).format(now))
    }

    val base = parts.joinToString(delim).ifBlank { counterText }
    return if (base.endsWith(".jpg", true) || base.endsWith(".jpeg", true)) {
        base
    } else {
        "$base.jpg"
    }
}