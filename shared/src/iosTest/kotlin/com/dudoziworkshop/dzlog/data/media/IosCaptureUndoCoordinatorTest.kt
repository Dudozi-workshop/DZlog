package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

class IosCaptureUndoCoordinatorTest {
    @Test
    fun confirmedPhotoKitDeleteRollsWorkspaceBackExactlyOnce() = withFixture { defaults, request, assets ->
        var current = request.after
        val media = FakeDeletionStore(assets)
        val coordinator = IosCaptureUndoCoordinator(media, IosCaptureUndoJournal(defaults, "undo"))

        assertTrue(coordinator.deleteAndRollback(request, { current }, { current = it; true }))
        assertEquals(request.before, current)
        assertEquals(listOf(request.captureId), media.deletedCaptureIds)
        assertFalse(coordinator.hasPendingUndo())
        assertTrue(coordinator.recoverPending({ current }, { error("must not persist twice") }))
    }

    @Test
    fun cancelledPhotoKitDeleteKeepsWorkspaceAndClearsJournal() = withFixture { defaults, request, assets ->
        var current = request.after
        val media = FakeDeletionStore(assets, deleteResult = false)
        val coordinator = IosCaptureUndoCoordinator(media, IosCaptureUndoJournal(defaults, "undo"))

        assertFalse(coordinator.deleteAndRollback(request, { current }, { current = it; true }))
        assertEquals(request.after, current)
        assertFalse(coordinator.hasPendingUndo())
    }

    @Test
    fun workspaceWriteFailureRecoversAfterDeletedAssetsDisappear() = withFixture { defaults, request, assets ->
        var current = request.after
        val media = FakeDeletionStore(assets)
        val journal = IosCaptureUndoJournal(defaults, "undo")
        val first = IosCaptureUndoCoordinator(media, journal)

        assertFalse(first.deleteAndRollback(request, { current }, { false }))
        assertEquals(request.after, current)
        assertTrue(first.hasPendingUndo())
        val reopened = IosCaptureUndoCoordinator(media, IosCaptureUndoJournal(defaults, "undo"))
        assertTrue(reopened.recoverPending({ current }, { current = it; true }))
        assertEquals(request.before, current)
        assertFalse(reopened.hasPendingUndo())
    }

    @Test
    fun inaccessibleAssetsAndUnrelatedWorkspaceEditFailClosed() = withFixture { defaults, request, assets ->
        var current = request.after
        val journal = IosCaptureUndoJournal(defaults, "undo")
        journal.stage(request, current)
        val inaccessible = assets.map { it.copy(availability = CaptureMediaAvailability.INACCESSIBLE) }
        val coordinator = IosCaptureUndoCoordinator(FakeDeletionStore(inaccessible), journal)
        assertFalse(coordinator.recoverPending({ current }, { current = it; true }))
        assertTrue(coordinator.hasPendingUndo())

        defaults.removeObjectForKey("undo")
        defaults.synchronize()
        val changed = request.after.copy(settings = request.after.settings.copy(qualityMode = 2))
        assertFailsWith<IllegalStateException> {
            runSynchronously {
                IosCaptureUndoCoordinator(FakeDeletionStore(assets), IosCaptureUndoJournal(defaults, "undo2"))
                    .deleteAndRollback(request, { changed }, { true })
            }
        }
    }

    private fun withFixture(
        block: suspend (NSUserDefaults, IosCaptureUndoRequest, List<SavedCaptureMedia>) -> Unit,
    ) = runSynchronously {
        val suite = "dzlog.capture.undo.test.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val template = SavedTableTemplate("template", "기록", TableTemplateState(1, 1, emptyList()),
                TableStyleState(), 0L)
            val before = EditorWorkspace(listOf(EditorDocument(template, nextCounter = 12)), template.id)
            val after = before.copy(documents = listOf(before.active!!.copy(nextCounter = 13)))
            val request = IosCaptureUndoRequest("capture-12", before, after)
            val assets = listOf(CaptureMediaRole.RESULT, CaptureMediaRole.ORIGINAL).map { role ->
                SavedCaptureMedia(
                    request.captureId,
                    PersistedCaptureAsset(CaptureMediaTarget(role,
                        if (role == CaptureMediaRole.RESULT) "Pictures/DZlog/A/" else "Pictures/DZlog/A/original/",
                        "A_0012.jpg"), "asset-${role.name.lowercase()}"),
                    CaptureMediaAvailability.PRESENT,
                )
            }
            block(defaults, request, assets)
        } finally {
            defaults.removePersistentDomainForName(suite)
        }
    }

    private fun <T> runSynchronously(block: suspend () -> T): T {
        var completed: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { completed = result }
        })
        return checkNotNull(completed) {
            "This test fixture must complete its suspend calls without async suspension."
        }.getOrThrow()
    }

    private class FakeDeletionStore(
        initial: List<SavedCaptureMedia>,
        private val deleteResult: Boolean = true,
    ) : IosCaptureDeletionStore {
        private var assets = initial
        val deletedCaptureIds = mutableListOf<String>()

        override suspend fun listSavedAssets(): List<SavedCaptureMedia> = assets

        override suspend fun deleteCaptureAssets(captureId: String): Boolean {
            deletedCaptureIds += captureId
            if (deleteResult) assets = assets.filterNot { it.captureId == captureId }
            return deleteResult
        }
    }
}
