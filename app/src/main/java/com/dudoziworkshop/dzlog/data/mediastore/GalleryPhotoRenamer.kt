package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.dudoziworkshop.dzlog.data.project.CaptureRecordRoomStore
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GALLERY_PHOTO_RENAME_LIMIT
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenameExecutor
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenameGroup
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenamePlan
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenamePolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenameResult
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenameStorage
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryRenameLocation
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryStoredPhotoName
import com.dudoziworkshop.dzlog.feature.log.policy.renamePath
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class GalleryPhotoRenameSnapshot(
    val groups: List<GalleryPhotoRenameGroup>,
    val photos: List<MediaImageItem>,
    val otherFiles: Set<GalleryRenameLocation>,
)

internal class GalleryPhotoRenamer(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val storage = MediaStoreRenameStorage(resolver)

    suspend fun prepare(selectedIds: Set<Long>): GalleryPhotoRenameSnapshot {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { "이 Android 버전에서는 사진 이름 변경을 지원하지 않습니다." }
        requireWithinLimit(selectedIds.size)
        val photos = DzlogMediaStoreReader(resolver).loadImagesUnderPrefix(GalleryFolderIndexPolicy.ROOT)
        val selected = photos.filter { it.id in selectedIds }
        check(selected.size == selectedIds.size) { "선택한 파일 일부를 찾지 못했습니다." }
        val groups = GalleryPhotoRenamePolicy.groups(selected, photos, recordedPairs(selected, photos))
        requireWithinLimit(groups.sumOf { it.photos.size })
        val paths = groups.flatMap { group -> group.photos.map { renamePath(it.relativePath) } }.toSet()
        val relevantPhotos = photos.filter { renamePath(it.relativePath) in paths }
        val visibleIds = relevantPhotos.mapTo(mutableSetOf()) { it.id }
        val otherFiles = paths.flatMap { path ->
            storage.folderFiles(path).filter { it.first !in visibleIds }
                .map { GalleryRenameLocation.of(path, it.second) }
        }.toSet()
        return GalleryPhotoRenameSnapshot(groups, relevantPhotos, otherFiles)
    }

    suspend fun rename(plan: GalleryPhotoRenamePlan): GalleryPhotoRenameResult = writeLock.withLock {
        val fresh = DzlogMediaStoreReader(resolver).loadImagesUnderPrefix(GalleryFolderIndexPolicy.ROOT)
        val selectedIds = plan.ready.mapTo(mutableSetOf()) { it.group.id }
        val selected = fresh.filter { it.id in selectedIds }
        val currentGroups = if (selected.isEmpty()) emptyMap() else {
            GalleryPhotoRenamePolicy.groups(selected, fresh, recordedPairs(selected, fresh)).associateBy { it.id }
        }
        val unavailable = plan.ready.mapNotNull { row ->
            val group = currentGroups[row.group.id]
            val currentIds = group?.photos?.map { it.id }?.toSet()
            val plannedIds = row.group.photos.map { it.id }.toSet()
            val issue = group?.pairingIssue ?: if (currentIds != plannedIds) "연결된 사진이 바뀌어 제외했습니다." else null
            issue?.let { row.group.id to it }
        }.toMap()
        GalleryPhotoRenameExecutor(storage).execute(plan, unavailable)
    }

    private suspend fun recordedPairs(
        selected: List<MediaImageItem>,
        live: List<MediaImageItem>,
    ): Map<Long, Set<Long>> {
        // MediaStore can return the same row through external or external_primary.
        val byId = live.associateBy { it.id }
        val nearby = live.groupBy { GalleryRenameLocation.of(it.relativePath, it.displayName) }
        val lookup = selected.flatMap { photo ->
            val possiblePair = GalleryPhotoRenamePolicy.counterpartPath(photo.relativePath)
                ?.let { nearby[GalleryRenameLocation.of(it, photo.displayName)] }.orEmpty()
            listOf(photo) + possiblePair
        }.distinctBy { it.id }
        val urisById = lookup.associate { photo ->
            photo.id to setOf(
                photo.uri.toString(),
                ContentUris.withAppendedId(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), photo.id,
                ).toString(),
            )
        }
        val recordsByUri = CaptureRecordRoomStore(appContext)
            .findByMediaUris(urisById.values.flatten().distinct())
            .flatMap { record -> record.mediaUris.map { it to record } }
            .groupBy({ it.first }, { it.second })
        return buildMap {
            for ((photoId, uris) in urisById) {
                val matchingRecords = uris.flatMap { recordsByUri[it].orEmpty() }.distinctBy { it.captureId }
                if (matchingRecords.isEmpty()) continue
                if (matchingRecords.size > 1) {
                    put(photoId, setOf(Long.MIN_VALUE))
                    continue
                }
                val record = matchingRecords.single()
                val pairIds = record.mediaUris.filterNot { it in uris }.mapNotNull { partner ->
                    val partnerUri = android.net.Uri.parse(partner)
                    val id = partnerUri.lastPathSegment?.toLongOrNull() ?: return@mapNotNull Long.MIN_VALUE
                    if (id in byId) id else {
                        // Only a confirmed missing row may be treated as an absent original/result.
                        val observation = runCatching { storage.readUri(partnerUri) }
                        if (observation.isSuccess && observation.getOrNull() == null) null else id
                    }
                }.toSet()
                put(photoId, pairIds)
            }
        }
    }

    private fun requireWithinLimit(fileCount: Int) {
        require(fileCount <= GALLERY_PHOTO_RENAME_LIMIT) {
            "원본 포함 ${GALLERY_PHOTO_RENAME_LIMIT}개 파일까지 일괄 변경할 수 있습니다."
        }
    }

    private companion object {
        val writeLock = Mutex()
    }
}

private class MediaStoreRenameStorage(private val resolver: ContentResolver) : GalleryPhotoRenameStorage {
    override fun folderFiles(path: String): List<Pair<Long, String>> {
        val where = MediaStoreQueryPolicy.whereExactRelativePath(path)
        return checkNotNull(resolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
            where.selection, where.selectionArgs, null,
        )) { "대상 폴더의 파일명을 확인하지 못했습니다." }.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1) ?: continue
                    add(cursor.getLong(0) to name)
                }
            }
        }
    }

    override fun update(photo: MediaImageItem, name: String): Boolean {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name) }
        return resolver.update(photo.uri, values, null, null) > 0
    }

    override fun read(photo: MediaImageItem): GalleryStoredPhotoName? = readUri(photo.uri)

    fun readUri(uri: android.net.Uri): GalleryStoredPhotoName? =
        checkNotNull(resolver.query(
            uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH),
            null, null, null,
        )) { "파일을 확인하지 못했습니다." }.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val name = cursor.getString(0) ?: return@use null
            val path = cursor.getString(1) ?: return@use null
            GalleryStoredPhotoName(name, renamePath(path))
        }
}
