package com.dudoziworkshop.dzlog.domain.preview

import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.table.applyPatch
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Preview, filename, folder and persisted phrase progress must be one transaction. */
@RunWith(RobolectricTestRunner::class)
class RotatingCaptureCommitPersistenceTest {
    private val now = Date(0L)

    private fun starter(mode: RotatingCounterProgressMode): TableTemplateState {
        val phrase = TableCellState(
            rowIndex = 0, colIndex = 0, cellId = "rotating-cell",
            dataType = TableCellDataType.ROTATING_TEXT, phraseSetId = "set"
        )
        return TableTemplateState(
            rows = 1, cols = 1, cells = listOf(phrase),
            phraseSets = listOf(RotatingPhraseSet("set", "Treatment", listOf("A", "B", "C"), 2, mode)),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft("MANUAL", "prefix", manualText = "DZlog"),
                TableEditorSlotDraft("CELL", "phrase", cellId = phrase.cellId),
            ) + List(FILE_NAME_SLOT_COUNT - 2) { null },
            pathSlotDrafts = listOf(
                TableEditorSlotDraft("CELL", "folder", cellId = phrase.cellId)
            ) + List(PATH_SLOT_UI_MAX_COUNT - 1) { null },
        )
    }

    private fun preview(template: TableTemplateState, counter: Int): FinalCapturePreview {
        val scope = buildScope(CaptureScopeInput(
            template, now, 3, "yyyyMMdd", "HHmm", "_", true, SaveMode.BOTH, 1
        ))
        val capture = buildCapturePreview(scope, FinalCapturePreviewInput(
            template, now, 3, "yyyyMMdd", "HHmm", "_",
            true, true, SaveMode.BOTH, counter, 1
        ))
        val display = buildPreview(PreviewInput(
            template, now, 3, "yyyyMMdd", "HHmm", "_",
            true, true, SaveMode.BOTH, counter, 1
        ))
        assertEquals(display.previewNaming.displayName, capture.displayName)
        assertEquals(display.previewNaming.relativePath, capture.relativePathPreview)
        assertEquals(display.plan.resolvedCells, capture.resolvedCells)
        return capture
    }

    @Test
    fun cancelled_shots_do_not_advance_and_success_then_restart_keeps_names_and_folders() {
        RotatingCounterProgressMode.entries.forEach { mode ->
            var state = starter(mode)
            val phraseAtShot = listOf("A", "A", "B", "B", "C", "C", "A")
            phraseAtShot.forEachIndexed { index, phrase ->
                val number = index + 1
                val captured = preview(state, number)

                // A failed or cancelled shutter only plans; it never commits the patch.
                val untouched = requireNotNull(tableTemplateStateFromJson(state.toJsonString()))
                assertEquals(captured.displayName, preview(untouched, number).displayName)
                assertEquals(state.cells.single().phraseIndex, untouched.cells.single().phraseIndex)
                assertEquals(state.cells.single().phraseCompletedShots, untouched.cells.single().phraseCompletedShots)
                assertEquals("mode=$mode shot=$number",
                    "DZlog_" + phrase + "_" + number.toString().padStart(3, '0') + ".jpg",
                    captured.displayName)
                assertEquals("Pictures/DZlog/" + phrase + "/", captured.relativePathPreview)
                assertEquals(phrase, captured.resolvedCells.single().resolvedText)
                assertEquals(number, captured.usedCounter)

                // Saved media confirmed: apply exactly the planned counter/phrase patch.
                state = state.applyPatch(captured.tablePatch)
                assertEquals(((index + 1) / 2) % 3, state.cells.single().phraseIndex)
                assertEquals((index + 1) % 2, state.cells.single().phraseCompletedShots)

                if (number == 3) {
                    // Restart recreates state from persisted JSON, without reusing in-memory state.
                    state = requireNotNull(tableTemplateStateFromJson(state.toJsonString()))
                    assertEquals("B", preview(state, 4).resolvedCells.single().resolvedText)
                }
            }
        }
    }
}
