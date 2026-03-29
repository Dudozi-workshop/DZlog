package com.dudoziworkshop.dzlog.domain.capturepolicy

import com.dudoziworkshop.dzlog.domain.counter.buildCounterScopeParts
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class CaptureNamingPolicyPathConsistencyTest {

    @Test
    fun `path slot relativePath is reused by counter scope key`() {
        val result = CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = baseContext(),
            usedCounter = 1,
        )

        assertEquals("Pictures/DZlog/A/B/", result.relativePath)
        assertEquals(result.relativePath, result.counterScope.relativePathKey)
    }

    @Test
    fun `path scope on splits and off keeps when slot order changes`() {
        val first = CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = baseContext(
                pathSlotDrafts = listOf(
                    TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "A"),
                    TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "B"),
                )
            ),
            usedCounter = 1,
        )
        val swapped = CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = baseContext(
                pathSlotDrafts = listOf(
                    TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "B"),
                    TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "A"),
                )
            ),
            usedCounter = 1,
        )

        val onA = buildCounterScopeParts(first.counterScope.relativePathKey, first.counterScope.streamPrefix, includePathInScope = true, includeFilenameInScope = false)
        val onB = buildCounterScopeParts(swapped.counterScope.relativePathKey, swapped.counterScope.streamPrefix, includePathInScope = true, includeFilenameInScope = false)
        val offA = buildCounterScopeParts(first.counterScope.relativePathKey, first.counterScope.streamPrefix, includePathInScope = false, includeFilenameInScope = false)
        val offB = buildCounterScopeParts(swapped.counterScope.relativePathKey, swapped.counterScope.streamPrefix, includePathInScope = false, includeFilenameInScope = false)

        assertEquals(false, onA.scopeKey == onB.scopeKey)
        assertEquals(offA.scopeKey, offB.scopeKey)
    }

    @Test
    fun `save mode path policy uses water stream for BOTH and separates ORIGINAL_ONLY`() {
        val water = CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = baseContext(saveMode = SaveMode.WATERMARK_ONLY),
            usedCounter = 1,
        )
        val both = CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = baseContext(saveMode = SaveMode.BOTH),
            usedCounter = 1,
        )
        val originalOnly = CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = baseContext(saveMode = SaveMode.ORIGINAL_ONLY),
            usedCounter = 1,
        )

        assertEquals("Pictures/DZlog/A/B/", water.counterScope.relativePathKey)
        assertEquals(water.counterScope.relativePathKey, both.counterScope.relativePathKey)
        assertEquals("Pictures/DZlog/A/B/original/", originalOnly.counterScope.relativePathKey)
    }

    @Test
    fun `group level differences do not change relativePath when path slots are same`() {
        val none = resolvedTextCell(GroupLevel.NONE)
        val g2 = resolvedTextCell(GroupLevel.G2)

        val withNone = CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = baseContext(resolvedCells = listOf(none)),
            usedCounter = 1,
        )
        val withG2 = CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = baseContext(resolvedCells = listOf(g2)),
            usedCounter = 1,
        )

        assertEquals(withNone.relativePath, withG2.relativePath)
    }

    private fun baseContext(
        resolvedCells: List<ResolvedCell> = listOf(resolvedTextCell(GroupLevel.NONE)),
        pathSlotDrafts: List<TableEditorSlotDraft?> = listOf(
            TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "A"),
            TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = "B"),
        ),
        saveMode: SaveMode = SaveMode.WATERMARK_ONLY,
    ): CaptureContext {
        return CaptureContext(
            resolvedCells = resolvedCells,
            captureNow = Date(0),
            fileNameSlotDrafts = listOf(null, null, null),
            pathSlotDrafts = pathSlotDrafts,
            fnDelim = "_",
            counterDigits = 2,
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
            includeFilenameInCounterScope = true,
            saveMode = saveMode,
        )
    }

    private fun resolvedTextCell(groupLevel: GroupLevel): ResolvedCell {
        val id = "c1"
        val text = "unused"
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            cellId = id,
            rawText = text,
            dataType = TableCellDataType.TEXT,
            groupLevel = groupLevel,
        )
        return ResolvedCell(id = id, type = raw.dataType, raw = raw, resolvedText = text, isEmpty = text.isBlank())
    }
}
