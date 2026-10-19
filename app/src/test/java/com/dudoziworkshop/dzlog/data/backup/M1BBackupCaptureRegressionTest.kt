package com.dudoziworkshop.dzlog.data.backup

import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import com.dudoziworkshop.dzlog.domain.preview.*
import com.dudoziworkshop.dzlog.domain.naming.capturePhysicalSavePaths
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Fixed synthetic schema-1 archive -> existing Android orchestration -> shared B2/B3/B4 rules. */
@RunWith(RobolectricTestRunner::class)
class M1BBackupCaptureRegressionTest {
    private fun fixtureText(): String = requireNotNull(javaClass.getResourceAsStream(
        "/backup/m1b-schema1-six-types.dzlog"
    )).bufferedReader(Charsets.UTF_8).use { it.readText() }

    @Test
    fun frozenSchemaOnePayloadIsReadWithoutCanonicalizingItsChecksumInput() {
        val text = fixtureText()
        val root = JSONObject(text)
        val payload = root.getString("payload")
        assertNotEquals(payload, JSONArray(payload).toString())
        val archive = LocalTemplateBackupCodec.decode(text)
        val item = archive.templates.single()
        assertEquals("fixture-template", archive.activeTemplateId)
        assertEquals(1792422299000L, archive.createdAtMillis)
        assertEquals("합성 호환 시험 🌱 \"A\"\nB", item.name)
        assertEquals(1792422000000L, item.modifiedAt)
        assertEquals(1792422100000L, item.lastUsedAt)
        assertEquals(TableStyleState(2, false, 1, 0, 125, 2, 177), item.styleState)
        assertEquals(listOf(1f, 2f), item.templateState.rowWeights)
        assertEquals(listOf(1f, 2f, 1f), item.templateState.colWeights)
        assertEquals(TableCellDataType.entries, item.templateState.cells.map { it.dataType })
        assertEquals(listOf("text", "number", "date", "time", "counter", "phrase"), item.templateState.cells.map { it.cellId })
        assertEquals(CellValue.CounterSeed(47), item.templateState.cells[4].typedValue)
        assertFalse(item.templateState.cells.first().quickEditable)
        assertEquals(TimeFormatOptions(HourSystem.H12, true, TimeSeparator.NONE), item.templateState.cells[3].timeFormatOptions)
        assertEquals(RotatingCounterProgressMode.CONTINUOUS, item.templateState.phraseSets.single().counterProgressMode)
        assertEquals(5, item.templateState.fileNameSlotDrafts.size)
        assertEquals(5, item.templateState.pathSlotDrafts.size)
        assertNull(item.templateState.pathSlotDrafts[2])
        // Same JSON value, different payload bytes: the original checksum must reject it.
        root.put("payload", JSONArray(payload).toString())
        assertTrue(runCatching { LocalTemplateBackupCodec.decode(root.toString()) }.isFailure)
    }

    @Test
    fun backupRoundTripKeepsSixCellCaptureNamesPathsAndDeferredPatches() {
        val original = LocalTemplateBackupCodec.decode(fixtureText()).templates.single()
        val encoded = LocalTemplateBackupCodec.encode(listOf(original), original.id, 1792422299000L)
        val restored = LocalTemplateBackupCodec.decode(encoded).templates.single()
        val portable = original.copy(templateState = original.templateState.copy(cells = original.templateState.cells.map {
            if (it.dataType == TableCellDataType.COUNTER) it.copy(typedValue = CellValue.CounterSeed(1)) else it
        }))
        // Full supported model equality, including IDs, style, slot refs and phrase progress.
        assertEquals(portable, restored)
        val oldLocale = Locale.getDefault()
        val oldZone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.US)
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
            for (mode in SaveMode.entries) {
                val before = finalPreview(original, mode)
                val after = finalPreview(restored, mode)
                assertEquals(before.displayName, after.displayName)
                assertEquals(before.relativePathPreview, after.relativePathPreview)
                assertEquals(before.tablePatch, after.tablePatch)
                assertEquals(before.counterScope, after.counterScope)
                assertEquals(before.usedCounter, after.usedCounter)
                assertEquals(listOf("Draper", "32", "261020", "0004", "0012", "처리 B"), after.resolvedCells.map { it.resolvedText })
                assertEquals("Draper_261020_0004_처리 B_0012.jpg", after.displayName)
                assertEquals("Pictures/DZlog/연구실/Draper/1020/처리 B/", after.relativePathPreview)
                assertEquals(mapOf("counter" to "13"), after.tablePatch.updatesByCellId)
                assertEquals(mapOf("phrase" to CellPhraseProgress(0, 0)), after.tablePatch.phraseProgressByCellId)
                assertEquals(7, after.nextPhraseProgressCursor)
                val physical = capturePhysicalSavePaths(after.relativePathPreview, mode)
                assertEquals(if (mode == SaveMode.BOTH) 2 else 1, physical.size)
                assertTrue(physical.all { it.startsWith(after.relativePathPreview) })
            }
        } finally {
            Locale.setDefault(oldLocale)
            TimeZone.setDefault(oldZone)
        }
        // Planning and export never commit counter/phrase progress to the source model.
        assertEquals(CellValue.CounterSeed(47), original.templateState.cells[4].typedValue)
        assertEquals(CellValue.CounterSeed(1), restored.templateState.cells[4].typedValue)
        assertEquals(1, original.templateState.cells[5].phraseCompletedShots)
    }

    private fun finalPreview(item: SavedTableTemplate, mode: SaveMode): FinalCapturePreview {
        val now = Date(1792422299000L)
        val scope = buildScope(CaptureScopeInput(item.templateState, now, 4, "yyyyMMdd", "HHmm", "_", true, mode, 7))
        return buildCapturePreview(scope, FinalCapturePreviewInput(item.templateState, now, 4, "yyyyMMdd", "HHmm", "_", true, true, mode, 12, 7))
    }
}
