package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.dudoziworkshop.dzlog.domain.gallery.SessionThumbnailPolicy
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem

/**
 * DZlog 결과물 조회용 MediaStore Reader.
 *
 * 정책:
 * - 포함: Pictures/DZlog/ 하위 전체 (original 포함)
 */
class DzlogMediaStoreReader(
    private val contentResolver: ContentResolver
) {
    private val visiblePhotoClause: String
        get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
            " AND (${MediaStore.Images.Media.IS_PENDING} = 0 OR ${MediaStore.Images.Media.IS_PENDING} IS NULL)"
        else ""

    private val sortOrderDateAddedDesc = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"

    /**
     * 특정 경로(프로젝트=저장경로) 아래의 결과물 이미지 목록.
     * - RELATIVE_PATH 는 기기/버전에 따라 trailing slash 표현이 달라질 수 있어
     *   withSlash/withoutSlash 둘 다 허용하되, 폴더 단위 완전일치(=) 정책은 유지한다.
     */
    /**
     * Depth-independent folder index. Empty directory paths can be supplied by a
     * future SAF directory enumerator; MediaStore alone cannot discover empty folders.
     * Execute on Dispatchers.IO (not on the Compose main thread).
     */
    data class GallerySnapshot(
        val index: com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndex,
        val directPhotos: List<MediaImageItem>,
        val allPhotos: List<MediaImageItem>,
        // Prepared for the next UI patch; existing callers keep using index/photos.
        val summariesByPath: Map<String, com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummary> = emptyMap(),
    )

    /**
     * Exactly one MediaStore query per navigation, including the root.
     * An inaccessible SAF tree must not hide already-readable pictures.
     */
    fun loadGallerySnapshot(
        relativePath: String,
        existingFolderPaths: List<String> = emptyList(),
    ): GallerySnapshot {
        val prefix = MediaStoreQueryPolicy.normalizeRelativePath(relativePath)
        val all = loadImagesUnderPrefix(prefix)
        val index = com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy.index(
            currentRelativePath = prefix,
            imageRelativePaths = all.map { it.relativePath },
            existingFolderPaths = existingFolderPaths,
        )

        // Project existing MediaStore rows into the shared, Android-independent summary policy.
        // DATE_ADDED is the only available timestamp in MediaImageItem at this stage.
        val summaryImages = all.map { photo ->
            com.dudoziworkshop.dzlog.feature.log.policy.GallerySummaryImage(
                stableId = photo.id.toString(),
                directoryPath = photo.relativePath,
                isOriginal = photo.relativePath.trimEnd('/')
                    .substringAfterLast('/').equals("original", ignoreCase = true),
                addedAtMillis = photo.dateAddedSeconds.takeIf { it > 0L }?.times(1000L),
            )
        }

        // Include intermediate ancestors: an image in A/B/C implies A/B exists,
        // even if MediaStore returns no photo directly inside A/B.
        val rootPath = com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummaryPolicy
            .normalizeFolderPath(prefix)
        val knownFolders = (existingFolderPaths + all.map { it.relativePath }).flatMap { rawPath ->
            val folder = com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummaryPolicy
                .normalizeFolderPath(rawPath)
            if (folder != rootPath && !folder.startsWith("$rootPath/")) {
                emptyList()
            } else {
                var parent = rootPath
                folder.removePrefix(rootPath).trim('/').split('/')
                    .filter { it.isNotBlank() }
                    .map { part ->
                        parent += "/$part"
                        "$parent/"
                    }
            }
        }.distinct()

        val summaryPaths = listOf(prefix) + index.children.map { it.relativePath }
        val summaries = summaryPaths.associateWith { folderPath ->
            com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummaryPolicy.summarize(
                folderPath = folderPath,
                images = summaryImages,
                folderPaths = knownFolders,
            )
        }
        return GallerySnapshot(
            index = index,
            directPhotos = all.filter { it.relativePath.trimEnd('/') == prefix.trimEnd('/') },
            allPhotos = all,
            summariesByPath = summaries,
        )
    }

    fun loadFolderIndex(
        relativePath: String,
        existingFolderPaths: List<String> = emptyList(),
    ): com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndex {
        val paths = loadImagesUnderPrefix(relativePath).map { it.relativePath }
        return com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy.index(
            currentRelativePath = relativePath,
            imageRelativePaths = paths,
            existingFolderPaths = existingFolderPaths,
        )
    }

    fun loadImages(relativePath: String, requireReadable: Boolean = false): List<MediaImageItem> {
        val where = MediaStoreQueryPolicy.whereExactRelativePath(relativePath)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )
        return queryImages(where, projection, requireReadable)
    }

    /**
     * prefix 하위 전체를 조회한다.
     * - normalizeRelativePath로 trailing slash를 강제해 Exact 조회와 용도를 분리한다.
     * - 예: "Pictures/DZlog/N600/" -> LIKE "Pictures/DZlog/N600/%"
     */
    fun loadImagesUnderPrefix(relativePathPrefix: String): List<MediaImageItem> {
        val normalizedPrefix = MediaStoreQueryPolicy.normalizeRelativePath(relativePathPrefix)
        val where = MediaStoreQueryPolicy.whereRelativePathLike("${normalizedPrefix}%")
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )
        return queryImages(where, projection, requireReadable = true)
    }

    private fun queryImages(
        where: MediaStoreQueryPolicy.WhereClause,
        projection: Array<String>,
        requireReadable: Boolean = false,
    ): List<MediaImageItem> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val out = mutableListOf<MediaImageItem>()

        val cursor = contentResolver.query(uri, projection, where.selection + visiblePhotoClause, where.selectionArgs, sortOrderDateAddedDesc)
        check(!requireReadable || cursor != null) { "사진 목록을 조회하지 못했습니다. 다시 시도해 주세요." }
        cursor?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val relIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val displayName = c.getString(nameIdx).orEmpty()
                val rp = c.getString(relIdx).orEmpty()
                val dateAdded = c.getLong(dateIdx)

                val contentUri = ContentUris.withAppendedId(uri, id)
                out.add(
                    MediaImageItem(
                        id = id,
                        uri = contentUri,
                        displayName = displayName,
                        relativePath = rp,
                        dateAddedSeconds = dateAdded
                    )
                )
            }
        }
        return out
    }

    /**
     * DZlog 전체 결과물 중 "가장 최근" 1장.
     * - Pictures/DZlog/ 하위 (original 포함)
     */
    fun loadLatestImage(): MediaImageItem? {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )
        val where = MediaStoreQueryPolicy.whereRelativePathLike(dzlogBaseLike)
        contentResolver.query(uri, projection, where.selection + visiblePhotoClause, where.selectionArgs, sortOrderDateAddedDesc)?.use { c ->
            if (!c.moveToFirst()) return null
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val relIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            val id = c.getLong(idIdx)
            val displayName = c.getString(nameIdx).orEmpty()
            val rp = c.getString(relIdx).orEmpty()
            val dateAdded = c.getLong(dateIdx)

            val contentUri = ContentUris.withAppendedId(uri, id)
            return MediaImageItem(
                id = id,
                uri = contentUri,
                displayName = displayName,
                relativePath = rp,
                dateAddedSeconds = dateAdded
            )
        }
        return null
    }


    fun countImagesInRelativePath(relativePath: String): Int {
        val where = MediaStoreQueryPolicy.whereExactRelativePath(relativePath)
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)

        contentResolver.query(uri, projection, where.selection, where.selectionArgs, null)?.use { c ->
            return c.count
        }
        return 0
    }

    /**
     * The camera thumbnail follows committed captures in this camera session,
     * not the next image's destination folder. Try the latest shutter first;
     * a deleted result may fall back to its original, then to an older capture.
     * Each URI is looked up directly rather than scanning the whole gallery.
     */
    fun loadLatestImageInSession(savedUriGroups: List<List<Uri>>): MediaImageItem? =
        SessionThumbnailPolicy.newestAvailable(
            groupsOldestFirst = savedUriGroups,
            lookup = ::loadSavedSessionImage,
            isOriginal = { photo ->
                photo.relativePath.trimEnd('/').substringAfterLast('/')
                    .equals("original", ignoreCase = true)
            },
        )

    private fun loadSavedSessionImage(contentUri: Uri): MediaImageItem? {
        if (contentUri.scheme != "content" || contentUri.authority != "media") return null
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val where = MediaStoreQueryPolicy.whereRelativePathLike(dzlogBaseLike)
        return contentResolver.query(
            contentUri, projection, where.selection + visiblePhotoClause,
            where.selectionArgs, null,
        )?.use { c ->
            if (!c.moveToFirst()) return@use null
            val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            MediaImageItem(
                id = id,
                uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                displayName = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)).orEmpty(),
                relativePath = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)).orEmpty(),
                dateAddedSeconds = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)),
            )
        }
    }

    fun loadLatestImageInRelativePath(relativePath: String): MediaImageItem? {
        val where = MediaStoreQueryPolicy.whereExactRelativePath(relativePath)
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )
        contentResolver.query(uri, projection, where.selection + visiblePhotoClause, where.selectionArgs, sortOrderDateAddedDesc)?.use { c ->
            if (!c.moveToFirst()) return null
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val relIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            val id = c.getLong(idIdx)
            val displayName = c.getString(nameIdx).orEmpty()
            val rp = c.getString(relIdx).orEmpty()
            val dateAdded = c.getLong(dateIdx)

            val contentUri = ContentUris.withAppendedId(uri, id)
            return MediaImageItem(
                id = id,
                uri = contentUri,
                displayName = displayName,
                relativePath = rp,
                dateAddedSeconds = dateAdded
            )
        }
        return null
    }


    // -------------------------
    // Internal helpers
    // -------------------------

    private fun ensureTrailingSlash(path: String): String {
        return if (path.endsWith('/')) path else "$path/"
    }

    companion object {
        // The retired two-level grouping enum is gone. This label is only
        // retained for older, non-active delete helper compatibility.
        const val ROOT_G1 = "DZlog"
        const val GROUP_ROOT_LABEL = "하위 그룹 없음"
    }
}
