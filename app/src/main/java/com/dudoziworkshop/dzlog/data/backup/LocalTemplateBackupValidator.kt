package com.dudoziworkshop.dzlog.data.backup

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellKind
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import org.json.JSONArray
import org.json.JSONObject

/** Reject malformed or lossy archives before using the existing permissive catalog parser. */
internal object LocalTemplateBackupValidator {
    fun validate(array: JSONArray) {
        require(array.length() in 1..500) { "백업된 템플릿 수가 올바르지 않습니다." }
        val templateIds = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: error("템플릿 형식이 올바르지 않습니다.")
            val id = requiredString(item, "id")
            require(templateIds.add(id)) { "템플릿 ID가 중복되었습니다." }
            requiredString(item, "name")
            val modifiedAt = item.opt("modifiedAt")
            require(modifiedAt is Number && modifiedAt.toLong() >= 0) { "수정 시각이 올바르지 않습니다." }
            val table = item.optJSONObject("template") ?: error("표 데이터가 없습니다.")
            val rows = exactInt(table, "rows", 1..100)
            val cols = exactInt(table, "cols", 1..100)
            val cells = table.optJSONArray("cells") ?: error("셀 데이터가 없습니다.")
            require(cells.length() in 1..10_000) { "셀 수가 올바르지 않습니다." }

            val ids = mutableSetOf<String>()
            // Real editor retains one backing cell per coordinate, even when a
            // merged root visually covers neighboring cells. Those covered
            // backing cells are valid and must not be treated as overlapping.
            val anchors = mutableSetOf<Pair<Int, Int>>()
            val mergedCoverage = mutableSetOf<Pair<Int, Int>>()
            for (index in 0 until cells.length()) {
                val c = cells.optJSONObject(index) ?: error("셀 형식이 올바르지 않습니다.")
                val cellId = requiredString(c, "cellId")
                require(ids.add(cellId)) { "셀 ID가 중복되었습니다." }
                val r = exactInt(c, "rowIndex", 0 until rows)
                val col = exactInt(c, "colIndex", 0 until cols)
                require(anchors.add(r to col)) { "셀 위치가 중복되었습니다." }
                val rs = exactInt(c, "rowSpan", 1..rows)
                val cs = exactInt(c, "colSpan", 1..cols)
                require(r + rs <= rows && col + cs <= cols) { "병합 셀 범위를 벗어났습니다." }
                if (rs > 1 || cs > 1) {
                    for (rr in r until r + rs) for (cc in col until col + cs) {
                        require(mergedCoverage.add(rr to cc)) { "병합 셀 영역이 겹칩니다." }
                    }
                }
                enumValue<TableCellKind>(requiredString(c, "kind"))
                enumValue<TableCellDataType>(requiredString(c, "dataType"))
                enumValue<GroupLevel>(requiredString(c, "groupLevel"))
                require(c.opt("rawText") is String) { "셀 문자열이 올바르지 않습니다." }
                c.optString("phraseSetId").takeIf { it.isNotBlank() }?.let { phraseId ->
                    require(phraseId.length < 200) { "순환문구 참조가 잘못되었습니다." }
                }
            }
            require(anchors.size == rows * cols) { "셀 구성에 빈 영역이 있습니다." }
            checkWeights(table, "rowWeights", rows)
            checkWeights(table, "colWeights", cols)
            val phraseSets = table.optJSONArray("phraseSets")
            val phraseIds = mutableSetOf<String>()
            if (phraseSets != null) for (index in 0 until phraseSets.length()) {
                val p = phraseSets.optJSONObject(index) ?: error("순환문구 세트가 올바르지 않습니다.")
                require(phraseIds.add(requiredString(p, "id"))) { "순환문구 ID가 중복되었습니다." }
                val phrases = p.optJSONArray("items") ?: error("순환문구 목록이 없습니다.")
                require(phrases.length() <= 1000) { "순환문구 항목 수가 올바르지 않습니다." }
                for (j in 0 until phrases.length()) {
                    require(phrases.opt(j) is String && phrases.getString(j).isNotBlank()) {
                        "순환문구 항목이 올바르지 않습니다."
                    }
                }
                require(exactInt(p, "defaultEvery", 1..100000) > 0)
            }
            for (index in 0 until cells.length()) {
                val c = cells.getJSONObject(index)
                val phraseId = c.optString("phraseSetId")
                if (phraseId.isNotBlank()) require(phraseId in phraseIds) {
                    "순환문구 참조를 찾을 수 없습니다."
                }
            }
            checkSlots(table, "fileNameSlotDrafts", 5, ids)
            checkSlots(table, "pathSlotDrafts", 5, ids)
            val style = item.optJSONObject("style") ?: error("표 스타일이 없습니다.")
            exactInt(style, "bgStyle", 0..2)
            exactInt(style, "textColorMode", 0..1)
            exactInt(style, "manualTextColor", 0..1)
            exactInt(style, "valueScale", 60..160)
            exactInt(style, "textAlign", 0..2)
            exactInt(style, "bgAlpha", 0..255)
            require(style.opt("gridEnabled") is Boolean) { "격자 스타일 값이 올바르지 않습니다." }
        }
    }

    private fun checkWeights(table: JSONObject, key: String, size: Int) {
        if (!table.has(key)) return
        val arr = table.optJSONArray(key) ?: error("표 비율이 올바르지 않습니다.")
        require(arr.length() == size) { "표 비율 수가 맞지 않습니다." }
        for (i in 0 until arr.length()) {
            val value = arr.opt(i)
            require(value is Number && value.toDouble().isFinite() && value.toDouble() > 0) {
                "표 비율이 올바르지 않습니다."
            }
        }
    }

    private fun checkSlots(table: JSONObject, key: String, max: Int, cellIds: Set<String>) {
        val slots = table.optJSONArray(key) ?: error("저장 규칙이 없습니다.")
        require(slots.length() <= max) { "저장 규칙 슬롯이 너무 많습니다." }
        for (i in 0 until slots.length()) {
            val slot = slots.opt(i)
            if (slot == JSONObject.NULL) continue
            val obj = slot as? JSONObject ?: error("저장 규칙이 올바르지 않습니다.")
            val kind = requiredString(obj, "kind")
            requiredString(obj, "label")
            if (kind.equals("CELL", ignoreCase = true)) {
                require(requiredString(obj, "cellId") in cellIds) {
                    "저장 규칙의 셀 참조를 찾을 수 없습니다."
                }
            }
        }
    }

    private fun requiredString(obj: JSONObject, key: String): String {
        val value = obj.opt(key)
        require(value is String && value.isNotBlank()) { "${key} 정보가 올바르지 않습니다." }
        return value.trim()
    }

    private fun exactInt(obj: JSONObject, key: String, range: IntRange): Int {
        val value = obj.opt(key)
        require(value is Int && value in range) { "${key} 범위가 올바르지 않습니다." }
        return value
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String) {
        require(enumValues<T>().any { it.name == value }) { "지원하지 않는 값이 있습니다." }
    }
}
