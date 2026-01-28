package com.example.dzlog.data.template

import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.HourSystem
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellKind
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.TimeFormatOptions
import com.example.dzlog.domain.model.TimeSeparator
import org.json.JSONArray
import org.json.JSONObject

fun tableTemplateStateFromJson(json: String): TableTemplateState? {
    return runCatching {
        val root = JSONObject(json)
        val rows = root.getInt("rows")
        val cols = root.getInt("cols")
        val arr = root.getJSONArray("cells")
        val cells = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)

                val dataType = TableCellDataType.valueOf(
                    o.optString("dataType", TableCellDataType.TEXT.name)
                )

                val rawText = o.optString("rawText", "")

                // typedValue는 최소 저장만 하고, 대부분 dataType/rawText로 복원한다.
                val typedValue: CellValue = when (dataType) {
                    TableCellDataType.COUNTER -> {
                        val seed = o.optInt("counterSeed", 1).coerceAtLeast(0)
                        CellValue.CounterSeed(seed)
                    }
                    TableCellDataType.TEXT -> CellValue.Text(rawText)
                    TableCellDataType.NUMBER -> CellValue.Number(rawText)
                    else -> CellValue.Auto
                }

                val timeOpts = if (o.has("timeFormatOptions")) {
                    val t = o.getJSONObject("timeFormatOptions")
                    TimeFormatOptions(
                        hourSystem = HourSystem.valueOf(t.optString("hourSystem", HourSystem.H24.name)),
                        includeSeconds = t.optBoolean("includeSeconds", false),
                        separator = TimeSeparator.valueOf(t.optString("separator", TimeSeparator.COLON.name))
                    )
                } else null

                add(
                    TableCellState(
                        rowIndex = o.getInt("rowIndex"),
                        colIndex = o.getInt("colIndex"),
                        kind = TableCellKind.valueOf(o.getString("kind")),
                        rawText = rawText,
                        typedValue = typedValue,
                        timeFormatOptions = timeOpts,
                        fileNameInclude = o.optBoolean("fileNameInclude", false),
                        groupLevel = GroupLevel.valueOf(o.optString("groupLevel", GroupLevel.NONE.name)),
                        cellId = o.optString("cellId", java.util.UUID.randomUUID().toString()),
                        rowSpan = o.optInt("rowSpan", 1),
                        colSpan = o.optInt("colSpan", 1),
                        dataType = dataType,
                        label = o.optString("label", ""),
                        formatPattern = o.optString("formatPattern", "")
                    )
                )
            }
        }
        TableTemplateState(rows = rows, cols = cols, cells = cells)
    }.getOrNull()
}

fun defaultTableTemplateState(): TableTemplateState {
    val rows = 2
    val cols = 4
    return TableTemplateState(
        rows = rows,
        cols = cols,
        cells = listOf(
            TableCellState(
                rowIndex = 0,
                colIndex = 0,
                kind = TableCellKind.INPUT,
                rawText = "T1",
                typedValue = CellValue.Text("T1"),
                fileNameInclude = true,
                groupLevel = GroupLevel.G1,
                label = "Treatment"
            ),
            TableCellState(
                rowIndex = 0,
                colIndex = 1,
                kind = TableCellKind.INPUT,
                rawText = "S1",
                typedValue = CellValue.Text("S1"),
                fileNameInclude = true,
                groupLevel = GroupLevel.G2,
                label = "Strain"
            ),
            TableCellState(
                rowIndex = 0,
                colIndex = 2,
                kind = TableCellKind.INPUT,
                rawText = "DZlog",
                typedValue = CellValue.Text("DZlog"),
                fileNameInclude = true,
                label = "Prefix"
            ),
            TableCellState(
                rowIndex = 0,
                colIndex = 3,
                kind = TableCellKind.INPUT,
                rawText = "B3",
                typedValue = CellValue.Text("B3"),
                fileNameInclude = false,
                label = "Batch"
            ),
            TableCellState(
                rowIndex = 1,
                colIndex = 0,
                kind = TableCellKind.INPUT,
                rawText = "",
                typedValue = CellValue.Text(""),
                fileNameInclude = false,
                label = "Note"
            ),
            TableCellState(
                rowIndex = 1,
                colIndex = 1,
                kind = TableCellKind.INPUT,
                rawText = "",
                typedValue = CellValue.Text(""),
                fileNameInclude = false,
                label = "Sample"
            ),
            TableCellState(
                rowIndex = 1,
                colIndex = 2,
                kind = TableCellKind.INPUT,
                rawText = "",
                typedValue = CellValue.Text(""),
                fileNameInclude = false,
                label = "Memo 1"
            ),
            TableCellState(
                rowIndex = 1,
                colIndex = 3,
                kind = TableCellKind.INPUT,
                rawText = "",
                typedValue = CellValue.Text(""),
                fileNameInclude = false,
                label = "Memo 2"
            )
        )
    )
}

fun TableTemplateState.toJsonString(): String {
    val root = JSONObject()
    root.put("rows", rows)
    root.put("cols", cols)
    val arr = JSONArray()
    for (c in cells) {
        val o = JSONObject()
        o.put("rowIndex", c.rowIndex)
        o.put("colIndex", c.colIndex)
        o.put("kind", c.kind.name)
        o.put("rawText", c.rawText)
        o.put("fileNameInclude", c.fileNameInclude)
        o.put("groupLevel", c.groupLevel.name)
        o.put("cellId", c.cellId)
        o.put("rowSpan", c.rowSpan)
        o.put("colSpan", c.colSpan)
        o.put("dataType", c.dataType.name)
        o.put("label", c.label)
        o.put("formatPattern", c.formatPattern)

        // COUNTER seed 저장
        if (c.dataType == TableCellDataType.COUNTER) {
            val seed = (c.typedValue as? CellValue.CounterSeed)?.start ?: 1
            o.put("counterSeed", seed)
        }

        // TIME 옵션 저장
        c.timeFormatOptions?.let { t ->
            val tj = JSONObject()
            tj.put("hourSystem", t.hourSystem.name)
            tj.put("includeSeconds", t.includeSeconds)
            tj.put("separator", t.separator.name)
            o.put("timeFormatOptions", tj)
        }

        arr.put(o)
    }
    root.put("cells", arr)
    return root.toString()
}
