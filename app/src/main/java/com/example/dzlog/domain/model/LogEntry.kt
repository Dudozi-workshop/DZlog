package com.example.dzlog.domain.model

import android.net.Uri

data class LogEntry(
    val mediaStoreId: Long,
    val contentUri: Uri,
    val createdAt: Long,
    val projectKey: String,
    val group1: String,
    val group2: String,
)
