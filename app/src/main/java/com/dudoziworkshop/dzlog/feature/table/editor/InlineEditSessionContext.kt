package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState

/**
 * `editSessionOriginalCellState`는 선택된 셀 기준 세션 원본 스냅샷이다.
 * 현재는 세션 중 snapshot 동기화와 undo 보조 판단에만 사용하며, template SSOT를 대체하지 않는다.
 */
data class InlineEditSessionContext(
    val currentTemplate: TableTemplateState,
    val inlineEdit: InlineEditState,
    val selectedCellId: String?,
    val inlineSessionState: InlineEditSessionState,
    val editSessionOriginalCellState: TableCellState?,
    val autoNextCounterValue: Int,
    val lowCounterWarningLatchedInSession: Boolean,
    val updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
)
