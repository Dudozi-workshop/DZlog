package com.example.dzlog.domain.table

import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val currentCounter = parseCounter(counterCell?.valueText) ?: 1
        val counterResolved = currentCounter.toString().padStart(config.counterDigits.coerceIn(1, 6), '0')
        val nextCounter = currentCounter + 1

        val defaultDateText = SimpleDateFormat(config.dateFormat, config.locale).format(captureNow)
        val defaultTimeText = SimpleDateFormat(config.timeFormat, config.locale).format(captureNow)


        val resolved = ordered.map { cell ->
            resolveCell(
                cell = cell,
                counterResolved = counterResolved,
                captureNow = captureNow,
                config = config,
                defaultDateText = defaultDateText,
                defaultTimeText = defaultTimeText
            )
        }

        val patch = if (counterCell != null) {
            TablePatch(mapOf(counterCell.cellId to nextCounter.toString()))
        } else {
            TablePatch.EMPTY
        }

        return ResolvePlan(resolvedCells = resolved, patch = patch)
    }

    private fun resolveCell(
        cell: TableCellState,
        counterResolved: String,
        captureNow: Date,
        config: Config,
        defaultDateText: String,
        defaultTimeText: String
    ): ResolvedCell {
        val rawText = cell.valueText

        return when (cell.dataType) {
            TableCellDataType.TEXT -> {
                val t = rawText.trim()
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = t,
                    isEmpty = t.isBlank()
                )
            }

            TableCellDataType.NUMBER -> {
                val t = rawText.trim()
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = t,
                    isEmpty = t.isBlank()
                )
            }

            TableCellDataType.COUNTER -> {
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = counterResolved,
                    isEmpty = false
                )
            }

            TableCellDataType.DATE -> {
                val resolved = if (rawText.isBlank()) {
                    val pattern = cell.formatPattern.ifBlank { config.dateFormat }
                    SimpleDateFormat(pattern, config.locale).format(captureNow)
                } else rawText.trim()
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = resolved,
                    isEmpty = resolved.isBlank()
                )
            }

            TableCellDataType.TIME -> {
                val resolved = if (rawText.isBlank()) {
                    val pattern = cell.formatPattern.ifBlank { config.timeFormat }
                    SimpleDateFormat(pattern, config.locale).format(captureNow)
                } else rawText.trim()
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = resolved,
                    isEmpty = resolved.isBlank()
                )
            }
        }
    }

    private fun parseCounter(valueText: String?): Int? {
        val t = valueText?.trim().orEmpty()
        if (t.isBlank()) return null
        return t.toIntOrNull()?.takeIf { it >= 0 }
    }
}
