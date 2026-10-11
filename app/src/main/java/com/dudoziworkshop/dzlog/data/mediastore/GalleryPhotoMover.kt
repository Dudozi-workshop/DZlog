package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePlan
import java.util.Locale

data class GalleryPhotoMoveResult(
    val moved: Int,
    val failed: Int,
    val detail: String?,
    val movedIds: Set<Long> = emptySet(),
    val failedIds: Set<Long> = emptySet(),
)

/** Update paths without copying or deleting files; verify each resulting path and filename. */
class GalleryPhotoMover(private val resolver: ContentResolver) {
    fun move(plan: GalleryPhotoMovePlan): GalleryPhotoMoveResult {
        val items = plan.items.distinctBy { it.id }
        val allIds = items.mapTo(mutableSetOf()) { it.id }
        val movedIds = mutableSetOf<Long>()
        fun result(detail: String?): GalleryPhotoMoveResult {
            val failedIds = allIds - movedIds
            return GalleryPhotoMoveResult(movedIds.size, failedIds.size,
                detail.takeIf { failedIds.isNotEmpty() }, movedIds.toSet(), failedIds)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return result("이 Android 버전에서는 사진 이동을 지원하지 않습니다.")
        }
        val pending = mutableListOf<com.dudoziworkshop.dzlog.domain.model.MediaImageItem>()
        try {
            require(items.isNotEmpty() && items.size <= 100)
            for (photo in items) {
                val target = requireNotNull(plan.destinations[photo.id]).trimEnd('/') + "/"
                require(target.startsWith(GalleryFolderIndexPolicy.ROOT) &&
                    photo.relativePath.startsWith(GalleryFolderIndexPolicy.ROOT))
                val actual = metadata(photo.uri) ?: return result("선택한 사진 일부를 찾지 못했습니다.")
                if (actual.first != photo.displayName) return result("선택한 파일명이 변경됐습니다. 갤러리에서 다시 확인해 주세요.")
                when (actual.second) {
                    target -> movedIds += photo.id // A verified previous write must not be repeated on retry.
                    photo.relativePath.trimEnd('/') + "/" -> pending += photo
                    else -> return result("선택한 사진의 위치가 변경됐습니다. 갤러리에서 다시 확인해 주세요.")
                }
            }
            val duplicateNames = pending.groupBy {
                (plan.destinations[it.id]?.trimEnd('/') + "/") to it.displayName.lowercase(Locale.ROOT)
            }.values.any { it.size > 1 }
            if (duplicateNames) return result("이동할 파일명이 대상 위치에서 겹칩니다.")
            // Inspect every destination before performing any write.
            val existing = mutableMapOf<String, Set<String>>()
            for (path in pending.map { requireNotNull(plan.destinations[it.id]) }.distinct()) {
                val where = MediaStoreQueryPolicy.whereExactRelativePath(path)
                val names: Set<String> = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Images.Media.DISPLAY_NAME), where.selection, where.selectionArgs, null
                )?.use { cursor ->
                    buildSet<String> { while (cursor.moveToNext()) add(cursor.getString(0).orEmpty().lowercase(Locale.ROOT)) }
                } ?: return result("대상 폴더 확인에 실패했습니다.")
                existing[path] = names
            }
            if (pending.any { it.displayName.lowercase(Locale.ROOT) in existing[plan.destinations[it.id]].orEmpty() }) {
                return result("대상 폴더에 같은 이름의 사진이 있습니다. 파일을 덮어쓰지 않았습니다.")
            }
        } catch (e: SecurityException) {
            return result("사진을 확인할 권한이 없습니다. 이동하지 못한 파일을 확인해 주세요.")
        } catch (e: Exception) {
            return result("사진 또는 대상 폴더를 확인하지 못했습니다.")
        }
        var failure: String? = null
        for (photo in pending) {
            val target = requireNotNull(plan.destinations[photo.id]).trimEnd('/') + "/"
            try {
                val values = ContentValues().apply { put(MediaStore.Images.Media.RELATIVE_PATH, target) }
                if (resolver.update(photo.uri, values, null, null) <= 0) {
                    failure = "일부 사진을 이동하지 못했습니다."
                } else if (metadata(photo.uri) == (photo.displayName to target)) {
                    movedIds += photo.id
                } else {
                    failure = "일부 파일의 변경 결과를 확인하지 못했습니다. 갤러리에서 실제 경로와 파일명을 확인해 주세요."
                }
            } catch (e: SecurityException) {
                failure = "사진 수정 권한이 없어 일부 이동하지 못했습니다."
            } catch (e: Exception) {
                failure = "일부 사진의 이동 결과를 확인하지 못했습니다."
            }
        }
        return result(failure)
    }

    private fun metadata(uri: Uri): Pair<String, String>? = resolver.query(uri,
        arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.RELATIVE_PATH), null, null, null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val name = cursor.getString(0) ?: return@use null
        val path = cursor.getString(1) ?: return@use null
        name to (path.trimEnd('/') + "/")
    }
}
