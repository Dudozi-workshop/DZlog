package com.dudoziworkshop.dzlog.ui.camera.state

import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.counter.buildScopedCounter
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.deriveFileNameCellSlotsFromDrafts
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.CaptureScopeInput
import com.dudoziworkshop.dzlog.domain.preview.CaptureScopeState
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildCapturePreview
import com.dudoziworkshop.dzlog.domain.preview.buildScope
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequest
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import java.util.Date

internal data class CameraDerivedState(
    val captureScopeState: CaptureScopeState,
    val scopedCounterStream: CaptureScopedCounterStream,
    val finalCapturePreview: FinalCapturePreview?,
    val displayCounter: Int?,
    val topDisplayName: String,
    val isTemplateReady: Boolean,
)

internal fun computeCameraDerivedState(
    tableTemplateState: TableTemplateState,
    now: Date,
    counterDigits: Int,
    phraseProgressCounter: Int,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
    saveMode: SaveMode,
    syncedNextCounter: Int?,
    syncedRequest: CounterRequest?,
    tableResolver: TableResolver,
): CameraDerivedState {
    val fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
    val dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT
    val timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT

    val captureScopeState = buildScope(
        input = CaptureScopeInput(
            templateState = tableTemplateState,
            captureNow = now,
            counterDigits = counterDigits,
            dateFormat = dateFormat,
            timeFormat = timeFormat,
            fnDelim = fnDelim,
            includeFilenameInCounterScope = includeFilenameInCounterScope,
            saveMode = saveMode,
            phraseProgressCursor = phraseProgressCounter,
        ),
        tableResolver = tableResolver,
    )

    val counterScope = captureScopeState.counterScope

    val scopedCounterStream = buildScopedCounter(
        counterScope = counterScope,
        includePathInScope = includePathInCounterScope,
        includeFilenameInScope = includeFilenameInCounterScope,
        scanPrefix = captureScopeState.scanPrefix,
    )

    val finalCapturePreview: FinalCapturePreview? = run {
        val currentRequest = CounterRequestResolver.fromCamera(
            saveMode = saveMode,
            relativePathKey = counterScope.relativePathKey,
            prefix = counterScope.streamPrefix,
            scanPrefix = captureScopeState.scanPrefix,
            includePathInScope = includePathInCounterScope,
            includeFilenameInScope = includeFilenameInCounterScope,
        )
        // Never reuse the previous folder/name/mode's seed while a new read is pending.
        if (syncedRequest != currentRequest) return@run null
        val syncedCounter = syncedNextCounter ?: return@run null
        buildCapturePreview(
            scopeState = captureScopeState,
            input = FinalCapturePreviewInput(
                templateState = tableTemplateState,
                captureNow = now,
                counterDigits = counterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
                fnDelim = fnDelim,
                includePathInCounterScope = includePathInCounterScope,
                includeFilenameInCounterScope = includeFilenameInCounterScope,
                saveMode = saveMode,
                syncedCounter = syncedCounter,
                phraseProgressCursor = phraseProgressCounter,
            ),
            tableResolver = tableResolver,
        )
    }

    val displayCounter = finalCapturePreview?.usedCounter
    val topDisplayName = finalCapturePreview?.displayName ?: captureScopeState.preSyncDisplayName

    val hasTemplateCells = tableTemplateState.cells.isNotEmpty()
    val hasAnyFilenameSlot = deriveFileNameCellSlotsFromDrafts(tableTemplateState.fileNameSlotDrafts).any { it != null }
    val allFilenameSlotsOff = deriveFileNameCellSlotsFromDrafts(tableTemplateState.fileNameSlotDrafts).all { it == null }
    val isTemplateReady = hasTemplateCells && (
        !includeFilenameInCounterScope ||
            allFilenameSlotsOff ||
            hasAnyFilenameSlot
        )

    return CameraDerivedState(
        captureScopeState = captureScopeState,
        scopedCounterStream = scopedCounterStream,
        finalCapturePreview = finalCapturePreview,
        displayCounter = displayCounter,
        topDisplayName = topDisplayName,
        isTemplateReady = isTemplateReady,
    )
}

