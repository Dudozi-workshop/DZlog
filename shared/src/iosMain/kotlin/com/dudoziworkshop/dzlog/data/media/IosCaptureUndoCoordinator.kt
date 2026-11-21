package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.EditorWorkspaceCodec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.Foundation.NSUserDefaults

data class IosCaptureUndoRequest(
    val captureId: String,
    val before: EditorWorkspace,
    val after: EditorWorkspace,
) {
    init {
        require(captureId.isNotBlank() && before != after)
    }
}

interface IosCaptureDeletionStore {
    suspend fun listSavedAssets(): List<SavedCaptureMedia>
    suspend fun deleteCaptureAssets(captureId: String): Boolean
}

/** Durable boundary for PhotoKit deletion followed by workspace rollback. */
class IosCaptureUndoJournal(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
    private val key: String = "dzlog.ios.capture.pending-undo.v1",
) {
    fun hasPending(): Boolean = defaults.stringForKey(key) != null

    internal fun pending(): IosCaptureUndoRequest? = defaults.stringForKey(key)?.let(::decode)

    fun stage(request: IosCaptureUndoRequest, current: EditorWorkspace) {
        check(pending() == null) { "Recover the previous capture Undo first." }
        check(current == request.after) { "Workspace changed before capture Undo." }
        defaults.setObject(encode(request), forKey = key)
        check(defaults.synchronize()) { "Could not persist capture Undo journal." }
    }

    /** Call only after PhotoKit confirms that every role was deleted. */
    fun completeDeletion(
        loadWorkspace: () -> EditorWorkspace,
        persistWorkspace: (EditorWorkspace) -> Boolean,
    ): Boolean {
        val request = pending() ?: return true
        val current = loadWorkspace()
        if (current != request.after && current != request.before) return false
        if (current == request.after && !persistWorkspace(request.before)) return false
        defaults.removeObjectForKey(key)
        return defaults.synchronize()
    }

    /** PhotoKit cancellation proves the assets were not deleted, so rollback is abandoned. */
    fun abortConfirmedNoDeletion(captureId: String, current: EditorWorkspace): Boolean {
        val request = pending() ?: return true
        if (request.captureId != captureId || current != request.after) return false
        defaults.removeObjectForKey(key)
        return defaults.synchronize()
    }

    private fun encode(request: IosCaptureUndoRequest): String = JsonObject(mapOf(
        "schema" to JsonPrimitive(1),
        "captureId" to JsonPrimitive(request.captureId),
        "before" to JsonPrimitive(EditorWorkspaceCodec.encode(request.before)),
        "after" to JsonPrimitive(EditorWorkspaceCodec.encode(request.after)),
    )).toString()

    private fun decode(raw: String): IosCaptureUndoRequest {
        val value = Json.parseToJsonElement(raw).jsonObject
        fun field(name: String) = value.getValue(name).jsonPrimitive.content
        check(field("schema") == "1") { "Unsupported capture Undo journal." }
        return IosCaptureUndoRequest(
            captureId = field("captureId"),
            before = EditorWorkspaceCodec.decode(field("before")),
            after = EditorWorkspaceCodec.decode(field("after")),
        )
    }
}

class IosCaptureUndoCoordinator(
    private val media: IosCaptureDeletionStore,
    private val journal: IosCaptureUndoJournal = IosCaptureUndoJournal(),
) {
    fun hasPendingUndo(): Boolean = journal.hasPending()

    suspend fun deleteAndRollback(
        request: IosCaptureUndoRequest,
        loadWorkspace: () -> EditorWorkspace,
        persistWorkspace: (EditorWorkspace) -> Boolean,
    ): Boolean {
        check(!journal.hasPending()) { "Recover the previous capture Undo first." }
        val current = loadWorkspace()
        journal.stage(request, current)
        val deleted = media.deleteCaptureAssets(request.captureId)
        if (!deleted) {
            journal.abortConfirmedNoDeletion(request.captureId, loadWorkspace())
            return false
        }
        return journal.completeDeletion(loadWorkspace, persistWorkspace)
    }

    /** Resume an already-authorized Undo after interruption or workspace write failure. */
    suspend fun recoverPending(
        loadWorkspace: () -> EditorWorkspace,
        persistWorkspace: (EditorWorkspace) -> Boolean,
    ): Boolean {
        val request = journal.pending() ?: return true
        val matches = media.listSavedAssets().filter { it.captureId == request.captureId }
        if (matches.any { it.availability == CaptureMediaAvailability.INACCESSIBLE }) return false
        if (matches.isNotEmpty()) {
            val deleted = media.deleteCaptureAssets(request.captureId)
            if (!deleted) return false
        }
        return journal.completeDeletion(loadWorkspace, persistWorkspace)
    }
}
