package com.example.dzlog.domain.model

import java.util.UUID

data class TableCellState(
    val rowIndex: Int,
    val colIndex: Int,
    val kind: TableCellKind = TableCellKind.BASE,
    val valueText: String = "",
    val fileNameInclude: Boolean = false,
    val groupLevel: GroupLevel = GroupLevel.NONE,
    val cellId: String = UUID.randomUUID().toString(),
    val rowSpan: Int = 1,
    val colSpan: Int = 1,
    val dataType: TableCellDataType = TableCellDataType.TEXT,
    val label: String = "",
    // DATE/TIME 전용 포맷(빈값이면 Resolver Config 기본값 사용)
    val formatPattern: String = ""    )

enum class TableCellKind {
    BASE,
    INPUT
}

enum class TableCellDataType {
    TEXT,
    NUMBER,
    DATE,
    TIME,
    COUNTER
}

enum class GroupLevel {
    NONE,
    G1,
    G2
}