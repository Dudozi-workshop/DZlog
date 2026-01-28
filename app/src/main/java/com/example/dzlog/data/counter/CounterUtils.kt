package com.example.dzlog.data.counter

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import org.json.JSONArray

const val COUNTER_DIGITS_DEFAULT = 4

fun clampCounterDigits(v: Int) = v.coerceIn(1, 6)

private fun encodeCounterSetJson(values: Set<Int>): String {
    val arr = JSONArray()
    values.sorted().forEach { arr.put(it) }
    return arr.toString()
}

private fun decodeCounterSetJson(json: String?): MutableSet<Int> {
    if (json.isNullOrBlank()) return mutableSetOf()
    return runCatching {
        val arr = JSONArray(json)
        val out = mutableSetOf<Int>()
        for (i in 0 until arr.length()) out.add(arr.getInt(i))
        out
    }.getOrElse { mutableSetOf() }
}

private fun parseCounterFromDisplayName(displayName: String): Int? {
    val base = displayName.substringBeforeLast('.', displayName)
    val token = base.substringAfterLast('_', missingDelimiterValue = "").trim()
    val v = token.toIntOrNull() ?: return null
    return if (v >= 0) v else null
}

suspend fun scanUsedCountersFromMediaStore(
    context: Context,
    relativePathPrefix: String
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
                parseCounterFromDisplayName(name)?.let(out::add)
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
            parseCounterFromDisplayName(name)?.let(out::add)
        }
    }
    return out
}

fun decodeCounterSet(json: String?): MutableSet<Int> = decodeCounterSetJson(json)

fun encodeCounterSet(values: Set<Int>): String = encodeCounterSetJson(values)
