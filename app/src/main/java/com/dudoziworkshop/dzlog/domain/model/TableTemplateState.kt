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
    // legacy 호환 필드: fileNameSlotDrafts(CELL 타입)에서 파생된 결과만 저장한다.
    val fileNameSlots: List<CellKey?> = List(FILE_NAME_SLOT_COUNT) { null },
    // 정책 보강: TableEditor draft(파일명/저장경로 슬롯 편집 내용) 저장 복원을 위한 payload.
    // - 기존 템플릿과의 하위 호환을 위해 optional로 유지한다.
    val fileNameSlotDrafts: List<TableEditorSlotDraft?> = List(FILE_NAME_SLOT_COUNT) { null },
    val pathSlotDrafts: List<TableEditorSlotDraft?> = List(PATH_SLOT_COUNT) { null },
)

private const val FILE_NAME_SLOT_KIND_CELL = "CELL"

fun deriveLegacyFileNameSlotsFromDrafts(drafts: List<TableEditorSlotDraft?>): List<CellKey?> {
    val normalizedDrafts = drafts.take(FILE_NAME_SLOT_COUNT) +
        List((FILE_NAME_SLOT_COUNT - drafts.size).coerceAtLeast(0)) { null }
    return normalizedDrafts.map { draft ->
        val isCellSlot = draft?.kind.equals(FILE_NAME_SLOT_KIND_CELL, ignoreCase = true)
        if (isCellSlot) draft?.cellId else null
    }
}

const val FILE_NAME_SLOT_COUNT: Int = 3
const val PATH_SLOT_COUNT: Int = 2
