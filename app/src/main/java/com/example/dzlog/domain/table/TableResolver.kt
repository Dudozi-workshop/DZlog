package com.example.dzlog.domain.table

import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.HourSystem
import com.example.dzlog.domain.model.RotatingPhraseSet
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
        config: Config,
        counterSeedOverride: Int? = null,
        phraseSets: List<RotatingPhraseSet> = emptyList()
    ): ResolvePlan {
        // 안정적 순서: row/col 기준
        val ordered = cells.sortedWith(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex })

        val counterCell = ordered.firstOrNull { it.dataType == TableCellDataType.COUNTER }
        // COUNTER 표기 토글은 표현 레벨이며,
        // 카운터 계산은 stream SSOT(scopeNextCounter) override가 있으면 그것을 우선한다.
        val currentCounter = counterSeedOverride?.coerceAtLeast(1)
            ?: (parseCounterSeed(counterCell) ?: 1)
        val digits = config.counterDigits.coerceIn(0, 6)
        val counterResolved = if (digits == 0) {
            currentCounter.toString()
        } else {
            currentCounter.toString().padStart(digits, '0')
        }
        val nextCounter = currentCounter + 1
        val phraseSetMap = phraseSets.associateBy { it.id }

        // NOTE: DATE/TIME은 셀의 원본 텍스트(rawText)를 신뢰하지 않는다.
        //       항상 captureNow 기준으로 포맷하여 표시/저장한다.

        val resolved = ordered.map { cell ->
            resolveCell(
                cell = cell,
                counterResolved = counterResolved,
                usedCounter = currentCounter,
                phraseSetMap = phraseSetMap,
                captureNow = captureNow,
                config = config
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
        usedCounter: Int,
        phraseSetMap: Map<String, RotatingPhraseSet>,
        captureNow: Date,
        config: Config
    ): ResolvedCell {
        return when (cell.dataType) {
            TableCellDataType.TEXT -> {
                val t = cell.rawText.trim()
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = t,
                    isEmpty = t.isBlank()
                )
            }

            TableCellDataType.NUMBER -> {
                val t = cell.rawText.trim()
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
                // DATE는 항상 현재(captureNow) 기준으로 표시한다.
                val pattern = cell.formatPattern.ifBlank { config.dateFormat }
                val resolved = SimpleDateFormat(pattern, config.locale).format(captureNow)
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = resolved,
                    isEmpty = resolved.isBlank()
                )
            }

            TableCellDataType.TIME -> {
                // TIME은 항상 현재(captureNow) 기준으로 표시한다.
                // NOTE: Step3에서 UI 토글(timeFormatOptions)로 완전 전환한다.
                val pattern = cell.timeFormatOptions?.toTimePattern()
                    ?: cell.formatPattern.ifBlank { config.timeFormat }

                val resolved = SimpleDateFormat(pattern, config.locale).format(captureNow)
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = resolved,
                    isEmpty = resolved.isBlank()
                )
            }

            TableCellDataType.ROTATING_TEXT -> {
                val phraseSetId = cell.phraseSetId?.takeIf { it.isNotBlank() }
                val phraseSet = phraseSetId?.let { phraseSetMap[it] }
                val resolvedText = when {
                    phraseSet == null -> ""
                    phraseSet.items.isEmpty() -> ""
                    else -> {
                        val effectiveEvery = (cell.everyOverride ?: phraseSet.defaultEvery).coerceAtLeast(1)
                        val index = ((usedCounter - 1) / effectiveEvery) % phraseSet.items.size
                        phraseSet.items[index]
                    }
                }
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = resolvedText,
                    isEmpty = resolvedText.isBlank()
                )
            }
        }
    }

    private fun parseCounterSeed(cell: TableCellState?): Int? {
        if (cell == null) return null
        val seed = (cell.typedValue as? CellValue.CounterSeed)?.start
        return seed?.takeIf { it >= 0 }
    }

    private fun com.example.dzlog.domain.model.TimeFormatOptions.toTimePattern(): String {
        val sep = this.separator.token
        val base = when (this.hourSystem) {
            HourSystem.H24 -> "HH${sep}mm"
            HourSystem.H12 -> "hh${sep}mm"
        }
        val withSec = if (this.includeSeconds) "$base${sep}ss" else base
        return if (this.hourSystem == HourSystem.H12) "$withSec a" else withSec
    }
}
