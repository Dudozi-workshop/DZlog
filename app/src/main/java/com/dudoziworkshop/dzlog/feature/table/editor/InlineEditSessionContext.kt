package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState

data class InlineEditSessionContext(
    val currentTemplate: TableTemplateState,
    val inlineEdit: InlineEditState,
    val selectedCellId: String?,
    val inlineSessionState: InlineEditSessionState,
    val editSessionOriginalCellState: TableCellState?,
    val editSessionSnapshotCellId: String?,
    val autoNextCounterValue: Int,
    val lowCounterWarningLatchedInSession: Boolean,
    val updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
)
