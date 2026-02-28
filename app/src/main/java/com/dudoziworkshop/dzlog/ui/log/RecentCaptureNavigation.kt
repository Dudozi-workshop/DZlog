package com.dudoziworkshop.dzlog.ui.log

import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader

const val ORIGINAL_PHOTOS_TITLE = "원본사진"

/**
 * relativePath 예: "Pictures/DZlog/G1/G2/"
 *
 * 정책:
 * - G1이 없으면 ROOT("DZlog"), G2가 없으면 빈 문자열로 채움
 * - 원본 하위 경로는 G1/G2 이름으로 취급하지 않고 "원본사진"으로 태깅
 */
fun parseG1G2FromRelativePath(relativePath: String): Pair<String, String> {
    val p = relativePath.trim()
    val idx = p.indexOf("DZlog/")
    if (idx < 0) return DzlogMediaStoreReader.ROOT_G1 to ""

    val tail = p.substring(idx + "DZlog/".length).trim('/')
    if (tail.isBlank()) return DzlogMediaStoreReader.ROOT_G1 to ""

    val parts = tail.split('/').filter { it.isNotBlank() }

    if (parts.size == 1 && parts[0] == "original") {
        return DzlogMediaStoreReader.ROOT_G1 to ORIGINAL_PHOTOS_TITLE
    }

    if (parts.size >= 2 && parts[1] == "original") {
        return (parts[0].ifBlank { DzlogMediaStoreReader.ROOT_G1 }) to ORIGINAL_PHOTOS_TITLE
    }

    val g1 = parts.getOrNull(0) ?: DzlogMediaStoreReader.ROOT_G1
    val g2 = parts.getOrNull(1).orEmpty()
    return g1 to g2
}
