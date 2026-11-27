package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

/** App-private metadata only: these checks never create or delete Photos assets. */
class PhotoKitMediaDefaultsIsolationTest {
    @Test fun separateTestSuitesKeepCountsFoldersFavoritesAndPendingIndependentAcrossReopen() {
        val suiteA = "dzlog.ios.uitest.metadata-isolation.${NSUUID().UUIDString}"
        val suiteB = "dzlog.ios.uitest.metadata-isolation.${NSUUID().UUIDString}"
        val defaultsA = NSUserDefaults(suiteName = suiteA)
        val defaultsB = NSUserDefaults(suiteName = suiteB)
        val a = PhotoKitCaptureMediaIndex(defaultsA)
        val b = PhotoKitCaptureMediaIndex(defaultsB)
        val entry = IndexedCaptureMedia("capture-a", PersistedCaptureAsset(
            CaptureMediaTarget(CaptureMediaRole.RESULT, "Pictures/DZlog/A/", "A_0012.jpg"), "fixture-asset-a"))
        a.append(listOf(entry))
        a.setFavorite(entry.asset.assetId, true)
        a.createFolder("Pictures/DZlog/", "empty-a")

        assertEquals(1, a.load().size)
        assertTrue(b.load().isEmpty())
        assertEquals(setOf("fixture-asset-a"), PhotoKitCaptureMediaRepository(defaultsA).favoriteGalleryAssetIds())
        assertTrue(PhotoKitCaptureMediaRepository(defaultsB).favoriteGalleryAssetIds().isEmpty())
        assertTrue("Pictures/DZlog/empty-a/" in PhotoKitCaptureMediaRepository(defaultsA).listGalleryFolders())
        assertFalse("Pictures/DZlog/empty-a/" in PhotoKitCaptureMediaRepository(defaultsB).listGalleryFolders())

        a.stagePending(listOf(entry.copy(captureId = "pending-a")))
        assertTrue(b.pending().isEmpty())
        val reopened = PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suiteA))
        assertEquals(listOf(entry), reopened.load())
        assertEquals("pending-a", reopened.pending().single().captureId)
        assertEquals(setOf("fixture-asset-a"), reopened.favoriteAssetIds())
        assertTrue(PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suiteB)).load().isEmpty())
    }

    @Test fun captureAndUndoPendingChecksUseTheInjectedSuiteNotAnotherTestsJournal() {
        val suiteA = "dzlog.ios.uitest.journal-isolation.${NSUUID().UUIDString}"
        val suiteB = "dzlog.ios.uitest.journal-isolation.${NSUUID().UUIDString}"
        val a = NSUserDefaults(suiteName = suiteA)
        val b = NSUserDefaults(suiteName = suiteB)
        // hasPending intentionally tests presence before decoding. Do not invoke recovery
        // on these sentinel values; journal encode/recovery have dedicated native tests.
        a.setObject("pending capture sentinel", forKey = "dzlog.ios.capture.pending-workspace.v1")
        a.setObject("pending undo sentinel", forKey = "dzlog.ios.capture.pending-undo.v1")
        assertTrue(a.synchronize())
        assertTrue(IosCaptureSaveCoordinator(PhotoKitCaptureMediaRepository(a), IosCaptureWorkspaceJournal(a)).hasPendingCapture())
        assertTrue(IosCaptureUndoCoordinator(PhotoKitCaptureMediaRepository(a), IosCaptureUndoJournal(a)).hasPendingUndo())
        assertFalse(IosCaptureSaveCoordinator(PhotoKitCaptureMediaRepository(b), IosCaptureWorkspaceJournal(b)).hasPendingCapture())
        assertFalse(IosCaptureUndoCoordinator(PhotoKitCaptureMediaRepository(b), IosCaptureUndoJournal(b)).hasPendingUndo())
        val reopened = NSUserDefaults(suiteName = suiteA)
        assertTrue(IosCaptureWorkspaceJournal(reopened).hasPending())
        assertTrue(IosCaptureUndoJournal(reopened).hasPending())
    }
}
