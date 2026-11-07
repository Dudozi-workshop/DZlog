package com.dudoziworkshop.dzlog.data.project

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dudoziworkshop.dzlog.domain.project.CaptureRecord

/**
 * Project membership is metadata, not a folder. The nullable project ID also
 * represents ordinary shooting; file references are never rewritten by a switch.
 */
@Entity(
    tableName = "capture_records",
    indices = [
        Index(value = ["projectId", "capturedAtMillis"]),
        Index(value = ["originalUri"], unique = true),
        Index(value = ["resultUri"], unique = true),
    ],
)
internal data class CaptureRecordEntity(
    @PrimaryKey val captureId: String,
    val projectId: String?,
    val templateId: String?,
    val capturedAtMillis: Long,
    val originalUri: String?,
    val resultUri: String?,
    val originalRelativePath: String?,
    val resultRelativePath: String?,
) {
    fun toDomain(): CaptureRecord = CaptureRecord(
        captureId = captureId,
        projectId = projectId,
        templateId = templateId,
        capturedAtMillis = capturedAtMillis,
        originalUri = originalUri,
        resultUri = resultUri,
        originalRelativePath = originalRelativePath,
        resultRelativePath = resultRelativePath,
    )

    companion object {
        fun from(record: CaptureRecord): CaptureRecordEntity = CaptureRecordEntity(
            captureId = record.captureId,
            projectId = record.projectId,
            templateId = record.templateId,
            capturedAtMillis = record.capturedAtMillis,
            originalUri = record.originalUri,
            resultUri = record.resultUri,
            originalRelativePath = record.originalRelativePath,
            resultRelativePath = record.resultRelativePath,
        )
    }
}
