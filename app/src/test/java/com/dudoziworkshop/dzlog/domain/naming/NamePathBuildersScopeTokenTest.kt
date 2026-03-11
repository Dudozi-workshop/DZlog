package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.RotatingCounterMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class NamePathBuildersScopeTokenTest {

    @Test
    fun `manual draft is always included in filename scope tokens`() {
        val tokens = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "A"),
                null,
                null,
            ),
            resolvedCells = emptyList(),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals(listOf("A"), tokens)
    }

    @Test
    fun `time format token is normalized to minute`() {
        val tokens = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "FORMAT", label = "시간", formatType = "TIME"),
                null,
                null,
            ),
            resolvedCells = emptyList(),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals(listOf("0000"), tokens)
    }

    @Test
    fun `rotating text scope token is included only in per phrase mode`() {
        val perPhrase = resolvedRotatingCell("r1", "WHY", RotatingCounterMode.PER_PHRASE)
        val global = resolvedRotatingCell("r2", "WOW", RotatingCounterMode.GLOBAL)

        val perPhraseTokens = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(TableEditorSlotDraft(kind = "FORMAT", label = "순환문구", formatType = "ROTATING_TEXT"), null, null),
            resolvedCells = listOf(perPhrase),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )
        val globalTokens = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(TableEditorSlotDraft(kind = "FORMAT", label = "순환문구", formatType = "ROTATING_TEXT"), null, null),
            resolvedCells = listOf(global),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals(listOf("WHY"), perPhraseTokens)
        assertEquals(emptyList<String>(), globalTokens)
    }

    @Test
    fun `date time cell tokens follow counterScopeMode include`() {
        val dateCell = resolvedCell(
            id = "d1",
            type = TableCellDataType.DATE,
            text = "20260101",
            counterScopeMode = CounterScopeMode.INCLUDE,
        )
        val timeCell = resolvedCell(
            id = "t1",
            type = TableCellDataType.TIME,
            text = "13:14:59",
            counterScopeMode = CounterScopeMode.EXCLUDE,
        )

        val tokens = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "d1"),
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "t1"),
                null,
            ),
            resolvedCells = listOf(dateCell, timeCell),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals(listOf("20260101"), tokens)
    }



    @Test
    fun `time cell scope token uses HHmm when include is enabled`() {
        val timeCell = resolvedCell(
            id = "t2",
            type = TableCellDataType.TIME,
            text = "13:14:59",
            counterScopeMode = CounterScopeMode.INCLUDE,
        )

        val tokens = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "t2"), null, null),
            resolvedCells = listOf(timeCell),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals(listOf("1314"), tokens)
    }

    @Test
    fun `counter cell is excluded from filename scope tokens`() {
        val counterCell = resolvedCell(
            id = "c1",
            type = TableCellDataType.COUNTER,
            text = "12",
            counterScopeMode = CounterScopeMode.INCLUDE,
        )

        val tokens = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "c1"), null, null),
            resolvedCells = listOf(counterCell),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals(emptyList<String>(), tokens)
    }


    @Test
    fun `manual value change changes filename scope tokens`() {
        val draftsA = listOf(TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "A"), null, null)
        val draftsB = listOf(TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "B"), null, null)

        val tokensA = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = draftsA,
            resolvedCells = emptyList(),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )
        val tokensB = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = draftsB,
            resolvedCells = emptyList(),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals(listOf("A"), tokensA)
        assertEquals(listOf("B"), tokensB)
    }

    @Test
    fun `filename scope tokens preserve slot order`() {
        val cell = resolvedCell(
            id = "x1",
            type = TableCellDataType.TEXT,
            text = "CELL",
            counterScopeMode = CounterScopeMode.EXCLUDE,
        )

        val first = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "x1"),
                TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "B"),
                null,
            ),
            resolvedCells = listOf(cell),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )
        val second = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "B"),
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "x1"),
                null,
            ),
            resolvedCells = listOf(cell),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals(listOf("CELL", "B"), first)
        assertEquals(listOf("B", "CELL"), second)
    }

    @Test
    fun `rotating text global mode does not change scope tokens across phrase changes`() {
        val globalA = resolvedRotatingCell("r3", "ALPHA", RotatingCounterMode.GLOBAL)
        val globalB = resolvedRotatingCell("r3", "BETA", RotatingCounterMode.GLOBAL)

        val tokensA = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(TableEditorSlotDraft(kind = "FORMAT", label = "순환문구", formatType = "ROTATING_TEXT"), null, null),
            resolvedCells = listOf(globalA),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )
        val tokensB = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(TableEditorSlotDraft(kind = "FORMAT", label = "순환문구", formatType = "ROTATING_TEXT"), null, null),
            resolvedCells = listOf(globalB),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals(emptyList<String>(), tokensA)
        assertEquals(emptyList<String>(), tokensB)
    }

    @Test
    fun `counter format draft is excluded from filename scope tokens`() {
        val tokens = resolveFileNameScopeTokensFromDrafts(
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "FORMAT", label = "카운터", formatType = "COUNTER"),
                null,
                null,
            ),
            resolvedCells = emptyList(),
            now = Date(0),
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
        )

        assertEquals(emptyList<String>(), tokens)
    }

    private fun resolvedRotatingCell(id: String, text: String, mode: RotatingCounterMode): ResolvedCell {
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            rawText = text,
            dataType = TableCellDataType.ROTATING_TEXT,
            groupLevel = GroupLevel.NONE,
            rotatingCounterMode = mode,
            cellId = id,
        )
        return ResolvedCell(id = id, type = TableCellDataType.ROTATING_TEXT, raw = raw, resolvedText = text, isEmpty = false)
    }

    private fun resolvedCell(
        id: String,
        type: TableCellDataType,
        text: String,
        counterScopeMode: CounterScopeMode,
    ): ResolvedCell {
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            rawText = text,
            dataType = type,
            groupLevel = GroupLevel.NONE,
            counterScopeMode = counterScopeMode,
            cellId = id,
        )
        return ResolvedCell(id = id, type = type, raw = raw, resolvedText = text, isEmpty = false)
    }
}
