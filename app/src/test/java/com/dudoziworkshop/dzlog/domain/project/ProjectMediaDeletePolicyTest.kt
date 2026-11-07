package com.dudoziworkshop.dzlog.domain.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectMediaDeletePolicyTest {
    private val registry = CaptureProjectRegistry()
        .createProject("A", "A", 1L)
        .createProject("B", "B", 2L)

    private fun receipt(id: String, project: String): CaptureRecord =
        requireNotNull(CaptureReservation(id, project, "T", 10L).committed(
            resultUri = "content://media/external/images/media/" + id,
            originalUri = "content://media/external/images/media/" + id + "1",
        ))

    @Test fun targetsAreOnlyTheNamedProjectAndBothPhotos() {
        val a = receipt("4", "A")
        val result = ProjectMediaDeletePolicy.plan(
            "A", registry, listOf(a), a.mediaUris.toSet() + receipt("8", "B").mediaUris,
        )
        assertEquals(a.mediaUris, result.targetUris)
        assertEquals(2, result.fileCount)
        assertEquals(1, result.captureCount)
        assertTrue(result.allFilesAccountedFor)
    }

    @Test fun missingOriginalIsNotSilentlyReportedAsDeleted() {
        val a = receipt("4", "A")
        val plan = ProjectMediaDeletePolicy.plan("A", registry, listOf(a),
            setOf(requireNotNull(a.resultUri)))
        assertEquals(listOf(a.resultUri), plan.targetUris)
        assertEquals(listOf(a.originalUri), plan.unresolvedUris)
        assertFalse(plan.allFilesAccountedFor)
    }

    @Test fun preMigrationLegacyGroupsRemainPartOfDeletionTargets() {
        val registryV1 = registry.copy(
            projects = registry.projects.map { if (it.id == "A") it.copy(
                captureUriGroups = listOf(listOf("content://v1/result", "content://v1/original"))
            ) else it }
        )
        val result = ProjectMediaDeletePolicy.plan("A", registryV1, emptyList(),
            setOf("content://v1/result", "content://v1/original"))
        assertEquals(2, result.fileCount)
        assertEquals(0, result.captureCount)
    }

    @Test fun sameUriInLegacyAndRoomIsOnlyOneTarget() {
        val a = receipt("4", "A")
        val withV1 = registry.copy(
            projects = registry.projects.map { if (it.id == "A") it.copy(
                captureUriGroups = listOf(a.mediaUris)
            ) else it }
        )
        val result = ProjectMediaDeletePolicy.plan("A", withV1, listOf(a), a.mediaUris.toSet())
        assertEquals(2, result.fileCount)
    }

    @Test fun crossProjectRecordIsRejectedBeforeAnyDeletion() {
        val other = receipt("8", "B")
        assertThrows(IllegalArgumentException::class.java) {
            ProjectMediaDeletePolicy.plan("A", registry, listOf(other), other.mediaUris.toSet())
        }
        assertThrows(IllegalArgumentException::class.java) {
            ProjectMediaDeletePolicy.plan("missing", registry, emptyList(), emptySet())
        }
    }

    @Test fun partialDeleteOrNewCaptureNeverFinalizesProject() {
        val ids = listOf("one", "two")
        assertFalse(ProjectMediaDeletePolicy.canFinalize(ids, setOf("one"), false))
        assertFalse(ProjectMediaDeletePolicy.canFinalize(ids, ids.toSet(), true))
        assertTrue(ProjectMediaDeletePolicy.canFinalize(ids, ids.toSet(), false))
    }
}
