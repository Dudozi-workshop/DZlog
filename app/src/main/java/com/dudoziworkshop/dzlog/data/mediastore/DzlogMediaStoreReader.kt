package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
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

    data class G1Node(
        val name: String,
        val waterRel: String,
        val originalRel: String,
        val waterCount: Int,
        val originalCount: Int,
        val hasG2: Boolean,
        val latestDateAddedSeconds: Long,
        val latestContentUri: Uri?,
    )

    data class G2Node(
        val label: String,
        val waterRel: String,
        val originalRel: String,
        val waterCount: Int,
        val originalCount: Int,
        val latestDateAddedSeconds: Long,
        val latestContentUri: Uri?,
    )

    private val dzlogBaseLike = "Pictures/DZlog/%"
    fun loadG1Nodes(): List<G1Node> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val where = MediaStoreQueryPolicy.whereRelativePathLike(dzlogBaseLike)

        val g1Names = linkedSetOf<String>()
        val g1HasG2 = mutableMapOf<String, Boolean>()
        data class LatestAgg(var sec: Long, var id: Long)
        val g1Latest = mutableMapOf<String, LatestAgg>()
        val rootLatest = LatestAgg(Long.MIN_VALUE, -1L)

        contentResolver.query(uri, projection, where.selection, where.selectionArgs, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val relIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val rp = ensureTrailingSlash(c.getString(relIdx).orEmpty())
                val sec = c.getLong(dateIdx)
                val segments = extractDzlogSegments(rp)

                if (segments.isEmpty() || (segments.size == 1 && segments[0] == "original")) {
                    if (sec >= rootLatest.sec) {
                        rootLatest.sec = sec
                        rootLatest.id = id
                    }
                }

                if (segments.isEmpty()) continue

                val g1 = segments[0]
                if (g1 == "original") continue

                g1Names += g1
                val latest = g1Latest.getOrPut(g1) { LatestAgg(Long.MIN_VALUE, -1L) }
                if (sec >= latest.sec) {
                    latest.sec = sec
                    latest.id = id
                }
                if (segments.size >= 2 && segments[1] != "original") {
                    g1HasG2[g1] = true
                }
            }
        }

        val rootWaterRel = "Pictures/DZlog/"
        val rootOriginalRel = "Pictures/DZlog/original/"
        val rootNode = G1Node(
            name = ROOT_G1,
            waterRel = rootWaterRel,
            originalRel = rootOriginalRel,
            waterCount = countImagesInRelativePath(rootWaterRel),
            originalCount = countImagesInRelativePath(rootOriginalRel),
            hasG2 = g1Names.isNotEmpty(),
            latestDateAddedSeconds = if (rootLatest.sec == Long.MIN_VALUE) 0L else rootLatest.sec,
            latestContentUri = if (rootLatest.id > 0) ContentUris.withAppendedId(uri, rootLatest.id) else null,
        )

        val g1Nodes = g1Names.map { g1 ->
            val waterRel = "Pictures/DZlog/$g1/"
            val originalRel = "${waterRel}original/"
            val latestAgg = g1Latest[g1]
            val latestSec = latestAgg?.sec ?: Long.MIN_VALUE
            val latestId = latestAgg?.id ?: -1L
            G1Node(
                name = g1,
                waterRel = waterRel,
                originalRel = originalRel,
                waterCount = countImagesInRelativePath(waterRel),
                originalCount = countImagesInRelativePath(originalRel),
                hasG2 = g1HasG2[g1] == true,
                latestDateAddedSeconds = if (latestSec == Long.MIN_VALUE) 0L else latestSec,
                latestContentUri = if (latestId > 0) ContentUris.withAppendedId(uri, latestId) else null,
            )
        }.sortedBy { it.name }

        return listOf(rootNode) + g1Nodes
    }
    fun loadG2Nodes(g1: String): Pair<G2Node?, List<G2Node>> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val g1Key = g1.trim().takeIf { it.isNotBlank() } ?: ROOT_G1

        // ROOT_G1("DZlog")는 실제 G1 폴더가 아니라
        // UI상 루트를 의미하는 논리 라벨이다.
        //
        // 따라서 g1Key == ROOT_G1 인 경우
        // "Pictures/DZlog/DZlog/%" 형태의 잘못된 조회가 발생하지 않도록
        // G2 조회를 수행하지 않고 조기 반환한다.
        if (g1Key == ROOT_G1) {
            return null to emptyList()
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val where = MediaStoreQueryPolicy.whereRelativePathLike("Pictures/DZlog/$g1Key/%")

        data class LatestAgg(var sec: Long, var id: Long)
        val rootLatest = LatestAgg(Long.MIN_VALUE, -1L)
        val g2Latest = mutableMapOf<String, LatestAgg>()
        val g2Names = linkedSetOf<String>()

        contentResolver.query(uri, projection, where.selection, where.selectionArgs, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val relIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val rp = ensureTrailingSlash(c.getString(relIdx).orEmpty())
                val sec = c.getLong(dateIdx)
                val restSegments = extractAfterG1Segments(rp, g1Key)

                if (restSegments.isEmpty() || (restSegments.size == 1 && restSegments[0] == "original")) {
                    if (sec >= rootLatest.sec) {
                        rootLatest.sec = sec
                        rootLatest.id = id
                    }
                    continue
                }

                val g2Name = restSegments.firstOrNull().orEmpty()
                if (g2Name == "original" || g2Name.isBlank()) continue

                g2Names += g2Name
                val latest = g2Latest.getOrPut(g2Name) { LatestAgg(Long.MIN_VALUE, -1L) }
                if (sec >= latest.sec) {
                    latest.sec = sec
                    latest.id = id
                }
            }
        }

        val rootWaterRel = "Pictures/DZlog/$g1Key/"
        val rootOriginalRel = "${rootWaterRel}original/"
        val rootWaterCount = countImagesInRelativePath(rootWaterRel)
        val rootOriginalCount = countImagesInRelativePath(rootOriginalRel)
        val groupRootNode = if (rootWaterCount + rootOriginalCount > 0) {
            G2Node(
                label = GROUP_ROOT_LABEL,
                waterRel = rootWaterRel,
                originalRel = rootOriginalRel,
                waterCount = rootWaterCount,
                originalCount = rootOriginalCount,
                latestDateAddedSeconds = if (rootLatest.sec == Long.MIN_VALUE) 0L else rootLatest.sec,
                latestContentUri = if (rootLatest.id > 0) ContentUris.withAppendedId(uri, rootLatest.id) else null,
            )
        } else {
            null
        }

        val g2Nodes = g2Names.mapNotNull { g2 ->
            val waterRel = "Pictures/DZlog/$g1Key/$g2/"
            val originalRel = "${waterRel}original/"
            val waterCount = countImagesInRelativePath(waterRel)
            val originalCount = countImagesInRelativePath(originalRel)
            if (waterCount == 0 && originalCount == 0) {
                null
            } else {
                val latest = g2Latest[g2]
                val latestSec = latest?.sec ?: Long.MIN_VALUE
                val latestId = latest?.id ?: -1L
                G2Node(
                    label = g2,
                    waterRel = waterRel,
                    originalRel = originalRel,
                    waterCount = waterCount,
                    originalCount = originalCount,
                    latestDateAddedSeconds = if (latestSec == Long.MIN_VALUE) 0L else latestSec,
                    latestContentUri = if (latestId > 0) ContentUris.withAppendedId(uri, latestId) else null,
                )
            }
        }.sortedBy { it.label }

        return groupRootNode to g2Nodes
    }

    /**
     * 특정 경로(프로젝트=저장경로) 아래의 결과물 이미지 목록.
     * - RELATIVE_PATH 는 기기/버전에 따라 trailing slash 표현이 달라질 수 있어
     *   withSlash/withoutSlash 둘 다 허용하되, 폴더 단위 완전일치(=) 정책은 유지한다.
     */
    fun loadImages(relativePath: String): List<MediaImageItem> {
        val where = MediaStoreQueryPolicy.whereExactRelativePath(relativePath)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val out = mutableListOf<MediaImageItem>()

        contentResolver.query(uri, projection, where.selection, where.selectionArgs, sortOrder)?.use { c ->
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
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(uri, projection, where.selection, where.selectionArgs, sortOrder)?.use { c ->
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

    fun loadLatestImageInRelativePath(relativePath: String): MediaImageItem? {
        val where = MediaStoreQueryPolicy.whereExactRelativePath(relativePath)
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(uri, projection, where.selection, where.selectionArgs, sortOrder)?.use { c ->
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

    private fun extractDzlogSegments(relativePath: String): List<String> {
        val prefix = "Pictures/DZlog/"
        if (!relativePath.startsWith(prefix)) return emptyList()
        return relativePath
            .removePrefix(prefix)
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() }
    }

    private fun extractAfterG1Segments(relativePath: String, g1: String): List<String> {
        val prefix = "Pictures/DZlog/$g1/"
        val norm = ensureTrailingSlash(relativePath)
        if (!norm.startsWith(prefix)) return emptyList()
        return norm
            .removePrefix(prefix)
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() }
    }

    companion object {
        /**
         * ROOT_G1은 실제 G1 폴더명이 아니라
         * "경로 미지정(루트)"을 표현하기 위한 UI 논리상 라벨이다.
         *
         * ⚠️ 중요:
         * - 이 값은 실제 저장 폴더명이 아니다.
         * - 사용자가 G1 이름을 "DZlog"로 직접 지정할 경우
         *   루트와 충돌하여 G2 인식이 정상 동작하지 않을 수 있다.
         *
         * 현재 설계 가정:
         * - 사용자가 의도적으로 G1을 "DZlog"로 설정하지 않는다는 전제.
         *
         * 구조적으로 완전한 안전성을 확보하려면
         * ROOT는 문자열이 아니라 null 기반 표현으로 리팩터링해야 한다.
         * (현재 단계에서는 안정성 유지를 위해 문자열 유지)
         */
        const val ROOT_G1 = "DZlog"
        const val GROUP_ROOT_LABEL = "하위 그룹 없음"
    }
}
