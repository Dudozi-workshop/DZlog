package com.example.dzlog.ui.log

private const val DEFAULT_GROUP = "(기본)"

/**
 * relativePath 예: "Pictures/DZlog/G1/G2/"
 * - G1/G2가 없으면 "(기본)"으로 채움
 */
fun parseG1G2FromRelativePath(relativePath: String): Pair<String, String> {
    val p = relativePath.trim()
    val idx = p.indexOf("DZlog/")
    if (idx < 0) return DEFAULT_GROUP to DEFAULT_GROUP

    val tail = p.substring(idx + "DZlog/".length).trim('/')
    if (tail.isBlank()) return DEFAULT_GROUP to DEFAULT_GROUP

    val parts = tail.split('/').filter { it.isNotBlank() }
    val g1 = parts.getOrNull(0) ?: DEFAULT_GROUP
    val g2 = parts.getOrNull(1) ?: DEFAULT_GROUP
    return g1 to g2
}
