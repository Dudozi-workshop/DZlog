package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDeletePolicyTest {

    @Test
    fun `g1 selection builds root exact and item prefix targets`() {
        val targets = buildDeleteTargetsForG1Selection(
            selected = setOf(DzlogMediaStoreReader.ROOT_G1, "N600"),
            rootSelected = true,
        )

        val expected = listOf(
            AlbumDeleteTarget.Exact("Pictures/DZlog/"),
            AlbumDeleteTarget.Exact("Pictures/DZlog/original/"),
            AlbumDeleteTarget.Prefix("Pictures/DZlog/N600/"),
        )

        assertEquals(expected.sortedForCompare(), targets.sortedForCompare())
    }

    @Test
    fun `g1 does not create root exact when rootSelected is false`() {
        val targets = buildDeleteTargetsForG1Selection(
            selected = setOf(DzlogMediaStoreReader.ROOT_G1),
            rootSelected = false,
        )

        assertTrue(targets.isEmpty())
    }

    @Test
    fun `g1 ignores blank labels and keeps trailing slash for prefix`() {
        val targets = buildDeleteTargetsForG1Selection(
            selected = setOf("", "   ", "N600"),
            rootSelected = false,
        )

        assertEquals(listOf(AlbumDeleteTarget.Prefix("Pictures/DZlog/N600/")), targets)
        assertTrue(targets.all { it.relPathForAssert().endsWith("/") })
    }

    @Test
    fun `g2 selection builds group root and node exact targets`() {
        val targets = buildDeleteTargetsForG2Selection(
            g1 = "N600",
            selected = setOf(DzlogMediaStoreReader.GROUP_ROOT_LABEL, "A1"),
            groupRootSelected = true,
        )

        val expected = listOf(
            AlbumDeleteTarget.Exact("Pictures/DZlog/N600/"),
            AlbumDeleteTarget.Exact("Pictures/DZlog/N600/original/"),
            AlbumDeleteTarget.Exact("Pictures/DZlog/N600/A1/"),
            AlbumDeleteTarget.Exact("Pictures/DZlog/N600/A1/original/"),
        )

        assertEquals(expected.sortedForCompare(), targets.sortedForCompare())
    }

    @Test
    fun `g2 returns empty when g1 is blank`() {
        val targets = buildDeleteTargetsForG2Selection(
            g1 = "   ",
            selected = setOf("A1"),
            groupRootSelected = false,
        )

        assertTrue(targets.isEmpty())
    }

    @Test
    fun `g2 with group root only creates two exact targets`() {
        val targets = buildDeleteTargetsForG2Selection(
            g1 = "N600",
            selected = setOf(DzlogMediaStoreReader.GROUP_ROOT_LABEL),
            groupRootSelected = true,
        )

        val expected = listOf(
            AlbumDeleteTarget.Exact("Pictures/DZlog/N600/"),
            AlbumDeleteTarget.Exact("Pictures/DZlog/N600/original/"),
        )
        assertEquals(expected.sortedForCompare(), targets.sortedForCompare())
    }

    @Test
    fun `g2 ignores blank labels and keeps trailing slash for exact`() {
        val targets = buildDeleteTargetsForG2Selection(
            g1 = "N600",
            selected = setOf("", "   ", "A1"),
            groupRootSelected = false,
        )

        val expected = listOf(
            AlbumDeleteTarget.Exact("Pictures/DZlog/N600/A1/"),
            AlbumDeleteTarget.Exact("Pictures/DZlog/N600/A1/original/"),
        )
        assertEquals(expected.sortedForCompare(), targets.sortedForCompare())
        assertTrue(targets.all { it.relPathForAssert().endsWith("/") })
    }
}

private fun List<AlbumDeleteTarget>.sortedForCompare(): List<AlbumDeleteTarget> =
    sortedBy { it.toString() }

private fun AlbumDeleteTarget.relPathForAssert(): String {
    return when (this) {
        is AlbumDeleteTarget.Exact -> rel
        is AlbumDeleteTarget.Prefix -> relPrefix
    }
}
