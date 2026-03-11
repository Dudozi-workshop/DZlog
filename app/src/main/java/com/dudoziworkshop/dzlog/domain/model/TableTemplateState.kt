package com.dudoziworkshop.dzlog.domain.model

typealias CellKey = String

data class TableEditorSlotDraft(
    val kind: String,
    val label: String,
    val cellId: String? = null,
    val manualText: String? = null,
    val formatType: String? = null,
)

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
    val colWeights: List<Float>? = null,
    val phraseSets: List<RotatingPhraseSet> = emptyList(),
    // 정책: 파일명/저장경로 슬롯의 단일 SSOT는 draft payload다.
    val fileNameSlotDrafts: List<TableEditorSlotDraft?> = List(FILE_NAME_SLOT_COUNT) { null },
    val pathSlotDrafts: List<TableEditorSlotDraft?> = List(PATH_SLOT_COUNT) { null },
)

private const val FILE_NAME_SLOT_KIND_CELL = "CELL"
private const val PATH_SLOT_KIND_CELL = "CELL"

fun deriveFileNameCellSlotsFromDrafts(drafts: List<TableEditorSlotDraft?>): List<CellKey?> {
    val normalizedDrafts = drafts.take(FILE_NAME_SLOT_COUNT) +
        List((FILE_NAME_SLOT_COUNT - drafts.size).coerceAtLeast(0)) { null }
    return normalizedDrafts.map { draft ->
        val isCellSlot = draft?.kind.equals(FILE_NAME_SLOT_KIND_CELL, ignoreCase = true)
        if (isCellSlot) draft?.cellId else null
    }
}

const val FILE_NAME_SLOT_COUNT: Int = 3
const val PATH_SLOT_COUNT: Int = 2


fun derivePathCellSlotsFromDrafts(drafts: List<TableEditorSlotDraft?>): List<CellKey?> {
    val normalizedDrafts = drafts.take(PATH_SLOT_COUNT) +
        List((PATH_SLOT_COUNT - drafts.size).coerceAtLeast(0)) { null }
    return normalizedDrafts.map { draft ->
        val isCellSlot = draft?.kind.equals(PATH_SLOT_KIND_CELL, ignoreCase = true)
        if (isCellSlot) draft?.cellId else null
    }
}

fun derivePathSlotIndexByCellId(drafts: List<TableEditorSlotDraft?>, cellId: CellKey): Int? {
    return derivePathCellSlotsFromDrafts(drafts).indexOf(cellId).takeIf { it >= 0 }
}
