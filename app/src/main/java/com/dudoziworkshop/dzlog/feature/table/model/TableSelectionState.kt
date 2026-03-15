package com.dudoziworkshop.dzlog.feature.table.model

data class TableSelectionState(
    val selectedCellIds: Set<String> = emptySet(),
    val lastSelectedCellId: String? = null,
    val minRow: Int? = null,
    val maxRow: Int? = null,
    val minCol: Int? = null,
    val maxCol: Int? = null,
)
