package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.counter.CounterScanPrefixes
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.time.CaptureTime
import kotlin.test.*

class NamingMigrationContractTest {
    private val capture = CaptureTime(1792422299000L, "ko-KR", "Asia/Seoul")
    private fun manual(text: String) = TableEditorSlotDraft("MANUAL", "", manualText = text)
    private fun format(type: String, pattern: String) = TableEditorSlotDraft("FORMAT", "", formatType = type, formatPattern = pattern)

    @Test
    fun fiveLogicalSegmentsRetainOrderAndPerSlotFormatsAcrossMidnight() {
        val slots = listOf(manual(" .연구/실. "), null, format("DATE", "yyMMdd"), format("TIME", "HH:mm:ss"), manual("T1"), manual("ignored"))
        for (locale in listOf("ko-KR", "en-US")) {
            val time = capture.copy(localeTag = locale)
            assertEquals(listOf("연구_실", "261020", "0004", "T1"), buildLogicalSaveSegments(emptyList(), slots, time, "yyyyMMdd", "HHmm"))
            assertEquals("Pictures/DZlog/연구_실/261020/0004/T1/", buildSavePath(emptyList(), slots, time, "yyyyMMdd", "HHmm"))
            assertEquals("연구_실-261020-0004-T1_0012.jpg", buildFileName(emptyList(), slots, "-", 4, 12, time, "yyyyMMdd", "HHmm"))
            assertEquals("261019", resolveFileNameDraftToken(format("DATE", "yyMMdd"), emptyList(), time.copy(timeZoneId = "UTC"), "yyyyMMdd", "HHmm"))
        }
        assertEquals(emptyList(), buildLogicalSaveSegments(emptyList(), emptyList(), capture, "yyyyMMdd", "HHmm"))
    }

    @Test
    fun repeatedCellHighlightsFollowSlotPositionsAndCounterAppearsOnlyAsSuffix() {
        val raw = TableCellState(0, 0, cellId = "text", rawText = "A")
        val cells = listOf(ResolvedCell("text", TableCellDataType.TEXT, raw, "A", false))
        val slots = listOf(manual("A"), TableEditorSlotDraft("CELL", "", cellId = "text"), null,
            TableEditorSlotDraft("CELL", "", cellId = "text"), format("COUNTER", ""))
        assertEquals(NamingCellPreview("A_A_A_0007.jpg", listOf(2..2, 4..4)), buildNamingCellPreview(true, "text", cells, slots, "_", 4, 7, capture, "yyyyMMdd", "HHmm"))
        assertEquals(listOf("A", "A", "A"), resolveFileNameScopeTokensFromDrafts(slots, cells, capture, "yyyyMMdd", "HHmm"))
        assertEquals(SaveMode.BOTH, SaveMode.from(-1))
        assertEquals(listOf(0, 1, 2), SaveMode.entries.map { it.v })
    }

    @Test
    fun continuousPhraseScanKeepsFixedSlotsExactAndRejectsMalformedEscapes() {
        val raw = TableCellState(0, 0, cellId = "phrase", dataType = TableCellDataType.ROTATING_TEXT)
        val phrases = RotatingPhraseSet("set", "처리", listOf("실험 A", "실험 B"), counterProgressMode = RotatingCounterProgressMode.CONTINUOUS)
        val cell = ResolvedCell("phrase", TableCellDataType.ROTATING_TEXT, raw, "실험 B", false, rotatingPhraseSet = phrases)
        val slots = listOf(manual("T1"), TableEditorSlotDraft("CELL", "", cellId = "phrase"))
        val scan = buildCounterScanPrefix(listOf(cell), slots, "_", capture, "yyyyMMdd", "HHmm")
        assertTrue(CounterScanPrefixes.isEncoded(scan))
        assertTrue(CounterScanPrefixes.matches("T1_실험 A", scan, "_"))
        assertTrue(CounterScanPrefixes.matches("T1_실험 B", scan, "_"))
        assertFalse(CounterScanPrefixes.matches("T2_실험 A", scan, "_"))
        assertFalse(CounterScanPrefixes.matches("T1_실험 C", scan, "_"))
        val encoded = CounterScanPrefixes.encode(listOf(listOf("실험 A", "실험 B"), listOf("a,b|c%+")))
        assertEquals("|rp-scan-v1|%EC%8B%A4%ED%97%98+A,%EC%8B%A4%ED%97%98+B|a%2Cb%7Cc%25%2B", encoded)
        assertTrue(CounterScanPrefixes.matches("실험 B_a,b|c%+", encoded, "_"))
        for (bad in listOf("%ZZ", "%A")) assertFalse(CounterScanPrefixes.matches("anything", "|rp-scan-v1|$bad", "_"))
    }
}
