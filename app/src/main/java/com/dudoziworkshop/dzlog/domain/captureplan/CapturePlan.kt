package com.dudoziworkshop.dzlog.domain.captureplan

import com.dudoziworkshop.dzlog.domain.counter.CounterStreamContext
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.table.TablePatch

/**
 * 프리뷰/저장 공용 캡처 계획.
 *
 * 정책:
 * - 프리뷰에서 계산한 결과를 저장 시 재계산 없이 그대로 사용한다.
 * - 클릭 시점에는 I/O(저장/commit)만 수행하고, 계산 로직은 CameraScreen에서 단일화한다.
 */
data class CapturePlan(
    val resolvedCells: List<ResolvedCell>,
    val tablePatch: TablePatch,
    val displayName: String,
    val usedCounter: Int,
    val nextPhraseProgressCursor: Int,
    val streamContext: CounterStreamContext,
    val relativePathPreview: String,
)
