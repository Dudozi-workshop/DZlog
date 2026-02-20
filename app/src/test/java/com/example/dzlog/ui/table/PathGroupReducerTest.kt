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
    // 3-1️⃣ G1 해제 시 G2 자동 해제(경계) 테스트
    // -----------------------------
    @Test
    fun removing_g1_must_clear_g2_as_well() {
        val state = template(
            cell("A", GroupLevel.G1),
            cell("B", GroupLevel.G2)
        )

        val result = applyPathGroupAction(
            state = state,
            targetCellId = "A", // G1 셀
            action = PathGroupAction.NONE // G1 제거
        )

        val hasG1 = result.cells.any { it.groupLevel == GroupLevel.G1 }
        val hasG2 = result.cells.any { it.groupLevel == GroupLevel.G2 }

        assertFalse("G1을 제거하면 G1은 없어야 한다", hasG1)
        assertFalse(
            "G1 없이 G2만 남는 상태는 금지이므로, G2도 같이 제거되어야 한다",
            hasG2
        )
    }

    // -----------------------------
    // 3-2️⃣ normalize 단독(결정적 tie-breaker + G2 유지 조건) 테스트
    // -----------------------------
    @Test
    fun normalize_picks_preferred_candidate_and_keeps_g2_only_when_g1_exists() {
        // legacy/오염 상태:
        // - G1이 2개(B, C)
        // - G2가 1개(A)
        val dirty = template(
            cell("A", GroupLevel.G2),
            cell("B", GroupLevel.G1),
            cell("C", GroupLevel.G1)
        )

        // preferredCellId가 G1 후보(C)에 포함되므로 C가 G1로 살아야 함
        val normalized = normalizePathGroups(dirty, preferredCellId = "C")

        val a = normalized.cells.first { it.cellId == "A" }
        val b = normalized.cells.first { it.cellId == "B" }
        val c = normalized.cells.first { it.cellId == "C" }

        assertEquals("preferredCellId(C)가 G1 후보면 C가 G1로 선택되어야 한다", GroupLevel.G1, c.groupLevel)
        assertEquals("나머지 G1 후보(B)는 정리되어야 한다", GroupLevel.NONE, b.groupLevel)
        assertEquals("G1이 존재하는 상태에서는 G2(A)는 유지 가능", GroupLevel.G2, a.groupLevel)

        // 그리고 G1이 사라지면, 어떤 이유로든 G2는 유지되면 안 됨
        val noG1 = template(cell("A", GroupLevel.G2), cell("B", GroupLevel.NONE))
        val normalizedNoG1 = normalizePathGroups(noG1, preferredCellId = "A")
        assertFalse(
            "G1 없이 G2만 남는 상태는 normalize로 반드시 제거되어야 한다",
            normalizedNoG1.cells.any { it.groupLevel == GroupLevel.G2 }
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
