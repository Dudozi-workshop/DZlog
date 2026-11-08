package com.dudoziworkshop.dzlog.domain.phrase

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet

/**
 * UTF-8 CSV for one phrase set.
 * New format: order,phrase,every. Empty every = inherit the set default.
 * Older order,phrase CSVs remain importable as entirely inherited entries.
 * Other cell progress, IDs, and number-progress modes are never transferred.
 */
internal object RotatingPhraseCsvCodec {
    const val MAX_ITEMS: Int = 1000
    private const val MAX_TEXT_LENGTH: Int = 500
    private const val MAX_FILE_LENGTH: Int = 1_000_000
    private const val HEADER = "order,phrase,every"

    sealed interface ImportResult {
        data class Success(
            val items: List<String>,
            val itemEveryOverrides: List<Int?> = emptyList(),
        ) : ImportResult
        data class Failure(val message: String) : ImportResult
    }

    fun encode(items: List<String>, itemEveryOverrides: List<Int?> = emptyList()): String = buildString {
        append('\uFEFF')
        append(HEADER).append("\r\n")
        items.forEachIndexed { index, phrase ->
            append(index + 1).append(',')
            append('"').append(phrase.replace("\"", "\"\"")).append('"').append(',')
            itemEveryOverrides.getOrNull(index)?.takeIf { it >= 1 }?.let { append(it) }
            append("\r\n")
        }
    }

    fun decode(csv: String): ImportResult {
        if (csv.length > MAX_FILE_LENGTH) return failure("CSV 파일이 너무 큽니다 (최대 1 MB).")
        val rows = parseRows(csv.removePrefix("\uFEFF"))
            ?: return failure("CSV 형식이 올바르지 않습니다. 따옴표와 열 구성을 확인하세요.")
        if (rows.isEmpty()) return failure("CSV 파일이 비어 있습니다.")
        val header = rows[0].map { it.trim().lowercase() }
        val legacy = header == listOf("order", "phrase")
        val extended = header == listOf("order", "phrase", "every")
        if (!legacy && !extended) {
            return failure("첫 줄은 order,phrase,every 또는 기존 order,phrase 형식이어야 합니다.")
        }
        val data = rows.drop(1)
        if (data.isEmpty()) return failure("가져올 문구가 없습니다. 기존 목록은 유지됩니다.")
        if (data.size > MAX_ITEMS) return failure("문구는 최대 1000개까지 가져올 수 있습니다.")

        data class Row(val order: Int, val phrase: String, val every: Int?)
        val numbered = ArrayList<Row>(data.size)
        for ((index, row) in data.withIndex()) {
            if (row.size != header.size) {
                return failure((index + 2).toString() + "행의 열 개수가 올바르지 않습니다.")
            }
            val order = row[0].trim().toIntOrNull()
                ?: return failure((index + 2).toString() + "행의 순서는 숫자여야 합니다.")
            val phrase = row[1].trim()
            if (order !in 1..MAX_ITEMS) return failure((index + 2).toString() + "행의 순서는 1 이상이어야 합니다.")
            if (phrase.isEmpty()) return failure((index + 2).toString() + "행의 문구가 비어 있습니다.")
            if (phrase.length > MAX_TEXT_LENGTH) return failure((index + 2).toString() + "행의 문구가 너무 깁니다.")
            if ('\n' in phrase || '\r' in phrase) {
                return failure((index + 2).toString() + "행 문구에는 줄바꿈을 넣을 수 없습니다.")
            }
            val rawEvery = if (extended) row[2].trim() else ""
            val every: Int? = if (rawEvery.isEmpty()) {
                null
            } else {
                rawEvery.toIntOrNull()?.takeIf { it in 1..RotatingPhraseSet.MAX_EVERY }
                    ?: return failure((index + 2).toString() + "행의 변경 주기는 1 이상의 숫자이거나 빈칸이어야 합니다.")
            }
            numbered += Row(order, phrase, every)
        }
        val sorted = numbered.sortedBy { it.order }
        if (sorted.indices.any { sorted[it].order != it + 1 }) {
            return failure("순서(order)는 중복 없이 1부터 연속된 숫자여야 합니다.")
        }
        return ImportResult.Success(sorted.map { it.phrase }, sorted.map { it.every })
    }

    private fun failure(message: String): ImportResult.Failure = ImportResult.Failure(message)

    /** Reject malformed quotes and accept CRLF/LF, commas and escaped quotes. */
    private fun parseRows(csv: String): List<List<String>>? {
        val rows = ArrayList<List<String>>()
        val columns = ArrayList<String>()
        val field = StringBuilder()
        var quoted = false
        var closedQuote = false
        var started = false

        fun endField() {
            columns += field.toString()
            field.setLength(0)
            started = false
            closedQuote = false
        }

        fun endRow() {
            endField()
            if (columns.size != 1 || columns[0].isNotBlank()) rows += columns.toList()
            columns.clear()
        }

        var i = 0
        while (i < csv.length) {
            val c = csv[i]
            when {
                c == '"' && quoted && i + 1 < csv.length && csv[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                c == '"' && quoted -> {
                    quoted = false
                    closedQuote = true
                }
                c == '"' && !started && !closedQuote -> {
                    quoted = true
                    started = true
                }
                c == '"' -> return null
                quoted -> field.append(c)
                c == ',' -> endField()
                c == '\r' || c == '\n' -> {
                    endRow()
                    if (c == '\r' && i + 1 < csv.length && csv[i + 1] == '\n') i++
                }
                closedQuote -> return null
                else -> {
                    field.append(c)
                    started = true
                }
            }
            if (rows.size > MAX_ITEMS + 1) return null
            i++
        }
        if (quoted) return null
        if (started || closedQuote || field.isNotEmpty() || columns.isNotEmpty()) endRow()
        return rows
    }
}
