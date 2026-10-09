package com.dudoziworkshop.dzlog.domain.capturepolicy

import com.dudoziworkshop.dzlog.domain.counter.RotatingFilenamePolicy
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.counter.CounterScopeOptions
import com.dudoziworkshop.dzlog.domain.counter.buildCounterScope
import com.dudoziworkshop.dzlog.domain.naming.buildCounterPath
import com.dudoziworkshop.dzlog.domain.naming.buildCounterScanPrefix
import com.dudoziworkshop.dzlog.domain.naming.buildFileName
import com.dudoziworkshop.dzlog.domain.naming.buildSavePath
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
        val counterScope: CounterScope,
        val scanPrefix: String,
        val relativePath: String,
        val displayName: String,
        val usedCounter: Int?
    )

    /**
     * displayName 생성 정책(순수 함수)
     *
     * - 프리뷰/촬영/표 프리뷰 등 "파일명 문자열"이 필요한 호출부가
     *   NamePathBuilders를 직접 호출하지 않고 정책 파일만 참조하도록 만든다.
     * - draft 기반 공용 builder(buildFileName)를 호출해 모든 화면과 동일 규칙을 사용한다.
     */
    internal fun buildDisplayNameForCounter(
        resolvedCells: List<com.dudoziworkshop.dzlog.domain.table.ResolvedCell>,
        fileNameSlotDrafts: List<com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft?>,
        fnDelim: String,
        counterDigits: Int,
        usedCounter: Int?,
        now: Date,
        dateFormat: String,
        timeFormat: String,
    ): String {
        return buildFileName(
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
        usedCounter: Int?
    ): Result {
        val resolvedCells = captureContext.resolvedCells
        val baseRelativePath = buildSavePath(
            resolvedCells = resolvedCells,
            pathSlotDrafts = captureContext.pathSlotDrafts,
            now = captureContext.captureNow,
            dateFormat = captureContext.dateFormat,
            timeFormat = captureContext.timeFormat,
        )
        // CounterScope는 키 계산 모델이라 nextCounter는 nullable을 받지 않는다.
        // 미동기화(null) 상태에서는 최소값(1)을 내부 모델 값으로만 유지하고,
        // 실제 표시(displayName suffix)는 nullable usedCounter 정책으로 분리한다.
        val normalizedScopeCounter = usedCounter?.coerceAtLeast(1) ?: 1
        val scanPrefix = buildCounterScanPrefix(
            resolvedCells = resolvedCells,
            fileNameSlotDrafts = captureContext.fileNameSlotDrafts,
            fnDelim = captureContext.fnDelim,
            now = captureContext.captureNow,
            dateFormat = captureContext.dateFormat,
            timeFormat = captureContext.timeFormat,
        )
        // 주요 정책(촬영모드별 카운터 스트림 경로):
        // - WATERMARK_ONLY/BOTH는 baseRelativePath(Pictures/DZlog/...)를 공유
        // - ORIGINAL_ONLY는 original/ 하위 경로를 스트림 기준으로 분리
        val counterStreamPath = buildCounterPath(
            baseRelativePath = baseRelativePath,
            saveMode = captureContext.saveMode,
        )
        val counterScope = buildCounterScope(
            resolvedCells = resolvedCells,
            fileNameSlots = captureContext.fileNameCellSlots,
            nextCounter = normalizedScopeCounter,
            isManualMode = false,
            fnDelim = captureContext.fnDelim,
            includeFilenameInScope = captureContext.includeFilenameInCounterScope,
            scopeOptions = CounterScopeOptions(
                rotatingPolicyScopeValues = RotatingFilenamePolicy.scopeValues(captureContext.fileNameSlotDrafts, resolvedCells),
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
            relativePathOverride = counterStreamPath,
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
            counterScope = counterScope,
            scanPrefix = scanPrefix,
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

