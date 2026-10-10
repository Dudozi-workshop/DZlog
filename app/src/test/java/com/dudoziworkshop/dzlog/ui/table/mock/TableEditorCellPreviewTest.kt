package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class TableEditorCellPreviewTest {
    private val now = requireNotNull(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("2026-10-09 15:44"))

    private fun preview(template: TableTemplateState, cursor: Int = 1) = buildPreview(
        PreviewInput(
            templateState = template,
            captureNow = now,
            counterDigits = 4,
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
            fnDelim = "_",
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
            saveMode = SaveMode.WATERMARK_ONLY,
            scopeNextCounter = 12,
            phraseProgressCursor = cursor,
        )
    )

    private fun ui(template: TableTemplateState, cursor: Int = 1): List<TableEditorCellUiModel> {
        val result = preview(template, cursor)
        return mockCellsFromTemplate(template, result.plan.resolvedCells.associate { it.id to it.resolvedText })
    }

    @Test fun dateAndTimeUseTheSameCurrentValuesAsDomainPreview() {
        val cells = listOf("yyyyMMdd", "yyMMdd", "MMdd").mapIndexed { index, pattern ->
            TableCellState(rowIndex = 0, colIndex = index, cellId = "date-$index",
                dataType = TableCellDataType.DATE, formatPattern = pattern, rawText = "old")
        } + TableCellState(rowIndex = 0, colIndex = 3, cellId = "time",
            dataType = TableCellDataType.TIME, formatPattern = "HH:mm:ss", rawText = "old")
        val template = TableTemplateState(rows = 1, cols = 4, cells = cells)

        assertEquals(listOf("20261009", "261009", "1009", "1544"), ui(template).map { it.previewValue })
        assertEquals(cells, template.cells)
    }

    @Test fun rotatingCellAndFilenameUseCurrentCursorInsteadOfFirstPhrase() {
        val cell = TableCellState(rowIndex = 0, colIndex = 0, cellId = "phrase",
            dataType = TableCellDataType.ROTATING_TEXT, phraseSetId = "set")
        val template = TableTemplateState(rows = 1, cols = 1, cells = listOf(cell),
            phraseSets = listOf(RotatingPhraseSet("set", "ABC", listOf("A", "B", "C"))),
            fileNameSlotDrafts = List(FILE_NAME_SLOT_COUNT) { index ->
                if (index == 0) TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "phrase") else null
            })
        val result = preview(template, cursor = 3)

        assertEquals("C", ui(template, cursor = 3).single().previewValue)
        assertTrue(result.previewNaming.displayName.startsWith("C_"))
        assertEquals("C", ui(template, cursor = 3).single().previewValue)
        assertEquals(listOf(cell), template.cells)
    }

    @Test fun phraseEveryOverrideUsesSharedResolver() {
        val template = TableTemplateState(rows = 1, cols = 1,
            cells = listOf(TableCellState(rowIndex = 0, colIndex = 0, cellId = "phrase",
                dataType = TableCellDataType.ROTATING_TEXT, phraseSetId = "set", everyOverride = 2)),
            phraseSets = listOf(RotatingPhraseSet("set", "ABC", listOf("A", "B", "C"))))
        assertEquals("B", ui(template, cursor = 3).single().previewValue)
    }

    @Test fun previewNeverOverwritesEditableCounterSeedOrText() {
        val cells = listOf(
            TableCellState(rowIndex = 0, colIndex = 0, cellId = "counter",
                dataType = TableCellDataType.COUNTER, typedValue = CellValue.CounterSeed(7)),
            TableCellState(rowIndex = 0, colIndex = 1, cellId = "text",
                dataType = TableCellDataType.TEXT, rawText = "  draft  "),
        )
        val template = TableTemplateState(rows = 1, cols = 2, cells = cells)
        val result = ui(template)
        assertEquals("7", result[0].value)
        assertEquals("0012", result[0].previewValue)
        assertEquals("  draft  ", result[1].value)
        assertEquals("draft", result[1].previewValue)
        assertEquals(cells, template.cells)
    }

    @Test fun emptyResolvedPhraseStaysEmptyRatherThanShowingPlaceholderAsPhotoContent() {
        val template = TableTemplateState(rows = 1, cols = 1,
            cells = listOf(TableCellState(rowIndex = 0, colIndex = 0, cellId = "phrase",
                dataType = TableCellDataType.ROTATING_TEXT, phraseSetId = "missing")))
        assertEquals("", ui(template).single().previewValue)
        assertEquals("순환문구", ui(template).single().value)
    }
}
