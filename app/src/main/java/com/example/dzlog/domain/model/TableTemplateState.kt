package com.example.dzlog.domain.model

data class TableTemplateState(
    val rows: Int,
    val cols: Int,
    val cells: List<TableCellState>,
    /**
     * 행 높이(비율) 가중치. 길이는 rows와 동일해야 한다.
     * - null이면 "균등 분할"(전부 1.0)로 간주한다.
     * - Stage 1: 데이터만 도입하고, 실제 렌더링 반영은 후속 단계에서 수행한다.
     */
    val rowWeights: List<Float>? = null,
    /**
     * 열 너비(비율) 가중치. 길이는 cols와 동일해야 한다.
     * - null이면 "균등 분할"(전부 1.0)로 간주한다.
     * - Stage 1: 데이터만 도입하고, 실제 렌더링 반영은 후속 단계에서 수행한다.
     */
    val colWeights: List<Float>? = null
)
