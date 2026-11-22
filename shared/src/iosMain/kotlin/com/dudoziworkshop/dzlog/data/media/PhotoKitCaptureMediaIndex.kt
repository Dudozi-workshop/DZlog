package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeleteScope
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderNamePolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderMovePolicy
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

internal data class IndexedFolderContents(
    val source: String,
    val folders: Set<String>,
    val entries: Set<IndexedCaptureMedia>,
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
        require(folders().none { it.equals(path, ignoreCase = true) }) { "같은 이름의 폴더가 이미 있습니다." }
        persistState(load(), explicitFolders() + path)
        return path
    }

    fun renameFolder(path: String, name: String): String {
        check(pending().isEmpty()) { "Recover the outstanding capture first." }
        require(path != ROOT && path in folders()) { "Select an existing child folder." }
        require(!GalleryFolderNamePolicy.isOriginalFolder(path)) { "원본사진 전용 폴더는 이름을 변경할 수 없습니다." }
        val parent = path.trimEnd('/').substringBeforeLast('/') + "/"
        val replacement = parent + folderName(name) + "/"
        if (replacement == path) return path
        require(folders().none { it.equals(replacement, ignoreCase = true) }) { "같은 이름의 폴더가 이미 있습니다." }
        return relocateFolder(path, replacement)
    }

    fun moveFolder(path: String, destination: String): String {
        check(pending().isEmpty()) { "이전 촬영을 먼저 복구해 주세요." }
        val indexedFolders = folders()
        require(path in indexedFolders && destination in indexedFolders) { "선택한 폴더를 찾을 수 없습니다." }
        val replacement = GalleryFolderMovePolicy.movedPath(path, destination)
        require(indexedFolders.none { it.equals(replacement, ignoreCase = true) }) { "대상 위치에 같은 이름의 폴더가 있습니다." }
        return relocateFolder(path, replacement)
    }

    private fun relocateFolder(path: String, replacement: String): String {
        fun remap(value: String) = if (value.startsWith(path)) replacement + value.removePrefix(path) else value
        fun parentOf(value: String) = value.trimEnd('/').substringBeforeLast('/') + "/"
        val updatedFolders = (explicitFolders() + path).mapTo(mutableSetOf(), ::remap)
        val oldParent = parentOf(path)
        if (oldParent != ROOT && oldParent != parentOf(replacement)) updatedFolders.add(oldParent)
        // Folder and descendant photo paths are written together in one durable snapshot.
        persistState(load().map { entry -> entry.copy(asset = entry.asset.copy(
            target = entry.asset.target.copy(relativePath = remap(entry.asset.target.relativePath)))) },
            updatedFolders)
        return replacement
    }

    fun projectEntries(captureIds: Set<String>): Set<IndexedCaptureMedia> {
        val all = load()
        val linked = all.filter { it.captureId in captureIds }
        check(linked.mapTo(mutableSetOf()) { it.captureId } == captureIds) {
            "사진 연결을 확인할 수 없는 촬영 기록이 있습니다. 프로젝트를 유지합니다."
        }
        val assetIds = linked.mapTo(mutableSetOf()) { it.asset.assetId }
        check(assetIds.size == linked.size && all.count { it.asset.assetId in assetIds } == linked.size) {
            "여러 촬영 기록이 같은 사진을 가리킵니다. 프로젝트를 유지합니다."
        }
        return linked.toSet()
    }

    fun folderContents(path: String): IndexedFolderContents {
        check(pending().isEmpty()) { "이전 촬영을 먼저 복구해 주세요." }
        val source = GalleryFolderDeleteScope.source(path)
        val subtree = folders().filterTo(mutableSetOf()) { it.startsWith(source) }
        require(source in subtree) { "삭제할 폴더를 찾을 수 없습니다." }
        val indexed = load()
        val entries = indexed.filter { it.asset.target.relativePath.startsWith(source) }
        require(entries.size + subtree.size <= GalleryFolderDeleteScope.MAX_ENTRIES) {
            "폴더 내용이 너무 많습니다. 하위 폴더를 나누어 삭제해 주세요."
        }
        val ids = entries.mapTo(mutableSetOf()) { it.asset.assetId }
        check(ids.size == entries.size && indexed.count { it.asset.assetId in ids } == entries.size) {
            "사진의 저장 위치를 확인할 수 없습니다."
        }
        return IndexedFolderContents(source, subtree, entries.toSet())
    }

    fun finishFolderDeletion(confirmed: IndexedFolderContents) {
        // During the system prompt another capture can add a photo or an empty child.
        // Keep changed folders for a fresh confirmation. Only confirmed Photos IDs are gone.
        val entries = load()
        val folders = folders().toSet()
        val unchanged = pending().isEmpty() &&
            folders.filterTo(mutableSetOf()) { it.startsWith(confirmed.source) } == confirmed.folders &&
            entries.filterTo(mutableSetOf()) { it.asset.target.relativePath.startsWith(confirmed.source) } == confirmed.entries
        val ids = confirmed.entries.mapTo(mutableSetOf()) { it.asset.assetId }
        val foldersToKeep = if (unchanged) folders - confirmed.folders else folders
        persistState(entries.filterNot { it.asset.assetId in ids }, foldersToKeep, favoriteAssetIds() - ids)
        check(unchanged) { "확인한 사진은 정리했습니다. 폴더가 변경되어 남은 항목을 다시 확인해 주세요." }
    }

    fun deleteEmptyFolder(path: String) {
        check(pending().isEmpty()) { "Recover the outstanding capture first." }
        require(path != ROOT && path in folders()) { "Select an existing child folder." }
        require(load().none { it.asset.target.relativePath.startsWith(path) } &&
            folders().none { it != path && it.startsWith(path) }) { "Only empty folders can be deleted. Move photos and subfolders first." }
        persistState(load(), explicitFolders() - path)
    }

    private fun folderName(name: String): String {
        val value = GalleryFolderNamePolicy.normalize(name)
        require(value.none(Char::isISOControl)) { "폴더 이름에 제어 문자를 사용할 수 없습니다." }
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
    fun move(assetId: String, relativePath: String) = move(setOf(assetId), relativePath)

    fun move(assetIds: Set<String>, relativePath: String) = move(assetIds.associateWith { relativePath })

    fun move(destinations: Map<String, String>) {
        require(destinations.isNotEmpty() && destinations.keys.all { it.isNotBlank() }) { "Select at least one photo." }
        check(pending().isEmpty()) { "Recover the outstanding capture before moving photos." }
        for (path in destinations.values.toSet()) {
            require(path.startsWith(ROOT) && path.endsWith('/'))
            require(path == ROOT || path.removePrefix(ROOT).removeSuffix("/").split('/').all {
                it.isNotBlank() && it != "." && it != ".." && it.none { c -> c == '\\' || c.isISOControl() }
            })
        }
        val entries = load()
        val selected = entries.filter { it.asset.assetId in destinations }
        check(selected.size == destinations.size && selected.map { it.asset.assetId }.toSet() == destinations.keys) {
            "A selected photo is no longer uniquely indexed."
        }
        val names = selected.map { destinations.getValue(it.asset.assetId) to it.asset.target.displayName }.toSet()
        check(names.size == selected.size && entries.none {
            it.asset.assetId !in destinations && (it.asset.target.relativePath to it.asset.target.displayName) in names
        }) { "대상 폴더에 같은 이름의 사진이 있습니다." }
        persistState(entries.map { entry ->
            val path = destinations[entry.asset.assetId] ?: return@map entry
            entry.copy(asset = entry.asset.copy(target = entry.asset.target.copy(relativePath = path)))
        }, folders().toSet() + destinations.values)
    }

    /** Call only after PhotoKit confirms deletion; a cancelled prompt keeps the index. */
    fun forgetDeleted(assetId: String) = forgetDeleted(setOf(assetId))

    fun forgetDeleted(assetIds: Set<String>) {
        require(assetIds.isNotEmpty() && assetIds.all { it.isNotBlank() }) { "Select at least one photo." }
        check(pending().isEmpty()) { "Recover the outstanding capture before deleting photos." }
        persistState(load().filterNot { it.asset.assetId in assetIds }, folders().toSet(), favoriteAssetIds() - assetIds)
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
