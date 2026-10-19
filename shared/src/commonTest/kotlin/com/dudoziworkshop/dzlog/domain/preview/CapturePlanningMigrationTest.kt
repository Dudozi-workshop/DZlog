package com.dudoziworkshop.dzlog.domain.preview

import com.dudoziworkshop.dzlog.data.backup.LocalTemplateBackupCodec
import com.dudoziworkshop.dzlog.data.backup.fixedArchive
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import com.dudoziworkshop.dzlog.domain.table.applyPatch
import com.dudoziworkshop.dzlog.domain.time.CaptureTime
import kotlin.test.*

class CapturePlanningMigrationTest {
    @Test fun restoredBackupUsesTheSameCapturePlanAndCommitsOnlyTheReturnedPatch() {
        val state = LocalTemplateBackupCodec.decode(fixedArchive).templates.single().templateState
        for (locale in listOf("ko-KR", "en-US")) for (mode in SaveMode.entries) {
            val time = CaptureTime(1792422299000L, locale, "Asia/Seoul")
            val scope = buildScope(CaptureScopeInput(state, time, 4, "yyyyMMdd", "HHmm", "_", true, mode, 7))
            val final = buildCapturePreview(scope, FinalCapturePreviewInput(state, time, 4, "yyyyMMdd", "HHmm", "_", true, true, mode, 12, 7))
            assertEquals(listOf("Draper", "32", "261020", "0004", "0012", "처리 B"), final.resolvedCells.map { it.resolvedText })
            assertEquals("Draper_261020_0004_처리 B_0012.jpg", final.displayName)
            assertEquals("Pictures/DZlog/연구실/Draper/1020/처리 B/", final.relativePathPreview)
            assertEquals(mapOf("counter" to "13"), final.tablePatch.updatesByCellId)
            assertEquals(mapOf("phrase" to CellPhraseProgress(0, 0)), final.tablePatch.phraseProgressByCellId)
            assertEquals(7, final.nextPhraseProgressCursor)
            assertEquals(CellValue.CounterSeed(47), state.cells[4].typedValue)
            assertEquals(1, state.cells[5].phraseIndex)
            val committed = state.applyPatch(final.tablePatch)
            assertEquals(CellValue.CounterSeed(13), committed.cells[4].typedValue)
            assertEquals(0, committed.cells[5].phraseIndex)
            assertEquals(0, committed.cells[5].phraseCompletedShots)
            assertEquals(state.cells.take(4), committed.cells.take(4))
        }
    }
}
