package com.dudoziworkshop.dzlog.domain.model

import android.net.Uri

data class LogEntry(
    val mediaStoreId: Long,
    val contentUri: Uri,
    val savedContentUris: List<Uri>,
    val displayName: String,
    val isNameAdjusted: Boolean,
    val createdAt: Long,
    val group1: String,
    val group2: String
)
