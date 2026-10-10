package com.dudoziworkshop.dzlog.ui.table.mock

internal enum class TableEditorBackAction {
    IGNORE, CLOSE_DETAIL, CLOSE_ADVANCED_STYLE, CLOSE_PANEL, CLOSE_CELL, CONFIRM_EXIT, EXIT,
}

/** Closing a UI layer must not discard or persist the editor draft. */
internal fun resolveTableEditorBackAction(
    isSaving: Boolean,
    hasDetail: Boolean,
    activeTab: MockBottomTab,
    hasAdvancedStyle: Boolean,
    hasSelectedCell: Boolean,
    hasUnsavedChanges: Boolean,
): TableEditorBackAction = when {
    isSaving -> TableEditorBackAction.IGNORE
    hasDetail -> TableEditorBackAction.CLOSE_DETAIL
    activeTab == MockBottomTab.STYLE && hasAdvancedStyle -> TableEditorBackAction.CLOSE_ADVANCED_STYLE
    activeTab != MockBottomTab.CONTENT -> TableEditorBackAction.CLOSE_PANEL
    hasSelectedCell -> TableEditorBackAction.CLOSE_CELL
    hasUnsavedChanges -> TableEditorBackAction.CONFIRM_EXIT
    else -> TableEditorBackAction.EXIT
}
