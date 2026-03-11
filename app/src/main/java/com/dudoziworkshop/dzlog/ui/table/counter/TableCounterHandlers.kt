package com.dudoziworkshop.dzlog.ui.table.counter

/**
 * Counter handlers extracted from TableEditorScreen.
 *
 * Rule:
 * - UI layer delegates counter behavior through this file + TableCounterPolicyCoordinator.
 * - Do not call CounterManager/CaptureCounterPolicy directly from UI call-sites.
 */

import android.content.Context
import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.counter.CounterStreamContext
import com.dudoziworkshop.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.counter.policy.buildCounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.counter.toCaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogEffect
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterPolicyCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        // preserveManual:
        // UI 표시용 manual counter seed 유지 여부만 의미한다.
        // 저장소(auto-next 기준값) 갱신과는 분리된다.
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
    saveMode: SaveMode,
    counterUi: TableCounterUiState,
    onTemplateChange: (TableTemplateState) -> Unit,
    setCounterUi: (TableCounterUiState) -> Unit,
    updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
    scope: CoroutineScope
) {
    // Counter edit policy:
    //
    // manual seed 입력은
    // - UI 표시값만 변경
    // - 저장 기준값(auto-next)은 변경하지 않는다.
    //
    // 실제 저장 기준값은
    // - 사진 저장
    // - auto reset
    // - 명시적 정책 업데이트
    // 에서만 변경된다.
    val normalizedSeed = seed.coerceAtLeast(1)
    val updated = updateCell(templateState, cellId) { c ->
        c.copy(typedValue = CellValue.CounterSeed(normalizedSeed))
    }
    onTemplateChange(updated)
    setCounterUi(applyCounterSeed(counterUi, normalizedSeed, preserveManual))

    // 정책 정리:
    // - manual 입력은 "현재 표시/저장 후보값"만 바꾸고 실제 auto-next(readback 기준값)는 오염시키지 않는다.
    // - 따라서 저장소 next counter는 강제 정책 업데이트가 명시된 경우에만 갱신한다.
    if (forcePolicyUpdate) {
        scope.launch {
            TableCounterPolicyCoordinator.setNextCounter(
                context = context,
                scopedStream = scopedCounterStream,
                desired = normalizedSeed,
                force = forcePolicyUpdate,
                counterDigits = previewCounterDigits,
                saveMode = saveMode,
            )
        }
    }
}

internal suspend fun fetchAutoNextCounter(
    context: Context,
    scopedCounterStream: CaptureScopedCounterStream,
    previewCounterDigits: Int,
    saveMode: SaveMode,
): Int = TableCounterPolicyCoordinator.resetToAutoNext(
    context = context,
    scopedStream = scopedCounterStream,
    counterDigits = previewCounterDigits,
    saveMode = saveMode,
).coerceAtLeast(1)

internal fun restoreCounterCellToAutoNext(
    context: Context,
    templateState: TableTemplateState,
    cellId: String,
    scopedCounterStream: CaptureScopedCounterStream,
    previewCounterDigits: Int,
    saveMode: SaveMode,
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
            previewCounterDigits = previewCounterDigits,
            saveMode = saveMode,
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
            saveMode = saveMode,
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
    saveMode: SaveMode,
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
                // 수동 확정도 auto-next 저장 기준값을 오염시키지 않도록 UI 후보값으로만 반영한다.
                forcePolicyUpdate = false,
                scopedCounterStream = scopedCounterStream,
                previewCounterDigits = previewCounterDigits,
                saveMode = saveMode,
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
                saveMode = saveMode,
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
    val updatedTemplateState: TableTemplateState?,
    val nextFilenameScopeSignature: String,
)

internal suspend fun syncCounterStateForScope(
    context: Context,
    templateState: TableTemplateState,
    counterUi: TableCounterUiState,
    counterStreamContext: CounterStreamContext,
    scopedCounterStream: CaptureScopedCounterStream,
    previewCounterDigits: Int,
    saveMode: SaveMode,
    isManualCounterModeDisplay: Boolean,
    lastScopeSnapshot: CounterScopeSnapshot?,
    filenameScopeSignature: String,
    isFilenameScopeSignatureChanged: Boolean,
    isExternalResync: Boolean,
    updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
): TableCounterSyncResult {
    val counterCell = templateState.cells.firstOrNull { it.dataType == TableCellDataType.COUNTER }
    val currentSeed = (counterCell?.typedValue as? CellValue.CounterSeed)?.start ?: 1
    suspend fun readNextSeed(): Int = TableCounterPolicyCoordinator.getNextCounter(
        context = context,
        scopedStream = scopedCounterStream,
        counterDigits = previewCounterDigits,
        saveMode = saveMode,
    ).coerceAtLeast(1)

    val streamNext = if (isExternalResync) {
        val a = readNextSeed()
        delay(200)
        val b = readNextSeed()
        if (a == b) a else b
    } else {
        readNextSeed()
    }
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
            manualSeedOverride = counterUi.manualSeedOverride,
            // filename scope 사용 시 slot 시그니처가 바뀌면 새 scope로 강제 판정한다.
            forceTreatAsNewScope = isFilenameScopeSignatureChanged,
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
        updatedTemplateState = updatedTemplateState,
        nextFilenameScopeSignature = filenameScopeSignature,
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
    state: com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogState,
): TableCounterUiState = counterUi.copy(counterConflictDialogState = state)
