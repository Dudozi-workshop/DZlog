package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

class PhotoKitCaptureMediaIndexTest {
    private fun batchEntry(id: String, name: String, path: String = "Pictures/DZlog/A/") =
        IndexedCaptureMedia("capture-$id", PersistedCaptureAsset(
            CaptureMediaTarget(CaptureMediaRole.RESULT, path, name), id))

    @Test
    fun batchMoveValidatesEveryMemberBeforePersistingAndPreservesOtherAssets() {
        val suite = "dzlog.photokit.batch-move.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val index = PhotoKitCaptureMediaIndex(defaults)
            val entries = listOf(batchEntry("a", "A.jpg"), batchEntry("b", "B.jpg"), batchEntry("c", "C.jpg"))
            index.append(entries)
            index.setFavorite("a", true)
            assertFailsWith<IllegalArgumentException> { index.move(emptySet(), "Pictures/DZlog/B/") }
            assertFailsWith<IllegalStateException> { index.move(setOf("a", "missing"), "Pictures/DZlog/B/") }
            assertEquals(entries, index.load())
            index.move(setOf("a", "b"), "Pictures/DZlog/B/")
            val restored = PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite))
            assertEquals(listOf("Pictures/DZlog/B/", "Pictures/DZlog/B/", "Pictures/DZlog/A/"),
                restored.load().map { it.asset.target.relativePath })
            assertEquals(entries.last(), restored.load().last())
            assertEquals(setOf("a"), restored.favoriteAssetIds())
            assertTrue("Pictures/DZlog/B/" in restored.folders())
        } finally { defaults.removePersistentDomainForName(suite) }
    }

    @Test
    fun batchMoveRejectsInternalAndDestinationNameConflictsWithoutPartialChanges() {
        val suite = "dzlog.photokit.batch-conflict.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val index = PhotoKitCaptureMediaIndex(defaults)
            val entries = listOf(batchEntry("a", "Same.jpg"), batchEntry("b", "Same.jpg", "Pictures/DZlog/B/"),
                batchEntry("c", "Other.jpg"))
            index.append(entries)
            assertFailsWith<IllegalStateException> { index.move(setOf("a", "b"), "Pictures/DZlog/C/") }
            assertFailsWith<IllegalStateException> { index.move(setOf("a", "c"), "Pictures/DZlog/B/") }
            assertEquals(entries, PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite)).load())
            index.stagePending(listOf(batchEntry("pending", "Pending.jpg")))
            assertFailsWith<IllegalStateException> { index.move(setOf("a", "c"), "Pictures/DZlog/C/") }
            assertFailsWith<IllegalStateException> { index.forgetDeleted(setOf("a", "c")) }
            assertEquals(entries, index.load())
        } finally { defaults.removePersistentDomainForName(suite) }
    }

    @Test
    fun confirmedBatchDeletionRemovesOnlySelectedIdsAndFavoritesInOneSnapshot() {
        val suite = "dzlog.photokit.batch-delete.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val index = PhotoKitCaptureMediaIndex(defaults)
            val result = batchEntry("result", "Photo.jpg")
            val original = result.copy(asset = PersistedCaptureAsset(
                CaptureMediaTarget(CaptureMediaRole.ORIGINAL, "Pictures/DZlog/A/original/", "Photo.jpg"), "original"))
            val unrelated = batchEntry("other", "Other.jpg")
            index.append(listOf(result, original, unrelated))
            listOf("result", "original", "other").forEach { index.setFavorite(it, true) }
            index.forgetDeleted(setOf("result", "other"))
            val restored = PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite))
            assertEquals(listOf(original), restored.load())
            assertEquals(setOf("original"), restored.favoriteAssetIds())
            assertTrue("Pictures/DZlog/A/" in restored.folders())
            // A retry after a confirmed deletion is idempotent, not another Photos action.
            restored.forgetDeleted(setOf("result", "other"))
            assertEquals(listOf(original), restored.load())
        } finally { defaults.removePersistentDomainForName(suite) }
    }

    @Test
    fun emptyFoldersAndSubtreeRenameSurviveRelaunch() {
        val suite = "dzlog.photokit.folders.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val index = PhotoKitCaptureMediaIndex(defaults)
            val parent = index.createFolder("Pictures/DZlog/", "A")
            val child = index.createFolder(parent, "empty")
            index.append(listOf(IndexedCaptureMedia("capture", PersistedCaptureAsset(
                CaptureMediaTarget(CaptureMediaRole.RESULT, parent, "0012.jpg"), "result"))))
            assertFailsWith<IllegalArgumentException> { index.deleteEmptyFolder(parent) }
            assertFailsWith<IllegalArgumentException> { index.createFolder(parent, "../bad") }
            assertFailsWith<IllegalArgumentException> { index.createFolder(parent, "empty") }
            index.createFolder("Pictures/DZlog/", "occupied")
            assertFailsWith<IllegalArgumentException> { index.renameFolder(parent, "occupied") }
            val renamed = index.renameFolder(parent, "B")
            val restored = PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite))
            assertEquals(renamed, restored.load().single().asset.target.relativePath)
            assertTrue("Pictures/DZlog/B/empty/" in restored.folders())
            assertTrue(child !in restored.folders())
            restored.deleteEmptyFolder("Pictures/DZlog/B/empty/")
            assertTrue("Pictures/DZlog/B/empty/" !in PhotoKitCaptureMediaIndex(defaults).folders())
            assertFailsWith<IllegalArgumentException> { restored.deleteEmptyFolder("Pictures/DZlog/") }
            restored.forgetDeleted("result")
            assertTrue(renamed in restored.folders())
            restored.deleteEmptyFolder(renamed)
            assertTrue(renamed !in PhotoKitCaptureMediaIndex(defaults).folders())
        } finally { defaults.removePersistentDomainForName(suite) }
    }

    @Test
    fun legacyIndexMigratesOnFolderMutationWithoutDroppingAssets() {
        val suite = "dzlog.photokit.legacy.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            defaults.setObject("""[{"captureId":"old","role":"RESULT","relativePath":"Pictures/DZlog/A/","displayName":"old.jpg","assetId":"old-asset"}]""",
                forKey = "dzlog.ios.photokit.capture-index.v1")
            defaults.synchronize()
            val index = PhotoKitCaptureMediaIndex(defaults)
            val previous = index.load()
            assertEquals("old-asset", previous.single().asset.assetId)
            index.createFolder("Pictures/DZlog/", "empty")
            val restored = PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite))
            assertEquals(previous, restored.load())
            assertTrue("Pictures/DZlog/empty/" in restored.folders())
            assertTrue("Pictures/DZlog/A/" in restored.folders())
        } finally { defaults.removePersistentDomainForName(suite) }
    }

    @Test
    fun galleryMoveAndDeletePersistOnlyTheSelectedRole() {
        val suite = "dzlog.photokit.gallery.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val index = PhotoKitCaptureMediaIndex(defaults)
            val result = IndexedCaptureMedia("capture", PersistedCaptureAsset(
                CaptureMediaTarget(CaptureMediaRole.RESULT, "Pictures/DZlog/A/", "0012.jpg"), "result"))
            val original = IndexedCaptureMedia("capture", PersistedCaptureAsset(
                CaptureMediaTarget(CaptureMediaRole.ORIGINAL, "Pictures/DZlog/A/original/", "0012.jpg"), "original"))
            index.append(listOf(result, original))
            assertFailsWith<IllegalArgumentException> { index.move("result", "Pictures/DZlog/../") }
            assertFailsWith<IllegalArgumentException> { index.move("result", "Pictures/Other/") }
            assertFailsWith<IllegalStateException> { index.move("result", "Pictures/DZlog/A/original/") }
            assertEquals(listOf(result, original), index.load())
            index.setFavorite("result", true)
            assertEquals(setOf("result"), index.favoriteAssetIds())
            index.move("result", "Pictures/DZlog/B/")
            val restored = PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite))
            assertEquals("Pictures/DZlog/B/", restored.load().first().asset.target.relativePath)
            assertEquals(original, restored.load().last())
            assertEquals(setOf("result"), restored.favoriteAssetIds())
            restored.setFavorite("result", false)
            assertTrue(restored.favoriteAssetIds().isEmpty())
            restored.setFavorite("result", true)
            restored.forgetDeleted("result")
            assertEquals(listOf(original), PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite)).load())
            assertTrue(PhotoKitCaptureMediaIndex(defaults).favoriteAssetIds().isEmpty())
            assertFailsWith<IllegalStateException> { restored.ensureCaptureIsNew("capture") }
            assertFailsWith<IllegalStateException> { restored.move("result", "Pictures/DZlog/") }
        } finally { defaults.removePersistentDomainForName(suite) }
    }

    @Test
    fun pendingCaptureBlocksGalleryIndexMutations() {
        val suite = "dzlog.photokit.pending-gallery.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val index = PhotoKitCaptureMediaIndex(defaults)
            val entry = IndexedCaptureMedia("capture", PersistedCaptureAsset(
                CaptureMediaTarget(CaptureMediaRole.RESULT, "Pictures/DZlog/A/", "0012.jpg"), "result"))
            index.append(listOf(entry))
            index.stagePending(listOf(entry))
            assertFailsWith<IllegalStateException> { index.move("result", "Pictures/DZlog/B/") }
            assertFailsWith<IllegalStateException> { index.forgetDeleted("result") }
            assertFailsWith<IllegalStateException> { index.setFavorite("result", true) }
            assertEquals(listOf(entry), index.load())
            assertEquals(listOf(entry), index.pending())
        } finally { defaults.removePersistentDomainForName(suite) }
    }

    @Test
    fun sessionUndoForgetsAllCaptureRolesAndTheirFavoritesTogether() {
        val suite = "dzlog.photokit.undo.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val index = PhotoKitCaptureMediaIndex(defaults)
            val undone = listOf(CaptureMediaRole.RESULT to "result", CaptureMediaRole.ORIGINAL to "original")
                .map { (role, id) -> IndexedCaptureMedia("capture", PersistedCaptureAsset(
                    CaptureMediaTarget(role, if (role == CaptureMediaRole.RESULT) "Pictures/DZlog/A/"
                        else "Pictures/DZlog/A/original/", "A_0012.jpg"), id)) }
            val retained = IndexedCaptureMedia("other", PersistedCaptureAsset(
                CaptureMediaTarget(CaptureMediaRole.RESULT, "Pictures/DZlog/A/", "A_0013.jpg"), "other"))
            index.append(undone + retained)
            index.setFavorite("result", true)
            index.setFavorite("original", true)
            index.setFavorite("other", true)

            index.forgetCapture("capture")
            val restored = PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite))
            assertEquals(listOf(retained), restored.load())
            assertEquals(setOf("other"), restored.favoriteAssetIds())
            assertFailsWith<IllegalStateException> { restored.forgetCapture("capture") }
        } finally { defaults.removePersistentDomainForName(suite) }
    }

    @Test
    fun twoRolesRecoverWithLogicalPathsAfterIndexReinitialization() {
        val suite = "dzlog.photokit.test.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val first = PhotoKitCaptureMediaIndex(defaults)
            val records = listOf(
                IndexedCaptureMedia(
                    "capture-12",
                    PersistedCaptureAsset(
                        CaptureMediaTarget(CaptureMediaRole.RESULT, "Pictures/DZlog/T1/", "T1_0012.jpg"),
                        "asset-result-12",
                    ),
                ),
                IndexedCaptureMedia(
                    "capture-12",
                    PersistedCaptureAsset(
                        CaptureMediaTarget(CaptureMediaRole.ORIGINAL, "Pictures/DZlog/T1/original/", "T1_0012.jpg"),
                        "asset-original-12",
                    ),
                ),
            )
            first.stagePending(records)
            val relaunched = PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite))
            assertEquals(records, relaunched.pending())
            assertTrue(relaunched.load().isEmpty())
            assertFailsWith<IllegalStateException> { relaunched.ensureCaptureIsNew("capture-12") }

            // Simulates PhotoKit success followed by a process restart.
            relaunched.append(relaunched.pending())
            val interruptedAfterAppend = PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite))
            interruptedAfterAppend.append(interruptedAfterAppend.pending())
            interruptedAfterAppend.clearPending()

            val restored = PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite))
            assertEquals(records, restored.load())
            assertTrue(restored.pending().isEmpty())
            assertFailsWith<IllegalStateException> { restored.ensureCaptureIsNew("capture-12") }
        } finally {
            defaults.removePersistentDomainForName(suite)
        }
    }
}
