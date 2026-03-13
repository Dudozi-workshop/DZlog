package com.dudoziworkshop.dzlog.domain.capturepolicy

import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.deriveFileNameCellSlotsFromDrafts
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import java.util.Date

/**
 * Capture-time context snapshot.
 *
 * 목적
 * - 저장경로(relativePath), 파일명(displayName), 카운터 스트림키(StreamKey) 산출 입력을 1곳으로 모음
 * - UI/Repository가 계산 로직에 직접 관여하지 않도록(SSOT) 경계를 만들기 위한 기반 타입
 *
 * 주의
 * - 카운터 숫자 결정권은 정책(CaptureCounterPolicy)에만 있어야 함
 * - COUNTER 셀 ON/OFF는 "표현"이며, 스트림/증가 로직에 영향을 주면 안 됨
 */
internal data class CaptureContext(
    val resolvedCells: List<ResolvedCell>,
    val captureNow: Date,
    val fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    val pathSlotDrafts: List<TableEditorSlotDraft?>,
    val fnDelim: String,
    val counterDigits: Int,
    val dateFormat: String,
    val timeFormat: String,
    val includePathInCounterScope: Boolean,
    val includeFilenameInCounterScope: Boolean,
    val saveMode: SaveMode,
    val dateScopeValues: List<String> = emptyList(),
    val timeScopeValues: List<String> = emptyList(),
    val phraseScopeValues: List<String> = emptyList(),
) {
    val fileNameCellSlots by lazy(LazyThreadSafetyMode.NONE) {
        deriveFileNameCellSlotsFromDrafts(fileNameSlotDrafts)
    }

    init {
        require(fileNameSlotDrafts.size == FILE_NAME_SLOT_COUNT) {
            "fileNameSlotDrafts must have exactly $FILE_NAME_SLOT_COUNT entries."
        }
        require(pathSlotDrafts.size == PATH_SLOT_COUNT) {
            "pathSlotDrafts must have exactly $PATH_SLOT_COUNT entries."
        }
    }
}
