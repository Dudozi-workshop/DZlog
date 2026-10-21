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

/**
 * App-private logical folder/capture index. Photos asset IDs are NOT filesystem paths.
 * The index is durable across process relaunch; a missing/deleted Photos asset stays
 * indexed so the gallery can present a recoverable missing-photo state.
 *
 * M3-A PoC only: move to an atomic Application Support store with a pending-write
 * recovery journal before enabling production capture or multi-writer saves.
 */
internal class PhotoKitCaptureMediaIndex(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {
    fun load(): List<IndexedCaptureMedia> {
        val raw = defaults.stringForKey(INDEX_KEY) ?: return emptyList()
        return Json.parseToJsonElement(raw).jsonArray.map { element ->
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

    fun ensureCaptureIsNew(captureId: String) {
        check(load().none { it.captureId == captureId }) {
            "Capture ID was already saved. Refusing a duplicate PhotoKit write."
        }
    }

    fun append(entries: List<IndexedCaptureMedia>) {
        require(entries.isNotEmpty())
        val existing = load()
        check(entries.none { entry ->
            existing.any { it.captureId == entry.captureId || it.asset.assetId == entry.asset.assetId }
        }) { "Duplicate capture/PhotoKit asset identifier." }
        val encoded = JsonArray((existing + entries).map { entry ->
            JsonObject(mapOf(
                "captureId" to JsonPrimitive(entry.captureId),
                "role" to JsonPrimitive(entry.asset.target.role.name),
                "relativePath" to JsonPrimitive(entry.asset.target.relativePath),
                "displayName" to JsonPrimitive(entry.asset.target.displayName),
                "assetId" to JsonPrimitive(entry.asset.assetId),
            ))
        }).toString()
        defaults.setObject(encoded, forKey = INDEX_KEY)
        check(defaults.synchronize()) { "PhotoKit metadata index was not flushed to disk." }
    }

    companion object {
        private const val INDEX_KEY = "dzlog.ios.photokit.capture-index.v1"
    }
}