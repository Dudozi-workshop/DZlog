package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.data.counter.parseCounterForPolicy
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.preview.*
import com.dudoziworkshop.dzlog.feature.counter.table.SaveSettingsCounterController
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class RotatingCounterProgressTest {
    @Test
    fun phrase_sequence_uses_independent_or_continuous_history_for_all_save_modes_and_scope_options() {
        for (saveMode in SaveMode.entries) for (filenameScope in listOf(true, false)) {
            for (pathScope in listOf(true, false)) for (formatSlot in listOf(true, false)) {
                for (mode in RotatingCounterProgressMode.entries) {
                    val template = template(mode, formatSlot)
                    val history = mutableListOf<String>()
                    val observed = (1..4).map { cursor ->
                        val preview = preview(template, cursor, saveMode, filenameScope, pathScope)
                        val used = history.mapNotNull {
                            parseCounterForPolicy(it, preview.previewNaming.scanPrefix, "_")
                        }.toSet()
                        val next = CounterManager.nextFromUsed(used)
                        val actual = preview(template, cursor, saveMode, filenameScope, pathScope, next)
                        history += actual.previewNaming.displayName
                        next
                    }
                    val expected = if (mode == RotatingCounterProgressMode.PER_PHRASE) listOf(1, 1, 1, 2)
                        else listOf(1, 2, 3, 4)
                    assertEquals("$saveMode/$filenameScope/$pathScope/$formatSlot/$mode", expected, observed)
                }
            }
        }
    }

    @Test
    fun mode_switch_rescans_existing_history_and_delete_rescans_without_cached_maximum() {
        val names = mutableListOf("T1_A_0001.jpg", "T1_B_0003.jpg", "T1_C_0002.jpg", "UNRELATED_B_9999.jpg")
        fun next(mode: RotatingCounterProgressMode, cursor: Int): Int {
            val scan = preview(template(mode), cursor).previewNaming.scanPrefix
            return CounterManager.nextFromUsed(names.mapNotNull { parseCounterForPolicy(it, scan, "_") }.toSet())
        }
        assertEquals(2, next(RotatingCounterProgressMode.PER_PHRASE, 1))
        assertEquals(4, next(RotatingCounterProgressMode.CONTINUOUS, 1))
        names.remove("T1_B_0003.jpg")
        assertEquals(3, next(RotatingCounterProgressMode.CONTINUOUS, 2))
        assertEquals(1, next(RotatingCounterProgressMode.PER_PHRASE, 2))
    }

    @Test
    fun continuous_scope_is_stable_and_per_phrase_scope_is_distinct_even_with_filename_scope_off() {
        for (filenameScope in listOf(true, false)) {
            fun key(mode: RotatingCounterProgressMode, cursor: Int): String {
                val naming = preview(template(mode), cursor, filenameScope = filenameScope).previewNaming
                return buildCounterScopeParts(naming.counterScope.relativePathKey,
                    naming.counterScope.streamPrefix, true, filenameScope).scopeKey
            }
            assertEquals(key(RotatingCounterProgressMode.CONTINUOUS, 1), key(RotatingCounterProgressMode.CONTINUOUS, 2))
            assertNotEquals(key(RotatingCounterProgressMode.PER_PHRASE, 1), key(RotatingCounterProgressMode.PER_PHRASE, 2))
            assertNotEquals(key(RotatingCounterProgressMode.PER_PHRASE, 1), key(RotatingCounterProgressMode.CONTINUOUS, 1))
        }
    }

    @Test
    fun preview_capture_and_save_settings_share_cursor_scope_and_number_suffix() {
        for (mode in RotatingCounterProgressMode.entries) for (saveMode in SaveMode.entries) {
            val template = template(mode)
            val scope = buildScope(CaptureScopeInput(template, Date(0), 4, "yyyyMMdd", "HHmm", "_", true, saveMode, 2))
            val capture = buildCapturePreview(scope, FinalCapturePreviewInput(template, Date(0), 4,
                "yyyyMMdd", "HHmm", "_", true, true, saveMode, 12, 2))
            val preview = preview(template, 2, saveMode, next = 12)
            assertEquals("T1_B_0012.jpg", capture.displayName)
            assertEquals(preview.previewNaming.displayName, capture.displayName)
            assertEquals(preview.plan.resolvedCells, capture.resolvedCells)
            assertEquals(3, capture.nextPhraseProgressCursor)
            val request = SaveSettingsCounterController.buildRequest(2, template, saveMode, 4, true, true)
            assertEquals(scope.scanPrefix, request.scanPrefix)
            assertEquals(scope.counterScope.streamPrefix, request.prefix)
        }
    }

    @Test
    fun bounded_scan_handles_blank_sanitized_delimited_and_multiple_slots_without_regex_or_wildcard() {
        val encoded = CounterScanPrefixes.encode(listOf(listOf("T.1"), listOf("A_B", "", "C+|?"), listOf("X", "Y")))
        listOf("T.1_A_B_X", "T.1_X", "T.1_C+|?_Y").forEach { assertTrue(CounterScanPrefixes.matches(it, encoded, "_")) }
        listOf("Tz1_A_B_X", "T.1_D_X", "T.1_A_B_Z", "OTHER_A_B_X").forEach {
            assertFalse(CounterScanPrefixes.matches(it, encoded, "_"))
        }
        assertTrue(CounterScanPrefixes.matches("DZlog", CounterScanPrefixes.encode(listOf(listOf(""))), "_"))
        assertNull(parseCounterForPolicy("T.1_A_B_X_bad.jpg", encoded, "_"))
        assertEquals(12, parseCounterForPolicy("T.1_A_B_X_0012.jpg", encoded, "_"))
    }

    private fun template(mode: RotatingCounterProgressMode, formatSlot: Boolean = false): TableTemplateState {
        val cell = TableCellState(0, 0, dataType = TableCellDataType.ROTATING_TEXT, cellId = "phrase", phraseSetId = "set")
        val slot = if (formatSlot) TableEditorSlotDraft("FORMAT", "순환문구", formatType = "ROTATING_TEXT")
            else TableEditorSlotDraft("CELL", "셀", cellId = "phrase")
        return TableTemplateState(1, 1, listOf(cell), phraseSets = listOf(
            RotatingPhraseSet("set", "처리구", listOf("A", "B", "C"), counterProgressMode = mode)),
            fileNameSlotDrafts = listOf(TableEditorSlotDraft("MANUAL", "직접입력", manualText = "T1"), slot) +
                List(FILE_NAME_SLOT_COUNT - 2) { null })
    }

    private fun preview(template: TableTemplateState, cursor: Int, saveMode: SaveMode = SaveMode.BOTH,
        filenameScope: Boolean = true, pathScope: Boolean = true, next: Int? = null): PreviewState =
        buildPreview(PreviewInput(template, Date(0), 4, "yyyyMMdd", "HHmm", "_", pathScope,
            filenameScope, saveMode, next, cursor))
}
