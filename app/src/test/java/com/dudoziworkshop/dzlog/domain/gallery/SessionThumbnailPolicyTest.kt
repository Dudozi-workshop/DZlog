package com.dudoziworkshop.dzlog.domain.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionThumbnailPolicyTest {
    private data class Photo(val id: String, val original: Boolean = false)

    private fun find(
        groups: List<List<String>>,
        visible: Map<String, Photo>,
    ) = SessionThumbnailPolicy.newestAvailable(
        groupsOldestFirst = groups,
        lookup = visible::get,
        isOriginal = Photo::original,
    )

    @Test fun changingNextSavePathDoesNotEraseLastCommittedThumbnail() {
        val committed = listOf(listOf("A1"))
        assertEquals(
            "A1",
            find(committed, mapOf("A1" to Photo("A1")))?.id,
        )
        // Switching the NEXT destination path does not mutate committed URIs.
        assertEquals(
            "A1",
            find(committed, mapOf("A1" to Photo("A1")))?.id,
        )
        assertEquals(
            "B1",
            find(
                committed + listOf(listOf("B1")),
                mapOf("A1" to Photo("A1"), "B1" to Photo("B1")),
            )?.id,
        )
    }

    @Test fun processedPhotoWinsEvenWhenOriginalUriIsFirst() {
        assertEquals(
            "result",
            find(
                listOf(listOf("original", "result")),
                mapOf(
                    "original" to Photo("original", original = true),
                    "result" to Photo("result"),
                ),
            )?.id,
        )
    }

    @Test fun originalFallbackAndPreviousCaptureWorkAfterDeletion() {
        val groups = listOf(listOf("old"), listOf("result", "original"))
        val both = mapOf(
            "old" to Photo("old"),
            "original" to Photo("original", original = true),
        )
        assertEquals("original", find(groups, both)?.id)
        assertEquals("old", find(groups, mapOf("old" to Photo("old")))?.id)
        assertNull(find(groups, emptyMap()))
    }

    @Test fun duplicateUriInsideOneShutterDoesNotChangePriority() {
        assertEquals(
            "result",
            find(
                listOf(listOf("original", "original", "result")),
                mapOf(
                    "original" to Photo("original", original = true),
                    "result" to Photo("result"),
                ),
            )?.id,
        )
    }
}
