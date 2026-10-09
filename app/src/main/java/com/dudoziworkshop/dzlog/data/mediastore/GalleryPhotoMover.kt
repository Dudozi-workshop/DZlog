package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePlan

data class GalleryPhotoMoveResult(val moved: Int, val failed: Int, val detail: String?)

/**
 * MediaStore RELATIVE_PATH updates are only possible for media owned by this
 * installation, or for media the platform explicitly permits us to modify.
 * Never copy-then-delete: that risks data loss on interrupted operations.
 */
class GalleryPhotoMover(private val resolver: ContentResolver) {
    fun move(plan: GalleryPhotoMovePlan): GalleryPhotoMoveResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return GalleryPhotoMoveResult(0, plan.items.size, "이 Android 버전에서는 사진 이동을 지원하지 않습니다.")
        }
        // Preflight every destination before performing a single write.
        val existing = mutableMapOf<String, MutableSet<String>>()
        for (path in plan.destinations.values.distinct()) {
            val where = MediaStoreQueryPolicy.whereExactRelativePath(path)
            val names = mutableSetOf<String>()
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                where.selection, where.selectionArgs, null,
            )?.use { cursor ->
                while (cursor.moveToNext()) names += cursor.getString(0).orEmpty()
            } ?: return GalleryPhotoMoveResult(0, plan.items.size, "대상 폴더 확인에 실패했습니다.")
            existing[path] = names
        }
        val collision = plan.items.firstOrNull { item ->
            item.displayName in existing[plan.destinations[item.id]].orEmpty()
        }
        if (collision != null) {
            return GalleryPhotoMoveResult(0, plan.items.size, "대상 폴더에 같은 이름의 사진이 있습니다.")
        }
        var moved = 0
        var failed = 0
        var failure: String? = null
        for (photo in plan.items) {
            val target = requireNotNull(plan.destinations[photo.id])
            try {
                val values = ContentValues().apply { put(MediaStore.Images.Media.RELATIVE_PATH, target) }
                if (resolver.update(photo.uri, values, null, null) > 0) moved++ else failed++
            } catch (e: SecurityException) {
                failed++
                failure = "사진 수정 권한이 없어 일부 이동하지 못했습니다."
            } catch (e: Exception) {
                failed++
                failure = "일부 사진을 이동하지 못했습니다."
            }
        }
        return GalleryPhotoMoveResult(moved, failed, failure)
    }
}
