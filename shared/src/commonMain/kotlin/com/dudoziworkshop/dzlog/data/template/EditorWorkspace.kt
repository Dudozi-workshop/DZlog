package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.data.json.CompatJsonObject
import com.dudoziworkshop.dzlog.data.json.CompatJsonArray

/** Local preferences shared by the existing settings presentation. Platform effects remain separate. */
data class WorkspaceSettings(
    val qualityMode: Int = 1,
    val assistShutterEnabled: Boolean = false,
    val undoSavePathWarningEnabled: Boolean = true,
) { init { require(qualityMode in 0..2) } }

/** A collection preserves each editor document's scope, style and counter metadata independently. */
data class EditorWorkspace(
    val documents: List<EditorDocument>,
    val activeTemplateId: String?,
    val settings: WorkspaceSettings = WorkspaceSettings(),
) {
    init {
        require(documents.map { it.template.id }.toSet().size == documents.size)
        require(activeTemplateId == null || documents.any { it.template.id == activeTemplateId })
    }
    val active: EditorDocument? get() = documents.firstOrNull { it.template.id == activeTemplateId }
    fun save(document: EditorDocument): EditorWorkspace = copy(
        documents = (documents.filterNot { it.template.id == document.template.id } + document)
            .sortedByDescending { it.template.modifiedAt }, activeTemplateId = document.template.id,
    )
}

fun editorWorkspaceFromLegacy(legacy: EditorDocument?, seed: SavedTableTemplate): EditorWorkspace =
    EditorWorkspace(listOf(legacy ?: EditorDocument(seed)), legacy?.template?.id)

object EditorWorkspaceCodec {
    fun encode(value: EditorWorkspace): String {
        val documents = CompatJsonArray()
        value.documents.forEach { documents.put(EditorDocumentCodec.encode(it)) }
        return CompatJsonObject().put("schema", 2).put("documents", documents)
            .put("active", value.activeTemplateId ?: CompatJsonObject.NULL)
            .put("quality", value.settings.qualityMode)
            .put("assist", value.settings.assistShutterEnabled)
            .put("undoWarning", value.settings.undoSavePathWarningEnabled).toString()
    }
    fun decode(json: String): EditorWorkspace {
        val root = CompatJsonObject(json)
        require(root.getInt("schema") == 2)
        val array = root.getJSONArray("documents")
        // Decode every entry strictly: a damaged item must never silently disappear on the next write.
        val documents = (0 until array.length()).map { EditorDocumentCodec.decode(array.getString(it)) }
        val active = if (root.opt("active") == null || root.opt("active") == CompatJsonObject.NULL) null
            else root.getString("active")
        return EditorWorkspace(documents, active, WorkspaceSettings(root.getInt("quality"),
            root.optBoolean("assist", false), root.optBoolean("undoWarning", true)))
    }
}
