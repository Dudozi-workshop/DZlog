package com.example.dzlog.domain.capturepolicy

import com.example.dzlog.domain.counter.CounterStreamContext
import com.example.dzlog.domain.counter.buildCounterStreamContext
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.naming.buildDisplayNameFromResolvedCells
import com.example.dzlog.domain.naming.buildGalleryRelativePath
import com.example.dzlog.domain.naming.resolveGroupValue
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
     * - 현재는 기존 buildDisplayNameFromResolvedCells를 그대로 래핑(동작 불변).
     */
    internal fun buildDisplayNameForCounter(
        resolvedCells: List<com.example.dzlog.domain.table.ResolvedCell>,
        fnDelim: String,
        usedCounter: Int,
        now: Date,
        includeDate: Boolean = false,
        includeTime: Boolean = false
    ): String {
        return buildDisplayNameFromResolvedCells(
            resolvedCells = resolvedCells,
            fnDelim = fnDelim,
            includeDate = includeDate,
            includeTime = includeTime,
            counterOverride = usedCounter,
            now = now
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
        val streamContext = buildCounterStreamContext(
            resolvedCells = resolvedCells,
            nextCounter = usedCounter,
            isManualMode = false,
            fnDelim = captureContext.fnDelim
        )
        val g1 = resolveGroupValue(resolvedCells, GroupLevel.G1)
        val g2 = resolveGroupValue(resolvedCells, GroupLevel.G2)
        val baseRelativePath = buildGalleryRelativePath(g1, g2)

        val displayName = buildDisplayNameForCounter(
            resolvedCells = resolvedCells,
            fnDelim = captureContext.fnDelim,
            usedCounter = usedCounter,
            now = Date()
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
