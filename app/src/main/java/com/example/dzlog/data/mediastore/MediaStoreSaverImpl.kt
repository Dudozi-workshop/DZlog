package com.example.dzlog.data.mediastore

import com.example.dzlog.R

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

class MediaStoreSaverImpl : MediaStoreSaver {

    override fun saveJpeg(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        relativePath: String,
        jpegQuality: Int
    ): SavedMedia {
        val resolver = context.contentResolver
        val normalizedRelativePath = normalizeRelativePath(relativePath)
        val (resolvedName, isNameAdjusted) = resolveUniqueDisplayName(
            resolver = resolver,
            relativePath = normalizedRelativePath,
            displayName = displayName
        )

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, resolvedName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, normalizedRelativePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("MediaStore insert failed")

        try {
            resolver.openOutputStream(uri)?.use { out ->
                val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(1, 100), out)
                if (!ok) throw IllegalStateException(context.getString(R.string.error_bitmap_compress_failed))
            } ?: throw IllegalStateException("openOutputStream returned null")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }.also { done ->
                    resolver.update(uri, done, null, null)
                }
            }

            val id = ContentUris.parseId(uri)
            return SavedMedia(
                uri = uri,
                mediaStoreId = id,
                displayName = resolvedName,
                isNameAdjusted = isNameAdjusted
            )

        } catch (e: Exception) {
            // 실패 시 흔적 제거
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    private fun resolveUniqueDisplayName(
        resolver: android.content.ContentResolver,
        relativePath: String,
        displayName: String
    ): Pair<String, Boolean> {
        if (!doesNameExist(resolver, relativePath, displayName)) {
            return displayName to false
        }

        val (stem, ext) = splitFileName(displayName)
        var suffix = 2
        while (true) {
            val candidate = if (ext.isEmpty()) {
                "$stem ($suffix)"
            } else {
                "$stem ($suffix).$ext"
            }
            if (!doesNameExist(resolver, relativePath, candidate)) {
                return candidate to true
            }
            suffix++
        }
    }

    private fun doesNameExist(
        resolver: android.content.ContentResolver,
        relativePath: String,
        displayName: String
    ): Boolean {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH}=? AND " +
                "${MediaStore.Images.Media.DISPLAY_NAME}=?"
        val selectionArgs = arrayOf(relativePath, displayName)
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }

    private fun splitFileName(displayName: String): Pair<String, String> {
        val dot = displayName.lastIndexOf('.')
        if (dot <= 0 || dot == displayName.length - 1) {
            return displayName to ""
        }
        val stem = displayName.take(dot)
        val ext = displayName.drop(dot + 1)
        return stem to ext
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
        // counterDigits == 0: no padding mode. Accept any digit length.
        if (counterDigits == 0) {
            if (token.isBlank()) return 0
            if (!token.all { it.isDigit() }) return 0
            return token.toIntOrNull() ?: 0
        }

        if (token.length != counterDigits) return 0
        if (!token.all { it.isDigit() }) return 0
        return token.toIntOrNull() ?: 0
    }
}
