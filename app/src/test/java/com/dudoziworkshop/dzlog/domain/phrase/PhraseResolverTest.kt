package com.dudoziworkshop.dzlog.domain.phrase

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhraseResolverTest {

    private val set = RotatingPhraseSet(
        id = "set-1",
        name = "기본",
        items = listOf("왜", "헐", "오"),
        defaultEvery = 1
    )

    @Test
    fun `progress 1 returns first phrase and next cursor 2`() {
        val selection = PhraseResolver.resolve(
            phraseSet = set,
            every = 1,
            progressCursor = 1
        )

        requireNotNull(selection)
        assertEquals("왜", selection.text)
        assertEquals(0, selection.index)
        assertEquals(2, selection.nextProgressCursor)
    }

    @Test
    fun `progress 2 with every 1 returns second phrase`() {
        val selection = PhraseResolver.resolve(
            phraseSet = set,
            every = 1,
            progressCursor = 2
        )

        requireNotNull(selection)
        assertEquals("헐", selection.text)
        assertEquals(1, selection.index)
        assertEquals(3, selection.nextProgressCursor)
    }

    @Test
    fun `every 2 keeps same phrase for two captures`() {
        val first = PhraseResolver.resolve(
            phraseSet = set,
            every = 2,
            progressCursor = 1
        )
        val second = PhraseResolver.resolve(
            phraseSet = set,
            every = 2,
            progressCursor = 2
        )

        requireNotNull(first)
        requireNotNull(second)
        assertEquals("왜", first.text)
        assertEquals("왜", second.text)
    }

    @Test
    fun `phrase wraps around when progress exceeds item size`() {
        val selection = PhraseResolver.resolve(
            phraseSet = set,
            every = 1,
            progressCursor = 4
        )

        requireNotNull(selection)
        assertEquals("왜", selection.text)
        assertEquals(0, selection.index)
        assertEquals(5, selection.nextProgressCursor)
    }

    @Test
    fun `returns null when phrase set is null or empty`() {
        val nullResult = PhraseResolver.resolve(
            phraseSet = null,
            every = 1,
            progressCursor = 1
        )
        val emptyResult = PhraseResolver.resolve(
            phraseSet = set.copy(items = emptyList()),
            every = 1,
            progressCursor = 1
        )

        assertNull(nullResult)
        assertNull(emptyResult)
    }
}
