package com.dudoziworkshop.dzlog.ui.table.rotating

import org.junit.Assert.*
import org.junit.Test

class RotatingPhraseSearchPolicyTest {
    private val items = listOf("초기 조사", "중간 조사", "최종 조사", "중간 조사")

    @Test fun emptyQueryPreservesAllOriginalIndices() {
        assertEquals(listOf(0, 1, 2, 3), RotatingPhraseSearchPolicy.matchingIndices(items, ""))
        assertEquals(listOf(0, 1, 2, 3), RotatingPhraseSearchPolicy.matchingIndices(items, "   "))
        assertTrue(RotatingPhraseSearchPolicy.reorderEnabled(""))
        assertTrue(RotatingPhraseSearchPolicy.reorderEnabled("   "))
    }

    @Test fun matchesKoreanPartialTextWithoutMutatingItems() {
        val before = items.toList()
        assertEquals(listOf(1, 3), RotatingPhraseSearchPolicy.matchingIndices(items, "중간"))
        assertEquals(listOf(0, 2), RotatingPhraseSearchPolicy.matchingIndices(items, "  조") .filter { items[it] != "중간 조사" })
        assertEquals(emptyList<Int>(), RotatingPhraseSearchPolicy.matchingIndices(items, "누락"))
        assertEquals(before, items)
        assertFalse(RotatingPhraseSearchPolicy.reorderEnabled("중간"))
    }

    @Test fun duplicatesRetainDistinctOriginalIndicesForEditAndDelete() {
        val matches = RotatingPhraseSearchPolicy.matchingIndices(items, "중간")
        assertEquals(listOf(1, 3), matches)
        val updated = items.toMutableList().apply { this[matches[1]] = "중간 수정" }
        assertEquals("중간 조사", updated[1])
        assertEquals("중간 수정", updated[3])
        val deleted = items.filterIndexed { index, _ -> index != matches[0] }
        assertEquals(listOf("초기 조사", "최종 조사", "중간 조사"), deleted)
    }

    @Test fun filteredMultiSelectionChangesOnlyMatchingIndicesIncludingDuplicateLabels() {
        val set = com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet(
            "id", "조사", items, defaultEvery = 5,
        )
        val selected = RotatingPhraseSearchPolicy.matchingIndices(items, "중간")
        val changed = com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseSetItemPolicy
            .setEveryForItems(set, selected, 2)
        assertEquals(listOf(1, 3), selected)
        assertEquals(listOf(5, 2, 5, 2), changed.items.indices.map(changed::everyForItem))
        assertEquals(items, changed.items)
        assertEquals(listOf(null, 2, null, 2), changed.itemEveryOverrides)
    }

    @Test fun asciiSearchIsCaseInsensitiveAndWhitespaceTrimmed() {
        val values = listOf("Duke", "Liberty", "Draper")
        assertEquals(listOf(0), RotatingPhraseSearchPolicy.matchingIndices(values, " DUKE "))
        assertEquals(listOf(1), RotatingPhraseSearchPolicy.matchingIndices(values, "bErT"))
    }

    @Test fun noMatchesDoNotDisableOtherUnrelatedOperations() {
        assertTrue(RotatingPhraseSearchPolicy.matchingIndices(items, "missing").isEmpty())
        assertFalse(RotatingPhraseSearchPolicy.reorderEnabled("missing"))
        assertEquals(4, items.size)
    }
}
