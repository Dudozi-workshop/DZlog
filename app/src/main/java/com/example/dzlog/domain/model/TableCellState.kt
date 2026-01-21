package com.example.dzlog.domain.model

data class TableCellState(
    val rowIndex: Int,
    val colIndex: Int,
    val kind: TableCellKind,
    val valueText: String,
    val fileNameInclude: Boolean = false,
    val groupLevel: GroupLevel = GroupLevel.NONE
)

enum class TableCellKind {
    BASE,
    INPUT
}

enum class GroupLevel {
    NONE,
    G1,
    G2
}