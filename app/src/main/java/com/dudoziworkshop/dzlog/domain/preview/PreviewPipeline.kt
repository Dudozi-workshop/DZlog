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

internal data class PreviewPipelineInput(
    val templateState: TableTemplateState,
    val captureNow: Date,
    val counterDigits: Int,
    val dateFormat: String,
    val timeFormat: String,
    val fnDelim: String,
    val includePathInCounterScope: Boolean,
    val includeFilenameInCounterScope: Boolean,
    val scopeNextCounter: Int,
    val phraseProgressCursor: Int,
    val overrideCells: List<TableCellState>? = null,
    val selectedPhraseTextByCellIdOverride: Map<String, String>? = null,
)

internal data class PreviewPipelineResult(
    val selectedPhraseTextByCellId: Map<String, String>,
    val plan: ResolvePlan,
    val scopeValues: CounterScopeResolver.Result,
    val namingPreview: CaptureNamingPolicy.Result,
)

/**
 * 화면별 Preview 계산 경로를 단일화하기 위한 공용 pipeline helper.
 *
 * 순서(고정):
 * 1) selectedPhraseTextByCellId
 * 2) TableResolver.plan
 * 3) CounterScopeResolver.resolve
 * 4) CaptureNamingPolicy.buildForCaptureWithCounter
 */
internal fun buildPreviewPipeline(
    input: PreviewPipelineInput,
    tableResolver: TableResolver = TableResolver(),
): PreviewPipelineResult {
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

    val namingPreview = CaptureNamingPolicy.buildForCaptureWithCounter(
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
        usedCounter = input.scopeNextCounter,
    )

    return PreviewPipelineResult(
        selectedPhraseTextByCellId = selectedPhraseTextByCellId,
        plan = plan,
        scopeValues = scopeValues,
        namingPreview = namingPreview,
    )
}
