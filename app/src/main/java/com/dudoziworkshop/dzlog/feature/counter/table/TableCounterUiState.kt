package com.dudoziworkshop.dzlog.feature.counter.table

import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogState

data class TableCounterUiState(
    val scopeNextCounter: Int = 1,
    // 초기 진입/복귀 시 facade.read 동기화 전에는 기본 1을 즉시 표시하지 않기 위한 gate.
    val isScopeCounterSynced: Boolean = false,
    val preserveManualCounterSeed: Boolean = false,
    val manualSeedOverride: Int? = null,
    val isManualCounterMode: Boolean = false,
    val autoNextCounterValue: Int = 1,
    val includePathInCounterScope: Boolean = true,
    val includeFilenameInCounterScope: Boolean = true,
    val counterConflictDialogState: TableCounterConflictDialogState = TableCounterConflictDialogState()
)
