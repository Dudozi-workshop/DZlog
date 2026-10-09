package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GalleryFolderNamePolicyTest {
    @Test fun trims_name_without_modifying_other_characters() {
        assertEquals("실험 1", GalleryFolderNamePolicy.normalize("  실험 1  "))
    }

    @Test fun rejects_path_injection_and_reserved_original() {
        listOf("../other", "one/two", "one\\two", "original", "ORIGINAL", ".", "..", "A:", "x|y", "")
            .forEach { name ->
                assertThrows(IllegalArgumentException::class.java) {
                    GalleryFolderNamePolicy.normalize(name)
                }
            }
    }

    @Test fun rejects_excessively_long_names_and_trailing_dot() {
        assertThrows(IllegalArgumentException::class.java) {
            GalleryFolderNamePolicy.normalize("a".repeat(81))
        }
        assertThrows(IllegalArgumentException::class.java) {
            GalleryFolderNamePolicy.normalize("Example.")
        }
    }
}
