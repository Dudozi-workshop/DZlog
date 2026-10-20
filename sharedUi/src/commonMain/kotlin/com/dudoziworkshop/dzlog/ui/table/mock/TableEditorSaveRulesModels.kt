package com.dudoziworkshop.dzlog.ui.table.mock

enum class MockRuleSourceType(val label: String) {
    CELL("셀에서 가져오기"),
    MANUAL("직접 입력"),
    DATE("날짜"),
    TIME("시간"),
    ROTATING_TEXT("순환문구"),
}

data class MockRuleItem(
    val sourceType: MockRuleSourceType,
    val value: String,
    val cellId: String? = null,
    val formatPattern: String? = null,
)

/** A cell-bound rule is not valid until a selectable cell is confirmed. */
fun linkedRuleItemFromCell(cell: TableEditorCellUiModel?): MockRuleItem? {
    if (cell == null || cell.isCovered || cell.type == TableEditorCellType.COUNTER) return null
    val cellId = cell.domainCellId?.takeIf { it.isNotBlank() } ?: return null
    return MockRuleItem(MockRuleSourceType.CELL, cell.value, cellId)
}

data class MockSaveRulesDraft(
    val fileNameItems: List<MockRuleItem?>,
    val pathItems: List<MockRuleItem?>,
    val includePathInScope: Boolean,
    val includeFilenameInScope: Boolean,
)

// Legacy ROTATING_TEXT remains readable; new rules reference its cell through CELL.
val saveRuleSourceChoices = listOf(MockRuleSourceType.CELL, MockRuleSourceType.MANUAL,
    MockRuleSourceType.DATE, MockRuleSourceType.TIME)
