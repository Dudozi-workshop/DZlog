package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.counter.*
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.table.TableValueResolver
import com.dudoziworkshop.dzlog.domain.time.CaptureTime
import com.dudoziworkshop.dzlog.domain.time.formatCaptureTime
import kotlin.test.*

class NamingPolicyParityTest {
    private val time = CaptureTime(1791936300000L, "ko-KR", "Asia/Seoul")
    private val cells = listOf(
        TableCellState(0, 0, cellId = "text", rawText = " Draper "),
        TableCellState(0, 1, cellId = "number", rawText = "32", dataType = TableCellDataType.NUMBER),
        TableCellState(0, 2, cellId = "date", dataType = TableCellDataType.DATE, counterScopeMode = CounterScopeMode.INCLUDE),
        TableCellState(1, 0, cellId = "time", dataType = TableCellDataType.TIME, counterScopeMode = CounterScopeMode.INCLUDE),
        TableCellState(1, 1, cellId = "counter", dataType = TableCellDataType.COUNTER),
        TableCellState(1, 2, cellId = "phrase", dataType = TableCellDataType.ROTATING_TEXT, phraseSetId = "set"),
    )

    @Test
    fun allSixCellTypesFilenameAndLogicalFoldersMatchTheReferenceCase() {
        val plan = TableValueResolver().plan(cells, time,
            TableValueResolver.Config(4, "yyyyMMdd", "HHmm"), 42,
            mapOf("phrase" to "진행"), listOf(RotatingPhraseSet("set", "처리", listOf("진행"))))
        assertEquals(listOf("Draper", "32", "20261014", "0905", "0042", "진행"),
            plan.resolvedCells.map { it.resolvedText })
        assertEquals(mapOf("counter" to "43"), plan.patch.updatesByCellId)
        val slots = listOf(TableEditorSlotDraft("MANUAL", "", manualText = "LAB"),
            TableEditorSlotDraft("CELL", "", cellId = "date"),
            TableEditorSlotDraft("CELL", "", cellId = "time"))
        assertEquals("LAB_20261014_0905_0042.jpg", buildFileName(
            plan.resolvedCells, slots, "_", 4, 42, time, "yyyyMMdd", "HHmm"))
        val path = listOf(TableEditorSlotDraft("MANUAL", "", manualText = "2026"),
            TableEditorSlotDraft("CELL", "", cellId = "text"))
        assertEquals(listOf("2026", "Draper"), buildLogicalSaveSegments(
            plan.resolvedCells, path, time, "yyyyMMdd", "HHmm"))
        assertEquals("Pictures/DZlog/2026/Draper/", buildSavePath(
            plan.resolvedCells, path, time, "yyyyMMdd", "HHmm"))
        assertEquals(listOf("Pictures/DZlog/2026/Draper/", "Pictures/DZlog/2026/Draper/original/"),
            capturePhysicalSavePaths("Pictures/DZlog/2026/Draper/", SaveMode.BOTH))
    }

    @Test
    fun explicitTimezoneControlsMidnightYearBoundaryAndDaylightSaving() {
        val yearEnd = CaptureTime(1798729500000L, "en-US", "Asia/Seoul")
        assertEquals("20270101_0005", formatCaptureTime("yyyyMMdd_HHmm", yearEnd))
        assertEquals("20261231_0705", formatCaptureTime("yyyyMMdd_HHmm", yearEnd.copy(timeZoneId = "America/Los_Angeles")))
        assertEquals("0159", formatCaptureTime("HHmm", CaptureTime(1772963940000L, "en-US", "America/Los_Angeles")))
        assertEquals("0300", formatCaptureTime("HHmm", CaptureTime(1772964000000L, "en-US", "America/Los_Angeles")))
        assertEquals("19700101_0900", formatCaptureTime("yyyyMMdd_HHmm", time.copy(epochMilliseconds = 0)))
    }

    @Test
    fun dateChoicesTimeMinutePolicyAndCounterWidthKeepTheirMeaning() {
        for ((pattern, expected) in listOf("yyyyMMdd" to "20261014", "yyMMdd" to "261014", "MMdd" to "1014", "invalid" to "20261014")) {
            val plan = TableValueResolver().plan(listOf(cells[2].copy(formatPattern = pattern), cells[3]), time,
                TableValueResolver.Config(4, "yyyyMMdd", "HH:mm:ss"))
            assertEquals(listOf(expected, "0905"), plan.resolvedCells.map { it.resolvedText })
        }
        assertEquals(1, CounterRules.nextFromUsed(emptySet()))
        assertEquals(4, CounterRules.nextFromUsed(setOf(1, 3)))
        assertEquals(3, CounterRules.nextFromUsed(setOf(1, 2)))
    }

    @Test
    fun encodedCounterChoicesPreserveUnicodeSeparatorsAndMalformedInputIsRejected() {
        val encoded = CounterScanPrefixes.encode(listOf(listOf("실험 A", "실험 B"), listOf("a,b|c%+")))
        assertEquals("|rp-scan-v1|%EC%8B%A4%ED%97%98+A,%EC%8B%A4%ED%97%98+B|a%2Cb%7Cc%25%2B", encoded)
        assertTrue(CounterScanPrefixes.matches("실험 B_a,b|c%+", encoded, "_"))
        assertFalse(CounterScanPrefixes.matches("실험 C_a,b|c%+", encoded, "_"))
        assertFalse(CounterScanPrefixes.matches("anything", "|rp-scan-v1|%ZZ", "_"))
        assertFalse(CounterScanPrefixes.matches("anything", "|rp-scan-v1|%A", "_"))
    }
}
