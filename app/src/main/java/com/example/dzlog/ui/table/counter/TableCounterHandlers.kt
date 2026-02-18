package com.example.dzlog.ui.table.counter

/**
 * Counter handlers extracted from TableEditorScreen.
 *
 * Rule:
 * - UI layer delegates counter behavior through this file + TableCounterPolicyCoordinator.
 * - Do not call CounterManager/CaptureCounterPolicy directly from UI call-sites.
 */

import android.content.Context
import com.example.dzlog.domain.counter.CaptureScopedCounterStream
import com.example.dzlog.domain.counter.CounterStreamContext
import com.example.dzlog.domain.counter.buildCounterStreamContext
import com.example.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.example.dzlog.domain.counter.policy.buildCounterScopeSnapshot
import com.example.dzlog.domain.counter.toCaptureScopedCounterStream
import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.naming.NamingFormatDefaults
import com.example.dzlog.domain.table.ResolvedCell
import com.example.dzlog.feature.table.policy.TableCounterConflictDialogEffect
import com.example.dzlog.feature.table.policy.TableCounterPolicyCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun buildTableCounterStreamContext(
    resolvedCells: List<ResolvedCell>,
    scopeNextCounter: Int,
    isManualCounterModeDisplay: Boolean
): CounterStreamContext =
    buildCounterStreamContext(
        resolvedCells = resolvedCells,
        nextCounter = scopeNextCounter,
        isManualMode = isManualCounterModeDisplay,
        fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
    )

internal fun buildTableScopedCounterStream(
    counterStreamContext: CounterStreamContext,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean
): CaptureScopedCounterStream =
    toCaptureScopedCounterStream(
        streamContext = counterStreamContext,
        includePathInScope = includePathInCounterScope,
        includeFilenameInScope = includeFilenameInCounterScope
    )

internal fun buildTableCounterScopeSnapshot(
    counterStreamContext: CounterStreamContext,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean
): CounterScopeSnapshot =
    buildCounterScopeSnapshot(
        streamContext = counterStreamContext,
        includePathInScope = includePathInCounterScope,
        includeFilenameInScope = includeFilenameInCounterScope
    )

internal fun applyCounterSeed(
    counterUi: TableCounterUiState,
    seed: Int,
    preserveManual: Boolean
): TableCounterUiState {
    val normalizedSeed = seed.coerceAtLeast(1)
    return counterUi.copy(
        preserveManualCounterSeed = preserveManual,
        manualSeedOverride = if (preserveManual) normalizedSeed else null,
        scopeNextCounter = normalizedSeed
    )
}

internal fun updateCounterCellAndPolicy(
    context: Context,
    templateState: TableTemplateState,
    cellId: String,
    seed: Int,
    preserveManual: Boolean,
    forcePolicyUpdate: Boolean,
    scopedCounterStream: CaptureScopedCounterStream,
    previewCounterDigits: Int,
    counterUi: TableCounterUiState,
    onTemplateChange: (TableTemplateState) -> Unit,
    setCounterUi: (TableCounterUiState) -> Unit,
    updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
    scope: CoroutineScope
) {
    val normalizedSeed = seed.coerceAtLeast(1)
    val updated = updateCell(templateState, cellId) { c ->
        c.copy(typedValue = CellValue.CounterSeed(normalizedSeed))
    }
    onTemplateChange(updated)
    setCounterUi(applyCounterSeed(counterUi, normalizedSeed, preserveManual))

    if (preserveManual || forcePolicyUpdate) {
        scope.launch {
            TableCounterPolicyCoordinator.setNextCounter(
                context = context,
                scopedStream = scopedCounterStream,
                desired = normalizedSeed,
                force = forcePolicyUpdate,
                counterDigits = previewCounterDigits,
            )
        }
    }
}

internal suspend fun fetchAutoNextCounter(
    context: Context,
    scopedCounterStream: CaptureScopedCounterStream,
    previewCounterDigits: Int,
): Int = TableCounterPolicyCoordinator.resetToAutoNext(
    context = context,
    scopedStream = scopedCounterStream,
    counterDigits = previewCounterDigits,
).coerceAtLeast(1)

internal fun restoreCounterCellToAutoNext(
    context: Context,
    templateState: TableTemplateState,
    cellId: String,
    scopedCounterStream: CaptureScopedCounterStream,
    previewCounterDigits: Int,
    counterUi: TableCounterUiState,
    onTemplateChange: (TableTemplateState) -> Unit,
    setCounterUi: (TableCounterUiState) -> Unit,
    updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
    scope: CoroutineScope
) {
    scope.launch {
        val restored = fetchAutoNextCounter(
            context = context,
            scopedCounterStream = scopedCounterStream,
            previewCounterDigits = previewCounterDigits
        )
        updateCounterCellAndPolicy(
            context = context,
            templateState = templateState,
            cellId = cellId,
            seed = restored,
            preserveManual = false,
            forcePolicyUpdate = false,
            scopedCounterStream = scopedCounterStream,
            previewCounterDigits = previewCounterDigits,
            counterUi = counterUi,
            onTemplateChange = onTemplateChange,
            setCounterUi = setCounterUi,
            updateCell = updateCell,
            scope = scope
        )
    }
}

internal fun applyCounterConflictDialogEffect(
    effect: TableCounterConflictDialogEffect,
    context: Context,
    templateState: TableTemplateState,
    scopedCounterStream: CaptureScopedCounterStream,
    previewCounterDigits: Int,
    counterUi: TableCounterUiState,
    onTemplateChange: (TableTemplateState) -> Unit,
    setCounterUi: (TableCounterUiState) -> Unit,
    updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
    scope: CoroutineScope
) {
    when (effect) {
        is TableCounterConflictDialogEffect.ApplyManualSeed -> {
            updateCounterCellAndPolicy(
                context = context,
                templateState = templateState,
                cellId = effect.cellId,
                seed = effect.seed,
                preserveManual = true,
                forcePolicyUpdate = true,
                scopedCounterStream = scopedCounterStream,
                previewCounterDigits = previewCounterDigits,
                counterUi = counterUi,
                onTemplateChange = onTemplateChange,
                setCounterUi = setCounterUi,
                updateCell = updateCell,
                scope = scope
            )
        }

        is TableCounterConflictDialogEffect.RestoreAutoNext -> {
            restoreCounterCellToAutoNext(
                context = context,
                templateState = templateState,
                cellId = effect.cellId,
                scopedCounterStream = scopedCounterStream,
                previewCounterDigits = previewCounterDigits,
                counterUi = counterUi,
                onTemplateChange = onTemplateChange,
                setCounterUi = setCounterUi,
                updateCell = updateCell,
                scope = scope
            )
        }

        TableCounterConflictDialogEffect.None -> Unit
    }
}

internal data class TableCounterSyncResult(
    val counterUi: TableCounterUiState,
    val nextScopeSnapshot: CounterScopeSnapshot,
    val updatedTemplateState: TableTemplateState?
)

internal suspend fun syncCounterStateForScope(
    context: Context,
    templateState: TableTemplateState,
    counterUi: TableCounterUiState,
    counterStreamContext: CounterStreamContext,
    scopedCounterStream: CaptureScopedCounterStream,
    previewCounterDigits: Int,
    isManualCounterModeDisplay: Boolean,
    lastScopeSnapshot: CounterScopeSnapshot?,
    updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
): TableCounterSyncResult {
    val counterCell = templateState.cells.firstOrNull { it.dataType == TableCellDataType.COUNTER }
    val currentSeed = (counterCell?.typedValue as? CellValue.CounterSeed)?.start ?: 1
    val streamNext = TableCounterPolicyCoordinator.getNextCounter(
        context = context,
        scopedStream = scopedCounterStream,
        counterDigits = previewCounterDigits,
    ).coerceAtLeast(1)
    val isManualCounterMode = TableCounterPolicyCoordinator.isManualOverrideActive(
        context = context,
        scopedStream = scopedCounterStream
    )
    // `streamNext`와 같은 SSOT 값을 UI 표시에 재사용한다.
    val autoNextCounterValue = streamNext

    val syncResult = TableCounterPolicyCoordinator.resolveSeedForScope(
        input = TableCounterPolicyCoordinator.CounterSeedSyncInput(
            currentScopeSnapshot = buildTableCounterScopeSnapshot(
                counterStreamContext = counterStreamContext,
                includePathInCounterScope = counterUi.includePathInCounterScope,
                includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
            ),
            isManualMode = isManualCounterModeDisplay,
            hasCounterCell = (counterCell != null),
            currentSeed = currentSeed,
            streamNext = streamNext,
            previousScopeSnapshot = lastScopeSnapshot,
            preserveManualCounterSeed = counterUi.preserveManualCounterSeed,
            manualSeedOverride = counterUi.manualSeedOverride
        )
    )

    val nextCounterUi = counterUi.copy(
        isManualCounterMode = isManualCounterMode,
        autoNextCounterValue = autoNextCounterValue,
        preserveManualCounterSeed = syncResult.preserveManualCounterSeed,
        manualSeedOverride = if (syncResult.shouldClearManualOverride) null else counterUi.manualSeedOverride,
        scopeNextCounter = syncResult.desiredSeed
    )

    val nextScopeSnapshot = buildTableCounterScopeSnapshot(
        counterStreamContext = counterStreamContext,
        includePathInCounterScope = counterUi.includePathInCounterScope,
        includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
    )

    val updatedTemplateState = if (counterCell != null && currentSeed != syncResult.desiredSeed) {
        updateCell(templateState, counterCell.cellId) { c ->
            c.copy(typedValue = CellValue.CounterSeed(syncResult.desiredSeed))
        }
    } else {
        null
    }

    return TableCounterSyncResult(
        counterUi = nextCounterUi,
        nextScopeSnapshot = nextScopeSnapshot,
        updatedTemplateState = updatedTemplateState
    )
}

internal fun updateCounterUiScopeFlags(
    counterUi: TableCounterUiState,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
): TableCounterUiState = counterUi.copy(
    includePathInCounterScope = includePathInCounterScope,
    includeFilenameInCounterScope = includeFilenameInCounterScope,
)

internal fun updateCounterUiConflictDialogState(
    counterUi: TableCounterUiState,
    state: com.example.dzlog.feature.table.policy.TableCounterConflictDialogState,
): TableCounterUiState = counterUi.copy(counterConflictDialogState = state)