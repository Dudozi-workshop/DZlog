package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellKind
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.domain.table.applyPatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturePendingProjectionTest {
    private val counter = TableCellState(
        rowIndex = 0, colIndex = 0, kind = TableCellKind.INPUT,
        dataType = TableCellDataType.COUNTER, cellId = "counter",
        typedValue = CellValue.CounterSeed(1)
    )
    private val phrase = TableCellState(
        rowIndex = 0, colIndex = 1, kind = TableCellKind.INPUT,
        dataType = TableCellDataType.ROTATING_TEXT, cellId = "phrase",
    )
    private val template = TableTemplateState(rows = 1, cols = 2, cells = listOf(counter, phrase))
    private val scope = CaptureReservationScope("Pictures/DZlog/", "Test")

    private fun shot(id: String, number: Int, phraseIndex: Int, completedShots: Int = 0) =
        CapturePendingProjection.Shot(
            id, scope, number, TablePatch(
                updatesByCellId = mapOf("counter" to (number + 1).toString()),
                phraseProgressByCellId = mapOf("phrase" to CellPhraseProgress(phraseIndex, completedShots)),
            )
        )

    @Test fun speculativePreviewAdvancesWithoutMutatingSavedTemplate() {
        val projection = CapturePendingProjection()
        projection.add(shot("first", 1, 1))
        projection.add(shot("second", 2, 2))
        val preview = projection.previewTemplate(template)
        assertEquals(1, (template.cells[0].typedValue as CellValue.CounterSeed).start)
        assertEquals(3, (preview.cells[0].typedValue as CellValue.CounterSeed).start)
        assertEquals(2, preview.cells[1].phraseIndex)
        assertEquals(3, projection.nextCounter(scope, 1))
    }

    @Test fun rejectedDuplicateCounterDoesNotAlterPendingList() {
        val projection = CapturePendingProjection()
        projection.add(shot("first", 1, 1))
        assertThrows(IllegalStateException::class.java) {
            projection.add(shot("second", 1, 2))
        }
        assertEquals(1, projection.pendingCount())
    }

    @Test fun rejectsOutOfOrderCompletion() {
        val projection = CapturePendingProjection()
        projection.add(shot("first", 1, 1))
        projection.add(shot("second", 2, 2))
        assertThrows(IllegalStateException::class.java) {
            projection.completeFirst("second")
        }
        assertEquals(2, projection.pendingCount())
        assertEquals(1, projection.completeFirst("first").phraseProgressByCellId["phrase"]?.index)
        assertTrue(projection.hasPending())
    }

    @Test fun threeCapturesPerPhraseDoNotRewindWhileCatalogUpdatesLag() {
        val projection = CapturePendingProjection()
        val shots = listOf(
            shot("one", 1, 0, 1),
            shot("two", 2, 0, 2),
            shot("three", 3, 1, 0),
            shot("four", 4, 1, 1),
            shot("five", 5, 1, 2),
            shot("six", 6, 2, 0),
        )

        // Three quick shutters advance the NEXT preview to phrase 2.
        shots.take(3).forEach(projection::add)
        assertEquals(1, projection.previewTemplate(template).cells[1].phraseIndex)

        // Simulate the Compose catalog still supplying the old base on every
        // completed receipt. Removing a reservation must never rewind phrase 2.
        shots.take(3).forEach { shot ->
            val base = projection.committedBase(template)
            val applied = base.applyPatch(projection.completeFirst(shot.id))
            projection.recordCommittedTemplate(applied)
            assertEquals(1, projection.previewTemplate(template).cells[1].phraseIndex)
        }

        // The 4th and 5th captures must remain on phrase 2, not phrase 1.
        projection.add(shots[3])
        assertEquals(1, projection.previewTemplate(template).cells[1].phraseIndex)
        projection.add(shots[4])
        assertEquals(1, projection.previewTemplate(template).cells[1].phraseIndex)
        projection.add(shots[5])
        assertEquals(2, projection.previewTemplate(template).cells[1].phraseIndex)

        // Once the catalog receives the committed base it becomes authoritative.
        val committed = projection.committedBase(template)
        assertEquals(committed, projection.committedBase(committed))
    }
}
