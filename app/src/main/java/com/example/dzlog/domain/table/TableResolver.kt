package com.example.dzlog.domain.table

import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.HourSystem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Fixed Contract:
 * - 단일 진실의 원천(Single Source of Truth)
 * - 셀 타입 분기 / COUNTER 증가 판단 / DATE·TIME 자동 적용 / 빈값 처리 / 패딩
 * - Plan/Commit 분리: plan 단계에서는 상태 변경 없음, patch는 저장 성공 시에만 커밋
 */
class TableResolver {

    data class Config(
        val counterDigits: Int,
        val dateFormat: String,
        val timeFormat: String,
        val locale: Locale = Locale.getDefault()
    )

    fun plan(
        cells: List<TableCellState>,
        captureNow: Date,
        config: Config
    ): ResolvePlan {
        // 안정적 순서: row/col 기준
        val ordered = cells.sortedWith(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex })

        val counterCell = ordered.firstOrNull { it.dataType == TableCellDataType.COUNTER }
        val currentCounter = parseCounter(counterCell) ?: 1
        val counterResolved = currentCounter.toString().padStart(config.counterDigits.coerceIn(1, 6), '0')
        val nextCounter = currentCounter + 1

        val resolved = ordered.map { cell ->
            resolveCell(
                cell = cell,
                counterResolved = counterResolved,
                captureNow = captureNow,
                config = config
            )
        }

        val patch = if (counterCell != null) TablePatch(mapOf(counterCell.cellId to nextCounter.toString())) else TablePatch.EMPTY

        return ResolvePlan(resolvedCells = resolved, patch = patch)
    }

    private fun resolveCell(
        cell: TableCellState,
        counterResolved: String,
        captureNow: Date,
        config: Config
    ): ResolvedCell {

        return when (cell.dataType) {
            TableCellDataType.TEXT -> {
                val t = cell.rawText.trim()
                ResolvedCell(id = cell.cellId, type = cell.dataType, raw = cell, resolvedText = t, isEmpty = t.isBlank())
            }

            TableCellDataType.NUMBER -> {
                val t = cell.rawText.trim()
                ResolvedCell(id = cell.cellId, type = cell.dataType, raw = cell, resolvedText = t, isEmpty = t.isBlank())
            }

            TableCellDataType.COUNTER -> {
                ResolvedCell(id = cell.cellId, type = cell.dataType, raw = cell, resolvedText = counterResolved, isEmpty = false)
            }

            TableCellDataType.DATE -> {
                // 정책: DATE는 항상 현재(captureNow)를 사용.
                val pattern = cell.formatPattern.ifBlank { config.dateFormat }
                val dateSdf = SimpleDateFormat(pattern, config.locale)
                val resolved = dateSdf.format(captureNow)
                ResolvedCell(id = cell.cellId, type = cell.dataType, raw = cell, resolvedText = resolved, isEmpty = resolved.isBlank())
            }

            TableCellDataType.TIME -> {
                // 정책: TIME은 항상 현재(captureNow)를 사용.
                val resolved = formatTimeFromNow(captureNow = captureNow, options = cell.timeFormatOptions, locale = config.locale)
                ResolvedCell(id = cell.cellId, type = cell.dataType, raw = cell, resolvedText = resolved, isEmpty = resolved.isBlank())
            }
        }
    }

    private fun parseCounter(cell: TableCellState?): Int? {
        if (cell == null) return null
        return when (val tv = cell.typedValue) {
            is CellValue.Counter -> tv.value.takeIf { it >= 0 }
            else -> cell.rawText.trim().toIntOrNull()?.takeIf { it >= 0 }
        }
    }

    private fun formatTimeFromNow(captureNow: Date, options: com.example.dzlog.domain.model.TimeFormatOptions, locale: Locale): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(captureNow.time)
        val secOfDay = ((totalSeconds % 86400) + 86400) % 86400
        val hh24 = (secOfDay / 3600).toInt()
        val mm = ((secOfDay % 3600) / 60).toInt()
        val ss = (secOfDay % 60).toInt()

        val sep = options.separator.ch
        return when (options.hourSystem) {
            HourSystem.H24 -> {
                if (options.includeSeconds) {
                    String.format(locale, "%02d%c%02d%c%02d", hh24, sep, mm, sep, ss)
                } else {
                    String.format(locale, "%02d%c%02d", hh24, sep, mm)
                }
            }

            HourSystem.H12 -> {
                val am = hh24 < 12
                val hh12 = when (val h = hh24 % 12) { 0 -> 12; else -> h }
                val base = if (options.includeSeconds) {
                    String.format(locale, "%02d%c%02d%c%02d", hh12, sep, mm, sep, ss)
                } else {
                    String.format(locale, "%02d%c%02d", hh12, sep, mm)
                }
                // Locale-sensitive AM/PM
                val ampm = SimpleDateFormat("a", locale).format(captureNow)
                "$base $ampm"
            }
        }
    }
}
