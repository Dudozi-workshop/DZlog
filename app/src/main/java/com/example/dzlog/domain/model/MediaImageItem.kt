package com.example.dzlog.domain.model

import android.net.Uri

/**
 * 앱 내 로그(앨범)에서 사용할 최소 이미지 메타데이터.
 * - MediaStore 기반(갤러리에 보이는 저장소)
 */
data class MediaImageItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val relativePath: String,
    val dateAddedSeconds: Long
)
