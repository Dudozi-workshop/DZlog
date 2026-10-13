package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellKind
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import com.dudoziworkshop.dzlog.domain.table.TablePatch
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

    private fun shot(id: String, number: Int, phraseIndex: Int) =
        CapturePendingProjection.Shot(
            id, scope, number, TablePatch(
                updatesByCellId = mapOf("counter" to (number + 1).toString()),
                phraseProgressByCellId = mapOf("phrase" to CellPhraseProgress(phraseIndex, 0)),
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
}
