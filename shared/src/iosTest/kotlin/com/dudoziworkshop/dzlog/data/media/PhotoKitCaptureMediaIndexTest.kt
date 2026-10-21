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
