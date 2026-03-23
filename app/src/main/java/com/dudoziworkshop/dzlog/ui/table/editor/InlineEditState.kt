package com.dudoziworkshop.dzlog.ui.table.editor

/**
 * `editingValue`는 저장 전 데이터 본체가 아니라 CELL_EDIT 입력 UI가 들고 있는 현재 문자열이다.
 * 실제 값의 SSOT는 항상 `TableTemplateState`다.
 */
data class InlineEditState(
    val editingCellId: String? = null,
    val editingValue: String = "",
)

fun InlineEditState.isEditing(): Boolean = editingCellId != null
