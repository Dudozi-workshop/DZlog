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
    // 낮은 값 경고는 Table 상세 화면 세션에서 1회만 노출한다.
    // true면 "현재 low 상태 경고를 이미 1회 노출한 세션"으로 간주한다.
    val lowCounterWarningLatchedInSession: Boolean = false,
    val includePathInCounterScope: Boolean = true,
    val includeFilenameInCounterScope: Boolean = true,
    val counterConflictDialogState: TableCounterConflictDialogState = TableCounterConflictDialogState()
)
