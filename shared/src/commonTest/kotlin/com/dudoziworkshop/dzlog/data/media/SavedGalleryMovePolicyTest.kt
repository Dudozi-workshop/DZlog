package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SavedGalleryMovePolicyTest {
    private val root = SavedGalleryEntryPolicy.ROOT
    private val dest = root + "destination/"
    private fun photo(id: String, role: CaptureMediaRole = CaptureMediaRole.RESULT, capture: String = id,
        name: String = "same.jpg", path: String = root) = SavedCaptureMedia(capture,
        PersistedCaptureAsset(CaptureMediaTarget(role, path, name), id), CaptureMediaAvailability.PRESENT)

    private val result = photo("r")
    private val original = photo("o", CaptureMediaRole.ORIGINAL, result.captureId, path = root + "old/original/")
    private val other = photo("unrelated", CaptureMediaRole.ORIGINAL, path = root + "elsewhere/")
    private val all = listOf(result, original, other)

    @Test fun onlySelectedOrSameCaptureOriginalMoves() {
        val selection = setOf("r")
        assertEquals(mapOf("r" to dest), SavedGalleryMovePolicy.destinations(all, selection, dest, false))
        assertEquals(mapOf("r" to dest, "o" to dest + "original/"),
            SavedGalleryMovePolicy.destinations(all, selection, dest, true))
    }

    @Test fun explicitOriginalIsMovedExactlyOnceEvenAfterAnIndependentRename() {
        val renamed = photo("o", CaptureMediaRole.ORIGINAL, result.captureId, "renamed.jpg")
        val photos = listOf(result, renamed)
        assertEquals(listOf(renamed), SavedGalleryMovePolicy.additionalOriginals(photos, setOf("r")))
        assertEquals(mapOf("o" to dest + "original/"),
            SavedGalleryMovePolicy.destinations(photos, setOf("o"), dest, false))
        assertEquals(mapOf("r" to dest, "o" to dest + "original/"),
            SavedGalleryMovePolicy.destinations(photos, setOf("o", "r"), dest, false))
    }

    @Test fun collisionIsCheckedAtEachFinalLocation() {
        val occupant = photo("existing", path = dest + "original/")
        assertFailsWith<IllegalArgumentException> {
            SavedGalleryMovePolicy.destinations(all + occupant, setOf("r"), dest, true)
        }
        assertEquals(mapOf("r" to dest), SavedGalleryMovePolicy.destinations(all + occupant, setOf("r"), dest, false))
        assertFailsWith<IllegalArgumentException> {
            SavedGalleryMovePolicy.destinations(all + photo("other"), setOf("r", "other"), dest, false)
        }
    }

    @Test fun limitedAccessDoesNotSilentlyDropTheOriginal() {
        val photos = listOf(result, original.copy(availability = CaptureMediaAvailability.INACCESSIBLE))
        assertFailsWith<IllegalArgumentException> { SavedGalleryMovePolicy.destinations(photos, setOf("r"), dest, true) }
        assertEquals(mapOf("r" to dest), SavedGalleryMovePolicy.destinations(photos, setOf("r"), dest, false))
    }

    @Test fun originalCountsTowardsTheBatchLimit() {
        val many = (1..99).map { photo("r$it", name = "$it.jpg") } + result
        val ids = many.map { it.asset.assetId }.toSet()
        assertEquals(100, SavedGalleryMovePolicy.destinations(many + original, ids, dest, false).size)
        assertFailsWith<IllegalArgumentException> { SavedGalleryMovePolicy.destinations(many + original, ids, dest, true) }
    }
}
