package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GalleryFolderRenameTest {
    private val root = GalleryFolderIndexPolicy.ROOT
    private val source = root + "A/source/"
    private val folders = listOf(root, root + "A/", source, root + "A/taken/")

    @Test
    fun previewUsesParentAndAllowsTheExistingName() {
        assertEquals(root + "A/new/", GalleryFolderOperationPolicy.checkedRenamedPath(source, " new ", folders))
        assertEquals(source, GalleryFolderOperationPolicy.checkedRenamedPath(source, "source", folders))
        assertEquals(root + "A/new/", GalleryFolderOperationPolicy.checkedRenamedPath(source, "new", folders + (root + "new/")))
    }

    @Test
    fun collisionsAndInvalidNamesAreRejectedAtPreviewAndCommit() {
        for (name in listOf("TAKEN", "SOURCE", "original", "", "a/b", "a.", "a\u007fb")) {
            assertFailsWith<IllegalArgumentException> { GalleryFolderOperationPolicy.checkedRenamedPath(source, name, folders) }
        }
        for (changed in listOf(folders - source, folders + (root + "A/New/"))) {
            assertFailsWith<IllegalArgumentException> { GalleryFolderOperationPolicy.checkedRenamedPath(source, "new", changed) }
        }
    }

    @Test
    fun storedManualAndDynamicSlotsReportDifferentCaptureImpacts() {
        fun manual(text: String) = TableEditorSlotDraft("MANUAL", "직접 입력", manualText = text)
        val dynamic = TableEditorSlotDraft("DATE", "날짜")
        val matching = listOf(manual("A"), manual("source"), manual("day"))
        val possible = listOf(manual("A"), dynamic)
        val separate = listOf(manual("AB"), dynamic)
        fun impact(vararg drafts: List<TableEditorSlotDraft>) = GalleryFolderOperationPolicy.captureImpact(source, drafts.toList())
        assertEquals(CapturePathImpact.NONE, impact(separate))
        assertEquals(CapturePathImpact.POSSIBLE, impact(possible, separate))
        assertEquals(CapturePathImpact.MATCH, impact(possible, matching))
    }
}
