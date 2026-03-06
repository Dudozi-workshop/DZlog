package com.dudoziworkshop.dzlog.domain.table

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class TableResolverRotatingTextSelectionTest {

    @Test
    fun `plan uses selectedPhraseTextByCellId for rotating text resolved value`() {
        val rotatingCell = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            cellId = "r1",
            dataType = TableCellDataType.ROTATING_TEXT,
        )

        val plan = TableResolver().plan(
            cells = listOf(rotatingCell),
            captureNow = Date(0),
            config = TableResolver.Config(
                counterDigits = 3,
                dateFormat = "yyyy-MM-dd",
                timeFormat = "HH:mm",
            ),
            selectedPhraseTextByCellId = mapOf("r1" to "왜"),
        )

        assertEquals("왜", plan.resolvedCells.single().resolvedText)
    }
}
