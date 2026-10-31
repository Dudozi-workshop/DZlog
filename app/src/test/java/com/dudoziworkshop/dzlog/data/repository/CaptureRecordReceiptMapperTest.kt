package com.dudoziworkshop.dzlog.data.repository

import com.dudoziworkshop.dzlog.domain.model.SaveMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CaptureRecordReceiptMapperTest {
    private fun saved(mode: SaveMode, uris: List<String>, project: String? = "A") =
        CaptureRecordReceiptMapper.fromSavedOutputs(
            captureId = "capture-1",
            projectId = project,
            templateId = "T1",
            capturedAtMillis = 1234L,
            saveMode = mode,
            relativePath = "Pictures/DZlog/T1",
            savedUris = uris,
        )

    @Test fun bothModeKeepsOriginalAndResultInOneRecord() {
        val record = saved(
            SaveMode.BOTH,
            listOf("content://result", "content://original"),
        )
        assertEquals("capture-1", record.captureId)
        assertEquals("A", record.projectId)
        assertEquals("T1", record.templateId)
        assertEquals(1234L, record.capturedAtMillis)
        assertEquals("content://result", record.resultUri)
        assertEquals("content://original", record.originalUri)
        assertEquals("content://result", record.displayUri)
        assertEquals("Pictures/DZlog/T1/", record.resultRelativePath)
        assertEquals("Pictures/DZlog/T1/original/", record.originalRelativePath)
    }

    @Test fun originalOnlyModeDoesNotMisclassifyTheSavedFile() {
        val record = saved(SaveMode.ORIGINAL_ONLY, listOf("content://original"))
        assertEquals("content://original", record.originalUri)
        assertNull(record.resultUri)
        assertEquals("Pictures/DZlog/T1/original/", record.originalRelativePath)
        assertEquals("content://original", record.displayUri)
    }

    @Test fun resultOnlyModeDoesNotMisclassifyTheSavedFile() {
        val record = saved(SaveMode.WATERMARK_ONLY, listOf("content://result"))
        assertNull(record.originalUri)
        assertEquals("content://result", record.resultUri)
        assertEquals("Pictures/DZlog/T1/", record.resultRelativePath)
    }

    @Test fun noProjectMeansOrdinaryShootingEvenAfterSave() {
        val record = saved(SaveMode.WATERMARK_ONLY, listOf("content://result"), project = null)
        assertNull(record.projectId)
    }

    @Test fun rejectsMissingOrDuplicateMediaUrisInsteadOfInventingCaptures() {
        assertThrows(IllegalArgumentException::class.java) {
            saved(SaveMode.BOTH, listOf("content://result"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            saved(SaveMode.BOTH, listOf("content://same", "content://same"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            saved(SaveMode.ORIGINAL_ONLY, listOf(" "))
        }
    }
}
