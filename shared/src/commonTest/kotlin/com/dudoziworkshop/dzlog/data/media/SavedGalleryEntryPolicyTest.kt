package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SavedGalleryEntryPolicyTest {
    private val root = SavedGalleryEntryPolicy.ROOT
    private fun photo(id: String, date: Long?, path: String = root,
        role: CaptureMediaRole = CaptureMediaRole.RESULT) = SavedCaptureMedia(
        "capture-$id", PersistedCaptureAsset(CaptureMediaTarget(role, path, "$id.jpg"), id),
        CaptureMediaAvailability.PRESENT, date,
    )

    @Test fun rootPredicatesNormalizeTrailingSeparatorsOnly() {
        listOf("Pictures/DZlog", root, root + "/").forEach {
            assertTrue(SavedGalleryEntryPolicy.isDirectRootPhoto(it))
        }
        listOf("Pictures/DZlog2/", "Pictures/dzlog/", "Pictures/DZlog/child/", " Pictures/DZlog/").forEach {
            assertFalse(SavedGalleryEntryPolicy.isDirectRootPhoto(it))
        }
        assertTrue(SavedGalleryEntryPolicy.isDirectRootOriginalPhoto(root + "original"))
        assertTrue(SavedGalleryEntryPolicy.isDirectRootOriginalPhoto(root + "original//"))
        assertFalse(SavedGalleryEntryPolicy.isDirectRootOriginalPhoto(root + "original/nested/"))
        assertFalse(SavedGalleryEntryPolicy.isDirectRootOriginalPhoto(root + "child/original/"))
    }

    @Test fun defaultAndOriginalEntriesAreDisjointAndPathBased() {
        val direct = photo("result", 10)
        val movedOriginal = photo("moved-original", 20, role = CaptureMediaRole.ORIGINAL)
        val original = photo("original", 30, root + "original/", CaptureMediaRole.ORIGINAL)
        val nested = photo("nested", 40, root + "custom/")
        val nestedOriginal = photo("nested-original", 50, root + "custom/original/", CaptureMediaRole.ORIGINAL)
        val input = listOf(direct, movedOriginal, original, nested, nestedOriginal)
        assertEquals(listOf(direct, movedOriginal), SavedGalleryEntryPolicy.defaultPhotos(input))
        assertEquals(listOf(original), SavedGalleryEntryPolicy.rootOriginalPhotos(input))
        assertEquals(listOf(direct, movedOriginal, original, nested, nestedOriginal), input)
    }

    @Test fun recentIncludesOriginalOnlyAndBothCaptureRoles() {
        val onlyOriginal = photo("only-original", 30, root + "custom/original/", CaptureMediaRole.ORIGINAL)
        val result = photo("result", 20)
        val companion = photo("original", 20, root + "original/", CaptureMediaRole.ORIGINAL)
            .copy(captureId = result.captureId)
        val input = listOf(result, onlyOriginal, companion)
        assertEquals(listOf(onlyOriginal, result, companion), SavedGalleryEntryPolicy.recentAll(input))
        assertEquals(SavedGalleryEntryPolicy.recentAll(input), SavedGalleryEntryPolicy.recentPreview(input))
    }

    @Test fun previewCapsAtNineWithoutTruncatingAllOrMutatingInput() {
        val input = (1..12).map { photo("photo-$it", it.toLong()) }
        assertEquals((12 downTo 4).map { "photo-$it" },
            SavedGalleryEntryPolicy.recentPreview(input).map { it.asset.assetId })
        assertEquals(12, SavedGalleryEntryPolicy.recentAll(input).size)
        assertEquals((1..12).map { "photo-$it" }, input.map { it.asset.assetId })
    }

    @Test fun nullDatesAndEmptyInputsFollowExistingNewestPolicy() {
        val input = listOf(photo("unknown", null), photo("old", 1), photo("new", 2))
        assertEquals(SavedGalleryPhotoSort.NEWEST.sorted(input), SavedGalleryEntryPolicy.recentAll(input))
        assertEquals("unknown", SavedGalleryEntryPolicy.recentPreview(input).last().asset.assetId)
        assertEquals(emptyList(), SavedGalleryEntryPolicy.recentPreview(emptyList()))
        assertEquals(emptyList(), SavedGalleryEntryPolicy.recentAll(emptyList()))
        assertEquals(emptyList(), SavedGalleryEntryPolicy.defaultPhotos(emptyList()))
        assertEquals(emptyList(), SavedGalleryEntryPolicy.rootOriginalPhotos(emptyList()))
    }
}
