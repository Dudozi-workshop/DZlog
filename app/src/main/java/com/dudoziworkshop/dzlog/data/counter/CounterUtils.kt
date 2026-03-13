package com.dudoziworkshop.dzlog.data.counter

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.dudoziworkshop.dzlog.data.mediastore.MediaStoreQueryPolicy

// 0 = no padding (e.g., _1, _10, _5021)
const val COUNTER_DIGITS_DEFAULT = 0

fun clampCounterDigits(v: Int) = v.coerceIn(0, 6)

private fun parseCounter(
    displayName: String,
    fileNamePrefix: String,
    counterDigits: Int,
    fnDelim: String
): Int? = parseCounterForPolicy(displayName, fileNamePrefix, counterDigits, fnDelim)


internal fun parseCounterForPolicy(
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

fun scanUsedCounters(
    context: Context,
    relativePathPrefix: String,
    fileNamePrefix: String,
    counterDigits: Int,
    fnDelim: String
): Set<Int> {
    val out = mutableSetOf<Int>()
    val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    Log.d(
        "CounterReadback",
        "scanUsedCountersFromMediaStore start relativePathPrefix=$relativePathPrefix, fileNamePrefix=$fileNamePrefix, counterDigits=$counterDigits, fnDelim=$fnDelim"
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        val isWildcardPath = relativePathPrefix == "*" || relativePathPrefix.isBlank()
        // 주요 정책: RELATIVE_PATH는 단말/버전에 따라 trailing slash 유무가 달라질 수 있다.
        // 따라서 exact path 매칭은 with-slash/without-slash variant를 모두 포함해 수행한다.
        val where = if (isWildcardPath) {
            null
        } else {
            MediaStoreQueryPolicy.whereExactRelativePath(relativePathPrefix)
        }
        val selection = where?.selection
        val selectionArgs = where?.selectionArgs

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val pathIdx = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val normalizedPathVariants = if (isWildcardPath) {
                null
            } else {
                MediaStoreQueryPolicy.normalizeRelativePathVariants(relativePathPrefix).toList().toSet()
            }
            var sampleCount = 0
            while (cursor.moveToNext()) {
                val rel = if (pathIdx >= 0) cursor.getString(pathIdx) else ""
                if (!isWildcardPath && rel !in normalizedPathVariants.orEmpty()) continue
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                if (sampleCount < 5) {
                    Log.d(
                        "CounterReadback",
                        "scanUsedCountersFromMediaStore sample rel=$rel, name=$name"
                    )
                    sampleCount += 1
                }
                parseCounter(name, fileNamePrefix, counterDigits, fnDelim)?.let(out::add)
            }
        }
        Log.d(
            "CounterReadback",
            "scanUsedCountersFromMediaStore end relativePathPrefix=$relativePathPrefix, parsedCounters=$out"
        )
        return out
    }

    @Suppress("DEPRECATION")
    val dataCol = MediaStore.Images.Media.DATA
    val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME, dataCol)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val dataIdx = cursor.getColumnIndex(dataCol)
        var sampleCount = 0
        while (cursor.moveToNext()) {
            val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
            val abs = if (dataIdx >= 0) cursor.getString(dataIdx) else ""
            if (!(relativePathPrefix == "*" || relativePathPrefix.isBlank()) && !abs.contains("/$relativePathPrefix")) continue
            if (sampleCount < 5) {
                Log.d(
                    "CounterReadback",
                    "scanUsedCountersFromMediaStore sample preQ abs=$abs, name=$name"
                )
                sampleCount += 1
            }
            parseCounter(name, fileNamePrefix, counterDigits, fnDelim)?.let(out::add)
        }
    }
    Log.d(
        "CounterReadback",
        "scanUsedCountersFromMediaStore end preQ relativePathPrefix=$relativePathPrefix, parsedCounters=$out"
    )
    return out
}
