package com.dudoziworkshop.dzlog.domain.preview

import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureContext
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.counter.CounterScopeResolver
import com.dudoziworkshop.dzlog.domain.model.RotatingCounterMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.deriveFileNameCellSlotsFromDrafts
import com.dudoziworkshop.dzlog.domain.phrase.PhraseResolver
import com.dudoziworkshop.dzlog.domain.table.ResolvePlan
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import java.util.Date

internal data class PreviewInput(
    val templateState: TableTemplateState,
    val captureNow: Date,
    val counterDigits: Int,
    val dateFormat: String,
    val timeFormat: String,
    val fnDelim: String,
    val includePathInCounterScope: Boolean,
    val includeFilenameInCounterScope: Boolean,
    val saveMode: SaveMode,
    val scopeNextCounter: Int?,
    val phraseProgressCursor: Int,
    val overrideCells: List<TableCellState>? = null,
    val selectedPhraseTextByCellIdOverride: Map<String, String>? = null,
)

internal data class CaptureScopeInput(
    val templateState: TableTemplateState,
    val captureNow: Date,
    val counterDigits: Int,
    val dateFormat: String,
    val timeFormat: String,
    val fnDelim: String,
    val includeFilenameInCounterScope: Boolean,
    val saveMode: SaveMode,
    val phraseProgressCursor: Int,
    val overrideCells: List<TableCellState>? = null,
    val selectedPhraseTextByCellIdOverride: Map<String, String>? = null,
)

internal data class FinalCapturePreviewInput(
    val templateState: TableTemplateState,
    val captureNow: Date,
    val counterDigits: Int,
    val dateFormat: String,
    val timeFormat: String,
    val fnDelim: String,
    val includePathInCounterScope: Boolean,
    val includeFilenameInCounterScope: Boolean,
    val saveMode: SaveMode,
    val syncedCounter: Int,
    val phraseProgressCursor: Int,
)

internal data class CaptureScopeState(
    val effectiveCells: List<TableCellState>,
    val selectedPhraseTextByCellId: Map<String, String>,
    val plan: ResolvePlan,
    val scopeValues: CounterScopeResolver.Result,
    val counterScope: CounterScope,
    // 주요 정책: stream 분리용 counterScope와 DISPLAY_NAME 파싱용 scanPrefix를 분리 보관한다.
    val scanPrefix: String,
    val preSyncDisplayName: String,
    val relativePathPreview: String,
)

internal data class FinalCapturePreview(
    val resolvedCells: List<com.dudoziworkshop.dzlog.domain.table.ResolvedCell>,
    val tablePatch: com.dudoziworkshop.dzlog.domain.table.TablePatch,
    val displayName: String,
    val usedCounter: Int,
    val nextPhraseProgressCursor: Int,
    val counterScope: CounterScope,
    val relativePathPreview: String,
)

internal data class PreviewState(
    val selectedPhraseTextByCellId: Map<String, String>,
    val plan: ResolvePlan,
    val scopeValues: CounterScopeResolver.Result,
    val previewNaming: CaptureNamingPolicy.Result,
)

/**
 * 화면별 Preview 계산 경로를 단일화하기 위한 공용 state builder.
 */
internal fun buildPreviewState(
    input: PreviewInput,
    tableResolver: TableResolver = TableResolver(),
): PreviewState {
    val scopeState = buildScopeState(
        input = CaptureScopeInput(
            templateState = input.templateState,
            captureNow = input.captureNow,
            counterDigits = input.counterDigits,
            dateFormat = input.dateFormat,
            timeFormat = input.timeFormat,
            fnDelim = input.fnDelim,
            includeFilenameInCounterScope = input.includeFilenameInCounterScope,
            saveMode = input.saveMode,
            phraseProgressCursor = input.phraseProgressCursor,
            overrideCells = input.overrideCells,
            selectedPhraseTextByCellIdOverride = input.selectedPhraseTextByCellIdOverride,
        ),
        tableResolver = tableResolver,
    )

    // buildPreviewState는 외부 계약 유지: sync된 counter가 들어오면 plan도 동일 counter 기준으로 맞춘다.
    val previewPlan = if (input.scopeNextCounter != null) {
        tableResolver.plan(
            cells = scopeState.effectiveCells,
            captureNow = input.captureNow,
            config = TableResolver.Config(
                counterDigits = input.counterDigits,
                dateFormat = input.dateFormat,
                timeFormat = input.timeFormat,
            ),
            counterSeedOverride = input.scopeNextCounter.coerceAtLeast(1),
            selectedPhraseTextByCellId = scopeState.selectedPhraseTextByCellId,
        )
    } else {
        scopeState.plan
    }

    val previewNaming = CaptureNamingPolicy.buildForCaptureWithCounter(
        captureContext = CaptureContext(
            resolvedCells = previewPlan.resolvedCells,
            captureNow = input.captureNow,
            fileNameSlotDrafts = input.templateState.fileNameSlotDrafts,
            pathSlotDrafts = input.templateState.pathSlotDrafts,
            fnDelim = input.fnDelim,
            counterDigits = input.counterDigits,
            dateFormat = input.dateFormat,
            timeFormat = input.timeFormat,
            includePathInCounterScope = input.includePathInCounterScope,
            includeFilenameInCounterScope = input.includeFilenameInCounterScope,
            dateScopeValues = scopeState.scopeValues.dateScopeValues,
            timeScopeValues = scopeState.scopeValues.timeScopeValues,
            phraseScopeValues = scopeState.scopeValues.phraseScopeValues,
            saveMode = input.saveMode,
        ),
        usedCounter = input.scopeNextCounter,
    )

    return PreviewState(
        selectedPhraseTextByCellId = scopeState.selectedPhraseTextByCellId,
        plan = previewPlan,
        scopeValues = scopeState.scopeValues,
        previewNaming = previewNaming,
    )
}

internal fun buildScopeState(
    input: CaptureScopeInput,
    tableResolver: TableResolver = TableResolver(),
): CaptureScopeState {
    val effectiveCells = input.overrideCells ?: input.templateState.cells
    val selectedPhraseTextByCellId = input.selectedPhraseTextByCellIdOverride ?: PhraseResolver.resolveSelectedTextByCellId(
        cells = effectiveCells,
        phraseSets = input.templateState.phraseSets,
        progressCursor = input.phraseProgressCursor,
    )

    // pre-sync 단계: 카운터 seed 없이 scope 계산에 필요한 table plan만 확정한다.
    val plan = tableResolver.plan(
        cells = effectiveCells,
        captureNow = input.captureNow,
        config = TableResolver.Config(
            counterDigits = input.counterDigits,
            dateFormat = input.dateFormat,
            timeFormat = input.timeFormat,
        ),
        counterSeedOverride = null,
        selectedPhraseTextByCellId = selectedPhraseTextByCellId,
    )

    val fileNameCellSlots = deriveFileNameCellSlotsFromDrafts(input.templateState.fileNameSlotDrafts)
    val fileNameCellIds = fileNameCellSlots.filterNotNull().toSet()
    val isPerPhraseMode = effectiveCells.any { cell ->
        cell.dataType == TableCellDataType.ROTATING_TEXT &&
            cell.rotatingCounterMode == RotatingCounterMode.PER_PHRASE &&
            cell.cellId in fileNameCellIds
    }
    val scopeValues = CounterScopeResolver.resolve(
        CounterScopeResolver.Inputs(
            cells = effectiveCells,
            fileNameSlots = fileNameCellSlots,
            resolvedCells = plan.resolvedCells,
            isPerPhraseMode = isPerPhraseMode,
        )
    )

    val scopeNaming = CaptureNamingPolicy.buildForCaptureWithCounter(
        captureContext = CaptureContext(
            resolvedCells = plan.resolvedCells,
            captureNow = input.captureNow,
            fileNameSlotDrafts = input.templateState.fileNameSlotDrafts,
            pathSlotDrafts = input.templateState.pathSlotDrafts,
            fnDelim = input.fnDelim,
            counterDigits = input.counterDigits,
            dateFormat = input.dateFormat,
            timeFormat = input.timeFormat,
            includeFilenameInCounterScope = input.includeFilenameInCounterScope,
            includePathInCounterScope = true,
            dateScopeValues = scopeValues.dateScopeValues,
            timeScopeValues = scopeValues.timeScopeValues,
            phraseScopeValues = scopeValues.phraseScopeValues,
            saveMode = input.saveMode,
        ),
        usedCounter = null,
    )

    return CaptureScopeState(
        effectiveCells = effectiveCells,
        selectedPhraseTextByCellId = selectedPhraseTextByCellId,
        plan = plan,
        scopeValues = scopeValues,
        counterScope = scopeNaming.counterScope,
        scanPrefix = scopeNaming.scanPrefix,
        preSyncDisplayName = scopeNaming.displayName,
        relativePathPreview = scopeNaming.relativePath,
    )
}

internal fun buildCapturePreview(
    scopeState: CaptureScopeState,
    input: FinalCapturePreviewInput,
    tableResolver: TableResolver = TableResolver(),
): FinalCapturePreview {
    // 핵심 정책: post-sync final preview는 synced counter를 반영한 final plan을 다시 계산한다.
    // 이로써 displayName / resolvedCells / tablePatch가 같은 기준 counter를 사용한다.
    val finalPlan = tableResolver.plan(
        cells = scopeState.effectiveCells,
        captureNow = input.captureNow,
        config = TableResolver.Config(
            counterDigits = input.counterDigits,
            dateFormat = input.dateFormat,
            timeFormat = input.timeFormat,
        ),
        counterSeedOverride = input.syncedCounter.coerceAtLeast(1),
        selectedPhraseTextByCellId = scopeState.selectedPhraseTextByCellId,
    )

    val previewNaming = CaptureNamingPolicy.buildForCaptureWithCounter(
        captureContext = CaptureContext(
            resolvedCells = finalPlan.resolvedCells,
            captureNow = input.captureNow,
            fileNameSlotDrafts = input.templateState.fileNameSlotDrafts,
            pathSlotDrafts = input.templateState.pathSlotDrafts,
            fnDelim = input.fnDelim,
            counterDigits = input.counterDigits,
            dateFormat = input.dateFormat,
            timeFormat = input.timeFormat,
            includePathInCounterScope = input.includePathInCounterScope,
            includeFilenameInCounterScope = input.includeFilenameInCounterScope,
            dateScopeValues = scopeState.scopeValues.dateScopeValues,
            timeScopeValues = scopeState.scopeValues.timeScopeValues,
            phraseScopeValues = scopeState.scopeValues.phraseScopeValues,
            saveMode = input.saveMode,
        ),
        usedCounter = input.syncedCounter.coerceAtLeast(1),
    )

    return FinalCapturePreview(
        resolvedCells = finalPlan.resolvedCells,
        tablePatch = finalPlan.patch,
        displayName = previewNaming.displayName,
        usedCounter = requireNotNull(previewNaming.usedCounter),
        // 정책 정리: 문구 진행은 sync 이후 최종 preview가 확정된 뒤에만 다음 커서를 계산한다.
        nextPhraseProgressCursor = input.phraseProgressCursor.coerceAtLeast(1) + 1,
        counterScope = previewNaming.counterScope,
        relativePathPreview = previewNaming.relativePath,
    )
}
