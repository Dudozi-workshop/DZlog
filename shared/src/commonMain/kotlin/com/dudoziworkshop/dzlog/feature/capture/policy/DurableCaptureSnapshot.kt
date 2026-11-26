package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.json.CompatJsonArray
import com.dudoziworkshop.dzlog.data.json.CompatJsonObject
import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorDocumentCodec
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.EditorWorkspaceCodec
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.table.TablePatch

/** Immutable source image identity. The file itself is owned by the platform staging store. */
data class DurableCaptureSourceJpeg(
    val fileName: String,
    val byteCount: Long,
    val sha256: String,
) {
    init {
        require(fileName.isNotBlank() && fileName == fileName.substringAfterLast('/'))
        require(fileName != "." && fileName != "..")
        require(byteCount > 0L)
        require(sha256.length == 64 && sha256.all { it in '0'..'9' || it in 'a'..'f' })
    }
}

/** The renderer needs only stable, already-resolved values; mutable raw cell state is excluded. */
data class FrozenResolvedCell(
    val id: String,
    val type: TableCellDataType,
    val resolvedText: String,
    val isEmpty: Boolean,
    val scopeToken: String?,
) {
    init { require(id.isNotBlank()) }

    fun thaw(): ResolvedCell = ResolvedCell(
        id = id,
        type = type,
        resolvedText = resolvedText,
        isEmpty = isEmpty,
        scopeToken = scopeToken,
    )
}

data class FrozenCapturePreview(
    val resolvedCells: List<FrozenResolvedCell>,
    val tablePatch: TablePatch,
    val displayName: String,
    val usedCounter: Int,
    val nextPhraseProgressCursor: Int,
    val counterScope: CounterScope,
    val relativePathPreview: String,
) {
    init {
        require(resolvedCells.map(FrozenResolvedCell::id).distinct().size == resolvedCells.size)
        require(displayName.isNotBlank())
        require(usedCounter >= 1)
        require(nextPhraseProgressCursor >= 0)
    }

    fun thaw(): FinalCapturePreview = FinalCapturePreview(
        resolvedCells = resolvedCells.map(FrozenResolvedCell::thaw),
        tablePatch = tablePatch,
        displayName = displayName,
        usedCounter = usedCounter,
        nextPhraseProgressCursor = nextPhraseProgressCursor,
        counterScope = counterScope,
        relativePathPreview = relativePathPreview,
    )

    companion object {
        fun freeze(value: FinalCapturePreview): FrozenCapturePreview = FrozenCapturePreview(
            resolvedCells = value.resolvedCells.map {
                FrozenResolvedCell(it.id, it.type, it.resolvedText, it.isEmpty, it.scopeToken)
            },
            tablePatch = value.tablePatch,
            displayName = value.displayName,
            usedCounter = value.usedCounter,
            nextPhraseProgressCursor = value.nextPhraseProgressCursor,
            counterScope = value.counterScope,
            relativePathPreview = value.relativePathPreview,
        )
    }
}

/**
 * Complete deterministic input for resuming one capture after process death.
 * Live workspace state must never be consulted to rename, render or advance this capture.
 */
data class DurableCaptureSnapshot(
    val record: DurableCaptureRecord,
    val capturedDocument: EditorDocument,
    val workspaceBefore: EditorWorkspace,
    val workspaceAfter: EditorWorkspace,
    val preview: FrozenCapturePreview,
    val savePlan: CaptureMediaSavePlan,
    val sourceJpeg: DurableCaptureSourceJpeg,
) {
    init {
        require(record.captureId == savePlan.captureId)
        require(record.templateId == capturedDocument.template.id)
        require(record.counterValue == preview.usedCounter)
        require(record.counterValue == savePlan.usedCounter)
        require(record.phraseProgressCursor == preview.nextPhraseProgressCursor)
        require(workspaceBefore.active == capturedDocument)
        require(workspaceAfter.activeTemplateId == record.templateId)
        require(preview.tablePatch == savePlan.patchAfterSuccess)
        require(preview.counterScope == savePlan.counterScope)
        require(savePlan.targets.all { it.displayName == preview.displayName })
    }
}

object DurableCaptureSnapshotCodec {
    private const val SCHEMA = 1

    fun encode(value: DurableCaptureSnapshot): String {
        val cells = CompatJsonArray()
        value.preview.resolvedCells.forEach { cell -> cells.put(CompatJsonObject()
            .put("id", cell.id).put("type", cell.type.name).put("text", cell.resolvedText)
            .put("empty", cell.isEmpty).put("scope", cell.scopeToken ?: CompatJsonObject.NULL)) }
        val updates = CompatJsonArray()
        value.preview.tablePatch.updatesByCellId.forEach { (id, text) ->
            updates.put(CompatJsonObject().put("id", id).put("text", text))
        }
        val phrase = CompatJsonArray()
        value.preview.tablePatch.phraseProgressByCellId.forEach { (id, progress) ->
            phrase.put(CompatJsonObject().put("id", id)
                .put("index", progress.index).put("shots", progress.completedShots))
        }
        val targets = CompatJsonArray()
        value.savePlan.targets.forEach { target -> targets.put(CompatJsonObject()
            .put("role", target.role.name).put("path", target.relativePath).put("name", target.displayName)) }
        return CompatJsonObject().put("schema", SCHEMA)
            .put("record", encodeRecord(value.record))
            .put("document", EditorDocumentCodec.encode(value.capturedDocument))
            .put("workspaceBefore", EditorWorkspaceCodec.encode(value.workspaceBefore))
            .put("workspaceAfter", EditorWorkspaceCodec.encode(value.workspaceAfter))
            .put("preview", CompatJsonObject()
                .put("cells", cells).put("updates", updates).put("phrase", phrase)
                .put("displayName", value.preview.displayName)
                .put("usedCounter", value.preview.usedCounter)
                .put("nextPhrase", value.preview.nextPhraseProgressCursor)
                .put("scopePath", value.preview.counterScope.relativePathKey)
                .put("scopePrefix", value.preview.counterScope.streamPrefix)
                .put("scopeNext", value.preview.counterScope.nextCounter)
                .put("scopeManual", value.preview.counterScope.isManualMode)
                .put("relativePath", value.preview.relativePathPreview))
            .put("targets", targets)
            .put("source", CompatJsonObject().put("file", value.sourceJpeg.fileName)
                .put("bytes", value.sourceJpeg.byteCount).put("sha256", value.sourceJpeg.sha256))
            .toString()
    }

    fun decode(json: String): DurableCaptureSnapshot {
        val root = CompatJsonObject(json)
        require(root.getInt("schema") == SCHEMA)
        val record = decodeRecord(root.getJSONObject("record"))
        val previewJson = root.getJSONObject("preview")
        val cellsJson = previewJson.getJSONArray("cells")
        val cells = (0 until cellsJson.length()).map { index -> cellsJson.getJSONObject(index).let { cell ->
            FrozenResolvedCell(cell.getString("id"), TableCellDataType.valueOf(cell.getString("type")),
                cell.getString("text"), cell.optBoolean("empty", false), cell.nullableString("scope"))
        } }
        val updatesJson = previewJson.getJSONArray("updates")
        val updates = (0 until updatesJson.length()).associate { index -> updatesJson.getJSONObject(index).let {
            it.getString("id") to it.getString("text")
        } }
        val phraseJson = previewJson.getJSONArray("phrase")
        val phrase = (0 until phraseJson.length()).associate { index -> phraseJson.getJSONObject(index).let {
            it.getString("id") to CellPhraseProgress(it.getInt("index"), it.getInt("shots"))
        } }
        val preview = FrozenCapturePreview(cells, TablePatch(updates, phrase),
            previewJson.getString("displayName"), previewJson.getInt("usedCounter"),
            previewJson.getInt("nextPhrase"), CounterScope(previewJson.getString("scopePath"),
                previewJson.getString("scopePrefix"), previewJson.getInt("scopeNext"),
                previewJson.optBoolean("scopeManual", false)), previewJson.getString("relativePath"))
        val targetsJson = root.getJSONArray("targets")
        val targets = (0 until targetsJson.length()).map { index -> targetsJson.getJSONObject(index).let {
            CaptureMediaTarget(CaptureMediaRole.valueOf(it.getString("role")),
                it.getString("path"), it.getString("name"))
        } }
        val plan = CaptureMediaSavePlan(record.captureId, preview.usedCounter, preview.counterScope,
            preview.tablePatch, targets)
        val source = root.getJSONObject("source")
        return DurableCaptureSnapshot(record, EditorDocumentCodec.decode(root.getString("document")),
            EditorWorkspaceCodec.decode(root.getString("workspaceBefore")),
            EditorWorkspaceCodec.decode(root.getString("workspaceAfter")), preview, plan,
            DurableCaptureSourceJpeg(source.getString("file"), source.optLong("bytes", -1L),
                source.getString("sha256")))
    }

    private fun encodeRecord(value: DurableCaptureRecord): CompatJsonObject = CompatJsonObject()
        .put("id", value.captureId).put("order", value.reservationOrder)
        .put("template", value.templateId).put("project", value.projectId ?: CompatJsonObject.NULL)
        .put("counter", value.counterValue).put("phrase", value.phraseProgressCursor)
        .put("stage", value.stage.name)
        .put("reviewReason", value.reviewReason?.name ?: CompatJsonObject.NULL)

    private fun decodeRecord(value: CompatJsonObject): DurableCaptureRecord = DurableCaptureRecord(
        value.getString("id"), value.optLong("order", -1L), value.getString("template"),
        value.nullableString("project"), value.getInt("counter"), value.getInt("phrase"),
        DurableCaptureStage.valueOf(value.getString("stage")),
        value.nullableString("reviewReason")?.let(DurableCaptureReviewReason::valueOf),
    )

    private fun CompatJsonObject.nullableString(key: String): String? =
        if (opt(key) == null || opt(key) == CompatJsonObject.NULL) null else getString(key)
}
