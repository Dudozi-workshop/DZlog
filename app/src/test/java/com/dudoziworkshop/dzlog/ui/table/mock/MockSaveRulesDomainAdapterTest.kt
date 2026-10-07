package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.json.JSONObject
import org.junit.Test

class MockSaveRulesDomainAdapterTest {

    @Test
    fun `template slots round trip through v2 save rules draft`() {
        val template = TableTemplateState(
            rows = 1,
            cols = 1,
            cells = listOf(
                TableCellState(
                    rowIndex = 0,
                    colIndex = 0,
                    cellId = "cell-1",
                    rawText = "Draper",
                    dataType = TableCellDataType.TEXT,
                )
            ),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "cell-1"),
                TableEditorSlotDraft(kind = "FORMAT", label = "날짜", formatType = "DATE"),
                null,
            ),
            pathSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "A"),
                TableEditorSlotDraft(kind = "FORMAT", label = "날짜", formatType = "DATE"),
                null,
            ),
        )

        val draft = mockSaveRulesDraftFromTemplate(
            templateState = template,
            includePathInScope = true,
            includeFilenameInScope = false,
        )

        assertEquals("cell-1", draft.fileNameItems[0]?.cellId)
        assertEquals(MockRuleSourceType.DATE, draft.fileNameItems[1]?.sourceType)
        assertEquals("A", draft.pathItems[0]?.value)
        assertTrue(draft.includePathInScope)
        assertEquals(false, draft.includeFilenameInScope)

        val updated = applyMockSaveRulesDraft(
            templateState = template,
            draft = draft.copy(
                pathItems = listOf(
                    draft.pathItems[0],
                    draft.pathItems[1],
                    MockRuleItem(MockRuleSourceType.TIME, "1148"),
                )
            ),
        )

        assertEquals("CELL", updated.fileNameSlotDrafts[0]?.kind)
        assertEquals("cell-1", updated.fileNameSlotDrafts[0]?.cellId)
        assertEquals("TIME", updated.pathSlotDrafts[2]?.formatType)
        assertEquals(3, updated.pathSlotDrafts.size)
    }


    @Test
    fun `cell value edit persists text and number semantics`() {
        val template = TableTemplateState(
            rows = 1,
            cols = 1,
            cells = listOf(
                TableCellState(
                    rowIndex = 0,
                    colIndex = 0,
                    cellId = "cell-1",
                    rawText = "old",
                    dataType = TableCellDataType.TEXT,
                    typedValue = CellValue.Text("old"),
                )
            ),
        )

        val textUpdated = applyMockCellValue(
            templateState = template,
            domainCellId = "cell-1",
            nextValue = "Draper",
        )

        assertEquals("Draper", textUpdated.cells.single().rawText)
        assertEquals(CellValue.Text("Draper"), textUpdated.cells.single().typedValue)

        val numberTemplate = applyMockCellType(
            templateState = textUpdated,
            domainCellId = "cell-1",
            nextType = MockCellType.NUMBER,
        )
        val numberUpdated = applyMockCellValue(
            templateState = numberTemplate,
            domainCellId = "cell-1",
            nextValue = "12.5",
        )

        assertEquals(TableCellDataType.NUMBER, numberUpdated.cells.single().dataType)
        assertEquals(CellValue.Number("12.5"), numberUpdated.cells.single().typedValue)
    }

    @Test
    fun `cell type edit reuses production data type rules`() {
        val template = TableTemplateState(
            rows = 1,
            cols = 1,
            cells = listOf(
                TableCellState(
                    rowIndex = 0,
                    colIndex = 0,
                    cellId = "cell-1",
                    rawText = "memo",
                    dataType = TableCellDataType.TEXT,
                    typedValue = CellValue.Text("memo"),
                )
            ),
        )

        val dateUpdated = applyMockCellType(
            templateState = template,
            domainCellId = "cell-1",
            nextType = MockCellType.DATE,
        )

        assertEquals(TableCellDataType.DATE, dateUpdated.cells.single().dataType)
        assertEquals(CellValue.Auto, dateUpdated.cells.single().typedValue)

        val counterUpdated = applyMockCellType(
            templateState = template,
            domainCellId = "cell-1",
            nextType = MockCellType.COUNTER,
        )

        assertEquals(TableCellDataType.COUNTER, counterUpdated.cells.single().dataType)
        assertEquals(CellValue.CounterSeed(1), counterUpdated.cells.single().typedValue)
    }

    @Test
    fun `mock cell picker keeps domain cell ids`() {
        val template = TableTemplateState(
            rows = 1,
            cols = 2,
            cells = listOf(
                TableCellState(
                    rowIndex = 0,
                    colIndex = 0,
                    cellId = "alpha",
                    rawText = "A",
                    dataType = TableCellDataType.TEXT,
                ),
                TableCellState(
                    rowIndex = 0,
                    colIndex = 1,
                    cellId = "counter",
                    rawText = "0012",
                    dataType = TableCellDataType.COUNTER,
                ),
            ),
        )

        val cells = mockCellsFromTemplate(template)

        assertEquals("alpha", cells[0].domainCellId)
        assertEquals(MockCellType.TEXT, cells[0].type)
        assertEquals("counter", cells[1].domainCellId)
        assertEquals(MockCellType.COUNTER, cells[1].type)
    }
    @Test
    fun `date time and rotating detail mutations persist`() {
        val dateCell = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            cellId = "date",
            dataType = TableCellDataType.DATE,
        )
        val timeCell = TableCellState(
            rowIndex = 0,
            colIndex = 1,
            cellId = "time",
            dataType = TableCellDataType.TIME,
        )
        val rotatingCell = TableCellState(
            rowIndex = 0,
            colIndex = 2,
            cellId = "rotating",
            dataType = TableCellDataType.ROTATING_TEXT,
        )
        val template = TableTemplateState(
            rows = 1,
            cols = 3,
            cells = listOf(dateCell, timeCell, rotatingCell),
            phraseSets = listOf(
                RotatingPhraseSet(
                    id = "set-1",
                    name = "품종",
                    items = listOf("Draper", "Duke"),
                    defaultEvery = 2,
                )
            ),
        )

        val dateUpdated = applyMockDatePattern(template, "date", "yyMMdd")
        assertEquals("yyMMdd", dateUpdated.cells.first { it.cellId == "date" }.formatPattern)

        val timeUpdated = applyMockTimeFormatPolicy(dateUpdated, "time")
        val time = timeUpdated.cells.first { it.cellId == "time" }
        assertEquals("HHmm", time.formatPattern)
        assertEquals(false, time.timeFormatOptions?.includeSeconds)

        val phraseSelected = applyMockPhraseSet(timeUpdated, "rotating", "set-1")
        assertEquals("set-1", phraseSelected.cells.first { it.cellId == "rotating" }.phraseSetId)

        val everyUpdated = applyMockPhraseEvery(phraseSelected, "rotating", 4)
        assertEquals(4, everyUpdated.cells.first { it.cellId == "rotating" }.everyOverride)
    }


    @Test
    fun `phrase set CRUD updates references safely`() {
        val rotatingCell = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            cellId = "rotating",
            dataType = TableCellDataType.ROTATING_TEXT,
        )
        val base = TableTemplateState(
            rows = 1,
            cols = 1,
            cells = listOf(rotatingCell),
        )

        val created = createMockPhraseSet(base, "품종", id = "set-1")
        assertEquals("품종", created.phraseSets.single().name)

        val selected = applyMockPhraseSet(created, "rotating", "set-1")
        assertEquals("set-1", selected.cells.single().phraseSetId)

        val updated = updateMockPhraseSet(selected, "set-1") { set ->
            set.copy(items = listOf("Draper", "Duke"))
        }
        assertEquals(2, updated.phraseSets.single().items.size)

        val deleted = deleteMockPhraseSet(updated, "set-1")
        assertTrue(deleted.phraseSets.isEmpty())
        assertEquals(null, deleted.cells.single().phraseSetId)
        assertEquals(null, deleted.cells.single().everyOverride)
    }

    @Test
    fun `legacy path tail is dropped during json migration`() {
        val base = newBlankTableTemplateState(rows = 1, cols = 1)
        val root = JSONObject(base.toJsonString())
        val pathSlots = root.getJSONArray("pathSlotDrafts")
        pathSlots.put(
            JSONObject()
                .put("kind", "MANUAL")
                .put("label", "legacy")
                .put("manualText", "hidden-fourth"),
        )

        val migrated = tableTemplateStateFromJson(root.toString())
            ?: error("migration failed")

        assertEquals(3, migrated.pathSlotDrafts.size)
        assertTrue(migrated.pathSlotDrafts.none { it?.manualText == "hidden-fourth" })
    }


}
