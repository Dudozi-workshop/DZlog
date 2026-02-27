package com.dudoziworkshop.dzlog.domain.table

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState

/**
 * Fixed Contract: "해석이 끝난 결과 객체".
 * - UI/저장/파일명은 resolvedText만 소비한다.
 * - raw는 디버그/추적 목적이며 해석 결과에 영향을 주지 않는다.
 */
data class ResolvedCell(
    val id: String,
    val type: TableCellDataType,
    val raw: TableCellState? = null,
    val resolvedText: String,
    val isEmpty: Boolean,
    val scopeToken: String? = null,
)
