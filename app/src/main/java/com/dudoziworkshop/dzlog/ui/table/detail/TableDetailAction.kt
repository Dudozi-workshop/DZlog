package com.dudoziworkshop.dzlog.ui.table.detail

import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange

sealed interface TableDetailAction {
    data object ToggleStructureMode : TableDetailAction
    data class SelectSingleCell(val cellId: String, val additive: Boolean) : TableDetailAction
    data class SelectRange(val startCellId: String, val endCellId: String) : TableDetailAction

    data object AddRow : TableDetailAction
    data object RemoveRow : TableDetailAction
    data object AddColumn : TableDetailAction
    data object RemoveColumn : TableDetailAction
    data object ResetRowWeights : TableDetailAction
    data object ResetColumnWeights : TableDetailAction
    data class CommitRowWeightsDrag(val weights: List<Float>) : TableDetailAction
    data class CommitColumnWeightsDrag(val weights: List<Float>) : TableDetailAction

    data class SetStyleBgStyle(val bgStyle: Int) : TableDetailAction
    data class SetStyleBgAlpha(val alpha: Int) : TableDetailAction
    data class SetStyleGridEnabled(val enabled: Boolean) : TableDetailAction
    data class SetStyleTextColorMode(val mode: Int) : TableDetailAction
    data class SetStyleManualTextColor(val color: Int) : TableDetailAction
    data class SetStyleValueScale(val scale: Int) : TableDetailAction
    data class SetStyleTextAlign(val align: Int) : TableDetailAction

    data object Undo : TableDetailAction
    data object Save : TableDetailAction

    data class InjectSelectionRangeForTest(val range: TableSelectionRange?) : TableDetailAction
}
