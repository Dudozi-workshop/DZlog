package com.example.dzlog.ui.table

import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * PathGroupReducerTest
 *
 * 목적:
 * - 저장경로(G1/G2) 규칙이 절대 깨지지 않도록 보호하는 테스트
 * - UI/Compose/에뮬 없이 "규칙 엔진(reducer)"만 검증한다
 *
 * 이 테스트들이 막아주는 것:
 * - G1이 2개 생기는 버그
 * - G2만 남는 잘못된 상태
 * - G1↔G2 스왑 로직 붕괴
 * - 무효 입력으로 상태가 망가지는 문제
 *
 * ⚠️ 앱 동작에는 아무 영향 없음
 * ⚠️ 개발 중 실수만 조기에 잡아주는 안전벨트
 */
class PathGroupReducerTest {

    // -----------------------------
    // 테스트용 기본 셀/템플릿 생성 헬퍼
    // -----------------------------

    private fun cell(
        id: String,
        group: GroupLevel = GroupLevel.NONE
    ) = TableCellState(
        rowIndex = 0,
        colIndex = 0,
        rawText = "",
        typedValue = CellValue.Auto,
        groupLevel = group,
        fileNameInclude = false,
        cellId = id,
        dataType = TableCellDataType.TEXT
    )

    private fun template(vararg cells: TableCellState) =
        TableTemplateState(
            rows = 1,
            cols = cells.size,
            cells = cells.toList()
        )

    // -----------------------------
    // 1️⃣ G1 유일성 테스트
    // -----------------------------
    @Test
    fun only_one_g1_can_exist() {
        val state = template(
            cell("A", GroupLevel.G1),
            cell("B", GroupLevel.G1) // 잘못된 legacy 상태
        )

        val normalized = normalizePathGroups(state)

        val g1Cells = normalized.cells.filter { it.groupLevel == GroupLevel.G1 }
        assertEquals(
            "G1은 항상 1개 이하만 존재해야 한다",
            1,
            g1Cells.size
        )
    }

    // -----------------------------
    // 2️⃣ G2 단독 금지 테스트
    // -----------------------------
    @Test
    fun g2_cannot_exist_without_g1() {
        val state = template(
            cell("A", GroupLevel.G2) // 불법 상태
        )

        val normalized = normalizePathGroups(state)

        val hasG2 = normalized.cells.any { it.groupLevel == GroupLevel.G2 }
        assertFalse(
            "G1 없이 G2만 존재하는 상태는 허용되지 않는다",
            hasG2
        )
    }

    // -----------------------------
    // 3️⃣ G1 ↔ G2 스왑 테스트 (핵심)
    // -----------------------------
    @Test
    fun swap_g1_and_g2_when_g2_exists() {
        val state = template(
            cell("A", GroupLevel.G1),
            cell("B", GroupLevel.G2)
        )

        val result = applyPathGroupAction(
            state = state,
            targetCellId = "A", // 현재 G1 셀
            action = PathGroupAction.G2
        )

        val a = result.cells.first { it.cellId == "A" }
        val b = result.cells.first { it.cellId == "B" }

        assertEquals("G1 셀은 G2로 내려가야 한다", GroupLevel.G2, a.groupLevel)
        assertEquals("기존 G2 셀은 G1으로 올라가야 한다", GroupLevel.G1, b.groupLevel)
    }

    // -----------------------------
    // 3-1️⃣ G1 셀에서 [G2] 클릭 (G2 미존재) → no-op
    // -----------------------------
    @Test
    fun tapping_g2_on_g1_without_existing_g2_is_noop() {
        val state = template(
            cell("A", GroupLevel.G1),
            cell("B", GroupLevel.NONE)
        )

        val result = applyPathGroupAction(
            state = state,
            targetCellId = "A", // 현재 G1 셀
            action = PathGroupAction.G2
        )

        assertEquals(
            "G2가 없는 상태에서 G1 셀을 [G2]로 누르면 스왑할 대상이 없으므로 상태가 변하지 않아야 한다",
            state,
            result
        )
    }

    // -----------------------------
    // 4️⃣ 무효 입력 no-op 테스트
    // -----------------------------
    @Test
    fun invalid_action_is_noop() {
        val state = template(
            cell("A", GroupLevel.NONE)
        )

        val result = applyPathGroupAction(
            state = state,
            targetCellId = "NOT_EXIST", // 존재하지 않는 셀
            action = PathGroupAction.G1
        )

        assertEquals(
            "무효 입력은 상태를 변경하지 않아야 한다",
            state,
            result
        )
    }
}
