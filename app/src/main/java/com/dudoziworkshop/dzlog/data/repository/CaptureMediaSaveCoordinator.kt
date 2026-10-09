package com.dudoziworkshop.dzlog.data.repository

import android.net.Uri
import com.dudoziworkshop.dzlog.data.mediastore.SavedMedia
import com.dudoziworkshop.dzlog.domain.model.SaveMode

internal data class CaptureMediaSaveResult(
    val primary: SavedMedia,
    val files: List<SavedMedia>,
)

/** Completion means every requested image was saved; partial captures are compensated. */
internal object CaptureMediaSaveCoordinator {
    fun save(
        mode: SaveMode,
        saveWatermark: () -> SavedMedia,
        saveOriginal: () -> SavedMedia,
        deleteSaved: (Uri) -> Boolean,
    ): CaptureMediaSaveResult {
        val saved = mutableListOf<SavedMedia>()
        fun saveOne(action: () -> SavedMedia): SavedMedia {
            val media = action()
            if (media.uri != Uri.EMPTY) saved += media
            check(media.uri != Uri.EMPTY && media.mediaStoreId > 0L) { "저장된 사진을 확인할 수 없습니다." }
            return media
        }
        try {
            val primary = when (mode) {
                SaveMode.WATERMARK_ONLY -> saveOne(saveWatermark)
                SaveMode.ORIGINAL_ONLY -> saveOne(saveOriginal)
                SaveMode.BOTH -> saveOne(saveWatermark).also { saveOne(saveOriginal) }
            }
            return CaptureMediaSaveResult(primary, saved.toList())
        } catch (error: Exception) {
            saved.asReversed().forEach { media ->
                try {
                    check(deleteSaved(media.uri)) { "부분 저장 사진 삭제 실패: ${media.uri}" }
                } catch (cleanupError: Exception) {
                    error.addSuppressed(cleanupError)
                }
            }
            throw error
        }
    }
}
