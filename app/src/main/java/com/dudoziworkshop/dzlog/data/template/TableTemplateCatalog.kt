package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedTableTemplate(
    val id: String,
    val name: String,
    val templateState: TableTemplateState,
    val styleState: TableStyleState,
    val modifiedAt: Long,
)

fun createSavedTableTemplate(
    name: String,
    templateState: TableTemplateState,
    styleState: TableStyleState,
    modifiedAt: Long = System.currentTimeMillis(),
    id: String = UUID.randomUUID().toString(),
): SavedTableTemplate = SavedTableTemplate(
    id = id,
    name = name.trim().ifBlank { "새 템플릿" },
    templateState = templateState,
    styleState = styleState,
    modifiedAt = modifiedAt,
)

fun savedTableTemplatesToJson(items: List<SavedTableTemplate>): String {
    val array = JSONArray()
    items.forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id)
                .put("name", item.name)
                .put("modifiedAt", item.modifiedAt)
                .put("template", JSONObject(item.templateState.toJsonString()))
                .put(
                    "style",
                    JSONObject()
                        .put("bgStyle", item.styleState.bgStyle)
                        .put("gridEnabled", item.styleState.gridEnabled)
                        .put("textColorMode", item.styleState.textColorMode)
                        .put("manualTextColor", item.styleState.manualTextColor)
                        .put("valueScale", item.styleState.valueScale)
                        .put("textAlign", item.styleState.textAlign)
                        .put("bgAlpha", item.styleState.bgAlpha)
                )
        )
    }
    return array.toString()
}

fun savedTableTemplatesFromJson(json: String): List<SavedTableTemplate>? = runCatching {
    val array = JSONArray(json)
    buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id").trim()
            val name = item.optString("name").trim()
            val templateObject = item.optJSONObject("template") ?: continue
            val template = tableTemplateStateFromJson(templateObject.toString()) ?: continue
            val styleObject = item.optJSONObject("style")
            val style = if (styleObject == null) {
                TableStyleState()
            } else {
                TableStyleState(
                    bgStyle = styleObject.optInt("bgStyle", 0).coerceIn(0, 2),
                    gridEnabled = styleObject.optBoolean("gridEnabled", true),
                    textColorMode = styleObject.optInt("textColorMode", 0).coerceIn(0, 1),
                    manualTextColor = styleObject.optInt("manualTextColor", 1).coerceIn(0, 1),
                    valueScale = styleObject.optInt("valueScale", 100).coerceIn(60, 160),
                    textAlign = styleObject.optInt("textAlign", 1).coerceIn(0, 2),
                    bgAlpha = styleObject.optInt("bgAlpha", 80).coerceIn(0, 255),
                )
            }
            if (id.isBlank()) continue
            add(
                SavedTableTemplate(
                    id = id,
                    name = name.ifBlank { "새 템플릿" },
                    templateState = template,
                    styleState = style,
                    modifiedAt = item.optLong("modifiedAt", 0L),
                )
            )
        }
    }
}.getOrNull()

fun nextNewTemplateName(items: List<SavedTableTemplate>): String {
    val names = items.map { it.name }.toSet()
    if ("새 템플릿" !in names) return "새 템플릿"
    var suffix = 2
    while ("새 템플릿 $suffix" in names) suffix += 1
    return "새 템플릿 $suffix"
}

fun duplicateTemplateName(baseName: String, items: List<SavedTableTemplate>): String {
    val names = items.map { it.name }.toSet()
    val base = "$baseName 사본"
    if (base !in names) return base
    var suffix = 2
    while ("$base $suffix" in names) suffix += 1
    return "$base $suffix"
}
