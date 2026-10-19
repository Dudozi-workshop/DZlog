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
    // 현재 편집 가능한 저장경로 최대 5단계. 갤러리의 기존 데이터 열람 깊이와는 별도 정책이다.
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

/** 현재 개발/실사용 단계: 파일명 구성요소 5개 사용 가능. 향후 요금제별 편집 한도와 분리한다. */
const val FILE_NAME_SLOT_COUNT: Int = 5

/** 향후 무료 플랜의 구성요소 수(결제 권한 적용은 후속 단계). */
const val BASIC_SAVE_RULE_SLOT_COUNT: Int = 3

/** 저장경로 구성요소 최대 5개. UI/preview/counter/capture가 공유한다. */
const val PATH_SLOT_UI_MAX_COUNT: Int = 5

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
