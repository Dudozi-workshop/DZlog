package com.dudoziworkshop.dzlog.feature.table.editor.coordinator

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

data class TableEditorSaveApplyInput(
    val result: TableEditorSaveResult,
    val savePayload: TableTemplateState,
    val stylePayload: TableStyleState,
    val exitAfterSave: Boolean,
    val currentUndoRevision: Int,
)

data class TableEditorSaveAppliedState(
    val nextPlacement: TablePlacementState?,
    val nextInitialTemplateSnapshot: TableTemplateState?,
    val nextInitialStyleSnapshot: TableStyleState?,
    val nextInitialPlacementSnapshot: TablePlacementState?,
    val nextRowWeightsDragBaseTemplate: TableTemplateState?,
    val nextColWeightsDragBaseTemplate: TableTemplateState?,
    val shouldClearUndo: Boolean,
    val nextUndoRevision: Int,
    val nextIsSavingTemplate: Boolean,
    val toastMessage: String,
    val isLongToast: Boolean,
    val shouldNotifyTemplateChange: Boolean,
    val shouldExitAfterSave: Boolean,
)

object TableEditorSaveResultApplier {

    fun apply(input: TableEditorSaveApplyInput): TableEditorSaveAppliedState {
        return when (val result = input.result) {
            is TableEditorSaveResult.Failure -> TableEditorSaveAppliedState(
                nextPlacement = null,
                nextInitialTemplateSnapshot = null,
                nextInitialStyleSnapshot = null,
                nextInitialPlacementSnapshot = null,
                nextRowWeightsDragBaseTemplate = null,
                nextColWeightsDragBaseTemplate = null,
                shouldClearUndo = false,
                nextUndoRevision = input.currentUndoRevision,
                nextIsSavingTemplate = false,
                toastMessage = result.message,
                isLongToast = result.isLongToast,
                shouldNotifyTemplateChange = false,
                shouldExitAfterSave = false,
            )

            is TableEditorSaveResult.Success -> TableEditorSaveAppliedState(
                nextPlacement = result.savedPlacement,
                nextInitialTemplateSnapshot = input.savePayload,
                nextInitialStyleSnapshot = input.stylePayload,
                nextInitialPlacementSnapshot = result.savedPlacement,
                nextRowWeightsDragBaseTemplate = null,
                nextColWeightsDragBaseTemplate = null,
                shouldClearUndo = true,
                nextUndoRevision = input.currentUndoRevision + 1,
                nextIsSavingTemplate = false,
                toastMessage = "저장됨",
                isLongToast = false,
                shouldNotifyTemplateChange = true,
                shouldExitAfterSave = input.exitAfterSave,
            )
        }
    }
}
