package com.dudoziworkshop.dzlog.feature.table.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentDrivenTableLayoutTest {
    @Test
    fun sizesMergedAndMultilineCellsFromMeasuredLines() {
        val cells = listOf(
            ContentDrivenLayoutCell(0, 0, 1, 2, "시료 B-17\n18.5℃", false),
            ContentDrivenLayoutCell(0, 1, 1, 1, "covered", true),
            ContentDrivenLayoutCell(1, 0, 1, 1, "", false),
            ContentDrivenLayoutCell(1, 1, 1, 1, "iW", false),
        )
        val measured = mapOf("시료 B-17" to 180f, "18.5℃" to 85f, " " to 6f, "iW" to 32f)
        val lines = mutableListOf<String>()
        val layout = measureContentDrivenLayout(cells, 2, 2, 100, 34f) { line ->
            lines.add(line)
            measured.getValue(line)
        }
        assertEquals(measured.keys.toList(), lines)
        assertEquals(listOf(102f, 102f), layout.colWidthsPx)
        assertEquals(listOf(86f, 52f), layout.rowHeightsPx)
        val fitted = fitContentDrivenLayout(layout, null, listOf(2f, 1f), 153f, 500f, true)
        assertEquals(listOf(102f, 51f), fitted.scaledColWidthsPx)
        assertEquals(listOf(43f, 26f), fitted.scaledRowHeightsPx)
    }

    @Test
    fun longMeasuredLineCanShrinkToFitANarrowViewport() {
        val cell = ContentDrivenLayoutCell(0, 0, 1, 1, "W".repeat(5000), false)
        val layout = measureContentDrivenLayout(listOf(cell), 1, 1, 100, 34f) { it.length * 15f }
        val fitted = fitContentDrivenLayout(layout, null, null, 180f, 120f, true)
        assertTrue(fitted.fitScale < 0.01f)
        assertEquals(180f, fitted.scaledColWidthsPx.single(), 0.001f)
    }
}
