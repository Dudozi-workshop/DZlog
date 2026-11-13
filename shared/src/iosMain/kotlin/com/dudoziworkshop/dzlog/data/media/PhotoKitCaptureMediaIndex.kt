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
    private fun state(): JsonObject? = defaults.stringForKey(STATE_KEY)?.let {
        Json.parseToJsonElement(it).jsonObject
    }
    fun load(): List<IndexedCaptureMedia> = state()?.getValue("entries")?.let { decode(it.toString()) }
        ?: decode(defaults.stringForKey(INDEX_KEY))
    fun pending(): List<IndexedCaptureMedia> = decode(defaults.stringForKey(PENDING_KEY))

    private fun explicitFolders(): Set<String> = state()?.getValue("folders")?.jsonArray
        ?.map { it.jsonPrimitive.content }?.toSet().orEmpty()

    fun favoriteAssetIds(): Set<String> = state()?.get("favorites")?.jsonArray
        ?.map { it.jsonPrimitive.content }?.toSet().orEmpty()

    fun setFavorite(assetId: String, favorite: Boolean) {
        check(pending().isEmpty()) { "Recover the outstanding capture first." }
        check(load().any { it.asset.assetId == assetId }) { "Photo is no longer indexed." }
        val updated = if (favorite) favoriteAssetIds() + assetId else favoriteAssetIds() - assetId
        persistState(load(), explicitFolders(), updated)
    }

    fun folders(): List<String> = buildSet {
        add(ROOT)
        (explicitFolders() + load().map { it.asset.target.relativePath }).forEach { initial ->
            var path = initial
            while (path.startsWith(ROOT) && path != ROOT) {
                add(path)
                path = path.trimEnd('/').substringBeforeLast('/') + "/"
            }
        }
    }.sorted()

    fun createFolder(parent: String, name: String): String {
        check(pending().isEmpty()) { "Recover the outstanding capture first." }
        require(parent in folders()) { "Parent folder no longer exists." }
        val path = parent + folderName(name) + "/"
        require(path !in folders()) { "Folder already exists." }
        persistState(load(), explicitFolders() + path)
        return path
    }

    fun renameFolder(path: String, name: String): String {
        check(pending().isEmpty()) { "Recover the outstanding capture first." }
        require(path != ROOT && path in folders()) { "Select an existing child folder." }
        val parent = path.trimEnd('/').substringBeforeLast('/') + "/"
        val replacement = parent + folderName(name) + "/"
        if (replacement == path) return path
        require(replacement !in folders()) { "Folder already exists." }
        fun remap(value: String) = if (value.startsWith(path)) replacement + value.removePrefix(path) else value
        // Folder and descendant photo paths are written together in one durable snapshot.
        persistState(load().map { entry -> entry.copy(asset = entry.asset.copy(
            target = entry.asset.target.copy(relativePath = remap(entry.asset.target.relativePath)))) },
            (explicitFolders() + path).map(::remap).toSet())
        return replacement
    }

    fun deleteEmptyFolder(path: String) {
        check(pending().isEmpty()) { "Recover the outstanding capture first." }
        require(path != ROOT && path in folders()) { "Select an existing child folder." }
        require(load().none { it.asset.target.relativePath.startsWith(path) } &&
            folders().none { it != path && it.startsWith(path) }) { "Only empty folders can be deleted. Move photos and subfolders first." }
        persistState(load(), explicitFolders() - path)
    }

    private fun folderName(name: String): String {
        val value = name.trim()
        require(value.isNotEmpty() && value != "." && value != ".." &&
            value.none { it == '/' || it == '\\' || it.isISOControl() }) { "Invalid folder name." }
        return value
    }

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

    /** Only completed, indexed assets may be managed by the gallery. */
    fun move(assetId: String, relativePath: String) {
        check(pending().isEmpty()) { "Recover the outstanding capture before moving photos." }
        require(relativePath.startsWith("Pictures/DZlog/") && relativePath.endsWith('/'))
        require(relativePath.removePrefix("Pictures/DZlog/").removeSuffix("/")
            .split('/').all { it.isNotBlank() && it != "." && it != ".." && '\\' !in it } ||
            relativePath == "Pictures/DZlog/")
        val entries = load()
        check(entries.count { it.asset.assetId == assetId } == 1) { "Photo is no longer indexed." }
        val selected = entries.single { it.asset.assetId == assetId }
        check(entries.none { it.asset.assetId != assetId &&
            it.asset.target.relativePath == relativePath &&
            it.asset.target.displayName == selected.asset.target.displayName }) {
            "A photo with this filename already exists in the selected folder."
        }
        persist(INDEX_KEY, entries.map { entry ->
            if (entry.asset.assetId != assetId) entry else entry.copy(
                asset = entry.asset.copy(target = entry.asset.target.copy(relativePath = relativePath)),
            )
        })
    }

    /** Call only after PhotoKit confirms deletion; a cancelled prompt keeps the index. */
    fun forgetDeleted(assetId: String) {
        check(pending().isEmpty()) { "Recover the outstanding capture before deleting photos." }
        persistState(load().filterNot { it.asset.assetId == assetId }, folders().toSet(), favoriteAssetIds() - assetId)
    }

    /** Remove every role of one confirmed PhotoKit capture in a single index snapshot. */
    fun forgetCapture(captureId: String) {
        check(pending().isEmpty()) { "Recover the outstanding capture before deleting photos." }
        val entries = load()
        val removedIds = entries.filter { it.captureId == captureId }.map { it.asset.assetId }.toSet()
        check(removedIds.isNotEmpty()) { "Capture is no longer indexed." }
        persistState(entries.filterNot { it.captureId == captureId }, folders().toSet(),
            favoriteAssetIds() - removedIds)
    }

    private fun encode(entries: List<IndexedCaptureMedia>): JsonArray = JsonArray(entries.map { entry ->
            JsonObject(mapOf(
                "captureId" to JsonPrimitive(entry.captureId),
                "role" to JsonPrimitive(entry.asset.target.role.name),
                "relativePath" to JsonPrimitive(entry.asset.target.relativePath),
                "displayName" to JsonPrimitive(entry.asset.target.displayName),
                "assetId" to JsonPrimitive(entry.asset.assetId),
            ))
        })

    private fun persistState(entries: List<IndexedCaptureMedia>, folders: Set<String>,
        favorites: Set<String> = favoriteAssetIds()) {
        val value = JsonObject(mapOf("entries" to encode(entries),
            "folders" to JsonArray(folders.sorted().map { JsonPrimitive(it) }),
            "favorites" to JsonArray(favorites.filter { id -> entries.any { it.asset.assetId == id } }
                .sorted().map { JsonPrimitive(it) })))
        defaults.setObject(value.toString(), forKey = STATE_KEY)
        check(defaults.synchronize()) { "Could not persist gallery state." }
    }

    private fun persist(key: String, entries: List<IndexedCaptureMedia>) {
        if (key == INDEX_KEY) {
            persistState(entries, folders().toSet())
            return
        }
        defaults.setObject(encode(entries).toString(), forKey = key)
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
        private const val ROOT = "Pictures/DZlog/"
        private const val STATE_KEY = "dzlog.ios.photokit.gallery-state.v2"
        private const val INDEX_KEY = "dzlog.ios.photokit.capture-index.v1"
        private const val PENDING_KEY = "dzlog.ios.photokit.pending-capture.v1"
    }
}
