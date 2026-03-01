package com.dudoziworkshop.dzlog.feature.log.policy

import android.net.Uri
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.mediastore.MediaStoreQueryPolicy

sealed class AlbumDeleteTarget {
    data class Exact(val rel: String) : AlbumDeleteTarget()
    data class Prefix(val relPrefix: String) : AlbumDeleteTarget()
}

fun buildDeleteTargetsForG1Selection(
    selected: Set<String>,
    rootSelected: Boolean,
): List<AlbumDeleteTarget> {
    val targets = mutableListOf<AlbumDeleteTarget>()
    if (rootSelected) {
        targets += AlbumDeleteTarget.Exact(normalizeRel("Pictures/DZlog/"))
        targets += AlbumDeleteTarget.Exact(normalizeRel("Pictures/DZlog/original/"))
    }

    selected
        .filter { it.isNotBlank() && it != DzlogMediaStoreReader.ROOT_G1 }
        .forEach { g1 ->
            targets += AlbumDeleteTarget.Prefix(normalizeRel("Pictures/DZlog/$g1/"))
        }
    return targets.distinct()
}

fun buildDeleteTargetsForG2Selection(
    g1: String,
    selected: Set<String>,
    groupRootSelected: Boolean,
): List<AlbumDeleteTarget> {
    val safeG1 = g1.trim()
    if (safeG1.isBlank()) return emptyList()

    val targets = mutableListOf<AlbumDeleteTarget>()
    if (groupRootSelected) {
        targets += AlbumDeleteTarget.Exact(normalizeRel("Pictures/DZlog/$safeG1/"))
        targets += AlbumDeleteTarget.Exact(normalizeRel("Pictures/DZlog/$safeG1/original/"))
    }

    selected
        .filter { it.isNotBlank() && it != DzlogMediaStoreReader.GROUP_ROOT_LABEL }
        .forEach { g2 ->
            targets += AlbumDeleteTarget.Exact(normalizeRel("Pictures/DZlog/$safeG1/$g2/"))
            targets += AlbumDeleteTarget.Exact(normalizeRel("Pictures/DZlog/$safeG1/$g2/original/"))
        }
    return targets.distinct()
}

fun collectDeleteUris(
    reader: DzlogMediaStoreReader,
    targets: List<AlbumDeleteTarget>,
): List<Uri> {
    return targets
        .flatMap { target ->
            when (target) {
                is AlbumDeleteTarget.Exact -> reader.loadImages(target.rel)
                is AlbumDeleteTarget.Prefix -> reader.loadImagesUnderPrefix(target.relPrefix)
            }
        }
        .map { it.uri }
        .distinct()
}

private fun normalizeRel(path: String): String = MediaStoreQueryPolicy.normalizeRelativePath(path)
