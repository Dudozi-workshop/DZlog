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
            index.move("result", "Pictures/DZlog/B/")
            val restored = PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite))
            assertEquals("Pictures/DZlog/B/", restored.load().first().asset.target.relativePath)
            assertEquals(original, restored.load().last())
            restored.forgetDeleted("result")
            assertEquals(listOf(original), PhotoKitCaptureMediaIndex(NSUserDefaults(suiteName = suite)).load())
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
            assertEquals(listOf(entry), index.load())
            assertEquals(listOf(entry), index.pending())
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
