package com.dudoziworkshop.dzlog.feature.help.ui

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.ui.camera.preview.WatermarkUiArgs
import org.junit.Assert.*
import org.junit.Test

class TutorialCameraDemoTest {
    private val original = WatermarkUiArgs(WatermarkTableAnchor.BOTTOM_RIGHT,
        70, 30, 0, 0, 0, 0, 80, 0, 100, 0, 1, 1, true, 0)

    @Test fun presentationMovesAndResizesWithoutMutatingTheOriginal() {
        val before = original.copy()
        val start = guideWatermarkArgs(original, "move", 0f)
        val end = guideWatermarkArgs(original, "move", 1f)
        assertNotEquals(start.boundsOffsetX10000, end.boundsOffsetX10000)
        assertNotEquals(start.boundsOffsetY10000, end.boundsOffsetY10000)
        val small = guideWatermarkArgs(original, "size", -1f)
        val large = guideWatermarkArgs(original, "size", 2f)
        assertTrue(large.tableWidthRatio > small.tableWidthRatio)
        assertTrue(large.tableHeightRatio > small.tableHeightRatio)
        assertEquals(before, original)
        assertSame(original, guideWatermarkArgs(original, "quick", 1f))
    }

    @Test fun rotationDemoChangesOnlyItsCopy() {
        assertEquals(0, guideWatermarkArgs(original, "rotation", 0f).rotationCwDeg)
        assertEquals(90, guideWatermarkArgs(original, "rotation", 1f).rotationCwDeg)
        assertEquals(0, original.rotationCwDeg)
    }

    @Test fun quickExampleContainsEveryEditableTypeAndValidNamingLinks() {
        val sample = guideQuickTemplate()
        assertEquals(setOf(TableCellDataType.TEXT, TableCellDataType.NUMBER,
            TableCellDataType.DATE, TableCellDataType.TIME, TableCellDataType.ROTATING_TEXT,
            TableCellDataType.COUNTER), sample.cells.map { it.dataType }.toSet())
        val ids = sample.cells.map { it.cellId }.toSet()
        assertEquals(sample.cells.size, ids.size)
        (sample.fileNameSlotDrafts + sample.pathSlotDrafts).filterNotNull().forEach {
            assertTrue(it.cellId in ids)
        }
        assertTrue(sample.cells.all { it.rowIndex in 0 until sample.rows && it.colIndex in 0 until sample.cols })
        assertNotEquals(sample.phraseSets[0].items, sample.phraseSets[1].items)
    }
}
