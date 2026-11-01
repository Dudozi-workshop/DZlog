package com.dudoziworkshop.dzlog.domain.quickresults

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QuickResultsCaptureDeletePolicyTest {
    private fun photo(uri: String, path: String = "Pictures/DZlog/A/") =
        QuickResultPhoto(uri, "common-name.jpg", path)

    @Test fun bothMediaFilesBelongToOneCaptureDelete() {
        val capture = QuickResultCapture(
            0, "Pictures/DZlog/A/",
            result = photo("content://result"),
            original = photo("content://original", "Pictures/DZlog/A/original/"),
        )
        assertEquals(
            listOf("content://result", "content://original"),
            QuickResultsCaptureDeletePolicy.mediaUrisForCapture("content://result", listOf(capture)),
        )
    }

    @Test fun differentCaptureWithSameNameOrFolderIsNotDeleted() {
        val first = QuickResultCapture(
            0, "Pictures/DZlog/A/", photo("content://result1"),
            photo("content://original1", "Pictures/DZlog/A/original/"),
        )
        val second = QuickResultCapture(
            1, "Pictures/DZlog/A/", photo("content://result2"),
            photo("content://original2", "Pictures/DZlog/A/original/"),
        )
        assertEquals(
            listOf("content://result1", "content://original1"),
            QuickResultsCaptureDeletePolicy.mediaUrisForCapture(
                "content://result1", listOf(first, second),
            ),
        )
    }

    @Test fun originalOnlyAndResultOnlyDeleteExactlyOneFile() {
        val originalOnly = QuickResultCapture(0, "A", null, photo("content://original"))
        val resultOnly = QuickResultCapture(1, "A", photo("content://result"), null)
        assertEquals(
            listOf("content://original"),
            QuickResultsCaptureDeletePolicy.mediaUrisForCapture(
                "content://original", listOf(originalOnly, resultOnly),
            ),
        )
        assertEquals(
            listOf("content://result"),
            QuickResultsCaptureDeletePolicy.mediaUrisForCapture(
                "content://result", listOf(originalOnly, resultOnly),
            ),
        )
    }

    @Test fun staleOrDuplicatedCaptureLookupFailsClosed() {
        val capture = QuickResultCapture(0, "A", photo("content://same"), null)
        assertThrows(IllegalArgumentException::class.java) {
            QuickResultsCaptureDeletePolicy.mediaUrisForCapture(
                "content://missing", listOf(capture),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            QuickResultsCaptureDeletePolicy.mediaUrisForCapture(
                "content://same", listOf(capture, capture),
            )
        }
    }

    @Test fun duplicateOriginalAndResultUriMustNotDelete() {
        val capture = QuickResultCapture(
            0, "A", photo("content://same"), photo("content://same"),
        )
        assertThrows(IllegalArgumentException::class.java) {
            QuickResultsCaptureDeletePolicy.mediaUrisForCapture(
                "content://same", listOf(capture),
            )
        }
    }
}
