package com.example.dzlog.domain.watermark

import com.example.dzlog.domain.table.ResolvedCell

/**
 * Fixed Contract:
 * - Resolver는 "의미 해석"만 수행
 * - Builder는 "표현 조합"만 수행
 */
object WatermarkBuilder {

    data class WatermarkCell(
        val valueText: String
    )

    /**
     * 테이블 워터마크 렌더링용 셀 목록.
     * - 인덱스 기반 렌더러를 위해 입력 순서를 그대로 유지한다.
     */
    fun buildTableCells(resolvedCells: List<ResolvedCell>): List<WatermarkCell> {
        return resolvedCells.map { rc ->
            WatermarkCell(valueText = rc.resolvedText)
        }
    }
}
