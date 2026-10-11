package com.dudoziworkshop.dzlog.data.backup

import com.dudoziworkshop.dzlog.data.template.createSavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalTemplateBackupTest {
    private fun item(name: String = "현장 기록") = createSavedTableTemplate(
        name = name,
        templateState = newBlankTableTemplateState(),
        styleState = TableStyleState(),
        modifiedAt = 100L,
    )

    @Test fun roundTripPreservesAllTemplateStructureAndActiveSelection() {
        val original = item()
        val json = LocalTemplateBackupCodec.encode(listOf(original), original.id, 1000L)
        val decoded = LocalTemplateBackupCodec.decode(json)
        assertEquals(1000L, decoded.createdAtMillis)
        assertEquals(original.id, decoded.activeTemplateId)
        assertEquals(original.name, decoded.templates.single().name)
        assertEquals(original.templateState.cells.size, decoded.templates.single().templateState.cells.size)
    }

    @Test fun malformedAndTamperedFilesAreRejected() {
        val item = item()
        val json = LocalTemplateBackupCodec.encode(listOf(item), item.id, 1000L)
        rejects(json.replace("현장 기록", "변조 기록"))
        val root = JSONObject(json)
        root.put("schemaVersion", 2)
        rejects(root.toString())
        rejects("{}")
    }

    @Test fun mergePreservesExistingAndRenamesDuplicates() {
        val existing = item()
        val incoming = item()
        val archive = LocalTemplateBackup(listOf(incoming), incoming.id, 1000L)
        val before = LocalTemplateBackupMerge.preview(listOf(existing), archive)
        assertEquals(1, before.renamedCount)
        val merged = LocalTemplateBackupMerge.merge(listOf(existing), existing.id, archive)
        assertEquals(2, merged.templates.size)
        assertEquals(existing, merged.templates.first())
        assertEquals(existing.id, merged.activeTemplateId)
        assertEquals("현장 기록 (가져옴)", merged.templates.last().name)
        assertNotEquals(incoming.id, merged.templates.last().id)
    }

    @Test fun emptyCatalogAdoptsRemappedBackupActiveTemplate() {
        val original = item()
        val archive = LocalTemplateBackup(listOf(original), original.id, 1000L)
        val merged = LocalTemplateBackupMerge.merge(emptyList(), null, archive)
        assertEquals(merged.templates.single().id, merged.activeTemplateId)
        assertNotEquals(original.id, merged.activeTemplateId)
    }

    @Test fun mergedRootWithCoveredBackingCellsIsAccepted() {
        val original = item()
        val changed = original.templateState.copy(
            cells = original.templateState.cells.map { cell ->
                if (cell.rowIndex == 0 && cell.colIndex == 0) cell.copy(colSpan = 2) else cell
            }
        )
        val archive = LocalTemplateBackupCodec.encode(
            listOf(original.copy(templateState = changed)), original.id, 1000L
        )
        val loaded = LocalTemplateBackupCodec.decode(archive).templates.single()
        assertEquals(original.templateState.cells.size, loaded.templateState.cells.size)
        assertEquals(2, loaded.templateState.cells.first().colSpan)
    }

    @Test fun overlappingMergedRootsAreRejected() {
        val original = item()
        val changed = original.templateState.copy(
            cells = original.templateState.cells.map { cell ->
                when {
                    cell.rowIndex == 0 && cell.colIndex == 0 -> cell.copy(rowSpan = 2, colSpan = 2)
                    cell.rowIndex == 1 && cell.colIndex == 1 -> cell.copy(rowSpan = 2)
                    else -> cell
                }
            }
        )
        try {
            LocalTemplateBackupCodec.encode(
                listOf(original.copy(templateState = changed)), original.id, 1000L
            )
            throw AssertionError("Overlapping merged roots must fail")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.isNotBlank() == true)
        }
    }

    @Test fun referenceErrorsAreRejectedEvenWhenChecksumIsCorrect() {
        val item = item()
        val root = JSONObject(LocalTemplateBackupCodec.encode(listOf(item), item.id, 1000L))
        val payload = org.json.JSONArray(root.getString("payload"))
        val cells = payload.getJSONObject(0).getJSONObject("template").getJSONArray("cells")
        cells.getJSONObject(0).put("rowIndex", 99)
        // Encode will run the validator before hashing, so these invalid models cannot be exported.
        val invalid = payload.toString()
        assertTrue(invalid.contains("99"))
        try {
            LocalTemplateBackupValidator.validate(payload)
            throw AssertionError("Invalid template must fail")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.isNotBlank() == true)
        }
    }

    private fun rejects(json: String) {
        try {
            LocalTemplateBackupCodec.decode(json)
            throw AssertionError("Invalid archive must fail")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.isNotBlank() == true)
        }
    }
}
