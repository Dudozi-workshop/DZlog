package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy

data class GalleryPhotoDeletePlan(val items: List<MediaImageItem>) {
    val originalCount: Int get() = items.count { GalleryPhotoMovePolicy.isOriginalPath(it.relativePath) }
    val resultCount: Int get() = items.size - originalCount
}

data class GalleryPhotoDeleteResult(
    val deletedIds: Set<Long>,
    val failedIds: Set<Long>,
    val detail: String?,
)

/** Shared planning and verification for gallery and viewer. No deletion or permission request here. */
class GalleryPhotoDeletion(private val resolver: ContentResolver) {
    companion object {
        fun prepare(
            selected: List<MediaImageItem>,
            allPhotos: List<MediaImageItem>,
            includeOriginals: Boolean,
        ): GalleryPhotoDeletePlan {
            require(selected.isNotEmpty()) { "삭제할 사진을 선택해 주세요." }
            val snapshot = allPhotos.groupBy { it.id }
            require(selected.all { item ->
                snapshot[item.id]?.singleOrNull()?.let {
                    it.uri == item.uri && it.displayName == item.displayName && it.relativePath == item.relativePath
                } == true
            }) { "사진 목록이 변경됐습니다. 목록을 새로 확인해 주세요." }
            val originals = if (includeOriginals) GalleryPhotoMovePolicy.pairedOriginals(selected, allPhotos) else emptyList()
            return GalleryPhotoDeletePlan((selected + originals).distinctBy { it.id }).also(::validate)
        }

        private fun validate(plan: GalleryPhotoDeletePlan) {
            require(plan.items.isNotEmpty() && plan.items.size <= 100) { "한 번에 1~100개의 파일을 삭제할 수 있습니다." }
            require(plan.items.map { it.id }.distinct().size == plan.items.size) { "삭제 대상 파일이 중복됐습니다." }
            require(plan.items.all { item ->
                item.relativePath.startsWith(GalleryFolderIndexPolicy.ROOT) &&
                    item.uri.scheme == "content" && item.uri.authority == "media" &&
                    item.uri.pathSegments.dropLast(1).takeLast(2) == listOf("images", "media") &&
                    runCatching { ContentUris.parseId(item.uri) == item.id && item.id >= 0 }.getOrDefault(false)
            }) { "DZlog 내부의 MediaStore 사진만 삭제할 수 있습니다." }
        }
    }

    /** Read every item before requesting permission. A changed or unreadable item blocks the batch. */
    fun pendingItems(plan: GalleryPhotoDeletePlan): List<MediaImageItem> {
        validate(plan)
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { "이 Android 버전의 삭제 경로 검증은 아직 지원하지 않습니다." }
        return plan.items.filter { item ->
            val cursor = resolver.query(item.uri, arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.RELATIVE_PATH,
            ), null, null, null) ?: error("삭제 대상 사진을 조회하지 못했습니다.")
            cursor.use {
                if (!it.moveToFirst()) false
                else {
                    check(it.getString(0) == item.displayName &&
                        it.getString(1)?.trimEnd('/') == item.relativePath.trimEnd('/')) {
                        "삭제 대상 파일의 이름 또는 위치가 변경됐습니다. 목록을 새로 확인해 주세요."
                    }
                    true
                }
            }
        }
    }

    /** Only an empty, successful MediaStore query is absent. Null cursors/errors remain retry targets. */
    fun verifyResult(plan: GalleryPhotoDeletePlan): GalleryPhotoDeleteResult {
        validate(plan)
        val deleted = mutableSetOf<Long>()
        var firstError: String? = null
        plan.items.forEach { item ->
            runCatching {
                val cursor = resolver.query(item.uri, arrayOf(MediaStore.Images.Media._ID), null, null, null)
                    ?: error("삭제 결과를 조회하지 못했습니다.")
                cursor.use { !it.moveToFirst() }
            }.onSuccess { absent -> if (absent) deleted += item.id }
                .onFailure { if (firstError == null) firstError = it.message ?: "삭제 결과를 확인하지 못했습니다." }
        }
        val failed = plan.items.mapTo(mutableSetOf()) { it.id } - deleted
        return GalleryPhotoDeleteResult(deleted, failed,
            if (failed.isEmpty()) null else firstError ?: "삭제되지 않은 파일 ${failed.size}개가 남아 있습니다.")
    }
}
