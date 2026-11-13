package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TableCellTypeDraftRestoreTest {
    @Test
    fun switching_types_restores_each_previous_value_without_changing_text() {
        val start = TableCellState(
            rowIndex = 0, colIndex = 0, rawText = "blueberry",
            typedValue = CellValue.Text("blueberry"),
        )
        val number = start.withDataType(TableCellDataType.NUMBER).copy(
            numberDraft = "125.5", typedValue = CellValue.Number("125.5"),
        )
        val counter = number.withDataType(TableCellDataType.COUNTER).copy(
            counterSeedDraft = 42, typedValue = CellValue.CounterSeed(42),
        )
        val date = counter.withDataType(TableCellDataType.DATE).copy(
            formatPattern = "yyMMdd", dateFormatDraft = "yyMMdd",
        )
        val time = date.withDataType(TableCellDataType.TIME)
        val rotating = time.withDataType(TableCellDataType.ROTATING_TEXT).copy(
            phraseSetId = "set", phraseIndex = 1, phraseCompletedShots = 2, everyOverride = 5,
        )

        assertEquals("blueberry", rotating.withDataType(TableCellDataType.TEXT).rawText)
        assertEquals(
            CellValue.Number("125.5"),
            rotating.withDataType(TableCellDataType.NUMBER).typedValue,
        )
        assertEquals(
            CellValue.CounterSeed(42),
            rotating.withDataType(TableCellDataType.COUNTER).typedValue,
        )
        assertEquals("yyMMdd", rotating.withDataType(TableCellDataType.DATE).formatPattern)
        val restored = rotating.withDataType(TableCellDataType.TEXT)
            .withDataType(TableCellDataType.ROTATING_TEXT)
        assertEquals("set", restored.phraseSetId)
        assertEquals(1, restored.phraseIndex)
        assertEquals(2, restored.phraseCompletedShots)
        assertEquals(5, restored.everyOverride)
    }

    @Test
    fun inactive_type_drafts_round_trip_through_template_json() {
        val initial = TableCellState(
            rowIndex = 0, colIndex = 0,
            rawText = "original", typedValue = CellValue.Auto,
            dataType = TableCellDataType.ROTATING_TEXT,
            numberDraft = "-4.25", counterSeedDraft = 9, dateFormatDraft = "MMdd",
            phraseSetId = "set", phraseIndex = 1, phraseCompletedShots = 1, everyOverride = 3,
        )
        val template = newBlankTableTemplateState(1, 1).copy(
            cells = listOf(initial),
            phraseSets = listOf(RotatingPhraseSet("set", "Set", listOf("A", "B"), 3)),
        )
        val restored = requireNotNull(tableTemplateStateFromJson(template.toJsonString()))
        assertEquals(template, restored)
        assertEquals("-4.25", restored.cells.single().withDataType(TableCellDataType.NUMBER).numberDraft)
        assertEquals(9, restored.cells.single().withDataType(TableCellDataType.COUNTER).counterSeedDraft)
        assertEquals("MMdd", restored.cells.single().withDataType(TableCellDataType.DATE).formatPattern)
    }

    @Test
    fun untouched_legacy_cell_does_not_require_new_draft_fields() {
        val legacy = TableCellState(0, 0, rawText = "7", typedValue = CellValue.Number("7"),
            dataType = TableCellDataType.NUMBER)
        val template = newBlankTableTemplateState(1, 1).copy(cells = listOf(legacy))
        val restored = requireNotNull(tableTemplateStateFromJson(template.toJsonString()))
        assertEquals(template, restored)
        assertNull(restored.cells.single().numberDraft)
        assertEquals("7", restored.cells.single().withDataType(TableCellDataType.TEXT).rawText)
    }
}
