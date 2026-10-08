package com.dudoziworkshop.dzlog.domain.model

typealias CellKey = String

data class TableEditorSlotDraft(
    val kind: String,
    val label: String,
    val cellId: String? = null,
    val manualText: String? = null,
    val formatType: String? = null,
    val formatPattern: String? = null,
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
    // 저장경로는 도메인/저장/촬영 전 구간에서 최대 3단계만 허용한다.
    val pathSlotDrafts: List<TableEditorSlotDraft?> = List(PATH_SLOT_UI_MAX_COUNT) { null },
) {
    init {
        require(pathSlotDrafts.size <= PATH_SLOT_UI_MAX_COUNT) {
            "pathSlotDrafts must contain at most $PATH_SLOT_UI_MAX_COUNT items"
        }
    }
}

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

/** 저장경로의 도메인 최대 단계 수. UI/preview/counter/capture 모두 이 제한을 공유한다. */
const val PATH_SLOT_UI_MAX_COUNT: Int = 3

fun normalizePathSlotDrafts(
    drafts: List<TableEditorSlotDraft?>,
): List<TableEditorSlotDraft?> =
    drafts.take(PATH_SLOT_UI_MAX_COUNT) +
        List((PATH_SLOT_UI_MAX_COUNT - drafts.size).coerceAtLeast(0)) { null }

fun derivePathCellSlotsFromDrafts(drafts: List<TableEditorSlotDraft?>): List<CellKey?> {
    return normalizePathSlotDrafts(drafts).map { draft ->
        val isCellSlot = draft?.kind.equals(PATH_SLOT_KIND_CELL, ignoreCase = true)
        if (isCellSlot) draft?.cellId else null
    }
}

fun derivePathSlotIndexByCellId(drafts: List<TableEditorSlotDraft?>, cellId: CellKey): Int? {
    return derivePathCellSlotsFromDrafts(drafts).indexOf(cellId).takeIf { it >= 0 }
}
