package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.Foundation.NSUserDefaults

internal data class IndexedCaptureMedia(
    val captureId: String,
    val asset: PersistedCaptureAsset,
)

/**
 * A durable, app-private mapping, NOT a Photos folder or filepath.
 * An in-flight journal stores placeholders inside PhotoKit's change block; after an
 * interrupted save the next gallery lookup can reconcile committed asset IDs.
 * One outstanding capture is supported at a time.
 *
 * This is a PoC index. Production should use a transactional Application Support DB.
 */
internal class PhotoKitCaptureMediaIndex(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {
    fun load(): List<IndexedCaptureMedia> = decode(defaults.stringForKey(INDEX_KEY))
    fun pending(): List<IndexedCaptureMedia> = decode(defaults.stringForKey(PENDING_KEY))

    fun ensureCaptureIsNew(captureId: String) {
        check((load() + pending()).none { it.captureId == captureId }) {
            "Capture ID already exists. Refusing duplicate PhotoKit write."
        }
    }

    fun stagePending(entries: List<IndexedCaptureMedia>) {
        require(entries.isNotEmpty() && entries.map { it.captureId }.distinct().size == 1)
        check(pending().isEmpty()) { "Recover the outstanding PhotoKit save before writing another." }
        persist(PENDING_KEY, entries)
    }

    fun append(entries: List<IndexedCaptureMedia>) {
        require(entries.isNotEmpty())
        val previous = load()
        // Idempotent recovery: a crash after append but before journal clearing is safe.
        val newEntries = entries.filterNot { entry -> previous.contains(entry) }
        check(newEntries.none { entry ->
            previous.any { it.captureId == entry.captureId || it.asset.assetId == entry.asset.assetId }
        }) { "Conflicting PhotoKit asset/capture mapping." }
        if (newEntries.isNotEmpty()) persist(INDEX_KEY, previous + newEntries)
    }

    fun clearPending() {
        defaults.removeObjectForKey(PENDING_KEY)
        check(defaults.synchronize()) { "Could not flush the PhotoKit recovery journal." }
    }

    private fun persist(key: String, entries: List<IndexedCaptureMedia>) {
        val encoded = JsonArray(entries.map { entry ->
            JsonObject(mapOf(
                "captureId" to JsonPrimitive(entry.captureId),
                "role" to JsonPrimitive(entry.asset.target.role.name),
                "relativePath" to JsonPrimitive(entry.asset.target.relativePath),
                "displayName" to JsonPrimitive(entry.asset.target.displayName),
                "assetId" to JsonPrimitive(entry.asset.assetId),
            ))
        }).toString()
        defaults.setObject(encoded, forKey = key)
        check(defaults.synchronize()) { "Could not persist PhotoKit index/journal ($key)." }
    }

    private fun decode(value: String?): List<IndexedCaptureMedia> {
        if (value == null) return emptyList()
        return Json.parseToJsonElement(value).jsonArray.map { element ->
            val objectValue = element.jsonObject
            fun field(key: String): String = objectValue.getValue(key).jsonPrimitive.content
            IndexedCaptureMedia(
                captureId = field("captureId"),
                asset = PersistedCaptureAsset(
                    target = CaptureMediaTarget(
                        role = CaptureMediaRole.valueOf(field("role")),
                        relativePath = field("relativePath"),
                        displayName = field("displayName"),
                    ),
                    assetId = field("assetId"),
                ),
            )
        }
    }

    companion object {
        private const val INDEX_KEY = "dzlog.ios.photokit.capture-index.v1"
        private const val PENDING_KEY = "dzlog.ios.photokit.pending-capture.v1"
    }
}
