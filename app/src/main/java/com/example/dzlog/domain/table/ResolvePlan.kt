package com.example.dzlog.domain.table

/**
 * Plan/Commit 분리 구조.
 * - plan 단계에서는 어떤 상태도 변경하지 않는다.
 * - 저장 성공 후에만 patch를 커밋한다.
 */
data class ResolvePlan(
    val resolvedCells: List<ResolvedCell>,
    val patch: TablePatch
)

/**
 * 저장 성공 시에만 적용할 상태 변경분.
 * 현재 리팩터링 범위에서는 COUNTER 커밋만 포함한다.
 */
data class TablePatch(
    val updatesByCellId: Map<String, String>
) {
    companion object {
        val EMPTY = TablePatch(emptyMap())
    }
}
