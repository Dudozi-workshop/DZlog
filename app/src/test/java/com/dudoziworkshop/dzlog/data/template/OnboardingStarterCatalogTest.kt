package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.buildFileName
import com.dudoziworkshop.dzlog.domain.naming.buildSavePath
import com.dudoziworkshop.dzlog.domain.naming.buildNamingSlotPreview
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
class OnboardingStarterCatalogTest {
    @Test
    fun `eleven unique starters are all Free 3 plus 3 compatible`() {
        val defs = OnboardingStarterCatalog.definitions
        assertEquals(11, defs.size)
        assertEquals(11, defs.map { it.id }.distinct().size)
        assertEquals(1, defs.count { it.category == "basic" })
        assertEquals(5, defs.count { it.category == "daily" })
        assertEquals(5, defs.count { it.category == "work" })
        defs.forEach { def ->
            val state = requireNotNull(OnboardingStarterCatalog.buildState(def.id))
            assertEquals(2, state.rows)
            assertEquals(def.labels.size, state.cols)
            assertEquals(state.rows * state.cols, state.cells.size)
            assertTrue(state.fileNameSlotDrafts.count { it != null } <= 3)
            assertTrue(state.pathSlotDrafts.count { it != null } <= 3)
            val headers = state.cells.filter { it.rowIndex == 0 }
            assertEquals(def.labels, headers.sortedBy { it.colIndex }.map { it.rawText })
            assertTrue(headers.none { it.quickEditable })
            val values = state.cells.filter { it.rowIndex == 1 }
            assertTrue(values.filter { it.dataType == TableCellDataType.TEXT }.all { it.rawText.isEmpty() && it.quickEditable })
            assertEquals(def.dateColumn != null, values.any { it.dataType == TableCellDataType.DATE })
            assertEquals(def.counterColumn != null, values.any { it.dataType == TableCellDataType.COUNTER })
            val validCells = values.map { it.cellId }.toSet()
            (state.fileNameSlotDrafts + state.pathSlotDrafts)
                .filterNotNull().filter { it.kind == "CELL" }
                .forEach { assertTrue(it.cellId in validCells) }
        }
    }

    @Test
    fun `catalog fallback matches the fresh basic starter and contains no legacy sample data`() {
        val fallback = defaultTableTemplateState()
        val fresh = requireNotNull(OnboardingStarterCatalog.buildState(OnboardingStarterCatalog.DEFAULT_ID))
        assertEquals(2, fallback.rows)
        assertEquals(3, fallback.cols)
        assertEquals(fresh.cells.map { it.rawText }, fallback.cells.map { it.rawText })
        assertEquals(fresh.cells.map { it.dataType }, fallback.cells.map { it.dataType })
        assertEquals(listOf("기록명", "날짜", "번호"),
            fallback.cells.filter { it.rowIndex == 0 }.map { it.rawText })
        assertEquals("DZlog", fallback.fileNameSlotDrafts.first()?.manualText)
        assertEquals("yyyyMMdd", fallback.fileNameSlotDrafts.last()?.formatPattern)
        assertTrue(fallback.pathSlotDrafts.isEmpty())
        assertFalse(fallback.cells.any { it.rawText in setOf("T1", "S1", "B3") })
        val restored = requireNotNull(tableTemplateStateFromJson(fallback.toJsonString()))
        assertEquals(fallback.cells.map { it.rawText }, restored.cells.map { it.rawText })
        assertEquals(fallback.fileNameSlotDrafts.map { it?.kind }, restored.fileNameSlotDrafts.take(3).map { it?.kind })
    }

    @Test
    fun `basic starter has one editable label and automatic date and counter`() {
        val s = requireNotNull(OnboardingStarterCatalog.buildState(OnboardingStarterCatalog.DEFAULT_ID))
        assertEquals(listOf("기록명", "날짜", "번호"), s.cells.filter { it.rowIndex == 0 }.map { it.rawText })
        assertEquals(1, s.cells.count { it.rowIndex == 1 && it.quickEditable && it.dataType == TableCellDataType.TEXT })
        assertEquals(3, s.fileNameSlotDrafts.size)
        assertEquals("DZlog", s.fileNameSlotDrafts[0]?.manualText)
        assertEquals("yyyyMMdd", s.fileNameSlotDrafts[2]?.formatPattern)
        assertTrue(s.pathSlotDrafts.isEmpty())
    }

    @Test
    fun `all starters produce valid first filenames and save paths without example values`() {
        val fixed = Date(1_792_370_400_000L)
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(fixed)
        OnboardingStarterCatalog.definitions.forEach { def ->
            val template = requireNotNull(OnboardingStarterCatalog.buildState(def.id))
            val resolved = TableResolver().plan(
                cells = template.cells, captureNow = fixed,
                config = TableResolver.Config(3, "yyyyMMdd", "HHmm"),
            ).resolvedCells
            val name = buildFileName(
                resolved, template.fileNameSlotDrafts, "_", 3, 1,
                fixed, "yyyyMMdd", "HHmm",
            )
            val folder = buildSavePath(
                resolved, template.pathSlotDrafts, fixed, "yyyyMMdd", "HHmm",
            )
            assertTrue("${def.id}: ${name}", name.endsWith("_001.jpg"))
            assertFalse("${def.id}: ${name}", name.contains("null", ignoreCase = true))
            assertFalse("${def.id}: ${folder}", folder.contains("null", ignoreCase = true))
            assertTrue("${def.id}: ${folder}", folder.startsWith("Pictures/DZlog/"))
            assertTrue("${def.id}: ${folder}", folder.endsWith("/"))
            if (def.includeFileDate) assertTrue("${def.id}: ${name}", name.contains(date))
            val allNames = template.cells.filter { it.rowIndex == 1 && it.dataType == TableCellDataType.TEXT }
            assertTrue("${def.id}: sample value carried into real template", allNames.all { it.rawText.isBlank() })
        }
    }

    @Test
    fun `research starter preserves its folder while empty sample fields are omitted`() {
        val template = requireNotNull(OnboardingStarterCatalog.buildState("research"))
        val fixed = Date(1_792_370_400_000L)
        val cells = TableResolver().plan(
            template.cells, fixed, TableResolver.Config(3, "yyyyMMdd", "HHmm"),
        ).resolvedCells
        assertEquals("DZlog_001.jpg", buildFileName(
            cells, template.fileNameSlotDrafts, "_", 3, 1, fixed, "yyyyMMdd", "HHmm",
        ))
        assertEquals("Pictures/DZlog/연구/", buildSavePath(
            cells, template.pathSlotDrafts, fixed, "yyyyMMdd", "HHmm",
        ))
    }


    @Test
    fun `preview and persisted capture names agree for all eleven filled starters`() {
        val fixed = Date(1_792_370_400_000L)
        val config = TableResolver.Config(3, "yyyyMMdd", "HHmm")
        OnboardingStarterCatalog.definitions.forEach { def ->
            val starter = requireNotNull(OnboardingStarterCatalog.buildState(def.id))
            val populated = starter.copy(cells = starter.cells.map { cell ->
                if (cell.rowIndex == 1 && cell.dataType == TableCellDataType.TEXT) {
                    val sample = "샘플${cell.colIndex}"
                    cell.copy(rawText = sample, typedValue = CellValue.Text(sample))
                } else cell
            })
            val restored = requireNotNull(tableTemplateStateFromJson(populated.toJsonString()))
            val planned = TableResolver().plan(
                cells = populated.cells, captureNow = fixed, config = config,
                counterSeedOverride = 7,
            ).resolvedCells
            val afterReadback = TableResolver().plan(
                cells = restored.cells, captureNow = fixed, config = config,
                counterSeedOverride = 7,
            ).resolvedCells
            fun fileName(cells: List<com.dudoziworkshop.dzlog.domain.table.ResolvedCell>) =
                buildFileName(cells, restored.fileNameSlotDrafts, "_", 3, 7,
                    fixed, "yyyyMMdd", "HHmm")
            fun savePath(cells: List<com.dudoziworkshop.dzlog.domain.table.ResolvedCell>) =
                buildSavePath(cells, restored.pathSlotDrafts,
                    fixed, "yyyyMMdd", "HHmm")
            val expectedName = buildFileName(planned, populated.fileNameSlotDrafts, "_", 3,
                7, fixed, "yyyyMMdd", "HHmm")
            val expectedPath = buildSavePath(planned, populated.pathSlotDrafts,
                fixed, "yyyyMMdd", "HHmm")
            val actualName = fileName(afterReadback)
            val actualPath = savePath(afterReadback)
            assertEquals("${def.id}: file name changed after template serialization",
                expectedName, actualName)
            assertEquals("${def.id}: save path changed after template serialization",
                expectedPath, actualPath)
            assertEquals("${def.id}: preview differs from the actual file name",
                actualName, buildNamingSlotPreview(true, -1, afterReadback,
                    restored.fileNameSlotDrafts, "_", 3, 7,
                    fixed, "yyyyMMdd", "HHmm").text)
            assertEquals("${def.id}: preview differs from the actual save path",
                actualPath, buildNamingSlotPreview(false, -1, afterReadback,
                    restored.pathSlotDrafts, "_", 3, 7,
                    fixed, "yyyyMMdd", "HHmm").text)
            assertTrue("${def.id}: sample text missing from filename",
                actualName.contains("샘플0"))
            if (def.pathCells.isNotEmpty()) {
                assertTrue("${def.id}: sample text missing from folder",
                    actualPath.contains("샘플0"))
            }
            assertTrue("${def.id}: counter suffix missing", actualName.endsWith("_007.jpg"))
        }
    }

    @Test
    fun `unknown starter cannot create a template`() {
        assertNull(OnboardingStarterCatalog.buildState("missing"))
    }

    @Test
    fun `readonly headers survive template serialization and default old json remains editable`() {
        val input = requireNotNull(OnboardingStarterCatalog.buildState("research"))
        val decoded = requireNotNull(tableTemplateStateFromJson(input.toJsonString()))
        assertEquals(input.cells.map { it.quickEditable }, decoded.cells.map { it.quickEditable })
        assertEquals(input.fileNameSlotDrafts.map { it?.cellId }, decoded.fileNameSlotDrafts.take(3).map { it?.cellId })
        val legacyJson = JSONObject(input.toJsonString())
        val cells = legacyJson.getJSONArray("cells")
        for (index in 0 until cells.length()) {
            cells.getJSONObject(index).remove("quickEditable")
        }
        val legacy = legacyJson.toString()
        val back = requireNotNull(tableTemplateStateFromJson(legacy))
        assertTrue(back.cells.all { it.quickEditable })
    }
}
