package com.dudoziworkshop.dzlog.domain.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureProjectRegistryTest {
    @Test fun firstCaptureStartsProjectWithoutMovingFiles() {
        val started = CaptureProjectRegistry().appendCommittedCapture(
            listOf("content://photo/result", "content://photo/original"), "one", 1000,
        )
        assertEquals("one", started.activeId)
        assertEquals("프로젝트 1", started.active?.title)
        assertEquals(1, started.active?.captureCount)
        assertEquals(
            listOf("content://photo/result", "content://photo/original"),
            started.active?.captureUriGroups?.single(),
        )
    }

    @Test fun repeatReceiptDoesNotDuplicateCapture() {
        val first = CaptureProjectRegistry().appendCommittedCapture(listOf("content://photo/1"), "one", 100)
        val repeated = first.appendCommittedCapture(listOf("content://photo/1"), "unused", 110)
        assertEquals(first, repeated)
        assertEquals(1, repeated.active?.captureCount)
    }

    @Test fun continuingSessionAddsGroupsToSameProject() {
        val first = CaptureProjectRegistry().appendCommittedCapture(listOf("content://photo/1"), "one", 1)
        val second = first.appendCommittedCapture(listOf("content://photo/2"), "two", 2)
        assertEquals(1, second.projects.size)
        assertEquals("one", second.activeId)
        assertEquals(2, second.active?.captureCount)
    }

    @Test fun completionRetainsHistoryAndCreatesANewProjectForNextReceipt() {
        val first = CaptureProjectRegistry().appendCommittedCapture(listOf("content://photo/1"), "one", 1)
        val done = first.completeActive(10)
        assertNull(done.activeId)
        assertEquals(10L, done.find("one")?.completedAt)
        assertFalse(done.find("one")!!.isActive)
        val next = done.appendCommittedCapture(listOf("content://photo/2"), "two", 20)
        assertEquals(2, next.projects.size)
        assertEquals(1, next.find("one")?.captureCount)
        assertEquals("two", next.activeId)
        assertTrue(next.active!!.isActive)
    }

    @Test fun projectRenameDoesNotChangeCaptureUris() {
        val first = CaptureProjectRegistry().appendCommittedCapture(listOf("content://a", "content://b"), "one", 1)
        val next = first.rename("one", "  현장 촬영  ")
        assertEquals("현장 촬영", next.active?.title)
        assertEquals(first.active?.captureUriGroups, next.active?.captureUriGroups)
    }

    @Test fun invalidRegistryCannotPointToCompletedProject() {
        assertThrows(IllegalArgumentException::class.java) {
            CaptureProjectRegistry(
                listOf(CaptureProject("done", "완료", 1, completedAt = 10)),
                activeId = "done",
            )
        }
    }
}
