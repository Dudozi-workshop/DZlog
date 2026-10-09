package com.dudoziworkshop.dzlog.ui.camera.state

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequest
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.Date

class CameraDerivedStateTest {
    private val now = Date.from(Instant.parse("2026-10-09T13:05:00Z"))
    private val template = TableTemplateState(1, 1, listOf(TableCellState(0, 0, rawText = "오징어")),
        fileNameSlotDrafts = List(FILE_NAME_SLOT_COUNT) {
            if (it == 0) TableEditorSlotDraft("MANUAL", "이름", manualText = "T1") else null
        }, pathSlotDrafts = listOf(TableEditorSlotDraft("MANUAL", "폴더", manualText = "실험")))

    private fun state(t: TableTemplateState = template, mode: SaveMode = SaveMode.BOTH,
        request: CounterRequest? = null, next: Int? = 42, time: Date = now,
        pathScope: Boolean = true, nameScope: Boolean = true) = computeCameraDerivedState(
        tableTemplateState = t, now = time, counterDigits = 0, phraseProgressCounter = 1,
        includePathInCounterScope = pathScope, includeFilenameInCounterScope = nameScope,
        saveMode = mode, syncedNextCounter = next, syncedRequest = request,
        tableResolver = TableResolver())

    private fun request(t: TableTemplateState = template, mode: SaveMode = SaveMode.BOTH,
        time: Date = now, pathScope: Boolean = true, nameScope: Boolean = true): CounterRequest {
        val pre = state(t, mode, time = time, pathScope = pathScope, nameScope = nameScope).captureScopeState
        return CounterRequestResolver.fromCamera(mode, pre.counterScope.relativePathKey,
            pre.counterScope.streamPrefix, pre.scanPrefix, pathScope, nameScope)
    }

    @Test fun matchingReadEnablesFinalNameAndCounter() {
        val result = state(request = request())
        assertEquals(42, result.displayCounter)
        assertEquals("T1_42.jpg", result.topDisplayName)
        assertEquals("Pictures/DZlog/실험/", result.finalCapturePreview!!.relativePathPreview)
    }

    @Test fun pendingReadCannotUseOldCounter() {
        assertNull(state().finalCapturePreview)
        assertEquals("T1.jpg", state().topDisplayName)
        assertNull(state(request = request(), next = null).finalCapturePreview)
    }

    @Test fun folderChangeWaitsForItsOwnRead() {
        val changed = template.copy(pathSlotDrafts = listOf(TableEditorSlotDraft("MANUAL", "폴더", manualText = "새폴더")))
        assertNull(state(changed, request = request()).finalCapturePreview)
        assertEquals("Pictures/DZlog/새폴더/", state(changed, request = request(changed)).finalCapturePreview!!.relativePathPreview)
    }

    @Test fun filenameChangeWaitsForItsOwnRead() {
        val changed = template.copy(fileNameSlotDrafts = List(FILE_NAME_SLOT_COUNT) {
            if (it == 0) TableEditorSlotDraft("MANUAL", "이름", manualText = "T2") else null
        })
        assertNull(state(changed, request = request()).finalCapturePreview)
        assertEquals("T2_42.jpg", state(changed, request = request(changed)).topDisplayName)
    }

    @Test fun everySaveModeChangeWaitsForItsOwnRead() {
        for (old in SaveMode.entries) for (new in SaveMode.entries) {
            val result = state(mode = new, request = request(mode = old))
            if (old == new) assertNotNull(result.finalCapturePreview) else assertNull(result.finalCapturePreview)
        }
    }

    @Test fun scopeOptionChangeCannotUsePreviousRead() {
        assertNull(state(request = request(), pathScope = false).finalCapturePreview)
        assertNull(state(request = request(), nameScope = false).finalCapturePreview)
    }

    @Test fun timeFormatSlotBoundaryWaitsForNewRead() {
        val timed = template.copy(fileNameSlotDrafts = List(FILE_NAME_SLOT_COUNT) {
            if (it == 0) TableEditorSlotDraft("FORMAT", "시간", formatType = "TIME", formatPattern = "HHmm") else null
        })
        val later = Date(now.time + 60_000L)
        assertNull(state(timed, request = request(timed), time = later).finalCapturePreview)
        assertNotNull(state(timed, request = request(timed, time = later), time = later).finalCapturePreview)
    }

    @Test fun sameRequestKeepsOptimisticAdvanceAndUndoReadback() {
        val read = request()
        assertEquals(43, state(request = read, next = 43).displayCounter)
        assertEquals(42, state(request = read, next = 42).displayCounter)
        assertEquals(template, template.copy())
    }
}
