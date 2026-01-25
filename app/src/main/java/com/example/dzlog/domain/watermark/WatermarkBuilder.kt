package com.example.dzlog.domain.watermark

import com.example.dzlog.domain.table.ResolvedCell

/**
 * Fixed Contract:
 * - Resolver는 "의미 해석"만 수행
 * - Builder는 "표현 조합"만 수행
 */
object WatermarkBuilder {

    data class WatermarkCell(
        val label: String,
        val valueText: String
    )

    /**
     * 워터마크 정책(최종): value(resolvedText)만 출력.
     * 구분자는 현행(기존 정책) 유지. (현재는 줄바꿈)
     */
    fun buildText(resolvedCells: List<ResolvedCell>, separator: String = "\n"): String {
        return resolvedCells
            .asSequence()
            .map { it.resolvedText }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator)
    }

    /**
     * 테이블 워터마크 렌더링용 셀 목록.
     * - 인덱스 기반 렌더러를 위해 입력 순서를 그대로 유지한다.
     */
    fun buildTableCells(resolvedCells: List<ResolvedCell>): List<WatermarkCell> {
        return resolvedCells.map { rc ->
            val label = rc.raw?.label.orEmpty()
            WatermarkCell(label = label, valueText = rc.resolvedText)
        }
    }
}
