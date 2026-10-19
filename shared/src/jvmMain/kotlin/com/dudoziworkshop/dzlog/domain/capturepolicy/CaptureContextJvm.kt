package com.dudoziworkshop.dzlog.domain.capturepolicy

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.time.asCaptureTime
import java.util.Date

fun CaptureContext(
    resolvedCells: List<ResolvedCell>,
    captureNow: Date,
    fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    pathSlotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String,
    counterDigits: Int,
    dateFormat: String,
    timeFormat: String,
    includeFilenameInCounterScope: Boolean,
    saveMode: SaveMode,
    dateScopeValues: List<String> = emptyList(),
    timeScopeValues: List<String> = emptyList(),
    phraseScopeValues: List<String> = emptyList(),
): CaptureContext = CaptureContext(
    resolvedCells = resolvedCells,
    captureNow = captureNow.asCaptureTime(),
    fileNameSlotDrafts = fileNameSlotDrafts,
    pathSlotDrafts = pathSlotDrafts,
    fnDelim = fnDelim,
    counterDigits = counterDigits,
    dateFormat = dateFormat,
    timeFormat = timeFormat,
    includeFilenameInCounterScope = includeFilenameInCounterScope,
    saveMode = saveMode,
    dateScopeValues = dateScopeValues,
    timeScopeValues = timeScopeValues,
    phraseScopeValues = phraseScopeValues,
)

fun CaptureNamingPolicy.buildDisplayNameForCounter(
    resolvedCells: List<ResolvedCell>, fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String, counterDigits: Int, usedCounter: Int?, now: Date,
    dateFormat: String, timeFormat: String,
): String = buildDisplayNameForCounter(resolvedCells, fileNameSlotDrafts, fnDelim,
    counterDigits, usedCounter, now.asCaptureTime(), dateFormat, timeFormat)
