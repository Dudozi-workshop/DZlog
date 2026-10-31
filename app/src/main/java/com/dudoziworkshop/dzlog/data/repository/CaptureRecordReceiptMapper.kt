package com.dudoziworkshop.dzlog.data.repository

import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.project.CaptureRecord
import com.dudoziworkshop.dzlog.domain.project.CaptureReservation

/**
 * Build a single logical capture from the durable shutter-time snapshot.
 * Save order is defined by CaptureMediaSaveCoordinator:
 * BOTH = [watermarked result, original], other modes = [their one file].
 * Never resolve project membership from the current UI selection here.
 */
internal object CaptureRecordReceiptMapper {
    fun fromCompleted(receipt: CapturePendingStore.PendingCapture): CaptureRecord {
        require(receipt.status == CapturePendingStore.Status.COMMITTED) {
            "Only fully committed receipts may be indexed"
        }
        return fromSavedOutputs(
            captureId = receipt.reservation.id,
            projectId = receipt.snapshot.projectId,
            templateId = receipt.snapshot.templateId,
            capturedAtMillis = receipt.reservation.createdAtMillis,
            saveMode = receipt.snapshot.request.saveMode,
            relativePath = receipt.snapshot.request.relativePath,
            savedUris = receipt.outputUris,
        )
    }

    fun fromSavedOutputs(
        captureId: String,
        projectId: String?,
        templateId: String?,
        capturedAtMillis: Long,
        saveMode: SaveMode,
        relativePath: String,
        savedUris: List<String>,
    ): CaptureRecord {
        val expected = if (saveMode == SaveMode.BOTH) 2 else 1
        require(savedUris.size == expected &&
            savedUris.all(String::isNotBlank) &&
            savedUris.toSet().size == expected) {
            "Incomplete or duplicate capture output URIs"
        }
        val basePath = normalizeCaptureBaseRelativePath(relativePath)
        val originalPath = appendOriginalCaptureDirectory(basePath)
        val reservation = CaptureReservation(
            captureId = captureId,
            projectId = projectId,
            templateId = templateId,
            capturedAtMillis = capturedAtMillis,
        )
        return requireNotNull(when (saveMode) {
            SaveMode.ORIGINAL_ONLY -> reservation.committed(
                originalUri = savedUris[0],
                originalRelativePath = originalPath,
            )
            SaveMode.WATERMARK_ONLY -> reservation.committed(
                resultUri = savedUris[0],
                resultRelativePath = basePath,
            )
            SaveMode.BOTH -> reservation.committed(
                resultUri = savedUris[0],
                originalUri = savedUris[1],
                resultRelativePath = basePath,
                originalRelativePath = originalPath,
            )
        })
    }
}
