package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID

class PhotoKitProjectDeletionTest {
    private class Photos : PhotoKitAssetAccess() {
        val stored = mutableSetOf("linked", "neighbor")
        val hidden = mutableSetOf<String>()
        var readable = true
        var full = true
        var onRemove: (List<String>) -> Boolean = { ids -> stored.removeAll(ids.toSet()); true }
        val requests = mutableListOf<Set<String>>()

        override fun hasReadAccess() = readable
        override fun hasFullReadAccess() = readable && full
        override suspend fun ensureReadWriteAccess() { check(readable) }
        override fun presentIds(ids: Collection<String>): Set<String> =
            if (readable) ids.intersect(stored) - hidden else emptySet()

        override suspend fun remove(assetIds: List<String>): Boolean {
            requests.add(assetIds.toSet())
            return onRemove(assetIds)
        }
    }

    private class Fixture(defaults: NSUserDefaults) {
        val photos = Photos()
        val index = PhotoKitCaptureMediaIndex(defaults)
        val repo = PhotoKitCaptureMediaRepository(defaults, photos)
        var captureIds = setOf("capture-linked")
        var finalized = false
        init {
            index.append(photos.stored.map { id ->
                IndexedCaptureMedia("capture-$id", PersistedCaptureAsset(
                    CaptureMediaTarget(CaptureMediaRole.RESULT, SavedGalleryEntryPolicy.ROOT, "$id.jpg"), id))
            })
        }

        suspend fun confirm(plan: PhotoKitProjectDeletion, writeProject: Boolean = true) =
            repo.deleteProjectPhotos(plan, { captureIds }, { finalized = writeProject; writeProject })
    }

    private fun exercise(block: suspend Fixture.() -> Unit) {
        val suite = "dzlog.project.delete.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            var result: Result<Unit>? = null
            block.startCoroutine(Fixture(defaults), object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(outcome: Result<Unit>) { result = outcome }
            })
            checkNotNull(result).getOrThrow()
        } finally { defaults.removePersistentDomainForName(suite) }
    }

    @Test
    fun cancellationLeavesProjectAndPhotosForRetry() = exercise {
        val plan = repo.prepareProjectDeletion(captureIds)
        photos.onRemove = { false }
        assertFailsWith<IllegalStateException> { confirm(plan) }
        assertFalse(finalized)
        assertEquals(setOf("linked", "neighbor"), photos.stored)
        assertEquals(photos.stored, index.load().map { it.asset.assetId }.toSet())
    }

    @Test
    fun reducedAccessCannotTurnHiddenPhotosIntoDeletedPhotos() = exercise {
        val plan = repo.prepareProjectDeletion(captureIds)
        photos.full = false
        photos.hidden.add("linked")
        assertFailsWith<IllegalStateException> { confirm(plan) }
        assertTrue(photos.requests.isEmpty())
        assertFalse(finalized)
        assertEquals(2, index.load().size)
    }

    @Test
    fun permissionRevokedDuringSystemPromptPreservesTracking() = exercise {
        val plan = repo.prepareProjectDeletion(captureIds)
        photos.onRemove = { ids -> photos.stored.removeAll(ids.toSet()); photos.readable = false; true }
        assertFailsWith<IllegalStateException> { confirm(plan) }
        assertFalse(finalized)
        assertEquals(2, index.load().size)
    }

    @Test
    fun registryRetryDoesNotDeleteSameFolderNeighborOrPromptTwice() = exercise {
        val plan = repo.prepareProjectDeletion(captureIds)
        assertFailsWith<IllegalStateException> { confirm(plan, writeProject = false) }
        assertFalse(finalized)
        assertEquals(2, index.load().size)
        val retry = repo.prepareProjectDeletion(captureIds)
        assertEquals(1, retry.alreadyMissingCount)
        confirm(retry)
        assertTrue(finalized)
        assertEquals(listOf(setOf("linked")), photos.requests)
        assertEquals(setOf("neighbor"), photos.stored)
        assertEquals("neighbor", index.load().single().asset.assetId)
    }
}
