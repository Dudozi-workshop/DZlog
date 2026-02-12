package com.example.dzlog.domain.capturepolicy

import android.content.Context
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
     * 촬영 시 필요한 naming/counter/path를 한 번에 산출한다.
     *
     * 정책(현재 상태 유지):
     * - relativePath(물리 저장경로)는 (G1, G2 값)으로 계산한다.
     * - counter stream key는 "G2 그룹 활성+빈값"을 별도 스트림으로 분리한다.
     * - displayName suffix 카운터는 "단일 소스(usedCounter)"를 우선한다.
     */
    internal suspend fun buildForCapture(
        appContext: Context,
        captureContext: CaptureContext,
        counterDigits: Int
    ): Result {
        val resolvedCells = captureContext.resolvedCells
        // 1) 물리 저장경로(relativePath)
        val g1 = resolveGroupValue(resolvedCells, GroupLevel.G1)
        val g2 = resolveGroupValue(resolvedCells, GroupLevel.G2)
        val baseRelativePath = buildGalleryRelativePath(g1, g2)

        // 2) 스트림 키(단일 컨텍스트)
        val streamContext = buildCounterStreamContext(
            resolvedCells = resolvedCells,
            nextCounter = 1,
            isManualMode = false,
            fnDelim = captureContext.fnDelim
        )
        // 4) 단일 소스 counter 산출
        val usedCounter = CaptureCounterPolicy.getNextCounter(
            context = appContext,
            streamContext = streamContext,
            counterDigits = counterDigits,
            fnDelim = captureContext.fnDelim
        )

        // 5) 파일명 산출 (suffix 카운터는 usedCounter 우선)
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
}
