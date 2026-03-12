package com.dudoziworkshop.dzlog.domain.preview

import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureContext
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.dudoziworkshop.dzlog.domain.counter.CounterScopeResolver
import com.dudoziworkshop.dzlog.domain.model.RotatingCounterMode
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
    val scopeNextCounter: Int?,
    val phraseProgressCursor: Int,
    val overrideCells: List<TableCellState>? = null,
    val selectedPhraseTextByCellIdOverride: Map<String, String>? = null,
)

internal data class PreviewState(
    val selectedPhraseTextByCellId: Map<String, String>,
    val plan: ResolvePlan,
    val scopeValues: CounterScopeResolver.Result,
    val previewNaming: CaptureNamingPolicy.Result,
) {
    // 이전 이름 호환: 점진 이전 단계 동안 기존 호출부를 유지한다.
    val namingPreview: CaptureNamingPolicy.Result get() = previewNaming
}

/**
 * 화면별 Preview 계산 경로를 단일화하기 위한 공용 state builder.
 *
 * 핵심 진입점: buildPreviewState(PreviewInput)
 * (아래 buildPreviewPipeline은 legacy 호환 래퍼다.)
 *
 * 순서(고정):
 * 1) selectedPhraseTextByCellId
 * 2) TableResolver.plan
 * 3) CounterScopeResolver.resolve
 * 4) CaptureNamingPolicy.buildForCaptureWithCounter
 */
internal fun buildPreviewState(
    input: PreviewInput,
    tableResolver: TableResolver = TableResolver(),
): PreviewState {
    val effectiveCells = input.overrideCells ?: input.templateState.cells
    val selectedPhraseTextByCellId = input.selectedPhraseTextByCellIdOverride ?: PhraseResolver.resolveSelectedTextByCellId(
        cells = effectiveCells,
        phraseSets = input.templateState.phraseSets,
        progressCursor = input.phraseProgressCursor,
    )

    val plan = tableResolver.plan(
        cells = effectiveCells,
        captureNow = input.captureNow,
        config = TableResolver.Config(
            counterDigits = input.counterDigits,
            dateFormat = input.dateFormat,
            timeFormat = input.timeFormat,
        ),
        counterSeedOverride = input.scopeNextCounter,
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

    val previewNaming = CaptureNamingPolicy.buildForCaptureWithCounter(
        captureContext = CaptureContext(
            resolvedCells = plan.resolvedCells,
            captureNow = input.captureNow,
            fileNameSlotDrafts = input.templateState.fileNameSlotDrafts,
            pathSlotDrafts = input.templateState.pathSlotDrafts,
            fnDelim = input.fnDelim,
            counterDigits = input.counterDigits,
            dateFormat = input.dateFormat,
            timeFormat = input.timeFormat,
            includePathInCounterScope = input.includePathInCounterScope,
            includeFilenameInCounterScope = input.includeFilenameInCounterScope,
            dateScopeValues = scopeValues.dateScopeValues,
            timeScopeValues = scopeValues.timeScopeValues,
            phraseScopeValues = scopeValues.phraseScopeValues,
        ),
        // 미동기화(null) 상태에서는 preview builder가 counter를 임시 확정하지 않는다.
        usedCounter = input.scopeNextCounter,
    )

    return PreviewState(
        selectedPhraseTextByCellId = selectedPhraseTextByCellId,
        plan = plan,
        scopeValues = scopeValues,
        previewNaming = previewNaming,
    )
}

// legacy 호환 별칭: 외부 호출부 점진 이전용
internal typealias PreviewPipelineInput = PreviewInput
internal typealias PreviewPipelineResult = PreviewState

// legacy 호환 래퍼: 신규 코드는 buildPreviewState 사용
internal fun buildPreviewPipeline(
    input: PreviewPipelineInput,
    tableResolver: TableResolver = TableResolver(),
): PreviewPipelineResult = buildPreviewState(input, tableResolver)
