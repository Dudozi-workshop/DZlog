package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.counter.policy.normalizeTimeToMinute
import com.dudoziworkshop.dzlog.domain.model.CellKey
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.RotatingCounterMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell

/**
 * 카운터 scope 키 조합 입력값(date/time/phrase)을 단일 규칙으로 생성한다.
 *
 * 정책:
 * - DATE/TIME: counterScopeMode=INCLUDE 인 셀만 scope 값에 포함
 * - PHRASE: fileNameSlots에 포함된 ROTATING_TEXT 중 PER_PHRASE 셀만 `rp_<resolvedText>` 포함
 * - GLOBAL(통합) 모드 ROTATING_TEXT는 phrase scope에 포함하지 않음
 */
object CounterScopeResolver {

    data class Inputs(
        val cells: List<TableCellState>,
        val fileNameSlots: List<CellKey?>,
        val resolvedCells: List<ResolvedCell>,
        val isPerPhraseMode: Boolean,
    )

    data class Result(
        val dateScopeValues: List<String>,
        val timeScopeValues: List<String>,
        val phraseScopeValues: List<String>,
    )

    fun resolve(inputs: Inputs): Result {
        val resolvedById = inputs.resolvedCells.associateBy { it.id }
        val ordered = inputs.cells.sortedWith(
            compareBy<TableCellState> { it.rowIndex }
                .thenBy { it.colIndex }
                .thenBy { it.cellId }
        )

        val dateValues = ordered
            .asSequence()
            .filter { it.dataType == TableCellDataType.DATE && it.counterScopeMode == CounterScopeMode.INCLUDE }
            .mapNotNull { resolvedById[it.cellId]?.resolvedText?.takeIf(String::isNotBlank) }
            .toList()

        val timeValues = ordered
            .asSequence()
            .filter { it.dataType == TableCellDataType.TIME && it.counterScopeMode == CounterScopeMode.INCLUDE }
            .mapNotNull { resolvedById[it.cellId]?.resolvedText }
            .map(::normalizeTimeToMinute)
            .filter(String::isNotBlank)
            .toList()

        val fileNameCellIds = inputs.fileNameSlots.mapNotNull { it }.toSet()
        val phraseValues = if (!inputs.isPerPhraseMode) {
            emptyList()
        } else {
            ordered
                .asSequence()
                .filter { cell ->
                    cell.dataType == TableCellDataType.ROTATING_TEXT &&
                        cell.cellId in fileNameCellIds &&
                        cell.rotatingCounterMode == RotatingCounterMode.PER_PHRASE
                }
                .mapNotNull { cell -> resolvedById[cell.cellId]?.resolvedText?.trim() }
                .filter(String::isNotBlank)
                .map { text -> "rp_$text" }
                .toList()
        }

        return Result(
            dateScopeValues = dateValues,
            timeScopeValues = timeValues,
            phraseScopeValues = phraseValues,
        )
    }
}
