package com.dudoziworkshop.dzlog.ui.table.counter

import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogState

data class TableCounterUiState(
    val scopeNextCounter: Int = 1,
    val preserveManualCounterSeed: Boolean = false,
    val manualSeedOverride: Int? = null,
    val isManualCounterMode: Boolean = false,
    val autoNextCounterValue: Int = 1,
    val includePathInCounterScope: Boolean = true,
    val includeFilenameInCounterScope: Boolean = true,
    val counterConflictDialogState: TableCounterConflictDialogState = TableCounterConflictDialogState()
)
