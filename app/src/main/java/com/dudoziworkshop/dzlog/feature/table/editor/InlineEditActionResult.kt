package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogState
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState

enum class InlineTemplateApplyMode {
    NONE,
    PUSH_UNDO_THEN_APPLY,
    APPLY_DIRECTLY,
}

data class InlineEditActionResult(
    val nextInlineEdit: InlineEditState,
    val nextTemplate: TableTemplateState? = null,
    val nextInlineSessionState: InlineEditSessionState = InlineEditSessionState(),
    val nextEditSessionOriginalCellState: TableCellState? = null,
    val nextEditSessionSnapshotCellId: String? = null,
    val nextSelectedCellId: String? = null,
    val templateApplyMode: InlineTemplateApplyMode = InlineTemplateApplyMode.NONE,
    val actionLabel: String? = null,
    val wasBlocked: Boolean = false,
    val nextLowCounterWarningLatchedInSession: Boolean,
    val openedCounterConflict: TableCounterConflictDialogState? = null,
    val committedCounterSeed: Int? = null,
    val committedCellId: String? = null,
) {
    val shouldApplyTemplateWithUndo: Boolean
        get() = templateApplyMode == InlineTemplateApplyMode.PUSH_UNDO_THEN_APPLY

    val shouldUpdateTemplateDraftDirectly: Boolean
        get() = templateApplyMode == InlineTemplateApplyMode.APPLY_DIRECTLY
}
