package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlin.test.*

class EditorWorkspaceTest {
    private fun document(id: String) = EditorDocument(SavedTableTemplate(id, id,
        TableTemplateState(1, 1, listOf(TableCellState(cellId = "$id-cell", rowIndex = 0, colIndex = 0,
            rawText = "27", dataType = TableCellDataType.COUNTER, typedValue = CellValue.CounterSeed(27)))),
        TableStyleState(bgStyle = 2), 123), nextCounter = 28, usesAutoNext = false)
    @Test fun collectionSettingsAndCounterMetadataSurviveTogether() {
        val value = EditorWorkspace(listOf(document("a"), document("b").copy(nextCounter = 72)), "b",
            WorkspaceSettings(2, true, false))
        assertEquals(value, EditorWorkspaceCodec.decode(EditorWorkspaceCodec.encode(value)))
        val legacy = document("legacy")
        assertEquals(listOf(legacy), editorWorkspaceFromLegacy(legacy, document("seed").template).documents)
        assertEquals("legacy", editorWorkspaceFromLegacy(legacy, document("seed").template).activeTemplateId)
        assertEquals(28, value.save(document("b")).documents.first { it.template.id == "a" }.nextCounter)
    }
    @Test fun rejectsDamageDuplicatesAndMissingActiveWithoutDroppingDocuments() {
        val json = EditorWorkspaceCodec.encode(EditorWorkspace(listOf(document("a")), "a"))
        assertFails { EditorWorkspaceCodec.decode(json.replace("\"schema\":2", "\"schema\":3")) }
        assertFails { EditorWorkspaceCodec.decode("broken") }
        assertFails { EditorWorkspaceCodec.decode("""{"schema":2,"documents":["broken"],"active":null,"quality":1}""") }
        assertFails { EditorWorkspace(listOf(document("a"), document("a")), "a") }
        assertFails { EditorWorkspace(listOf(document("a")), "missing") }
    }
    @Test fun catalogActionsKeepOtherTemplatesAndActiveDeletionFallback() {
        val items = listOf(document("a").template, document("b").template)
        assertEquals("renamed", renameSavedTableTemplate(items, "a", " renamed ", 200).first().name)
        assertEquals(items, renameSavedTableTemplate(items, "a", " ", 200))
        val duplicate = duplicateSavedTableTemplate(items, "a")
        assertEquals(3, duplicate.size)
        assertNotEquals("a", duplicate.first().id)
        assertEquals(items.first().templateState, duplicate.first().templateState)
        assertEquals(items.last(), duplicate.last())
        assertEquals("b", deleteSavedTableTemplate(items, "a", "a").activeTemplateId)
        assertNull(deleteSavedTableTemplate(items.take(1), "a", "a").activeTemplateId)
    }
}
