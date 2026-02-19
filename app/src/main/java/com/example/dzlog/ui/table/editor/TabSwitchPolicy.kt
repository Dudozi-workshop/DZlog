package com.example.dzlog.ui.table.editor

/**
 * Returns true if tab switch should be blocked because inline edit is still active after commit attempt.
 */
fun shouldBlockTabSwitchAfterCommit(inlineEdit: InlineEditState): Boolean = inlineEdit.isEditing()
