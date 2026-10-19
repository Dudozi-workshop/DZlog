package com.dudoziworkshop.dzlog.domain.preview

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.time.asCaptureTime
import java.util.Date
import com.dudoziworkshop.dzlog.domain.table.TableResolver

fun PreviewInput(
    templateState: TableTemplateState,
    captureNow: Date,
    counterDigits: Int,
    dateFormat: String,
    timeFormat: String,
    fnDelim: String,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
    saveMode: SaveMode,
    scopeNextCounter: Int?,
    phraseProgressCursor: Int,
    overrideCells: List<TableCellState>? = null,
    selectedPhraseTextByCellIdOverride: Map<String, String>? = null,
): PreviewInput = PreviewInput(
    templateState = templateState,
    captureNow = captureNow.asCaptureTime(),
    counterDigits = counterDigits,
    dateFormat = dateFormat,
    timeFormat = timeFormat,
    fnDelim = fnDelim,
    includePathInCounterScope = includePathInCounterScope,
    includeFilenameInCounterScope = includeFilenameInCounterScope,
    saveMode = saveMode,
    scopeNextCounter = scopeNextCounter,
    phraseProgressCursor = phraseProgressCursor,
    overrideCells = overrideCells,
    selectedPhraseTextByCellIdOverride = selectedPhraseTextByCellIdOverride,
)

fun CaptureScopeInput(
    templateState: TableTemplateState,
    captureNow: Date,
    counterDigits: Int,
    dateFormat: String,
    timeFormat: String,
    fnDelim: String,
    includeFilenameInCounterScope: Boolean,
    saveMode: SaveMode,
    phraseProgressCursor: Int,
    overrideCells: List<TableCellState>? = null,
    selectedPhraseTextByCellIdOverride: Map<String, String>? = null,
): CaptureScopeInput = CaptureScopeInput(
    templateState = templateState,
    captureNow = captureNow.asCaptureTime(),
    counterDigits = counterDigits,
    dateFormat = dateFormat,
    timeFormat = timeFormat,
    fnDelim = fnDelim,
    includeFilenameInCounterScope = includeFilenameInCounterScope,
    saveMode = saveMode,
    phraseProgressCursor = phraseProgressCursor,
    overrideCells = overrideCells,
    selectedPhraseTextByCellIdOverride = selectedPhraseTextByCellIdOverride,
)

fun FinalCapturePreviewInput(
    templateState: TableTemplateState,
    captureNow: Date,
    counterDigits: Int,
    dateFormat: String,
    timeFormat: String,
    fnDelim: String,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
    saveMode: SaveMode,
    syncedCounter: Int,
    phraseProgressCursor: Int,
): FinalCapturePreviewInput = FinalCapturePreviewInput(
    templateState = templateState,
    captureNow = captureNow.asCaptureTime(),
    counterDigits = counterDigits,
    dateFormat = dateFormat,
    timeFormat = timeFormat,
    fnDelim = fnDelim,
    includePathInCounterScope = includePathInCounterScope,
    includeFilenameInCounterScope = includeFilenameInCounterScope,
    saveMode = saveMode,
    syncedCounter = syncedCounter,
    phraseProgressCursor = phraseProgressCursor,
)


// Required second argument avoids ambiguity with the common default resolver.
fun buildPreview(input: PreviewInput, tableResolver: TableResolver): PreviewState =
    buildPreview(input, tableResolver.commonResolver)
fun buildScope(input: CaptureScopeInput, tableResolver: TableResolver): CaptureScopeState =
    buildScope(input, tableResolver.commonResolver)
fun buildCapturePreview(scopeState: CaptureScopeState, input: FinalCapturePreviewInput,
    tableResolver: TableResolver): FinalCapturePreview = buildCapturePreview(scopeState, input, tableResolver.commonResolver)
