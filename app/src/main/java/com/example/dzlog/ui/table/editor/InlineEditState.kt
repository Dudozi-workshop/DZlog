package com.example.dzlog.ui.table.editor

data class InlineEditState(
    val editingCellId: String? = null,
    val editingValue: String = "",
    val editingOriginalValue: String = ""
)

fun InlineEditState.isEditing(): Boolean = editingCellId != null
