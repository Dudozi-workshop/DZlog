package com.example.dzlog.ui.table

import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState

/**
 * Source of truth: PATH_GROUP_RULES.txt
 * RULE_VERSION: 2026-02-06
 */
enum class PathGroupAction {
    NONE,
    G1,
    G2
}

/**
 * Applies a path-group action to one target cell and returns a normalized state.
 *
 * No-op policy:
 * - targetCellId not found
 * - action=G2 while no G1 exists
 * - action=G2 targeting a G1 cell
 */
internal fun applyPathGroupAction(
    state: TableTemplateState,
    targetCellId: String,
    action: PathGroupAction
): TableTemplateState {
    val target = state.cells.firstOrNull { it.cellId == targetCellId }
        ?: return state

    val updated = when (action) {
        PathGroupAction.NONE -> updateOnlyTargetGroupLevel(state, targetCellId, GroupLevel.NONE)

        PathGroupAction.G1 -> state.copy(
            cells = state.cells.map { cell ->
                when {
                    cell.cellId == targetCellId -> cell.copy(groupLevel = GroupLevel.G1)
                    cell.groupLevel == GroupLevel.G1 -> cell.copy(groupLevel = GroupLevel.NONE)
                    else -> cell
                }
            }
        )

        PathGroupAction.G2 -> {
            val hasG1 = state.cells.any { it.groupLevel == GroupLevel.G1 }
            if (!hasG1 || target.groupLevel == GroupLevel.G1) {
                return state
            }

            state.copy(
                cells = state.cells.map { cell ->
                    when {
                        cell.cellId == targetCellId -> cell.copy(groupLevel = GroupLevel.G2)
                        cell.groupLevel == GroupLevel.G2 -> cell.copy(groupLevel = GroupLevel.NONE)
                        else -> cell
                    }
                }
            )
        }
    }

    return normalizePathGroups(updated, preferredCellId = targetCellId)
}

/**
 * Source of truth: PATH_GROUP_RULES.txt
 * RULE_VERSION: 2026-02-06
 *
 * Deterministic tie-breaker:
 * 1) preferredCellId (if candidate)
 * 2) lexicographically smallest cellId
 */
internal fun normalizePathGroups(
    state: TableTemplateState,
    preferredCellId: String? = null
): TableTemplateState {
    val g1Candidates = state.cells.filter { it.groupLevel == GroupLevel.G1 }
    val g2Candidates = state.cells.filter { it.groupLevel == GroupLevel.G2 }

    val keepG1 = pickDeterministicCandidate(g1Candidates, preferredCellId)
    val keepG2BeforeRule = pickDeterministicCandidate(g2Candidates, preferredCellId)
    val keepG2 = if (keepG1 == null) null else keepG2BeforeRule

    return state.copy(
        cells = state.cells.map { cell ->
            when {
                keepG1 != null && cell.cellId == keepG1 -> cell.copy(groupLevel = GroupLevel.G1)
                keepG2 != null && cell.cellId == keepG2 -> cell.copy(groupLevel = GroupLevel.G2)
                cell.groupLevel == GroupLevel.G1 || cell.groupLevel == GroupLevel.G2 ->
                    cell.copy(groupLevel = GroupLevel.NONE)
                else -> cell
            }
        }
    )
}

private fun pickDeterministicCandidate(
    candidates: List<TableCellState>,
    preferredCellId: String?
): String? {
    if (candidates.isEmpty()) return null
    if (preferredCellId != null && candidates.any { it.cellId == preferredCellId }) {
        return preferredCellId
    }
    return candidates.minByOrNull { it.cellId }?.cellId
}

private fun updateOnlyTargetGroupLevel(
    state: TableTemplateState,
    cellId: String,
    level: GroupLevel
): TableTemplateState {
    return state.copy(
        cells = state.cells.map { cell ->
            if (cell.cellId == cellId) cell.copy(groupLevel = level) else cell
        }
    )
}
