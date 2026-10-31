package com.dudoziworkshop.dzlog.domain.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureProjectRegistryTest {
    private fun project(id: String, name: String = "조사") =
        CaptureProjectRegistry().createProject(id, name, 100L, "template-1")

    @Test fun ordinaryCaptureNeverCreatesAnAutomaticProject() {
        val empty = CaptureProjectRegistry()
        assertEquals(empty, empty.appendCommittedCapture(
            listOf("content://photo/result", "content://photo/original"),
        ))
        assertNull(empty.activeId)
        assertTrue(empty.projects.isEmpty())
    }

    @Test fun explicitCreationDoesNotSelectProjectOrMoveMedia() {
        val created = project("one", "  블루베리 조사 ")
        assertNull(created.activeId)
        assertEquals("블루베리 조사", created.find("one")?.title)
        assertEquals("template-1", created.find("one")?.lastTemplateId)
        assertTrue(created.find("one")!!.captureUriGroups.isEmpty())
    }

    @Test fun onlySelectedProjectReceivesSavedUriGroups() {
        val empty = project("one")
        val captured = empty.selectProject("one").appendCommittedCapture(
            listOf("content://photo/result", "content://photo/original"),
        )
        assertEquals("one", captured.activeId)
        assertEquals(1, captured.active?.captureCount)
        assertEquals(
            listOf("content://photo/result", "content://photo/original"),
            captured.active?.captureUriGroups?.single(),
        )
    }

    @Test fun repeatedReceiptDoesNotDuplicateCapture() {
        val first = project("one").selectProject("one")
            .appendCommittedCapture(listOf("content://photo/1"))
        val repeated = first.appendCommittedCapture(listOf("content://photo/1"))
        assertEquals(first, repeated)
        assertEquals(1, repeated.active?.captureCount)
    }

    @Test fun switchingProjectsAndReturningToGeneralShootingIsolated() {
        val a = project("one").createProject("two", "현장", 200L)
            .selectProject("one").appendCommittedCapture(listOf("content://photo/a"))
        val b = a.selectProject("two").appendCommittedCapture(listOf("content://photo/b"))
        val general = b.selectProject(null).appendCommittedCapture(listOf("content://photo/general"))
        assertNull(general.activeId)
        assertEquals(2, general.projects.size)
        assertEquals(1, general.find("one")?.captureCount)
        assertEquals(1, general.find("two")?.captureCount)
    }

    @Test fun completionPreservesCapturesAndSwitchesToGeneralShooting() {
        val captured = project("one").selectProject("one")
            .appendCommittedCapture(listOf("content://photo/1"))
        val done = captured.completeActive(200L)
        assertNull(done.activeId)
        assertEquals(200L, done.find("one")?.completedAt)
        assertFalse(done.find("one")!!.isActive)
        assertEquals(1, done.find("one")?.captureCount)
        assertEquals(done, done.appendCommittedCapture(listOf("content://photo/2")))
    }

    @Test fun reopeningProjectDoesNotSelectIt() {
        val done = project("one").selectProject("one").completeActive(200L)
        val reopened = done.reopenProject("one")
        assertNull(reopened.activeId)
        assertTrue(reopened.find("one")!!.isActive)
        assertEquals("one", reopened.selectProject("one").activeId)
    }

    @Test fun cannotSelectCompletedOrUnknownProject() {
        val done = project("one").completeProject("one", 200L)
        assertThrows(IllegalArgumentException::class.java) {
            done.selectProject("one")
        }
        assertThrows(IllegalArgumentException::class.java) {
            done.selectProject("missing")
        }
    }

    @Test fun generalAndEachProjectRememberOnlyTheirOwnTemplateId() {
        val general = CaptureProjectRegistry().rememberTemplate("general-T")
        val a = general.createProject("A", "A", 1L, "template-A")
            .createProject("B", "B", 2L, "template-B")
            .selectProject("A").rememberTemplate("template-A2")
        val b = a.selectProject("B").rememberTemplate("template-B2")
        assertEquals("general-T", b.generalLastTemplateId)
        assertEquals("template-A2", b.find("A")?.lastTemplateId)
        assertEquals("template-B2", b.find("B")?.lastTemplateId)
        assertEquals("general-T", b.selectProject(null).generalLastTemplateId)
    }

    @Test fun duplicateIdOrBlankNameCannotCreateProject() {
        val first = project("one")
        assertThrows(IllegalArgumentException::class.java) {
            first.createProject("one", "another", 200L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            first.createProject("two", "   ", 200L)
        }
    }

    @Test fun renameDoesNotChangePhotosOrTemplate() {
        val first = project("one").selectProject("one")
            .appendCommittedCapture(listOf("content://a", "content://b"))
        val next = first.rename("one", "  현장 촬영  ")
        assertEquals("현장 촬영", next.active?.title)
        assertEquals(first.active?.captureUriGroups, next.active?.captureUriGroups)
        assertEquals("template-1", next.active?.lastTemplateId)
    }

    @Test fun invalidRegistryCannotPointToCompletedProject() {
        assertThrows(IllegalArgumentException::class.java) {
            CaptureProjectRegistry(
                listOf(CaptureProject("done", "완료", 1L, completedAt = 10L)),
                activeId = "done",
            )
        }
    }

    @Test fun legacyProjectWithUriGroupsRemainsReadableByModel() {
        val legacy = CaptureProject(
            id = "old",
            title = "기존",
            createdAt = 1L,
            captureUriGroups = listOf(listOf("content://old/result", "content://old/original")),
        )
        val registry = CaptureProjectRegistry(projects = listOf(legacy), activeId = "old")
        assertEquals(1, registry.active?.captureCount)
        assertNull(registry.active?.lastTemplateId)
    }
}
