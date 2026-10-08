package com.dudoziworkshop.dzlog.feature.counter.table

import android.content.Context
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.feature.counter.core.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequest
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import java.util.Date

internal data class SaveSettingsCounterState(
    val next: Int,
    val usesAutoNext: Boolean,
)

internal object SaveSettingsCounterController {

    suspend fun readCurrent(
        context: Context,
        templateState: TableTemplateState,
        saveMode: SaveMode,
        counterPadding: Int,
        includePathInScope: Boolean,
        includeFilenameInScope: Boolean,
    ): SaveSettingsCounterState {
        val request = buildRequest(
            templateState = templateState,
            saveMode = saveMode,
            counterPadding = counterPadding,
            includePathInScope = includePathInScope,
            includeFilenameInScope = includeFilenameInScope,
        )
        val read = facade(context, counterPadding).read(request)
        return SaveSettingsCounterState(
            next = read.next.coerceAtLeast(1),
            usesAutoNext = !read.hasManualOverride,
        )
    }

    suspend fun readSavedImageNext(
        context: Context,
        templateState: TableTemplateState,
        saveMode: SaveMode,
        counterPadding: Int,
        includePathInScope: Boolean,
        includeFilenameInScope: Boolean,
    ): Int {
        val request = buildRequest(
            templateState = templateState,
            saveMode = saveMode,
            counterPadding = counterPadding,
            includePathInScope = includePathInScope,
            includeFilenameInScope = includeFilenameInScope,
        )
        return facade(context, counterPadding).readAutoNext(request).coerceAtLeast(1)
    }

    suspend fun applyNext(
        context: Context,
        templateState: TableTemplateState,
        saveMode: SaveMode,
        counterPadding: Int,
        includePathInScope: Boolean,
        includeFilenameInScope: Boolean,
        next: Int,
        usesAutoNext: Boolean,
    ) {
        val request = buildRequest(
            templateState = templateState,
            saveMode = saveMode,
            counterPadding = counterPadding,
            includePathInScope = includePathInScope,
            includeFilenameInScope = includeFilenameInScope,
        )
        val counterFacade = facade(context, counterPadding)
        if (usesAutoNext) {
            counterFacade.clearManualNext(request)
        } else {
            counterFacade.setManualNext(request, next.coerceAtLeast(1))
        }
    }

    private fun facade(context: Context, counterPadding: Int): CounterFacade =
        CounterFacade(
            context = context.applicationContext,
            counterDigits = counterPadding,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
        )

    private fun buildRequest(
        templateState: TableTemplateState,
        saveMode: SaveMode,
        counterPadding: Int,
        includePathInScope: Boolean,
        includeFilenameInScope: Boolean,
    ): CounterRequest {
        val preview = buildPreview(
            PreviewInput(
                templateState = templateState,
                captureNow = Date(),
                counterDigits = counterPadding,
                dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
                fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                includePathInCounterScope = includePathInScope,
                includeFilenameInCounterScope = includeFilenameInScope,
                saveMode = saveMode,
                scopeNextCounter = 1,
                phraseProgressCursor = 1,
            )
        )
        return CounterRequestResolver.fromTable(
            saveMode = saveMode,
            relativePathKey = preview.previewNaming.counterScope.relativePathKey,
            prefix = preview.previewNaming.counterScope.streamPrefix,
            scanPrefix = preview.previewNaming.scanPrefix,
            includePathInScope = includePathInScope,
            includeFilenameInScope = includeFilenameInScope,
            tableTemplateId = null,
        )
    }
}
