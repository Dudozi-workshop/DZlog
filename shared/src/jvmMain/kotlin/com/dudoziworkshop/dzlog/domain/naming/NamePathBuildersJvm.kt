package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.time.asCaptureTime
import java.util.Date

/** Source-compatible Date entry points. All naming decisions run in commonMain. */
fun resolveFileNameScopeTokensFromDrafts(
    fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    resolvedCells: List<ResolvedCell>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): List<String> = resolveFileNameScopeTokensFromDrafts(
    fileNameSlotDrafts = fileNameSlotDrafts,
    resolvedCells = resolvedCells,
    now = now.asCaptureTime(),
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)

fun resolveFileNameDraftToken(
    draft: TableEditorSlotDraft?,
    resolvedCells: List<ResolvedCell>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String = resolveFileNameDraftToken(
    draft = draft,
    resolvedCells = resolvedCells,
    now = now.asCaptureTime(),
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)

fun resolvePathDraftToken(
    draft: TableEditorSlotDraft?,
    resolvedCells: List<ResolvedCell>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String = resolvePathDraftToken(
    draft = draft,
    resolvedCells = resolvedCells,
    now = now.asCaptureTime(),
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)

fun buildCounterScanPrefix(
    resolvedCells: List<ResolvedCell>,
    fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String = buildCounterScanPrefix(
    resolvedCells = resolvedCells,
    fileNameSlotDrafts = fileNameSlotDrafts,
    fnDelim = fnDelim,
    now = now.asCaptureTime(),
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)

fun buildFileNamePrefix(
    resolvedCells: List<ResolvedCell>,
    fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String = buildFileNamePrefix(
    resolvedCells = resolvedCells,
    fileNameSlotDrafts = fileNameSlotDrafts,
    fnDelim = fnDelim,
    now = now.asCaptureTime(),
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)

fun buildFileName(
    resolvedCells: List<ResolvedCell>,
    fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String,
    counterDigits: Int,
    usedCounter: Int?,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String = buildFileName(
    resolvedCells = resolvedCells,
    fileNameSlotDrafts = fileNameSlotDrafts,
    fnDelim = fnDelim,
    counterDigits = counterDigits,
    usedCounter = usedCounter,
    now = now.asCaptureTime(),
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)

fun buildSavePath(
    resolvedCells: List<ResolvedCell>,
    pathSlotDrafts: List<TableEditorSlotDraft?>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String = buildSavePath(
    resolvedCells = resolvedCells,
    pathSlotDrafts = pathSlotDrafts,
    now = now.asCaptureTime(),
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)

fun buildDisplayNameFromSlotDrafts(
    resolvedCells: List<ResolvedCell>,
    fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String,
    counterDigits: Int,
    usedCounter: Int?,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String = buildDisplayNameFromSlotDrafts(
    resolvedCells = resolvedCells,
    fileNameSlotDrafts = fileNameSlotDrafts,
    fnDelim = fnDelim,
    counterDigits = counterDigits,
    usedCounter = usedCounter,
    now = now.asCaptureTime(),
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)

fun buildGalleryRelativePathFromSlotDrafts(
    resolvedCells: List<ResolvedCell>,
    pathSlotDrafts: List<TableEditorSlotDraft?>,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): String = buildGalleryRelativePathFromSlotDrafts(
    resolvedCells = resolvedCells,
    pathSlotDrafts = pathSlotDrafts,
    now = now.asCaptureTime(),
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)

fun buildNamingCellPreview(
    isFileName: Boolean,
    selectedCellId: String?,
    resolvedCells: List<ResolvedCell>,
    slotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String,
    counterDigits: Int,
    usedCounter: Int?,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): NamingCellPreview = buildNamingCellPreview(
    isFileName = isFileName,
    selectedCellId = selectedCellId,
    resolvedCells = resolvedCells,
    slotDrafts = slotDrafts,
    fnDelim = fnDelim,
    counterDigits = counterDigits,
    usedCounter = usedCounter,
    now = now.asCaptureTime(),
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)

fun buildNamingSlotPreview(
    isFileName: Boolean,
    slotIndex: Int,
    resolvedCells: List<ResolvedCell>,
    slotDrafts: List<TableEditorSlotDraft?>,
    fnDelim: String,
    counterDigits: Int,
    usedCounter: Int?,
    now: Date,
    dateFormat: String,
    timeFormat: String,
): NamingSlotPreview = buildNamingSlotPreview(
    isFileName = isFileName,
    slotIndex = slotIndex,
    resolvedCells = resolvedCells,
    slotDrafts = slotDrafts,
    fnDelim = fnDelim,
    counterDigits = counterDigits,
    usedCounter = usedCounter,
    now = now.asCaptureTime(),
    dateFormat = dateFormat,
    timeFormat = timeFormat,
)
