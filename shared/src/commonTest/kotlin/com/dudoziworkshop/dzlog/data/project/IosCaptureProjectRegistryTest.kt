package com.dudoziworkshop.dzlog.data.project

import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.EditorWorkspaceCodec
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.buildGalleryRelativePathFromSlotDrafts
import com.dudoziworkshop.dzlog.domain.time.CaptureTime
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosCaptureProjectRegistryTest {
    @Test
    fun projectPhotoDeletionRejectsConflictingOwnership() {
        val a = IosCaptureProject("a", "실험 1", 0L, captureIds = listOf("same"))
        val b = IosCaptureProject("b", "실험 2", 0L, captureIds = listOf("other"))
        assertEquals(setOf("same"), IosCaptureProjectRegistry(listOf(a, b)).exclusiveCaptureIds("a"))
        val conflict = IosCaptureProjectRegistry(listOf(a, b.copy(captureIds = listOf("same"))))
        assertFails { conflict.exclusiveCaptureIds("a") }
    }

    private fun document(id: String, folder: String, counter: Int): EditorDocument {
        val table = TableTemplateState(1, 1, listOf(TableCellState(0, 0, cellId = id)),
            pathSlotDrafts = listOf(TableEditorSlotDraft("MANUAL", "직접입력", manualText = folder)))
        return EditorDocument(SavedTableTemplate(id, id, table, TableStyleState(), 1L),
            nextCounter = counter, usesAutoNext = false)
    }

    private fun assertDestination(context: IosCaptureContext, folder: String, counter: Int) {
        val document = requireNotNull(context.workspace.active)
        val path = buildGalleryRelativePathFromSlotDrafts(emptyList(), document.template.templateState.pathSlotDrafts,
            CaptureTime(0, "en-US", "UTC"), "yyyyMMdd", "HHmm")
        assertEquals("Pictures/DZlog/$folder/", path)
        assertEquals(counter, document.nextCounter)
    }

    @Test
    fun switch_and_relaunch_restore_the_selected_templates_folder_and_number() {
        val general = document("a", "orchard", 12)
        val project = document("b", "lab", 40)
        val initial = IosCaptureContext(IosCaptureProjectRegistry().create("p", "조사", 0L, null),
            EditorWorkspace(listOf(general, project), "a"))
        val entered = initial.select("p")
        assertDestination(entered, "orchard", 12)
        val edited = entered.withWorkspace(entered.workspace.save(project.copy(nextCounter = 41)))
        assertEquals("b", edited.projects.active?.lastTemplateId)
        val restored = IosCaptureContext(
            IosCaptureProjectRegistryCodec.decode(IosCaptureProjectRegistryCodec.encode(edited.projects)),
            EditorWorkspaceCodec.decode(EditorWorkspaceCodec.encode(edited.workspace)),
        )
        assertDestination(restored.select(null), "orchard", 12)
        assertDestination(restored.select(null).select("p"), "lab", 41)
    }

    @Test
    fun leaving_corrects_stale_memory_and_missing_template_falls_back_to_the_current_document() {
        val doc = document("a", "field", 12)
        val registry = IosCaptureProjectRegistry().create("p", "조사", 0L, "deleted").rememberTemplate("old-general")
        val context = IosCaptureContext(registry, EditorWorkspace(listOf(doc), "a"))
        val entered = context.select("p")
        assertEquals("a", entered.projects.generalLastTemplateId)
        assertEquals("a", entered.projects.active?.lastTemplateId)
        assertEquals(entered, entered.select("p"))
        val advanced = entered.withWorkspace(entered.workspace.save(doc.copy(nextCounter = 13)))
        assertDestination(advanced.select(null), "field", 13)
        assertFails { context.select("missing-project") }
        assertEquals("a", context.copy(workspace = context.workspace.copy(activeTemplateId = null))
            .select("p").workspace.activeTemplateId)
    }

    @Test
    fun lifecycle_and_selection_match_android_project_contract() {
        val created = IosCaptureProjectRegistry()
            .create("p1", "  현장 조사  ", 10L, "template-a")
        assertNull(created.activeId)

        val selected = created.select("p1").rememberTemplate("template-b").appendCapture("capture-1")
        assertEquals("현장 조사", selected.active?.title)
        assertEquals("template-b", selected.active?.lastTemplateId)
        assertEquals(listOf("capture-1"), selected.active?.captureIds)

        val undone = selected.removeCapture("capture-1")
        assertTrue(undone.active?.captureIds.orEmpty().isEmpty())

        val completed = selected.complete("p1", 20L)
        assertNull(completed.activeId)
        assertFalse(requireNotNull(completed.find("p1")).isOpen)

        val reopened = completed.reopen("p1")
        assertTrue(requireNotNull(reopened.find("p1")).isOpen)
        assertNull(reopened.activeId)
    }

    @Test
    fun general_and_project_template_memory_are_independent() {
        val general = IosCaptureProjectRegistry()
            .create("p1", "프로젝트", 1L, null)
            .rememberTemplate("general-template")
        val project = general.select("p1").rememberTemplate("project-template")

        assertEquals("general-template", project.generalLastTemplateId)
        assertEquals("project-template", project.active?.lastTemplateId)
    }

    @Test
    fun codec_round_trip_is_strict_and_preserves_capture_ids() {
        val value = IosCaptureProjectRegistry()
            .create("p1", "프로젝트", 1L, "t1")
            .select("p1")
            .appendCapture("c1")
            .appendCapture("c1")
        val encoded = IosCaptureProjectRegistryCodec.encode(value)
        assertEquals(value, IosCaptureProjectRegistryCodec.decode(encoded))
        assertFails { IosCaptureProjectRegistryCodec.decode(encoded.replace("\"schema\":1", "\"schema\":2")) }
        assertFails { IosCaptureProjectRegistryCodec.decode("broken") }
    }

    @Test
    fun metadata_delete_does_not_reassign_capture_ids() {
        val registry = IosCaptureProjectRegistry()
            .create("p1", "프로젝트", 1L, null)
            .select("p1")
            .appendCapture("c1")
            .delete("p1")
        assertTrue(registry.projects.isEmpty())
        assertNull(registry.activeId)
    }
}
