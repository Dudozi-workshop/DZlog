package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDeletePolicyTest {

    @Test
    fun `G1 선택시 루트는 exact, 개별 G1은 prefix로 계산`() {
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
    fun `G1에서 rootSelected가 false면 selected에 ROOT가 있어도 루트 exact를 만들지 않는다`() {
        val targets = buildDeleteTargetsForG1Selection(
            selected = setOf(DzlogMediaStoreReader.ROOT_G1),
            rootSelected = false,
        )

        assertTrue(targets.isEmpty())
    }

    @Test
    fun `G1에서 빈 문자열과 공백은 무시하고 prefix는 trailing slash를 유지한다`() {
        val targets = buildDeleteTargetsForG1Selection(
            selected = setOf("", "   ", "N600"),
            rootSelected = false,
        )

        assertEquals(listOf(AlbumDeleteTarget.Prefix("Pictures/DZlog/N600/")), targets)
        assertTrue(targets.all { it.relPathForAssert().endsWith("/") })
    }

    @Test
    fun `G2 선택시 그룹루트와 G2 노드 경로를 exact로 계산`() {
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
    fun `G2에서 g1 공백이면 empty를 반환한다`() {
        val targets = buildDeleteTargetsForG2Selection(
            g1 = "   ",
            selected = setOf("A1"),
            groupRootSelected = false,
        )

        assertTrue(targets.isEmpty())
    }

    @Test
    fun `G2에서 그룹 루트만 선택하면 exact 2개만 생성된다`() {
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
    fun `G2에서 g2 라벨 빈값 공백은 무시하고 exact는 trailing slash를 유지한다`() {
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
