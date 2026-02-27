package com.dudoziworkshop.dzlog.data.counter

import android.content.Context
import android.os.Build
import android.provider.MediaStore

// 0 = no padding (e.g., _1, _10, _5021)
const val COUNTER_DIGITS_DEFAULT = 0

fun clampCounterDigits(v: Int) = v.coerceIn(0, 6)

private fun parseCounterFromDisplayName(
    displayName: String,
    fileNamePrefix: String,
    counterDigits: Int,
    fnDelim: String
): Int? = parseCounterFromDisplayNameForPolicy(displayName, fileNamePrefix, counterDigits, fnDelim)


internal fun parseCounterFromDisplayNameForPolicy(
    displayName: String,
    fileNamePrefix: String,
    counterDigits: Int,
    fnDelim: String
): Int? {
    // Phase 0 note:
    // This parser intentionally depends on (prefix + delimiter + numeric token) only,
    // so it can be shared by both watermark and original file scans.
    // Assumption: original file names follow the same counter suffix convention.

    val base = displayName.substringBeforeLast('.', displayName).trim()
    if (base.isBlank()) return null

    val wildcardPrefix = fileNamePrefix == "*" || fileNamePrefix.isBlank()
    val token = if (wildcardPrefix) {
        base.substringAfterLast(fnDelim, missingDelimiterValue = base).trim()
    } else {
        val prefixToken = "${fileNamePrefix}${fnDelim}"
        if (!base.startsWith(prefixToken)) return null
        base.removePrefix(prefixToken).trim()
    }

    if (token.isBlank()) return null
    if (!token.all { it.isDigit() }) return null
    // NOTE: parsing must be padding-agnostic.
    // counterDigits는 파일명 생성 포맷용이며, 기존 파일 스캔/리싱크에서는
    // 자릿수 변경(예: 3->4) 이후에도 기존 값을 읽어야 카운터가 초기화되지 않는다.
    val v = token.toIntOrNull() ?: return null
    return if (v >= 0) v else null
}

fun scanUsedCountersFromMediaStore(
    context: Context,
    relativePathPrefix: String,
    fileNamePrefix: String,
    counterDigits: Int,
    fnDelim: String
): Set<Int> {
    val out = mutableSetOf<Int>()
    val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        val isWildcardPath = relativePathPrefix == "*" || relativePathPrefix.isBlank()
        val selection = if (isWildcardPath) null else "${MediaStore.Images.Media.RELATIVE_PATH} = ?"
        val selectionArgs = if (isWildcardPath) null else arrayOf(relativePathPrefix)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val pathIdx = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val rel = if (pathIdx >= 0) cursor.getString(pathIdx) else ""
                if (!isWildcardPath && rel != relativePathPrefix) continue
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                parseCounterFromDisplayName(name, fileNamePrefix, counterDigits, fnDelim)?.let(out::add)
            }
        }
        return out
    }

    @Suppress("DEPRECATION")
    val dataCol = MediaStore.Images.Media.DATA
    val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME, dataCol)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val dataIdx = cursor.getColumnIndex(dataCol)
        while (cursor.moveToNext()) {
            val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
            val abs = if (dataIdx >= 0) cursor.getString(dataIdx) else ""
            if (!(relativePathPrefix == "*" || relativePathPrefix.isBlank()) && !abs.contains("/$relativePathPrefix")) continue
            parseCounterFromDisplayName(name, fileNamePrefix, counterDigits, fnDelim)?.let(out::add)
        }
    }
    return out
}
