package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellKind
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

/**
 * Immutable starter definitions. Merely browsing this catalog must not persist a template.
 * A caller creates one SavedTableTemplate only after explicit user confirmation.
 */
internal data class OnboardingStarterDefinition(
    val id: String,
    val title: String,
    val category: String,
    val labels: List<String>,
    val dateColumn: Int? = null,
    val counterColumn: Int? = null,
    val fileNameCells: List<Int> = emptyList(),
    val includeFileDate: Boolean = false,
    val filePrefix: String? = null,
    val pathPrefix: String? = null,
    val pathCells: List<Int> = emptyList(),
)

internal object OnboardingStarterCatalog {
    const val DEFAULT_ID = "basic"
    val definitions: List<OnboardingStarterDefinition> = listOf(
        OnboardingStarterDefinition("basic", "기본 촬영", "basic",
            listOf("기록명", "날짜", "번호"), dateColumn = 1, counterColumn = 2,
            fileNameCells = listOf(0), includeFileDate = true, filePrefix = "DZlog"),
        OnboardingStarterDefinition("daily", "일상 기본 기록", "daily",
            listOf("기록명", "대상", "날짜"), dateColumn = 2,
            fileNameCells = listOf(0, 1), includeFileDate = true,
            pathPrefix = "일상", pathCells = listOf(0)),
        OnboardingStarterDefinition("work", "업무 기본 기록", "work",
            listOf("업무명", "대상", "구분", "날짜"), dateColumn = 3,
            fileNameCells = listOf(0, 1, 2), pathCells = listOf(0, 1)),
        OnboardingStarterDefinition("trip", "여행 기록", "daily",
            listOf("여행명", "장소", "날짜"), dateColumn = 2,
            fileNameCells = listOf(0, 1), includeFileDate = true,
            pathPrefix = "여행", pathCells = listOf(0)),
        OnboardingStarterDefinition("plant", "식물 관찰", "daily",
            listOf("식물명", "관찰 내용", "날짜"), dateColumn = 2,
            fileNameCells = listOf(0, 1), includeFileDate = true,
            pathPrefix = "식물", pathCells = listOf(0)),
        OnboardingStarterDefinition("pet", "반려동물 기록", "daily",
            listOf("이름", "활동", "날짜"), dateColumn = 2,
            fileNameCells = listOf(0, 1), includeFileDate = true,
            pathPrefix = "반려동물", pathCells = listOf(0)),
        OnboardingStarterDefinition("craft", "취미 제작", "daily",
            listOf("작품명", "단계", "날짜"), dateColumn = 2,
            fileNameCells = listOf(0, 1), includeFileDate = true,
            pathPrefix = "제작", pathCells = listOf(0)),
        OnboardingStarterDefinition("site", "현장 기록", "work",
            listOf("현장명", "구역", "구분", "날짜"), dateColumn = 3,
            fileNameCells = listOf(0, 1, 2), pathCells = listOf(0, 1)),
        OnboardingStarterDefinition("inspection", "시설 점검", "work",
            listOf("시설명", "점검항목", "결과", "날짜"), dateColumn = 3,
            fileNameCells = listOf(0, 1), includeFileDate = true,
            pathPrefix = "점검", pathCells = listOf(0)),
        OnboardingStarterDefinition("research", "연구·실험", "work",
            listOf("시료", "조건", "회차"),
            fileNameCells = listOf(0, 1, 2),
            pathPrefix = "연구", pathCells = listOf(0)),
        OnboardingStarterDefinition("quality", "제조·품질", "work",
            listOf("제품", "공정", "검사구분", "날짜"), dateColumn = 3,
            fileNameCells = listOf(0, 1, 2), pathPrefix = "품질", pathCells = listOf(0)),
    )

    fun find(id: String): OnboardingStarterDefinition? =
        definitions.firstOrNull { it.id == id }

    fun buildState(id: String): TableTemplateState? {
        val def = find(id) ?: return null
        val columns = def.labels.size
        val headers = def.labels.mapIndexed { col, label ->
            TableCellState(
                rowIndex = 0, colIndex = col, kind = TableCellKind.BASE,
                rawText = label, typedValue = CellValue.Text(label),
                dataType = TableCellDataType.TEXT, quickEditable = false,
            )
        }
        val inputs = def.labels.indices.map { col ->
            val type = when (col) {
                def.dateColumn -> TableCellDataType.DATE
                def.counterColumn -> TableCellDataType.COUNTER
                else -> TableCellDataType.TEXT
            }
            TableCellState(
                rowIndex = 1, colIndex = col, kind = TableCellKind.INPUT,
                rawText = "", typedValue = when (type) {
                    TableCellDataType.COUNTER -> CellValue.CounterSeed(1)
                    TableCellDataType.DATE -> CellValue.Auto
                    else -> CellValue.Text("")
                },
                dataType = type,
                formatPattern = if (type == TableCellDataType.DATE) "yyMMdd" else "",
            )
        }
        fun cellSlot(col: Int) = TableEditorSlotDraft(
            kind = "CELL", label = "셀", cellId = inputs[col].cellId,
        )
        fun manualSlot(text: String) = TableEditorSlotDraft(
            kind = "MANUAL", label = "직접입력", manualText = text,
        )
        val fileName = buildList {
            def.filePrefix?.let { add(manualSlot(it)) }
            def.fileNameCells.forEach { add(cellSlot(it)) }
            if (def.includeFileDate) add(
                TableEditorSlotDraft(
                    kind = "FORMAT", label = "날짜",
                    formatType = "DATE", formatPattern = "yyyyMMdd",
                )
            )
        }
        val path = buildList {
            def.pathPrefix?.let { add(manualSlot(it)) }
            def.pathCells.forEach { add(cellSlot(it)) }
        }
        check(fileName.size <= 3 && path.size <= 3) {
            "Starter must remain usable with Free 3+3 slot limits: ${def.id}"
        }
        return TableTemplateState(
            rows = 2, cols = columns, cells = headers + inputs,
            fileNameSlotDrafts = fileName,
            pathSlotDrafts = path,
        )
    }
}
