package com.dudoziworkshop.dzlog.ui.table.editor

fun startInlineEditing(
    state: InlineEditState,
    cellId: String,
    initialText: String
): InlineEditState {
    return state.copy(
        editingCellId = cellId,
        editingValue = initialText,
    )
}

fun clearInlineEditing(state: InlineEditState): InlineEditState {
    return state.copy(
        editingCellId = null,
        editingValue = "",
    )
}
