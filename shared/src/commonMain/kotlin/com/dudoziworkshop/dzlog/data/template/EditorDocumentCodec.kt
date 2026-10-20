package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.data.json.CompatJsonObject
import com.dudoziworkshop.dzlog.domain.model.SaveMode

/** Local editor state. Reuses the existing catalog codec; it is not a .dzlog export. */
data class EditorDocument(
    val template: SavedTableTemplate,
    val includePathInScope: Boolean = true,
    val includeFilenameInScope: Boolean = true,
    val saveMode: SaveMode = SaveMode.BOTH,
    val counterPadding: Int = 4,
    val nextCounter: Int? = null,
    val usesAutoNext: Boolean = true,
)

object EditorDocumentCodec {
    fun encode(value: EditorDocument): String = CompatJsonObject()
        .put("schema", 1)
        .put("templates", savedTableTemplatesToJson(listOf(value.template)))
        .put("pathScope", value.includePathInScope)
        .put("filenameScope", value.includeFilenameInScope)
        .put("saveMode", value.saveMode.name)
        .put("padding", value.counterPadding)
        .put("next", value.nextCounter ?: CompatJsonObject.NULL)
        .put("autoNext", value.usesAutoNext)
        .toString()

    fun decode(json: String): EditorDocument {
        val root = CompatJsonObject(json)
        require(root.getInt("schema") == 1) { "지원하지 않는 편집 문서입니다." }
        val template = requireNotNull(savedTableTemplatesFromJson(root.getString("templates"))).single()
        val next = if (root.opt("next") == null || root.opt("next") == CompatJsonObject.NULL) null
            else root.getInt("next").also { require(it >= 1) }
        val padding = root.getInt("padding").also { require(it in 0..4) }
        return EditorDocument(template, root.optBoolean("pathScope", true),
            root.optBoolean("filenameScope", true), SaveMode.valueOf(root.getString("saveMode")),
            padding, next, root.optBoolean("autoNext", true))
    }
}
