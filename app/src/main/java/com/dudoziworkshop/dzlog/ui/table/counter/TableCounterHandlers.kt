package com.dudoziworkshop.dzlog.ui.table.counter

/**
 * Counter handlers extracted from TableEditorScreen.
 *
 * Rule:
 * - UI layer delegates counter behavior through this file + policy/coordinator helpers.
 * - Table counter read/write는 CounterFacade + CounterRequestResolver 경로로 수렴한다.
 */

import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.counter.policy.buildCounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.counter.policy.isNewCounterScope
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.counter.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.CounterReadResult
import com.dudoziworkshop.dzlog.feature.counter.CounterRequest
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogEffect
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterPolicyCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun buildTableCounterScopeSnapshot(
    counterScope: CounterScope,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
): CounterScopeSnapshot =
    buildCounterScopeSnapshot(
        counterScope = counterScope,
        includePathInScope = includePathInCounterScope,
        includeFilenameInScope = includeFilenameInCounterScope,
    )

internal fun buildFilenameScopeSignature(
    includeFilenameInCounterScope: Boolean,
    filenameScopeTokens: List<String>,
): String {
    if (!includeFilenameInCounterScope) return "filename-scope-disabled"

    // 주요 정책(파일명 축): 재동기화 시그니처는 draft 원문이 아니라
    // "최종 해석된 scope token 목록"(순서 포함)으로 계산한다.
    return filenameScopeTokens
        .mapIndexed { index, token -> "$index=$token" }
        .joinToString("|")
}

internal fun applyCounterSeed(
    counterUi: TableCounterUiState,
    seed: Int,
    preserveManual: Boolean,
): TableCounterUiState {
    val normalizedSeed = seed.coerceAtLeast(1)
    return counterUi.copy(
        // preserveManual:
        // UI 표시용 manual counter seed 유지 여부만 의미한다.
        // 저장소(auto-next 기준값) 갱신과는 분리된다.
        preserveManualCounterSeed = preserveManual,
        manualSeedOverride = if (preserveManual) normalizedSeed else null,
        scopeNextCounter = normalizedSeed,
        // 수동 입력 후보값을 반영한 시점부터는 화면 표시를 허용한다.
        isScopeCounterSynced = true,
    )
}

internal fun updateCounterCellAndPolicy(
    templateState: TableTemplateState,
    cellId: String,
    seed: Int,
    preserveManual: Boolean,
    persistToCounterPolicy: Boolean,
    counterRequest: CounterRequest,
    counterFacade: CounterFacade,
    counterUi: TableCounterUiState,
    onTemplateChange: (TableTemplateState) -> Unit,
    setCounterUi: (TableCounterUiState) -> Unit,
    updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
    scope: CoroutineScope,
) {
    val normalizedSeed = seed.coerceAtLeast(1)
    val updated = updateCell(templateState, cellId) { c ->
        c.copy(typedValue = CellValue.CounterSeed(normalizedSeed))
    }
    onTemplateChange(updated)
    setCounterUi(applyCounterSeed(counterUi, normalizedSeed, preserveManual))

    // 정책 정리:
    // - manual 입력은 "현재 표시/저장 후보값"만 바꾸고 실제 auto-next(readback 기준값)는 오염시키지 않는다.
    // - 표 상세 수동 입력은 기본적으로 임시값(UI 후보값)이며,
    // persistToCounterPolicy=true인 예외 상황에서만 domain manual override 저장을 건드린다.
    if (persistToCounterPolicy) {
        scope.launch {
            counterFacade.setManualNext(
                request = counterRequest,
                value = normalizedSeed,
            )
        }
    }
}

internal suspend fun fetchAutoNextCounter(
    counterRequest: CounterRequest,
    counterFacade: CounterFacade,
): Int {
    // 정책: auto reset의 SSOT는 항상 media readback(facade.read)이다.
    // clear 후 다시 read하여 auto 기준값으로 복귀한다.
    counterFacade.clearManualNext(counterRequest)
    return counterFacade.read(counterRequest).next.coerceAtLeast(1)
}

internal fun restoreCounterCellToAutoNext(
    templateState: TableTemplateState,
    cellId: String,
    counterRequest: CounterRequest,
    counterFacade: CounterFacade,
    counterUi: TableCounterUiState,
    onTemplateChange: (TableTemplateState) -> Unit,
    setCounterUi: (TableCounterUiState) -> Unit,
    updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
    scope: CoroutineScope,
) {
    scope.launch {
        val restored = fetchAutoNextCounter(
            counterRequest = counterRequest,
            counterFacade = counterFacade,
        )
        updateCounterCellAndPolicy(
            templateState = templateState,
            cellId = cellId,
            seed = restored,
            preserveManual = false,
            persistToCounterPolicy = false,
            counterRequest = counterRequest,
            counterFacade = counterFacade,
            counterUi = counterUi,
            onTemplateChange = onTemplateChange,
            setCounterUi = setCounterUi,
            updateCell = updateCell,
            scope = scope,
        )
    }
}

internal fun applyCounterConflictDialogEffect(
    effect: TableCounterConflictDialogEffect,
    templateState: TableTemplateState,
    counterRequest: CounterRequest,
    counterFacade: CounterFacade,
    counterUi: TableCounterUiState,
    onTemplateChange: (TableTemplateState) -> Unit,
    setCounterUi: (TableCounterUiState) -> Unit,
    updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
    scope: CoroutineScope,
) {
    when (effect) {
        is TableCounterConflictDialogEffect.ApplyManualSeed -> {
            updateCounterCellAndPolicy(
                templateState = templateState,
                cellId = effect.cellId,
                seed = effect.seed,
                preserveManual = true,
                // 수동 확정도 auto-next 저장 기준값을 오염시키지 않도록 UI 후보값으로만 반영한다.
                persistToCounterPolicy = false,
                counterRequest = counterRequest,
                counterFacade = counterFacade,
                counterUi = counterUi,
                onTemplateChange = onTemplateChange,
                setCounterUi = setCounterUi,
                updateCell = updateCell,
                scope = scope,
            )
        }

        is TableCounterConflictDialogEffect.RestoreAutoNext -> {
            restoreCounterCellToAutoNext(
                templateState = templateState,
                cellId = effect.cellId,
                counterRequest = counterRequest,
                counterFacade = counterFacade,
                counterUi = counterUi,
                onTemplateChange = onTemplateChange,
                setCounterUi = setCounterUi,
                updateCell = updateCell,
                scope = scope,
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
    templateState: TableTemplateState,
    counterUi: TableCounterUiState,
    counterScope: CounterScope,
    counterRequest: CounterRequest,
    counterFacade: CounterFacade,
    isManualCounterModeDisplay: Boolean,
    lastScopeSnapshot: CounterScopeSnapshot?,
    filenameScopeSignature: String,
    isFilenameScopeSignatureChanged: Boolean,
    isExternalResync: Boolean,
    updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
): TableCounterSyncResult {
    val counterCell = templateState.cells.firstOrNull { it.dataType == TableCellDataType.COUNTER }
    val currentSeed = (counterCell?.typedValue as? CellValue.CounterSeed)?.start ?: 1

    // 정책: 경고/재동기화 기준 stream next는 facade.read(media readback) 결과를 사용한다.
    suspend fun readCounter(): CounterReadResult = counterFacade.read(counterRequest)

    val firstRead = readCounter()
    val streamNext = if (isExternalResync) {
        delay(200)
        val secondRead = readCounter()
        if (firstRead.next == secondRead.next) firstRead.next else secondRead.next
    } else {
        firstRead.next
    }

    val currentScopeSnapshot = buildTableCounterScopeSnapshot(
        counterScope = counterScope,
        includePathInCounterScope = counterUi.includePathInCounterScope,
        includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
    )
    val isNewScope = isFilenameScopeSignatureChanged ||
        isNewCounterScope(previous = lastScopeSnapshot, current = currentScopeSnapshot)

    // 주요 정책(롤백 방지): 같은 scope에서는 화면 이동/빠른 재진입으로 streamNext가 내려와도 낮은 값으로 덮어쓰지 않는다.
    val stableStreamNext = stabilizeTableStreamNext(
        streamNext = streamNext,
        currentScopeNext = counterUi.scopeNextCounter,
        isNewScope = isNewScope,
    )
    val guardedStreamNext = if (!isNewScope && stableStreamNext < counterUi.scopeNextCounter) {
        counterUi.scopeNextCounter
    } else {
        stableStreamNext
    }

    val isManualCounterMode = firstRead.hasManualOverride
    val autoNextCounterValue = guardedStreamNext

    val syncResult = TableCounterPolicyCoordinator.resolveSeedForScope(
        input = TableCounterPolicyCoordinator.CounterSeedSyncInput(
            currentScopeSnapshot = currentScopeSnapshot,
            isManualMode = isManualCounterModeDisplay,
            hasCounterCell = (counterCell != null),
            currentSeed = currentSeed,
            streamNext = guardedStreamNext,
            previousScopeSnapshot = lastScopeSnapshot,
            preserveManualCounterSeed = counterUi.preserveManualCounterSeed,
            manualSeedOverride = counterUi.manualSeedOverride,
            // filename scope 사용 시 slot 시그니처가 바뀌면 새 scope로 강제 판정한다.
            forceTreatAsNewScope = isNewScope,
        ),
    )

    val nextCounterUi = counterUi.copy(
        isManualCounterMode = isManualCounterMode,
        autoNextCounterValue = autoNextCounterValue,
        preserveManualCounterSeed = syncResult.preserveManualCounterSeed,
        manualSeedOverride = if (syncResult.shouldClearManualOverride) null else counterUi.manualSeedOverride,
        scopeNextCounter = syncResult.desiredSeed,
        // 최초 facade.read 완료 이후부터 카운터 숫자 표시를 허용한다(초기 1 플리커 방지).
        isScopeCounterSynced = true,
    )

    val nextScopeSnapshot = buildTableCounterScopeSnapshot(
        counterScope = counterScope,
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

internal fun stabilizeTableStreamNext(
    streamNext: Int,
    currentScopeNext: Int,
    isNewScope: Boolean,
): Int {
    val normalizedStreamNext = streamNext.coerceAtLeast(1)
    val normalizedCurrent = currentScopeNext.coerceAtLeast(1)
    return if (isNewScope) normalizedStreamNext else maxOf(normalizedStreamNext, normalizedCurrent)
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
