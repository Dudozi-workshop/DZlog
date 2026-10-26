package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.EditorWorkspaceCodec
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.ConfirmedCaptureMediaSave
import com.dudoziworkshop.dzlog.domain.media.prepareCaptureWorkspaceUpdate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.Foundation.NSUserDefaults

internal data class PendingIosWorkspaceCapture(
    val captureId: String,
    val usedCounter: Int,
    val before: EditorWorkspace,
    val after: EditorWorkspace,
    val targets: List<CaptureMediaTarget>,
)

/**
 * Durable iOS-only handoff between PhotoKit and the workspace store.
 *
 * Stage BEFORE saving media. On restart, apply the prepared workspace only when
 * the complete matching capture is visible in the persistent PhotoKit index.
 * Incomplete/denied/unknown media and conflicting workspace edits fail closed.
 *
 * This adapter does not itself start a capture or mutate the workspace.
 */
internal class IosCaptureWorkspaceJournal(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
    private val key: String = "dzlog.ios.capture.pending-workspace.v1",
) {
    fun pending(): PendingIosWorkspaceCapture? =
        defaults.stringForKey(key)?.let(::decode)

    fun stage(
        before: EditorWorkspace,
        document: EditorDocument,
        plan: CaptureMediaSavePlan,
    ) {
        check(pending() == null) { "Resolve the previous capture before saving another." }
        val after = prepareCaptureWorkspaceUpdate(before, document, plan)
        val entry = PendingIosWorkspaceCapture(
            plan.captureId, plan.usedCounter, before, after, plan.targets,
        )
        defaults.setObject(encode(entry), forKey = key)
        check(defaults.synchronize()) { "Cannot persist pending capture journal." }
    }

    /**
     * Called only with PhotoKit's complete save receipt.
     * A failed workspace write keeps the journal for relaunch recovery.
     */
    fun complete(
        receipt: ConfirmedCaptureMediaSave,
        loadWorkspace: () -> EditorWorkspace,
        persistWorkspace: (EditorWorkspace) -> Boolean,
    ): Boolean {
        val entry = checkNotNull(pending()) { "No staged capture to complete." }
        check(entry.captureId == receipt.plan.captureId &&
            entry.usedCounter == receipt.plan.usedCounter &&
            entry.targets == receipt.plan.targets) {
            "PhotoKit receipt does not match staged capture."
        }
        return applyIfUnchanged(entry, loadWorkspace, persistWorkspace)
    }

    /**
     * Call after PhotoKit listSavedAssets() has reconciled pending asset writes.
     * Returns false and leaves the journal untouched unless ALL roles are PRESENT.
     * The caller must block further captures while a pending journal remains.
     */
    fun recover(
        savedAssets: List<SavedCaptureMedia>,
        loadWorkspace: () -> EditorWorkspace,
        persistWorkspace: (EditorWorkspace) -> Boolean,
    ): Boolean {
        val entry = pending() ?: return true
        val matches = savedAssets.filter { it.captureId == entry.captureId }
        if (matches.size != entry.targets.size ||
            matches.any { it.availability != CaptureMediaAvailability.PRESENT } ||
            matches.map { it.asset.target }.toSet() != entry.targets.toSet() ||
            matches.any { it.asset.assetId.isBlank() } ||
            matches.map { it.asset.assetId }.distinct().size != matches.size
        ) return false
        return applyIfUnchanged(entry, loadWorkspace, persistWorkspace)
    }

    private fun applyIfUnchanged(
        entry: PendingIosWorkspaceCapture,
        load: () -> EditorWorkspace,
        persist: (EditorWorkspace) -> Boolean,
    ): Boolean {
        val current = load()
        if (current != entry.before && current != entry.after) return false
        if (current == entry.before && !persist(entry.after)) return false
        defaults.removeObjectForKey(key)
        // A completed workspace is safe if clearing the journal fails:
        // next recovery sees entry.after and clears it idempotently.
        return defaults.synchronize()
    }

    private fun encode(entry: PendingIosWorkspaceCapture): String =
        JsonObject(mapOf(
            "schema" to JsonPrimitive(1),
            "captureId" to JsonPrimitive(entry.captureId),
            "usedCounter" to JsonPrimitive(entry.usedCounter),
            "before" to JsonPrimitive(EditorWorkspaceCodec.encode(entry.before)),
            "after" to JsonPrimitive(EditorWorkspaceCodec.encode(entry.after)),
            "targets" to JsonArray(entry.targets.map { target ->
                JsonObject(mapOf(
                    "role" to JsonPrimitive(target.role.name),
                    "path" to JsonPrimitive(target.relativePath),
                    "name" to JsonPrimitive(target.displayName),
                ))
            }),
        )).toString()

    private fun decode(raw: String): PendingIosWorkspaceCapture {
        val data = Json.parseToJsonElement(raw).jsonObject
        fun field(key: String): String = data.getValue(key).jsonPrimitive.content
        check(field("schema") == "1") { "Unsupported pending capture journal." }
        return PendingIosWorkspaceCapture(
            captureId = field("captureId"),
            usedCounter = field("usedCounter").toInt(),
            before = EditorWorkspaceCodec.decode(field("before")),
            after = EditorWorkspaceCodec.decode(field("after")),
            targets = data.getValue("targets").jsonArray.map {
                val target = it.jsonObject
                CaptureMediaTarget(
                    CaptureMediaRole.valueOf(target.getValue("role").jsonPrimitive.content),
                    target.getValue("path").jsonPrimitive.content,
                    target.getValue("name").jsonPrimitive.content,
                )
            },
        )
    }
}
