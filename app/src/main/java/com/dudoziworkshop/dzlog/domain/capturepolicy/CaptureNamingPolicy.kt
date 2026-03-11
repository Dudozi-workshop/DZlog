package com.dudoziworkshop.dzlog.domain.capturepolicy

import com.dudoziworkshop.dzlog.domain.counter.CounterStreamContext
import com.dudoziworkshop.dzlog.domain.counter.CounterScopeOptions
import com.dudoziworkshop.dzlog.domain.counter.buildCounterStreamContext
import com.dudoziworkshop.dzlog.domain.naming.buildDisplayNameFromSlotDrafts
import com.dudoziworkshop.dzlog.domain.naming.buildGalleryRelativePathFromSlotDrafts
import com.dudoziworkshop.dzlog.domain.naming.resolveFileNameScopeTokensFromDrafts
import java.util.Date

/**
 * "카운터 · 저장경로 · 파일명"을 한 번에 결정하는 정책 진입점.
 *
 * Step 3.2:
 * - 기존 로직을 호출해 결과를 산출할 수 있게 구현함.
 * - 아직 UI/Repository에서 실제 호출은 하지 않는다(동작 불변).
 */
internal object CaptureNamingPolicy {

    internal data class Result(
        val streamContext: CounterStreamContext,
        val relativePath: String,
        val displayName: String,
        val usedCounter: Int
    )

    /**
     * displayName 생성 정책(순수 함수)
     *
     * - 프리뷰/촬영/표 프리뷰 등 "파일명 문자열"이 필요한 호출부가
     *   NamePathBuilders를 직접 호출하지 않고 정책 파일만 참조하도록 만든다.
     * - draft 기반 공용 builder(buildDisplayNameFromSlotDrafts)를 호출해 모든 화면과 동일 규칙을 사용한다.
     */
    internal fun buildDisplayNameForCounter(
        resolvedCells: List<com.dudoziworkshop.dzlog.domain.table.ResolvedCell>,
        fileNameSlotDrafts: List<com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft?>,
        fnDelim: String,
        counterDigits: Int,
        usedCounter: Int,
        now: Date,
        dateFormat: String,
        timeFormat: String,
    ): String {
        return buildDisplayNameFromSlotDrafts(
            resolvedCells = resolvedCells,
            fileNameSlotDrafts = fileNameSlotDrafts,
            fnDelim = fnDelim,
            counterDigits = counterDigits,
            usedCounter = usedCounter,
            now = now,
            dateFormat = dateFormat,
            timeFormat = timeFormat,
        )
    }


    /**
     * 촬영 시 필요한 naming/path를 한 번에 산출한다. (카운터 값은 호출부가 주입)
     */
    internal fun buildForCaptureWithCounter(
        captureContext: CaptureContext,
        usedCounter: Int
    ): Result {
        val resolvedCells = captureContext.resolvedCells
        val baseRelativePath = buildGalleryRelativePathFromSlotDrafts(
            resolvedCells = resolvedCells,
            pathSlotDrafts = captureContext.pathSlotDrafts,
            now = captureContext.captureNow,
            dateFormat = captureContext.dateFormat,
            timeFormat = captureContext.timeFormat,
        )
        val streamContext = buildCounterStreamContext(
            resolvedCells = resolvedCells,
            fileNameSlots = captureContext.fileNameCellSlots,
            nextCounter = usedCounter,
            isManualMode = false,
            fnDelim = captureContext.fnDelim,
            includeFilenameInScope = captureContext.includeFilenameInCounterScope,
            scopeOptions = CounterScopeOptions(
                dateScopeValues = captureContext.dateScopeValues,
                timeScopeValues = captureContext.timeScopeValues,
                phraseScopeValues = captureContext.phraseScopeValues,
                filenameDraftScopeValues = resolveFileNameScopeTokensFromDrafts(
                    fileNameSlotDrafts = captureContext.fileNameSlotDrafts,
                    resolvedCells = resolvedCells,
                    now = captureContext.captureNow,
                    dateFormat = captureContext.dateFormat,
                    timeFormat = captureContext.timeFormat,
                ),
            ),
            relativePathOverride = baseRelativePath,
        )
        val displayName = buildDisplayNameForCounter(
            resolvedCells = resolvedCells,
            fileNameSlotDrafts = captureContext.fileNameSlotDrafts,
            fnDelim = captureContext.fnDelim,
            counterDigits = captureContext.counterDigits,
            usedCounter = usedCounter,
            now = captureContext.captureNow,
            dateFormat = captureContext.dateFormat,
            timeFormat = captureContext.timeFormat,
        )

        return Result(
            streamContext = streamContext,
            relativePath = baseRelativePath,
            displayName = displayName,
            usedCounter = usedCounter
        )
    }

    /**
     * 저장된 displayName에서 실제 사용된 카운터를 역파싱한다.
     * - fnDelim 기준 마지막 토큰을 카운터로 간주
     * - counterDigits > 0 이면 자리수 검증 수행
     */
    internal fun parseUsedCounterFromDisplayName(
        displayName: String,
        fnDelim: String,
        counterDigits: Int
    ): Int? {
        val base = displayName.substringBeforeLast('.', displayName)
        val token = base.substringAfterLast(fnDelim, missingDelimiterValue = "").trim()
        if (token.isBlank() || token.any { !it.isDigit() }) return null
        if (counterDigits > 0 && token.length != counterDigits) return null
        return token.toIntOrNull()?.takeIf { it >= 0 }
    }

}
