package com.example.dzlog.data.mediastore

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

class MediaStoreSaverImpl : MediaStoreSaver {

    // ✅ 폴더(Pictures/DZlog/.../...) 안에서 "displayNameBase_0001.jpg" 같은 파일명들 중
// ✅ 숫자(counter) 최대값을 찾아서 반환
    fun findMaxCounterInFolder(
        context: android.content.Context,
        relativePath: String,          // 예: "Pictures/DZlog/Proj/G1/G2/"
        displayNameBase: String,       // 예: "DZlog"
    ): Int {
        val cr = context.contentResolver

        // MediaStore에서 이 폴더의 이미지들만 조회
        val collection = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            android.provider.MediaStore.Images.Media._ID,
            android.provider.MediaStore.Images.Media.DISPLAY_NAME
        )

        // RELATIVE_PATH 정확히 일치 + 파일명에 base 포함
        val selection = "${android.provider.MediaStore.Images.Media.RELATIVE_PATH} = ? AND " +
                "${android.provider.MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"

        val selectionArgs = arrayOf(
            ensureTrailingSlash(relativePath),
            "%$displayNameBase%"
        )

        val sortOrder = "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC"

        var max = 0

        cr.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol) ?: continue

                // 파일명에서 "base_####" 숫자만 뽑기
                val n = extractCounterFromName(name, displayNameBase)
                if (n != null && n > max) max = n
            }
        }

        return max
    }

    // ----------------- helpers -----------------
    private fun ensureTrailingSlash(p: String): String =
        if (p.endsWith("/")) p else "$p/"

    // 예: base="DZlog", name="DZlog_0007.jpg" -> 7
    private fun extractCounterFromName(fileName: String, base: String): Int? {
        // base_숫자 형태만 인정
        // 확장자 제거
        val dot = fileName.lastIndexOf('.')
        val stem = if (dot > 0) fileName.substring(0, dot) else fileName

        // base_ 이후를 숫자로 파싱
        val prefix = "${base}_"
        val idx = stem.lastIndexOf(prefix)
        if (idx < 0) return null

        val numPart = stem.substring(idx + prefix.length)
        if (numPart.isBlank()) return null

        return numPart.toIntOrNull()
    }

    override fun saveJpeg(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        relativePath: String
    ): SavedMedia {
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, ensureJpg(displayName))
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, normalizeRelativePath(relativePath))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("MediaStore insert failed")

        try {
            resolver.openOutputStream(uri)?.use { out ->
                val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                if (!ok) throw IllegalStateException("Bitmap compress failed")
            } ?: throw IllegalStateException("openOutputStream returned null")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }.also { done ->
                    resolver.update(uri, done, null, null)
                }
            }

            val id = ContentUris.parseId(uri)
            return SavedMedia(uri = uri, mediaStoreId = id)

        } catch (e: Exception) {
            // 실패 시 흔적 제거
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    override fun deleteByUri(context: Context, uri: Uri): Boolean {
        return try {
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 촬영 버튼 클릭 시 호출 금지.
     * 트리거(최초 진입/프로젝트 변경/설정 저장/재진입)에서만 호출.
     *
     * counterDigits: 예) 4면 0001~9999
     * fnDelim: 파일명에서 카운터 앞 구분자(예: "_" 또는 "-")
     *
     * ⚠️ 파일명 규칙은 너 기존 규칙에 맞춰서 prefix/파싱만 조정하면 됨.
     */
    override fun findMaxCounterInFolder(
        context: Context,
        relativePath: String,
        counterDigits: Int,
        fnDelim: String
    ): Int {
        val resolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        // RELATIVE_PATH는 보통 끝에 "/"가 붙어 저장됨
        val rel = normalizeRelativePath(relativePath)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(rel)

        // 최신부터 보면 조기 종료 가능
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sort
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            var max = 0
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: continue
                val c = parseCounterFromName(name, counterDigits, fnDelim)
                if (c > max) {
                    max = c
                    // (선택) max가 충분히 큰 값이면 계속 돌 필요는 없지만,
                    // 파일명이 섞일 수 있어서 여기선 안전하게 끝까지 본다.
                }
            }
            return max
        }

        return 0
    }

    // ----------------------
    // Helpers
    // ----------------------

    private fun ensureJpg(displayName: String): String {
        val n = displayName.trim()
        return if (n.endsWith(".jpg", true) || n.endsWith(".jpeg", true)) n else "$n.jpg"
    }

    /**
     * RELATIVE_PATH는 "Pictures/DZlog/..." 형태로 들어오면
     * 내부적으로 끝에 "/"가 붙는 경우가 많아서 통일.
     */
    private fun normalizeRelativePath(relativePath: String): String {
        val p = relativePath
            .trim()
            .trimStart('/')
            .replace("\\", "/")
        return if (p.endsWith("/")) p else "$p/"
    }

    /**
     * 파일명에서 마지막 카운터를 찾는다.
     * 예: "ABC_DEF_0007.jpg" (fnDelim="_", counterDigits=4) -> 7
     *
     * 너 기존 파일명 규칙과 다르면 여기만 수정하면 됨.
     */
    private fun parseCounterFromName(name: String, counterDigits: Int, fnDelim: String): Int {
        val base = name.substringBeforeLast('.', name)

        // 가장 흔한 패턴: delim 뒤에 digits
        val token = base.substringAfterLast(fnDelim, missingDelimiterValue = "")
        if (token.length != counterDigits) return 0
        if (!token.all { it.isDigit() }) return 0
        return token.toIntOrNull() ?: 0
    }
}
