package com.dudoziworkshop.dzlog.ui.camera.state

import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.phrase.ManualCaptureStepPolicy
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import java.util.Date

/**
 * Project a manual count offset into the NEXT shot without changing the actual
 * persisted capture/template state or the MediaStore-backed counter readback.
 *
 * The camera counter sync controller must keep using the unmodified base state.
 * The projected result is for display and the next shot's exact snapshot only.
 */
internal fun computeCameraManualStepPreview(
    base: CameraDerivedState,
    template: TableTemplateState,
    offset: Int,
    now: Date,
    counterDigits: Int,
    phraseProgressCounter: Int,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
    saveMode: SaveMode,
    tableResolver: TableResolver,
): CameraDerivedState {
    if (offset == 0) return base
    val storedNext = base.displayCounter ?: return base
    val adjustedTemplate = ManualCaptureStepPolicy.offsetTemplate(template, offset)
    // Compute the prospective filename/path scope independently. A phrase can
    // itself be part of a scope key, and must not mutate the real counter read.
    val scope = computeCameraDerivedState(
        tableTemplateState = adjustedTemplate,
        now = now,
        counterDigits = counterDigits,
        phraseProgressCounter = phraseProgressCounter,
        includePathInCounterScope = includePathInCounterScope,
        includeFilenameInCounterScope = includeFilenameInCounterScope,
        saveMode = saveMode,
        syncedNextCounter = null,
        syncedRequest = null,
        tableResolver = tableResolver,
    ).captureScopeState
    val request = CounterRequestResolver.fromCamera(
        saveMode = saveMode,
        relativePathKey = scope.counterScope.relativePathKey,
        prefix = scope.counterScope.streamPrefix,
        scanPrefix = scope.scanPrefix,
        includePathInScope = includePathInCounterScope,
        includeFilenameInScope = includeFilenameInCounterScope,
    )
    return computeCameraDerivedState(
        tableTemplateState = adjustedTemplate,
        now = now,
        counterDigits = counterDigits,
        phraseProgressCounter = phraseProgressCounter,
        includePathInCounterScope = includePathInCounterScope,
        includeFilenameInCounterScope = includeFilenameInCounterScope,
        saveMode = saveMode,
        syncedNextCounter = ManualCaptureStepPolicy.adjustNextCounter(storedNext, offset),
        syncedRequest = request,
        tableResolver = tableResolver,
    )
}
