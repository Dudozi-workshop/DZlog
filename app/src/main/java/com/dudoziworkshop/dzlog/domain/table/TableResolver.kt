package com.dudoziworkshop.dzlog.domain.table

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
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
        selectedPhraseTextByCellId: Map<String, String> = emptyMap(),
        phraseSets: List<RotatingPhraseSet> = emptyList(),
    ): ResolvePlan {
        // 안정적 순서: row/col 기준
        val ordered =
            cells.sortedWith(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex })

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
        // NOTE: DATE/TIME은 셀의 원본 텍스트(rawText)를 신뢰하지 않는다.
        //       항상 captureNow 기준으로 포맷하여 표시/저장한다.

        val resolved = ordered.map { cell ->
            resolveCell(
                cell = cell,
                counterResolved = counterResolved,
                captureNow = captureNow,
                config = config,
                selectedPhraseTextByCellId = selectedPhraseTextByCellId,
            ).let { resolved ->
                if (cell.dataType == TableCellDataType.ROTATING_TEXT) {
                    resolved.copy(rotatingPhraseSet = phraseSets.firstOrNull { it.id == cell.phraseSetId })
                } else resolved
            }
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
        selectedPhraseTextByCellId: Map<String, String>,
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
                val pattern = normalizeDatePattern(cell.formatPattern.ifBlank { config.dateFormat })
                val resolved = SimpleDateFormat(pattern, config.locale).format(captureNow)
                val scopeToken = SimpleDateFormat("yyyyMMdd", Locale.US).format(captureNow)
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = resolved,
                    isEmpty = resolved.isBlank(),
                    scopeToken = scopeToken
                )
            }

            TableCellDataType.TIME -> {
                // TIME 정책: 파일명/저장경로/카운터 스코프 모두 분 단위 HHmm만 사용한다.
                val pattern = "HHmm"
                val resolved = SimpleDateFormat(pattern, config.locale).format(captureNow)
                val scopeToken = SimpleDateFormat("HHmm", Locale.US).format(captureNow)
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = resolved,
                    isEmpty = resolved.isBlank(),
                    scopeToken = scopeToken
                )
            }

            TableCellDataType.ROTATING_TEXT -> {
                // 정책 정리(4차): TableResolver는 문구를 "선택"하지 않는다.
                // 상위에서 선택된 문구(selectedPhraseTextByCellId)만 소비하여 해석 결과를 만든다.
                val resolvedText = selectedPhraseTextByCellId[cell.cellId].orEmpty()
                ResolvedCell(
                    id = cell.cellId,
                    type = cell.dataType,
                    raw = cell,
                    resolvedText = resolvedText,
                    isEmpty = resolvedText.isBlank(),
                    // 정책 변경: ROTATING_TEXT scopeToken 인덱스 분리는 제거.
                    // 상위 조합 로직에서 resolvedText 기반 phrase scope("rp_")를 생성한다.
                    scopeToken = null
                )
            }
        }
    }


    private fun normalizeDatePattern(pattern: String): String {
        return when (pattern) {
            "yyyyMMdd", "yyMMdd", "MMdd" -> pattern
            else -> "yyyyMMdd"
        }
    }

    private fun parseCounterSeed(cell: TableCellState?): Int? {
        if (cell == null) return null
        val seed = (cell.typedValue as? CellValue.CounterSeed)?.start
        return seed?.takeIf { it >= 0 }
    }
}
