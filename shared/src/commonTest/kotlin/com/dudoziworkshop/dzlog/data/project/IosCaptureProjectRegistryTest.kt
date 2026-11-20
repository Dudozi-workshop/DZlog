package com.dudoziworkshop.dzlog.data.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosCaptureProjectRegistryTest {
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
