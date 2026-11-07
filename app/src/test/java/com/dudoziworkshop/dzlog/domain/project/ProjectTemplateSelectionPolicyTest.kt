package com.dudoziworkshop.dzlog.domain.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectTemplateSelectionPolicyTest {
    private fun registry(): CaptureProjectRegistry =
        CaptureProjectRegistry().rememberTemplate("general-T")
            .createProject("A", "A", 1L, "template-A")
            .createProject("B", "B", 2L, "template-B")

    @Test fun selectingAProjectRestoresOnlyItsTemplateId() {
        val choice = ProjectTemplateSelectionPolicy.choose(
            registry(), "A", listOf("general-T", "template-A", "template-B"), "template-B")
        assertEquals("template-A", choice.templateId)
        assertFalse(choice.rememberedTemplateMissing)
    }

    @Test fun selectingGeneralRestoresItsOwnLastTemplate() {
        val choice = ProjectTemplateSelectionPolicy.choose(
            registry(), null, listOf("template-A", "general-T"), "template-A")
        assertEquals("general-T", choice.templateId)
    }

    @Test fun unknownOrDeletedTemplateFallsBackToCurrentWithoutDataCloning() {
        val choice = ProjectTemplateSelectionPolicy.choose(
            registry(), "B", listOf("template-A", "general-T"), "template-A")
        assertEquals("template-A", choice.templateId)
        assertTrue(choice.rememberedTemplateMissing)
        assertEquals("template-B", registry().find("B")?.lastTemplateId)
    }

    @Test fun deletedCurrentTemplateFallsBackToFirstValid() {
        val choice = ProjectTemplateSelectionPolicy.choose(
            registry(), "A", listOf("usable"), "removed")
        assertEquals("usable", choice.templateId)
        assertTrue(choice.rememberedTemplateMissing)
    }

    @Test fun noTemplatesIsHandledWithoutInventingOne() {
        val choice = ProjectTemplateSelectionPolicy.choose(
            registry(), "A", emptyList(), null)
        assertNull(choice.templateId)
        assertTrue(choice.rememberedTemplateMissing)
    }

    @Test fun newlyCreatedProjectUsesCurrentTemplateUntilOneIsRemembered() {
        val r = CaptureProjectRegistry().createProject("new", "new", 10L)
        val choice = ProjectTemplateSelectionPolicy.choose(
            r, "new", listOf("current", "other"), "current")
        assertEquals("current", choice.templateId)
        assertFalse(choice.rememberedTemplateMissing)
    }

    @Test fun completedProjectCannotBecomeCaptureTarget() {
        val completed = registry().completeProject("A", 100L)
        assertThrows(IllegalArgumentException::class.java) {
            ProjectTemplateSelectionPolicy.choose(
                completed, "A", listOf("template-A"), "template-A")
        }
    }
}
