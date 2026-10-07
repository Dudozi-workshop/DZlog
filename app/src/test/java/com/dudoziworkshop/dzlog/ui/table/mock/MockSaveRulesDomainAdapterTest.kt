package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "preserved-tail"),
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
        assertEquals("preserved-tail", updated.pathSlotDrafts[3]?.manualText)
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
}
