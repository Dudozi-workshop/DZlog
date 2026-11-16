package com.dudoziworkshop.dzlog.feature.table.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextFavoriteListPolicyTest {
    @Test
    fun star_adds_new_text_and_immediately_removes_existing_without_reordering_others() {
        val initial = listOf("자연", "인공", "드래퍼")
        assertEquals(
            listOf("6차", "자연", "인공", "드래퍼"),
            TextFavoriteListPolicy.toggle(initial, "6차", 20),
        )
        assertEquals(
            listOf("자연", "드래퍼"),
            TextFavoriteListPolicy.toggle(initial, "인공", 20),
        )
    }

    @Test
    fun full_list_never_evicts_existing_entries_implicitly() {
        val full = (1..20).map { "$it" }
        assertNull(TextFavoriteListPolicy.toggle(full, "새 문구", 20))
        assertEquals(full.drop(1), TextFavoriteListPolicy.toggle(full, "1", 20))
        assertNull(TextFavoriteListPolicy.toggle(full, " ", 20))
    }

    @Test
    fun editing_preserves_order_and_rejects_duplicates_or_blank() {
        val list = listOf("드래퍼", "자연", "인공")
        assertEquals(
            listOf("Draper", "자연", "인공"),
            TextFavoriteListPolicy.rename(list, "드래퍼", "Draper"),
        )
        assertNull(TextFavoriteListPolicy.rename(list, "드래퍼", "자연"))
        assertNull(TextFavoriteListPolicy.rename(list, "드래퍼", " "))
    }

    @Test
    fun manual_reordering_moves_only_requested_item() {
        val list = listOf("6차", "자연", "인공")
        assertEquals(
            listOf("자연", "6차", "인공"),
            TextFavoriteListPolicy.move(list, "6차", 1),
        )
        assertEquals(list, TextFavoriteListPolicy.move(list, "6차", -1))
        assertEquals(list, TextFavoriteListPolicy.move(list, "자연", 3))
    }
}
