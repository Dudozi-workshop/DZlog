package com.dudoziworkshop.dzlog.feature.table.policy

data class TableCounterConflictDialogState(
    val isVisible: Boolean = false,
    val editingCellId: String? = null,
    val pendingCounterCommitValue: Int = 0,
    val pendingCounterStreamNextValue: Int = 1
)

sealed interface TableCounterConflictDialogEffect {
    data object None : TableCounterConflictDialogEffect
    data class RestoreAutoNext(val cellId: String) : TableCounterConflictDialogEffect
    data class ApplyManualSeed(val cellId: String, val seed: Int) : TableCounterConflictDialogEffect
}

fun openCounterConflictDialog(
    editingCellId: String,
    conflict: CounterEditConflict
): TableCounterConflictDialogState {
    return TableCounterConflictDialogState(
        isVisible = true,
        editingCellId = editingCellId,
        pendingCounterCommitValue = conflict.pendingCounterCommitValue,
        pendingCounterStreamNextValue = conflict.streamNextValue
    )
}

fun confirmCounterConflictDialog(
    state: TableCounterConflictDialogState
): Pair<TableCounterConflictDialogState, TableCounterConflictDialogEffect> {
    val cellId = state.editingCellId ?: return closeCounterConflictDialog(state) to TableCounterConflictDialogEffect.None
    val seed = state.pendingCounterCommitValue.coerceAtLeast(1)
    return closeCounterConflictDialog(state) to TableCounterConflictDialogEffect.ApplyManualSeed(
        cellId = cellId,
        seed = seed
    )
}

fun dismissCounterConflictDialog(
    state: TableCounterConflictDialogState
): Pair<TableCounterConflictDialogState, TableCounterConflictDialogEffect> {
    val cellId = state.editingCellId ?: return closeCounterConflictDialog(state) to TableCounterConflictDialogEffect.None
    return closeCounterConflictDialog(state) to TableCounterConflictDialogEffect.RestoreAutoNext(cellId)
}

fun closeCounterConflictDialog(state: TableCounterConflictDialogState): TableCounterConflictDialogState {
    return state.copy(
        isVisible = false,
        editingCellId = null,
        pendingCounterCommitValue = 0,
        pendingCounterStreamNextValue = 1
    )
}
