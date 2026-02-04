package com.example.dzlog.data.counter

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
): Int? {
    val base = displayName.substringBeforeLast('.', displayName)
    val prefixToken = "${fileNamePrefix}${fnDelim}"
    if (!base.startsWith(prefixToken)) return null
    val token = base.removePrefix(prefixToken).trim()
    if (token.isBlank()) return null
    if (!token.all { it.isDigit() }) return null
    if (counterDigits != 0 && token.length != counterDigits) return null
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
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("$relativePathPrefix%")

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val pathIdx = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val rel = if (pathIdx >= 0) cursor.getString(pathIdx) else ""
                if (!rel.startsWith(relativePathPrefix)) continue
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
            if (!abs.contains("/$relativePathPrefix")) continue
            parseCounterFromDisplayName(name, fileNamePrefix, counterDigits, fnDelim)?.let(out::add)
        }
    }
    return out
}
