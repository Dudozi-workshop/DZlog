package com.example.dzlog.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
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
     * 1단(G1) 그룹 목록.
     * - Pictures/DZlog/<G1>/
     * - G1이 비어있는 경우("Pictures/DZlog/")는 (기본)으로 묶어서 반환
     */
    fun loadG1List(): List<String> {
        val paths = queryDistinctRelativePaths(like = dzlogBaseLike)
        val g1s = paths.map { extractG1(it) }.distinct()
        // (기본)은 항상 맨 위로
        return g1s.sortedWith(compareBy<String> { it != DEFAULT_G1 }.thenBy { it })
    }

    /**
     * 특정 G1 하위의 2단(G2) 목록.
     * - Pictures/DZlog/<G1>/<G2>/
     * - G2가 없는 경우는 (기본)으로 묶어서 반환
     */
    fun loadG2List(g1: String): List<String> {
        val g1Norm = normalizeG1(g1)

        // 정책: G1이 (기본)인 경우는 "Pictures/DZlog/" 바로 아래에 저장된 결과물만 의미함.
        // 이 경우 2단(G2) 탐색은 하지 않고 (기본)만 반환해서 바로 그리드로 이어지게 함.
        if (g1Norm == DEFAULT_G1) return listOf(DEFAULT_G2)

        val like = "Pictures/DZlog/$g1Norm/%"
        val paths = queryDistinctRelativePaths(like = like)
        val g2s = paths
            .map { extractG2(it, g1Norm) }
            .distinct()
        return g2s.sortedWith(compareBy<String> { it != DEFAULT_G2 }.thenBy { it })
    }

    /**
     * 특정 경로(프로젝트=저장경로) 아래의 결과물 이미지 목록.
     * - relativePath는 trailing "/" 포함 형태를 기대함.
     */
    fun loadImages(relativePath: String): List<MediaImageItem> {
        val rel = ensureTrailingSlash(relativePath)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )

        val selection = (
            "${MediaStore.Images.Media.RELATIVE_PATH} = ? AND " +
                "${MediaStore.Images.Media.RELATIVE_PATH} NOT LIKE ?"
            )
        val args = arrayOf(rel, "%/original/%")

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

    // -------------------------
    // Internal helpers
    // -------------------------

    private fun queryDistinctRelativePaths(like: String): List<String> {
        val projection = arrayOf(MediaStore.Images.Media.RELATIVE_PATH)
        val selection = (
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND " +
                "${MediaStore.Images.Media.RELATIVE_PATH} NOT LIKE ?"
            )
        val args = arrayOf(like, "%/original/%")
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val set = linkedSetOf<String>()
        contentResolver.query(uri, projection, selection, args, null)?.use { c ->
            val relIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            while (c.moveToNext()) {
                val rp = c.getString(relIdx).orEmpty()
                // ✅ 원본 폴더는 앱 로그에서 제외
                if (isOriginalPath(rp)) continue
                set.add(rp)
            }
        }
        return set.toList()
    }

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
        return if (first.isBlank()) DEFAULT_G1 else first
    }

    private fun extractG2(relativePath: String, g1: String): String {
        val norm = ensureTrailingSlash(relativePath)
        val prefix = if (g1 == DEFAULT_G1) "Pictures/DZlog/" else "Pictures/DZlog/$g1/"
        if (!norm.startsWith(prefix)) return DEFAULT_G2
        val rest = norm.removePrefix(prefix)
        // rest: "" or "<g2>/" or "<g2>/<...>/"
        val first = rest.substringBefore('/')
        return if (first.isBlank()) DEFAULT_G2 else first
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
