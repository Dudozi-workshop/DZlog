package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.counter.resolveRotatingCounterStreamIdentity
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val illegal = Regex("[\\\\/:*?\"<>|]")
    val cleaned = trimmed.replace(illegal, "_")
    return cleaned.trim().trim('.')
}

private fun sanitizeGroupPathSegment(input: String): String =
    sanitizeFilePart(input).replace('/', '_').replace('\\', '_').trim()

fun resolveGroupValue(resolvedCells: List<ResolvedCell>, level: GroupLevel): String {
    return resolvedCells
        .asSequence()
        .filter { (it.raw?.groupLevel ?: GroupLevel.NONE) == level }
        .sortedWith(compareBy({ it.raw?.rowIndex ?: 0 }, { it.raw?.colIndex ?: 0 }))
        .map { sanitizeGroupPathSegment(it.resolvedText) }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

fun buildGalleryRelativePath(group1: String, group2: String): String {
    val g1 = sanitizeGroupPathSegment(group1)
    val g2 = sanitizeGroupPathSegment(group2)
    return when {
        g1.isBlank() -> "Pictures/DZlog/"
        g2.isBlank() -> "Pictures/DZlog/$g1/"
        else -> "Pictures/DZlog/$g1/$g2/"
    }
}

private fun formatNow(pattern: String, now: Date): String {
    return runCatching { SimpleDateFormat(pattern, Locale.getDefault()).format(now) }
        .getOrElse { "" }
}

private fun resolveRotatingTextToken(resolvedCells: List<ResolvedCell>): String {
    return resolvedCells
        .asSequence()
        .filter { it.type == TableCellDataType.ROTATING_TEXT }
        .sortedWith(compareBy({ it.raw?.rowIndex ?: 0 }, { it.raw?.colIndex ?: 0 }))
        .map { it.resolvedText.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

private fun normalizeSlotDrafts(
    drafts: List<TableEditorSlotDraft?>,
    slotCount: Int,
): List<TableEditorSlotDraft?> {
    return drafts.take(slotCount) + List((slotCount - drafts.size).coerceAtLeast(0)) { null }
}

private fun resolveFileNameSlotToken(
    draft: TableEditorSlotDraft?,
    resolvedById: Map<String, ResolvedCell>,
    resolvedCells: List<ResolvedCell>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String {
    val value = when (draft?.kind?.uppercase(Locale.ROOT)) {
        "CELL" -> {
            val rc = draft.cellId?.let { resolvedById[it] }
            when (rc?.type) {
                TableCellDataType.COUNTER -> ""
                TableCellDataType.TIME -> normalizeTimeWithoutSeconds(rc.resolvedText)
                else -> rc?.resolvedText.orEmpty()
            }
        }
        "MANUAL" -> draft.manualText.orEmpty()
        "FORMAT" -> when (draft.formatType?.uppercase(Locale.ROOT)) {
            "DATE" -> formatNow(dateFormat, now)
            "TIME" -> normalizeTimeWithoutSeconds(formatNow(timeFormat, now))
            "ROTATING_TEXT" -> resolveRotatingTextToken(resolvedCells)
            // 정책: COUNTER는 파일명 suffix 자동 정책만 사용. slot token으로 추가하지 않는다.
            "COUNTER" -> ""
            else -> ""
        }
        else -> ""
    }
    return sanitizeFilePart(value)
}

private fun resolvePathSlotToken(
    draft: TableEditorSlotDraft?,
    resolvedById: Map<String, ResolvedCell>,
    resolvedCells: List<ResolvedCell>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String {
    val value = when (draft?.kind?.uppercase(Locale.ROOT)) {
        "CELL" -> {
            val rc = draft.cellId?.let { resolvedById[it] }
            if (rc?.type == TableCellDataType.TIME) normalizeTimeWithoutSeconds(rc.resolvedText) else rc?.resolvedText.orEmpty()
        }
        "MANUAL" -> draft.manualText.orEmpty()
        "FORMAT" -> when (draft.formatType?.uppercase(Locale.ROOT)) {
            "DATE" -> formatNow(dateFormat, now)
            "TIME" -> normalizeTimeWithoutSeconds(formatNow(timeFormat, now))
            "ROTATING_TEXT" -> resolveRotatingTextToken(resolvedCells)
            else -> ""
        }
        else -> ""
    }
    return sanitizeGroupPathSegment(value)
}



private fun normalizeTimeWithoutSeconds(text: String): String {
    val t = text.trim()
    if (t.isBlank()) return ""

    // 주요 정책: naming 경로의 시간 토큰은 항상 HHmm(분 단위)로 정규화한다.
    val colonMatch = Regex("""^(\d{1,2}):(\d{2})""").find(t)
    if (colonMatch != null) {
        val hour = colonMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return ""
        val minute = colonMatch.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return ""
        return "%02d%02d".format(hour, minute)
    }

    val digitMatch = Regex("""^(\d{2})(\d{2})""").find(t)
    if (digitMatch != null) {
        val hour = digitMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return ""
        val minute = digitMatch.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return ""
        return "%02d%02d".format(hour, minute)
    }

    return ""
}

fun resolveFileNameScopeTokensFromDrafts(
    fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    resolvedCells: List<ResolvedCell>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): List<String> {
    val resolvedById = resolvedCells.associateBy { it.id }
    val normalized = normalizeSlotDrafts(fileNameSlotDrafts, FILE_NAME_SLOT_COUNT)
    return normalized.mapNotNull { draft ->
        when (draft?.kind?.uppercase(Locale.ROOT)) {
            "CELL" -> {
                val rc = draft.cellId?.let { resolvedById[it] } ?: return@mapNotNull null
                when (rc.type) {
                    TableCellDataType.COUNTER -> null
                    TableCellDataType.DATE -> rc.raw?.counterScopeMode?.takeIf { it == CounterScopeMode.INCLUDE }?.let { sanitizeFilePart(rc.resolvedText) }
                    TableCellDataType.TIME -> rc.raw?.counterScopeMode?.takeIf { it == CounterScopeMode.INCLUDE }?.let { sanitizeFilePart(normalizeTimeWithoutSeconds(rc.resolvedText)) }
                    TableCellDataType.ROTATING_TEXT -> {
                        sanitizeFilePart(
                            resolveRotatingCounterStreamIdentity(
                                activePhraseText = rc.resolvedText,
                            )
                        )
                    }
                    else -> sanitizeFilePart(rc.resolvedText)
                }
            }
            "MANUAL" -> sanitizeFilePart(draft.manualText.orEmpty())
            "FORMAT" -> when (draft.formatType?.uppercase(Locale.ROOT)) {
                "DATE" -> sanitizeFilePart(formatNow(dateFormat, now))
                "TIME" -> sanitizeFilePart(normalizeTimeWithoutSeconds(formatNow(timeFormat, now)))
                "ROTATING_TEXT" -> {
                    val rotating = resolvedCells.firstOrNull { it.type == TableCellDataType.ROTATING_TEXT }
                    sanitizeFilePart(
                        resolveRotatingCounterStreamIdentity(
                            activePhraseText = rotating?.resolvedText,
                        )
                    )
                }
                else -> null
            }
            else -> null
        }?.takeIf { it.isNotBlank() }
    }
}

fun resolveFileNameDraftToken(
    draft: TableEditorSlotDraft?,
    resolvedCells: List<ResolvedCell>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String {
    val resolvedById = resolvedCells.associateBy { it.id }
    return resolveFileNameSlotToken(
        draft = draft,
        resolvedById = resolvedById,
        resolvedCells = resolvedCells,
        now = now,
        dateFormat = dateFormat,
        timeFormat = timeFormat,
    )
}

fun resolvePathDraftToken(
    draft: TableEditorSlotDraft?,
    resolvedCells: List<ResolvedCell>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String {
    val resolvedById = resolvedCells.associateBy { it.id }
    return resolvePathSlotToken(
        draft = draft,
        resolvedById = resolvedById,
        resolvedCells = resolvedCells,
        now = now,
        dateFormat = dateFormat,
        timeFormat = timeFormat,
    )
}

fun buildCounterScanPrefix(
    resolvedCells: List<ResolvedCell>,
    fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String {
    // 회전문구를 포함한 파일명 prefix 규칙과 counter scan prefix 규칙을 동일하게 유지한다.
    return buildFileNamePrefix(
        resolvedCells = resolvedCells,
        fileNameSlotDrafts = fileNameSlotDrafts,
        fnDelim = fnDelim,
        now = now,
        dateFormat = dateFormat,
        timeFormat = timeFormat,
    )
}

fun buildFileNamePrefix(
    resolvedCells: List<ResolvedCell>,
    fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String {
    val resolvedById = resolvedCells.associateBy { it.id }
    val delim = fnDelim.ifBlank { "_" }
    val prefixParts = normalizeSlotDrafts(fileNameSlotDrafts, FILE_NAME_SLOT_COUNT)
        .map { resolveFileNameSlotToken(it, resolvedById, resolvedCells, now, dateFormat, timeFormat) }
        .filter { it.isNotBlank() }

    return if (prefixParts.isEmpty()) "DZlog" else prefixParts.joinToString(delim)
}

fun buildFileName(
    resolvedCells: List<ResolvedCell>,
    fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String,
    counterDigits: Int,
    usedCounter: Int?,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String {
    val prefix = buildFileNamePrefix(
        resolvedCells = resolvedCells,
        fileNameSlotDrafts = fileNameSlotDrafts,
        fnDelim = fnDelim,
        now = now,
        dateFormat = dateFormat,
        timeFormat = timeFormat,
    )

    // suffix 카운터 정책 유지: 항상 자동 suffix 하나만 부여.
    val resolvedCounterText = resolvedCells
        .firstOrNull { it.type == TableCellDataType.COUNTER }
        ?.resolvedText
        ?.takeIf { it.isNotBlank() }
        .orEmpty()
    val resolvedWidth = resolvedCounterText.takeIf { it.all(Char::isDigit) }?.length ?: 0
    val width = maxOf(resolvedWidth, counterDigits.coerceAtLeast(0))
    val counterSuffix = usedCounter?.let { counter ->
        if (width > 0) counter.toString().padStart(width, '0') else counter.toString()
    }

    val base = if (counterSuffix != null) "${prefix}_$counterSuffix" else prefix
    val withExt = if (base.endsWith(".jpg", true) || base.endsWith(".jpeg", true)) base else "$base.jpg"
    return sanitizeFilePart(withExt)
}

fun buildSavePath(
    resolvedCells: List<ResolvedCell>,
    pathSlotDrafts: List<TableEditorSlotDraft?>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String {
    val resolvedById = resolvedCells.associateBy { it.id }
    val segments = pathSlotDrafts
        .map { resolvePathSlotToken(it, resolvedById, resolvedCells, now, dateFormat, timeFormat) }
        .filter { it.isNotBlank() }

    return if (segments.isEmpty()) {
        "Pictures/DZlog/"
    } else {
        "Pictures/DZlog/${segments.joinToString("/")}/"
    }
}

/**
 * 카운터 스트림 기준 경로 정책.
 * - WATERMARK_ONLY/BOTH: 워터마크 저장 기준(base 경로) 카운터 스트림 공유
 * - ORIGINAL_ONLY: original 하위 폴더를 카운터 스트림 기준 경로로 사용
 */
fun buildCounterPath(baseRelativePath: String, saveMode: SaveMode): String {
    val normalizedBase = baseRelativePath.trim().let { path ->
        if (path.isBlank()) "Pictures/DZlog/" else if (path.endsWith('/')) path else "$path/"
    }
    return when (saveMode) {
        SaveMode.ORIGINAL_ONLY -> if (normalizedBase.endsWith("original/")) normalizedBase else "${normalizedBase}original/"
        SaveMode.WATERMARK_ONLY,
        SaveMode.BOTH -> normalizedBase
    }
}


// legacy 호환 래퍼: 신규 코드는 buildFileName 사용
fun buildDisplayNameFromSlotDrafts(
    resolvedCells: List<ResolvedCell>,
    fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String,
    counterDigits: Int,
    usedCounter: Int?,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String = buildFileName(
    resolvedCells = resolvedCells,
    fileNameSlotDrafts = fileNameSlotDrafts,
    fnDelim = fnDelim,
    counterDigits = counterDigits,
    usedCounter = usedCounter,
    now = now,
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)

// legacy 호환 래퍼: 신규 코드는 buildSavePath 사용
fun buildGalleryRelativePathFromSlotDrafts(
    resolvedCells: List<ResolvedCell>,
    pathSlotDrafts: List<TableEditorSlotDraft?>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String = buildSavePath(
    resolvedCells = resolvedCells,
    pathSlotDrafts = pathSlotDrafts,
    now = now,
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)
