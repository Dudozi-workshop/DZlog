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
    val formatPattern: String = "",
    val label: String = ""
    )

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