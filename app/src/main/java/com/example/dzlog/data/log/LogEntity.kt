package com.example.dzlog.data.log

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val createdAt: Long,
    val imageUri: String,
    val fileName: String,
    val relativePath: String? = null,
    val templateId: String? = null,
    val templateName: String? = null,
    val valuesJson: String? = null
)
