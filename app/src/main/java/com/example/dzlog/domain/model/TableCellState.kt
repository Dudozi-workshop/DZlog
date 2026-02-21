package com.example.dzlog.domain.model

import java.util.UUID

/**
 * Core Rule:
 * - rawText는 사용자가 TEXT 편집으로 입력한 "원본 텍스트"를 보존한다. (타입 변경으로 자동 변경 금지)
 * - 실제 표시/저장용 값은 dataType/typedValue/options + captureNow(날짜/시간) 등으로 계산한다.
 */
data class TableCellState(
    val rowIndex: Int,
    val colIndex: Int,
    val kind: TableCellKind = TableCellKind.BASE,

    /** 사용자가 TEXT 편집에서 입력한 원본 텍스트(복원용). */
    val rawText: String = "",

    /** 타입별 값(정석형). TEXT/NUMBER/COUNTER seed 등 "의미값" 보관. */
    val typedValue: CellValue = CellValue.Auto,

    /** TIME 표시 옵션(토글 UI 연결용). TIME에서만 사용. */
    val timeFormatOptions: TimeFormatOptions? = null,

    val groupLevel: GroupLevel = GroupLevel.NONE,
    val cellId: String = UUID.randomUUID().toString(),
    val rowSpan: Int = 1,
    val colSpan: Int = 1,
    val dataType: TableCellDataType = TableCellDataType.TEXT,
    val phraseSetId: String? = null,
    val everyOverride: Int? = null,

    /**
     * Optional display pattern (currently used for DATE; TIME는 향후 timeFormatOptions로 완전 대체).
     * Example: "yyyy.MM.dd"
     */
    val formatPattern: String = ""
) {
    /**
     * UI(TextField)에 넣을 "편집용 문자열".
     *
     * 정책:
     * - TEXT/NUMBER/COUNTER만 인라인 편집 대상으로 본다.
     * - DATE/TIME은 값 자체를 타이핑으로 편집하지 않고(captureNow 자동), 옵션 팝업으로만 설정한다.
     */
    fun toEditableText(): String {
        return when (dataType) {
            TableCellDataType.TEXT -> rawText
            TableCellDataType.NUMBER -> rawText
            TableCellDataType.COUNTER -> when (val v = typedValue) {
                is CellValue.CounterSeed -> v.start.toString()
                else -> "1"
            }
            TableCellDataType.DATE,
            TableCellDataType.TIME,
            TableCellDataType.ROTATING_TEXT -> ""
        }
    }
}

sealed interface CellValue {
    /** DATE/TIME 등 "자동 적용" 타입(저장된 값이 없음). */
    data object Auto : CellValue

    data class Text(val text: String) : CellValue
    data class Number(val text: String) : CellValue

    /** COUNTER 시작값(seed). 저장 성공 시 patch로 증가되며 업데이트됨. */
    data class CounterSeed(val start: Int) : CellValue
}

data class TimeFormatOptions(
    val hourSystem: HourSystem = HourSystem.H24,
    val includeSeconds: Boolean = false,
    val separator: TimeSeparator = TimeSeparator.COLON
)

enum class HourSystem { H12, H24 }
enum class TimeSeparator(val token: String) {
    COLON(":"),
    NONE("")
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
    COUNTER,
    ROTATING_TEXT
}

enum class GroupLevel {
    NONE,
    G1,
    G2
}
