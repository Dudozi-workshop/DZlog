package com.example.dzlog.domain.model

import java.util.UUID

data class TableCellState(
    val rowIndex: Int,
    val colIndex: Int,
    val kind: TableCellKind = TableCellKind.BASE,
    /**
     * Original text written by the user.
     *
     * 정책:
     * - TEXT로 돌아갈 때 이 값을 복원한다.
     * - DATE/TIME 선택 시에도 이 값은 보존된다.
     */
    val rawText: String = "",
    /**
     * Typed value for the cell.
     *
     * - TEXT/NUMBER/COUNTER는 사용자가 입력한 값을 보관한다.
     * - DATE/TIME은 "현재"를 표시/해결(resolution)하기 위해 주로 Resolver가 사용한다.
     */
    val typedValue: CellValue = CellValue.Text(""),
    val fileNameInclude: Boolean = false,
    val groupLevel: GroupLevel = GroupLevel.NONE,
    val cellId: String = UUID.randomUUID().toString(),
    val rowSpan: Int = 1,
    val colSpan: Int = 1,
    val dataType: TableCellDataType = TableCellDataType.TEXT,
    /**
     * Optional display/parse pattern for DATE/TIME cells.
     * Example: "yyyy.MM.dd" or "HH.mm.ss".
     */
    val formatPattern: String = "",
    /**
     * TIME 형식 옵션(초 포함, 12/24, 구분자).
     * - TIME 타입에서 사용한다.
     */
    val timeFormatOptions: TimeFormatOptions = TimeFormatOptions.DEFAULT,
    val label: String = ""
    )

sealed interface CellValue {
    data class Text(val text: String) : CellValue
    data class Number(val text: String) : CellValue
    data class Counter(val value: Int) : CellValue
    data class Date(val epochDay: Int) : CellValue
    data class Time(val secondsOfDay: Int) : CellValue
}

enum class HourSystem { H12, H24 }

enum class TimeSeparator(val ch: Char) {
    COLON(':'),
    DOT('.'),
    DASH('-')
}

data class TimeFormatOptions(
    val hourSystem: HourSystem = HourSystem.H24,
    val includeSeconds: Boolean = false,
    val separator: TimeSeparator = TimeSeparator.COLON
) {
    companion object {
        val DEFAULT = TimeFormatOptions()
    }
}

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