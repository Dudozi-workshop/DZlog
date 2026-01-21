package com.example.dzlog.domain.model

data class TableTemplateState(
    val rows: Int,
    val cols: Int,
    val cells: List<TableCellState>
)