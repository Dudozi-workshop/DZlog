package com.example.dzlog.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import com.example.dzlog.domain.model.LogGroupSummary
import com.example.dzlog.domain.model.MediaImageItem

/**
 * DZlog 결과물(워터마크 이미지)만 조회하기 위한 MediaStore Reader.
 *
 * 정책:
 * - 포함: Pictures/DZlog/ 하위
 * - 제외: .../original/ 하위 (원본 저장 폴더)
 */
class DzlogMediaStoreReader(
    private val contentResolver: ContentResolver
) {

    private val dzlogBaseLike = "Pictures/DZlog/%"


    /**
     * G1 목록 + 대표 썸네일/개수/최근날짜 요약.
     * - 정렬: 최근날짜 DESC, 그 다음 이름 ASC (기본은 맨 위)
     */
    fun loadG1Summaries(): List<LogGroupSummary> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )

        val selection = (
                "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND " +
        "${MediaStore.Images.Media.RELATIVE_PATH} NOT LIKE ?" +
        trashClause()
        )
        val args = arrayOf(dzlogBaseLike, "%/original/%")


        data class Agg(var count: Int, var latestSec: Long, var latestId: Long)
        val map = linkedMapOf<String, Agg>()

        contentResolver.query(uri, projection, selection, args, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val relIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val rp = c.getString(relIdx).orEmpty()
                if (isOriginalPath(rp)) continue
                val sec = c.getLong(dateIdx)

                val g1 = extractG1(rp)
                val agg = map.getOrPut(g1) { Agg(count = 0, latestSec = Long.MIN_VALUE, latestId = -1L) }
                agg.count += 1
                if (sec >= agg.latestSec) {
                    agg.latestSec = sec
                    agg.latestId = id
                }
            }
        }

        val summaries = map.map { (name, agg) ->
            LogGroupSummary(
                name = name,
                photoCount = agg.count,
                latestDateAddedSeconds = if (agg.latestSec == Long.MIN_VALUE) 0L else agg.latestSec,
                latestContentUri = if (agg.latestId > 0) ContentUris.withAppendedId(uri, agg.latestId) else null
            )
        }

        return summaries.sortedWith(
            compareByDescending<LogGroupSummary> { it.name == DEFAULT_G1 } // false first
                .thenByDescending { it.latestDateAddedSeconds }
                .thenBy { it.name }
        ).let { list ->
            // (기본)은 항상 맨 위
            val base = list.filter { it.name == DEFAULT_G1 }
            val rest = list.filter { it.name != DEFAULT_G1 }
            base + rest
        }
    }


    /**
     * 특정 G1 하위의 G2 목록 + 대표 썸네일/개수/최근날짜 요약.
     * - 정렬: 최근날짜 DESC, 그 다음 이름 ASC (기본은 맨 위)
     */
    fun loadG2Summaries(g1: String): List<LogGroupSummary> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )

        val g1Norm = normalizeG1(g1)
        val like = if (g1Norm == DEFAULT_G1) dzlogBaseLike else "Pictures/DZlog/$g1Norm/%"
        val selection = (
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND " +
                    "${MediaStore.Images.Media.RELATIVE_PATH} NOT LIKE ?" +
                trashClause()
                )
        val args = arrayOf(like, "%/original/%")

        data class Agg(var count: Int, var latestSec: Long, var latestId: Long)
        val map = linkedMapOf<String, Agg>()

        contentResolver.query(uri, projection, selection, args, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val relIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val rp = c.getString(relIdx).orEmpty()
                if (isOriginalPath(rp)) continue
                val sec = c.getLong(dateIdx)

                val g2 = extractG2(rp, g1Norm)
                val agg = map.getOrPut(g2) { Agg(count = 0, latestSec = Long.MIN_VALUE, latestId = -1L) }
                agg.count += 1
                if (sec >= agg.latestSec) {
                    agg.latestSec = sec
                    agg.latestId = id
                }
            }
        }

        val summaries = map.map { (name, agg) ->
            LogGroupSummary(
                name = name,
                photoCount = agg.count,
                latestDateAddedSeconds = if (agg.latestSec == Long.MIN_VALUE) 0L else agg.latestSec,
                latestContentUri = if (agg.latestId > 0) ContentUris.withAppendedId(uri, agg.latestId) else null
            )
        }

        // (기본)은 항상 맨 위
        val base = summaries.filter { it.name == DEFAULT_G2 }
        val rest = summaries.filter { it.name != DEFAULT_G2 }
            .sortedWith(compareByDescending<LogGroupSummary> { it.latestDateAddedSeconds }.thenBy { it.name })
        return base + rest
    }

    /**
     * 특정 경로(프로젝트=저장경로) 아래의 결과물 이미지 목록.
     * - relativePath는 trailing "/" 포함 형태를 기대함.
     */
    fun loadImages(relativePath: String): List<MediaImageItem> {
        // 기기/OS 조합에 따라 RELATIVE_PATH 값에 trailing '/'가 붙지 않는 케이스가 있어
        // '=' 매칭이 실패할 수 있음. (e.g. "Pictures/DZlog/A/0812" vs "Pictures/DZlog/A/0812/")
        // 둘 다 매칭하도록 보수적으로 처리한다.
        val relWithSlash = ensureTrailingSlash(relativePath)
        val relNoSlash = relWithSlash.removeSuffix("/")
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )

        // A-2: exact match만 허용 (하위 폴더 포함 LIKE 제거)
        val selection = (
                "(" +
                        "(${MediaStore.Images.Media.RELATIVE_PATH} = ? OR ${MediaStore.Images.Media.RELATIVE_PATH} = ?) " +
                        ") AND " +
                        "${MediaStore.Images.Media.RELATIVE_PATH} NOT LIKE ?" +
                        trashClause()
                )
        val args = arrayOf(
            relWithSlash,
            relNoSlash,
            "%/original/%"
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val out = mutableListOf<MediaImageItem>()

        contentResolver.query(uri, projection, selection, args, sortOrder)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val relIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val displayName = c.getString(nameIdx).orEmpty()
                val rp = c.getString(relIdx).orEmpty()
                val dateAdded = c.getLong(dateIdx)

                // ✅ 원본 폴더는 앱 로그에서 제외
                if (isOriginalPath(rp)) continue

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
     * - Pictures/DZlog/ 하위
     * - .../original/ 제외
     */
    fun loadLatestImage(): MediaImageItem? {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )
        val selection = (
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND " +
                    "${MediaStore.Images.Media.RELATIVE_PATH} NOT LIKE ?" +
                    trashClause()
                )
        val args = arrayOf(dzlogBaseLike, "%/original/%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(uri, projection, selection, args, sortOrder)?.use { c ->
            if (!c.moveToFirst()) return null
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val relIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            val id = c.getLong(idIdx)
            val displayName = c.getString(nameIdx).orEmpty()
            val rp = c.getString(relIdx).orEmpty()
            val dateAdded = c.getLong(dateIdx)

            if (isOriginalPath(rp)) return null
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

    private fun isOriginalPath(relativePath: String): Boolean {
        return relativePath.contains("/original/")
    }

    private fun extractG1(relativePath: String): String {
        // 기대 형태: Pictures/DZlog/ 또는 Pictures/DZlog/<G1>/ 또는 .../<G1>/<G2>/
        val norm = ensureTrailingSlash(relativePath)
        val prefix = "Pictures/DZlog/"
        if (!norm.startsWith(prefix)) return DEFAULT_G1
        val rest = norm.removePrefix(prefix)
        if (rest.isBlank() || rest == "/") return DEFAULT_G1
        val first = rest.substringBefore('/')
        return first.ifBlank { DEFAULT_G1 }
    }

    private fun extractG2(relativePath: String, g1: String): String {
        val norm = ensureTrailingSlash(relativePath)
        val prefix = if (g1 == DEFAULT_G1) "Pictures/DZlog/" else "Pictures/DZlog/$g1/"
        if (!norm.startsWith(prefix)) return DEFAULT_G2
        val rest = norm.removePrefix(prefix)
        // rest: "" or "<g2>/" or "<g2>/<...>/"
        val first = rest.substringBefore('/')
        return first.ifBlank { DEFAULT_G2 }
    }

    private fun normalizeG1(g1: String): String {
        return g1.takeIf { it.isNotBlank() && it != DEFAULT_G1 } ?: DEFAULT_G1
    }

    private fun ensureTrailingSlash(path: String): String {
        return if (path.endsWith('/')) path else "$path/"
    }

    companion object {
        const val DEFAULT_G1 = "(기본)"
        const val DEFAULT_G2 = "(기본)"
    }
}

private fun trashClause(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        " AND (${MediaStore.Images.Media.IS_TRASHED} = 0 OR ${MediaStore.Images.Media.IS_TRASHED} IS NULL)"
    } else {
        ""
    }
}
