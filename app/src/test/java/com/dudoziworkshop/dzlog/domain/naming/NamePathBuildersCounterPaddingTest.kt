package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class NamePathBuildersCounterPaddingTest {

    @Test
    fun `counterOverride uses counterDigits even without counter cell`() {
        val textCell = resolvedCell(
            id = "cell-1",
            type = TableCellDataType.TEXT,
            text = "SITE",
        )

        val name = buildDisplayNameFromSlotDrafts(
            resolvedCells = listOf(textCell),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = textCell.id),
                null,
                null,
            ),
            fnDelim = "_",
            counterDigits = 3,
            usedCounter = 7,
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertTrue(name.startsWith("SITE_007"))
    }

    @Test
    fun `legacy counter draft token is ignored and only suffix counter remains`() {
        val textCell = resolvedCell(
            id = "cell-1",
            type = TableCellDataType.TEXT,
            text = "SITE",
        )

        val name = buildDisplayNameFromSlotDrafts(
            resolvedCells = listOf(textCell),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "FORMAT", label = "카운터", formatType = "COUNTER"),
                TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "M"),
                null,
            ),
            fnDelim = "_",
            counterDigits = 2,
            usedCounter = 4,
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals("M_04.jpg", name)
    }

    @Test
    fun `path drafts use manual plus formatted date and sanitize slashes`() {
        val path = buildGalleryRelativePathFromSlotDrafts(
            resolvedCells = emptyList(),
            pathSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "A/B"),
                TableEditorSlotDraft(kind = "FORMAT", label = "날짜", formatType = "DATE"),
            ),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals("Pictures/DZlog/A_B/19700101/", path)
    }

    @Test
    fun `rotating text format token uses resolved rotating value`() {
        val rotatingCell = resolvedCell(
            id = "rot-1",
            type = TableCellDataType.ROTATING_TEXT,
            text = "PHRASE",
        )

        val name = buildDisplayNameFromSlotDrafts(
            resolvedCells = listOf(rotatingCell),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "FORMAT", label = "순환문구", formatType = "ROTATING_TEXT"),
                null,
                null,
            ),
            fnDelim = "_",
            counterDigits = 1,
            usedCounter = 3,
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals("PHRASE_3.jpg", name)
    }


    @Test
    fun `path drafts normalize time token to HHmm`() {
        val path = buildGalleryRelativePathFromSlotDrafts(
            resolvedCells = emptyList(),
            pathSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "line"),
                TableEditorSlotDraft(kind = "FORMAT", label = "시간", formatType = "TIME"),
            ),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals("Pictures/DZlog/line/0000/", path)
    }

    private fun resolvedCell(
        id: String,
        type: TableCellDataType,
        text: String,
    ): ResolvedCell {
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            rawText = text,
            dataType = type,
            groupLevel = GroupLevel.NONE,
            cellId = id,
        )
        return ResolvedCell(
            id = id,
            type = type,
            raw = raw,
            resolvedText = text,
            isEmpty = false,
        )
    }
}
