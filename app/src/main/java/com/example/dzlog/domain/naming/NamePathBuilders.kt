package com.example.dzlog.domain.naming

import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellState
import kotlin.jvm.JvmName
import com.example.dzlog.domain.table.ResolvedCell
import com.example.dzlog.domain.model.TableCellDataType
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
    now: Date = Date()
): String {
    val delim = fnDelim.ifBlank { "_" }.take(3)
    val ordered = resolvedCells
        .sortedWith(compareBy<ResolvedCell> { it.raw?.rowIndex ?: 0 }.thenBy { it.raw?.colIndex ?: 0 })

    val parts = ordered
        .asSequence()
        .filter { rc ->
            val raw = rc.raw
            raw != null && raw.fileNameInclude
        }
        .map { sanitizeFilePart(it.resolvedText) }
        .filter { it.isNotBlank() }
        .toMutableList()

    // COUNTER는 resolver가 padding까지 완료한 값을 제공한다.
    val counterText = ordered.firstOrNull { it.type == TableCellDataType.COUNTER }?.resolvedText
        ?.takeIf { it.isNotBlank() }
        .orEmpty()
    if (counterText.isNotBlank()) parts.add(counterText)

    if (includeDate || includeTime) {
        if (includeDate) parts.add(SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now))
        if (includeTime) parts.add(SimpleDateFormat("HHmmss", Locale.getDefault()).format(now))
    }

    val base = parts.joinToString(delim).ifBlank { counterText.ifBlank { "0001" } }
    val withExt = if (base.endsWith(".jpg", true) || base.endsWith(".jpeg", true)) base else "$base.jpg"
    return sanitizeFilePart(withExt)
}