package com.dudoziworkshop.dzlog.ui.log

private const val DZLOG_ROOT = "Pictures/DZlog/"

/**
 * Display a title for a folder at any depth, without assuming G1/G2 slots.
 * Original-photo folders display the parent folder title; the grid supplies
 * a separate original-photo badge.
 */
fun folderTitleFromRelativePath(relativePath: String): String {
    val normalized = relativePath.trim().replace('\\', '/').trimEnd('/')
    if (normalized == DZLOG_ROOT.trimEnd('/')) return "DZlog"
    if (!normalized.startsWith(DZLOG_ROOT)) return "DZlog"
    val segments = normalized.removePrefix(DZLOG_ROOT).split('/').filter { it.isNotBlank() }
    val folders = if (segments.lastOrNull() == "original") segments.dropLast(1) else segments
    return folders.takeIf { it.isNotEmpty() }?.joinToString(" / ") ?: "DZlog"
}

fun isOriginalRelativePath(relativePath: String): Boolean {
    // The original-photo directory is recognized independent of folder depth.
    val normalized = relativePath.trim().let {
        if (it.endsWith('/')) it else "$it/"
    }
    return normalized.endsWith("/original/")
}
