package com.dudoziworkshop.dzlog.domain.model

import android.net.Uri

/**
 * 로그(G1/G2) 목록 화면에서 카드로 표시할 요약 정보.
 */
data class LogGroupSummary(
    val name: String,
    val photoCount: Int,
    val latestDateAddedSeconds: Long,
    val latestContentUri: Uri?
)
