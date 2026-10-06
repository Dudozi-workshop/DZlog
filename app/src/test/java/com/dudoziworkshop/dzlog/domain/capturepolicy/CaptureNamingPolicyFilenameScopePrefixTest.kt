package com.dudoziworkshop.dzlog.domain.capturepolicy

import com.dudoziworkshop.dzlog.domain.counter.buildScopedCounter
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Date

class CaptureNamingPolicyFilenameScopePrefixTest {

    @Test
    fun `per phrase rotating text changes scoped counter key`() {
        val alpha = buildResult(
            resolvedCells = listOf(resolvedRotating("ALPHA")),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "FORMAT", label = "순환문구", formatType = "ROTATING_TEXT"),
                null,
                null,
            ),
        )
        val beta = buildResult(
            resolvedCells = listOf(resolvedRotating("BETA")),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "FORMAT", label = "순환문구", formatType = "ROTATING_TEXT"),
                null,
                null,
            ),
        )

        val alphaScoped = buildScopedCounter(alpha.counterScope, includePathInScope = true, includeFilenameInScope = true, scanPrefix = alpha.scanPrefix)
        val betaScoped = buildScopedCounter(beta.counterScope, includePathInScope = true, includeFilenameInScope = true, scanPrefix = beta.scanPrefix)

        assertNotEquals(alphaScoped.captureStreamKey.prefix, betaScoped.captureStreamKey.prefix)
    }

    @Test
    fun `rotating text changes scoped counter key by phrase`() {
        val alpha = buildResult(
            resolvedCells = listOf(resolvedRotating("ALPHA")),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "FORMAT", label = "순환문구", formatType = "ROTATING_TEXT"),
                null,
                null,
            ),
        )
        val beta = buildResult(
            resolvedCells = listOf(resolvedRotating("BETA")),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "FORMAT", label = "순환문구", formatType = "ROTATING_TEXT"),
                null,
                null,
            ),
        )

        val alphaScoped = buildScopedCounter(alpha.counterScope, includePathInScope = true, includeFilenameInScope = true, scanPrefix = alpha.scanPrefix)
        val betaScoped = buildScopedCounter(beta.counterScope, includePathInScope = true, includeFilenameInScope = true, scanPrefix = beta.scanPrefix)

        assertNotEquals(alphaScoped.captureStreamKey.prefix, betaScoped.captureStreamKey.prefix)
    }

    @Test
    fun `manual value and slot order change scoped counter key`() {
        val first = buildResult(
            resolvedCells = listOf(resolvedText()),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "c1"),
                TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "A"),
                null,
            ),
        )
        val swapped = buildResult(
            resolvedCells = listOf(resolvedText()),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "A"),
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "c1"),
                null,
            ),
        )
        val changedManual = buildResult(
            resolvedCells = listOf(resolvedText()),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "c1"),
                TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "B"),
                null,
            ),
        )

        assertNotEquals(first.counterScope.streamPrefix, swapped.counterScope.streamPrefix)
        assertNotEquals(first.counterScope.streamPrefix, changedManual.counterScope.streamPrefix)
    }

    @Test
    fun `date time include exclude affects scoped counter key`() {
        val date = resolvedDate()
        val timeInclude = resolvedTime(CounterScopeMode.INCLUDE)
        val timeExclude = resolvedTime(CounterScopeMode.EXCLUDE)

        val include = buildResult(
            resolvedCells = listOf(date, timeInclude),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "d1"),
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "t1"),
                null,
            ),
        )
        val exclude = buildResult(
            resolvedCells = listOf(date, timeExclude),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "d1"),
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "t1"),
                null,
            ),
        )

        assertNotEquals(include.counterScope.streamPrefix, exclude.counterScope.streamPrefix)
    }

    @Test
    fun `counter slot does not affect scoped counter key`() {
        val noCounter = buildResult(
            resolvedCells = emptyList(),
            fileNameSlotDrafts = listOf(null, null, null),
        )
        val withCounter = buildResult(
            resolvedCells = listOf(resolvedCounter()),
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "k1"),
                TableEditorSlotDraft(kind = "FORMAT", label = "카운터", formatType = "COUNTER"),
                null,
            ),
        )

        assertEquals(noCounter.counterScope.streamPrefix, withCounter.counterScope.streamPrefix)
    }

    private fun buildResult(
        resolvedCells: List<ResolvedCell>,
        fileNameSlotDrafts: List<TableEditorSlotDraft?>,
    ): CaptureNamingPolicy.Result {
        return CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = CaptureContext(
                resolvedCells = resolvedCells,
                captureNow = Date(0),
                fileNameSlotDrafts = fileNameSlotDrafts,
                pathSlotDrafts = List(PATH_SLOT_COUNT) { null },
                fnDelim = "_",
                counterDigits = 2,
                dateFormat = "yyyyMMdd",
                timeFormat = "HH:mm:ss",
                includeFilenameInCounterScope = true,
                dateScopeValues = emptyList(),
                timeScopeValues = emptyList(),
                phraseScopeValues = emptyList(),
                saveMode = SaveMode.WATERMARK_ONLY,
            ),
            usedCounter = 1,
        )
    }

    private fun resolvedText(): ResolvedCell {
        val id = "c1"
        val text = "CELL"
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            rawText = text,
            dataType = TableCellDataType.TEXT,
            groupLevel = GroupLevel.NONE,
            cellId = id,
        )
        return ResolvedCell(id, raw.dataType, raw, text, isEmpty = text.isBlank())
    }

    private fun resolvedRotating(text: String): ResolvedCell {
        val id = "r1"
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            rawText = text,
            dataType = TableCellDataType.ROTATING_TEXT,
            groupLevel = GroupLevel.NONE,
            cellId = id,
        )
        return ResolvedCell(id, raw.dataType, raw, text, isEmpty = text.isBlank())
    }

    private fun resolvedDate(): ResolvedCell {
        val id = "d1"
        val text = "20260101"
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            rawText = text,
            dataType = TableCellDataType.DATE,
            groupLevel = GroupLevel.NONE,
            counterScopeMode = CounterScopeMode.INCLUDE,
            cellId = id,
        )
        return ResolvedCell(id, raw.dataType, raw, text, isEmpty = text.isBlank())
    }

    private fun resolvedTime(mode: CounterScopeMode): ResolvedCell {
        val id = "t1"
        val text = "13:14:59"
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            rawText = text,
            dataType = TableCellDataType.TIME,
            groupLevel = GroupLevel.NONE,
            counterScopeMode = mode,
            cellId = id,
        )
        return ResolvedCell(id, raw.dataType, raw, text, isEmpty = text.isBlank())
    }

    private fun resolvedCounter(): ResolvedCell {
        val id = "k1"
        val text = "77"
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            rawText = text,
            dataType = TableCellDataType.COUNTER,
            groupLevel = GroupLevel.NONE,
            counterScopeMode = CounterScopeMode.INCLUDE,
            cellId = id,
        )
        return ResolvedCell(id, raw.dataType, raw, text, isEmpty = text.isBlank())
    }
}
