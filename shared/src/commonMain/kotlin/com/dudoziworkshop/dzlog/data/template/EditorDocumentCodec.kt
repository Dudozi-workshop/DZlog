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
    val capturePlacement: EditorCapturePlacement? = null,
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
        // Optional extension: existing schema-1 documents have no placement key.
        .put("placementV1", value.capturePlacement?.let { placement ->
            CompatJsonObject()
                .put("anchor", placement.anchor.name)
                .put("x", placement.offsetXPercent)
                .put("y", placement.offsetYPercent)
                .put("width", placement.widthRatio)
                .put("height", placement.heightRatio)
                .put("rotation", placement.rotationCwDeg)
                .toString()
        } ?: CompatJsonObject.NULL)
        .toString()

    fun decode(json: String): EditorDocument {
        val root = CompatJsonObject(json)
        require(root.getInt("schema") == 1) { "지원하지 않는 편집 문서입니다." }
        val templatesJson = root.getString("templates")
        require(com.dudoziworkshop.dzlog.data.json.CompatJsonArray(templatesJson).length() == 1)
        val template = requireNotNull(savedTableTemplatesFromJson(templatesJson)).single()
        val next = if (root.opt("next") == null || root.opt("next") == CompatJsonObject.NULL) null
            else root.getInt("next").also { require(it >= 1) }
        val padding = root.getInt("padding").also { require(it in 0..4) }
        val rawPlacement = root.opt("placementV1")
        val placement = if (rawPlacement == null || rawPlacement == CompatJsonObject.NULL) null
        else CompatJsonObject(root.getString("placementV1")).let { saved ->
            EditorCapturePlacement(
                anchor = EditorCaptureAnchor.valueOf(saved.getString("anchor")),
                offsetXPercent = saved.getInt("x"),
                offsetYPercent = saved.getInt("y"),
                widthRatio = saved.getInt("width"),
                heightRatio = saved.getInt("height"),
                rotationCwDeg = saved.getInt("rotation"),
            )
        }
        return EditorDocument(template, root.optBoolean("pathScope", true),
            root.optBoolean("filenameScope", true), SaveMode.valueOf(root.getString("saveMode")),
            padding, next, root.optBoolean("autoNext", true), placement)
    }
}
