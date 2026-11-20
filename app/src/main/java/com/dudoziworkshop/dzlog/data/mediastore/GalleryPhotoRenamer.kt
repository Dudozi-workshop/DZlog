package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoRenamePlan
import java.util.Locale

data class GalleryPhotoRenameResult(
    val renamedIds: Set<Long>,
    val failedIds: Set<Long>,
    val detail: String?,
)

/** Rename via MediaStore; verify actual results and never touch file bytes. */
class GalleryPhotoRenamer(private val resolver: ContentResolver) {
    fun rename(plan: GalleryPhotoRenamePlan): GalleryPhotoRenameResult {
        val items = plan.items.distinctBy { it.id }
        val allIds = items.mapTo(mutableSetOf()) { it.id }
        val renamed = mutableSetOf<Long>()
        fun result(detail: String?): GalleryPhotoRenameResult =
            GalleryPhotoRenameResult(renamed.toSet(), allIds - renamed, detail)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return result("이 Android 버전에서는 사진 이름 변경을 지원하지 않습니다.")
        }
        val pending = mutableListOf<com.dudoziworkshop.dzlog.domain.model.MediaImageItem>()
        try {
            require(items.isNotEmpty() && items.size <= 100)
            for (item in items) {
                val name = requireNotNull(plan.newNames[item.id])
                val actual = metadata(item.uri) ?: return result("파일 일부를 찾지 못했습니다.")
                val path = item.relativePath.trimEnd('/') + "/"
                if (actual.second != path) return result("파일 위치가 변경되어 작업을 중단했습니다.")
                when (actual.first) {
                    name -> renamed += item.id
                    item.displayName -> pending += item
                    else -> return result("파일 이름이 변경되어 작업을 중단했습니다.")
                }
            }
            for (path in pending.map { it.relativePath }.distinct()) {
                val where = MediaStoreQueryPolicy.whereExactRelativePath(path)
                val existing = resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                    where.selection, where.selectionArgs, null,
                )?.use { c ->
                    buildSet { while (c.moveToNext()) add(c.getString(0).orEmpty().lowercase(Locale.ROOT)) }
                } ?: return result("대상 폴더의 파일명을 확인하지 못했습니다.")
                val targetNames = pending.filter { it.relativePath == path }
                    .map { requireNotNull(plan.newNames[it.id]).lowercase(Locale.ROOT) }
                if (targetNames.distinct().size != targetNames.size || targetNames.any { it in existing }) {
                    return result("같은 이름의 파일이 있어 덮어쓰기 없이 중단했습니다.")
                }
            }
        } catch (e: SecurityException) {
            return result("사진 수정 권한이 없습니다.")
        } catch (e: Exception) {
            return result(e.message ?: "사진 이름 변경을 준비하지 못했습니다.")
        }
        var failure: String? = null
        for (item in pending) {
            val name = requireNotNull(plan.newNames[item.id])
            try {
                val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, name) }
                if (resolver.update(item.uri, values, null, null) > 0 &&
                    metadata(item.uri) == (name to (item.relativePath.trimEnd('/') + "/"))) {
                    renamed += item.id
                } else {
                    failure = "일부 파일의 변경 결과를 확인하지 못했습니다."
                }
            } catch (e: SecurityException) {
                failure = "일부 사진에 대한 수정 권한이 없습니다."
            } catch (e: Exception) {
                failure = "일부 사진의 이름 변경에 실패했습니다."
            }
        }
        return result(failure)
    }

    private fun metadata(uri: Uri): Pair<String, String>? = resolver.query(
        uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.RELATIVE_PATH),
        null, null, null,
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val name = cursor.getString(0) ?: return@use null
        val path = cursor.getString(1) ?: return@use null
        name to (path.trimEnd('/') + "/")
    }
}
