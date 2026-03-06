package com.dudoziworkshop.dzlog.domain.phrase

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState

/**
 * 문구 선택 정책 전용 resolver.
 *
 * 정책:
 * - 통합/문구별 모드와 무관하게 progress 기반으로 현재 문구를 선택한다.
 * - 저장 성공 전에는 progress를 바꾸지 않고, 성공 후 nextProgressCursor를 반영한다.
 */
object PhraseResolver {

    data class PhraseSelection(
        val text: String,
        val index: Int,
        val nextProgressCursor: Int,
    )

    fun resolve(
        phraseSet: RotatingPhraseSet?,
        every: Int,
        progressCursor: Int
    ): PhraseSelection? {
        if (phraseSet == null || phraseSet.items.isEmpty()) return null

        val effectiveEvery = every.coerceAtLeast(1)
        val safeProgress = progressCursor.coerceAtLeast(1)
        val index = ((safeProgress - 1) / effectiveEvery) % phraseSet.items.size
        return PhraseSelection(
            text = phraseSet.items[index],
            index = index,
            nextProgressCursor = safeProgress + 1,
        )
    }

    /**
     * 템플릿의 ROTATING_TEXT 셀들에 대해 "현재 progress 기준 문구"를 cellId 맵으로 계산한다.
     * - 문구 선택 책임은 이 resolver가 담당하고,
     * - TableResolver는 이 맵을 소비해 resolvedCells만 생성한다.
     */
    fun resolveSelectedTextByCellId(
        cells: List<TableCellState>,
        phraseSets: List<RotatingPhraseSet>,
        progressCursor: Int,
    ): Map<String, String> {
        val phraseSetMap = phraseSets.associateBy { it.id }
        val safeProgress = progressCursor.coerceAtLeast(1)

        return cells
            .asSequence()
            .filter { it.dataType == TableCellDataType.ROTATING_TEXT }
            .map { cell ->
                val phraseSetId = cell.phraseSetId?.takeIf { it.isNotBlank() }
                val phraseSet = phraseSetId?.let { phraseSetMap[it] }
                val effectiveEvery = (cell.everyOverride ?: phraseSet?.defaultEvery ?: 1).coerceAtLeast(1)
                cell.cellId to (resolve(
                    phraseSet = phraseSet,
                    every = effectiveEvery,
                    progressCursor = safeProgress
                )?.text.orEmpty())
            }
            .toMap()
    }
}
