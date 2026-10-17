package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import org.junit.Assert.*
import org.junit.Test

class CapturePhraseUndoHistoryTest {
    private val set = RotatingPhraseSet("set", "Treatment", listOf("A", "B"), defaultEvery = 12)
    private fun template(index: Int = 0, shots: Int = 0) = TableTemplateState(
        rows = 1, cols = 1,
        cells = listOf(TableCellState(0, 0, cellId = "cell",
            dataType = TableCellDataType.ROTATING_TEXT, phraseSetId = set.id,
            phraseIndex = index, phraseCompletedShots = shots)),
        phraseSets = listOf(set),
    )

    @Test fun `undo last of twelve restores previous phrase`() {
        val journal = CapturePhraseUndoHistory()
        val before = template(shots = 11)
        val after = template(index = 1)
        journal.record("template-1", listOf("uri1"), before, after)
        val entry = requireNotNull(journal.deleted(listOf("uri1")))
        assertEquals(before.cells[0].phraseCompletedShots, journal.restore(after, entry)?.cells?.get(0)?.phraseCompletedShots)
        assertEquals(before.cells[0].phraseIndex, journal.restore(after, entry)?.cells?.get(0)?.phraseIndex)
        journal.acknowledgeRestored(entry)
    }

    @Test fun `multiple output capture requires full delete`() {
        val journal = CapturePhraseUndoHistory()
        journal.record("template-1", listOf("watermark", "original"), template(shots = 10), template(shots = 11))
        assertNull(journal.deleted(listOf("watermark")))
        assertTrue(journal.hasIncompleteDeletion())
        val entry = requireNotNull(journal.deleted(listOf("original")))
        assertFalse(journal.hasIncompleteDeletion())
        assertEquals(10, journal.restore(template(shots = 11), entry)?.cells?.single()?.phraseCompletedShots)
    }

    @Test fun `stale progress never overwrites newer capture`() {
        val journal = CapturePhraseUndoHistory()
        journal.record("template-1", listOf("uri1"), template(shots = 10), template(shots = 11))
        val entry = requireNotNull(journal.deleted(listOf("uri1")))
        assertNull(journal.restore(template(index = 1, shots = 0), entry))
        assertFalse(journal.canUndo(listOf("another"), "different-template") == false)
        assertFalse(journal.canUndo(listOf("uri1"), "template-1") == false)
    }

    @Test fun `different template must not undo tracked capture`() {
        val journal = CapturePhraseUndoHistory()
        journal.record("template-1", listOf("uri1"), template(shots = 10), template(shots = 11))
        assertFalse(journal.canUndo(listOf("uri1"), "template-2"))
        assertTrue(journal.canUndo(listOf("uri1"), "template-1"))
    }

    @Test fun `gallery mutation blocks old camera undo without blocking a new shot`() {
        val journal = CapturePhraseUndoHistory()
        journal.record("template-1", listOf("result-1", "original-1"), template(), template(shots = 1))
        journal.invalidateUndoForExistingCaptures(listOf("result-1", "original-1"))
        assertFalse(journal.canUndo(listOf("result-1", "original-1"), "template-1"))
        // Gallery changes never call deleted(), so phrase progress is untouched.
        assertFalse(journal.hasIncompleteDeletion())
        journal.record("template-1", listOf("result-2"), template(shots = 1), template(shots = 2))
        assertTrue(journal.canUndo(listOf("result-2"), "template-1"))
        assertFalse(journal.canUndo(listOf("original-1"), "template-1"))
        journal.clear()
        assertTrue(journal.canUndo(listOf("original-1"), "template-1"))
    }

    @Test fun `undo path stays associated with the exact shot`() {
        val journal = CapturePhraseUndoHistory()
        journal.record("template-1", listOf("shot-1-result", "shot-1-original"),
            template(), template(shots = 1), captureRelativePath = "Pictures/DZlog/FolderA/")
        journal.record("template-1", listOf("shot-2"),
            template(shots = 1), template(shots = 2), captureRelativePath = "Pictures/DZlog/FolderB/")
        assertEquals("Pictures/DZlog/FolderA/",
            journal.pathForUndo(listOf("shot-1-result", "shot-1-original")))
        assertEquals("Pictures/DZlog/FolderB/", journal.pathForUndo(listOf("shot-2")))
        assertNull(journal.pathForUndo(listOf("untracked")))
    }

    @Test fun `canceling confirmation does not consume undo state`() {
        val journal = CapturePhraseUndoHistory()
        journal.record("template-1", listOf("shot-1"),
            template(), template(shots = 1), captureRelativePath = "Pictures/DZlog/FolderA/")
        assertEquals("Pictures/DZlog/FolderA/", journal.pathForUndo(listOf("shot-1")))
        assertTrue(journal.canUndo(listOf("shot-1"), "template-1"))
        assertFalse(journal.hasIncompleteDeletion())
        // Dismissing a warning does not call deleted() or acknowledgeRestored().
        assertTrue(journal.canUndo(listOf("shot-1"), "template-1"))
    }

    @Test fun `editing another capture leaves latest untouched`() {
        val journal = CapturePhraseUndoHistory()
        journal.record("template-1", listOf("older"), template(), template(shots = 1))
        journal.record("template-1", listOf("newer"), template(shots = 1), template(shots = 2))
        journal.invalidateUndoForExistingCaptures(listOf("older"))
        assertTrue(journal.canUndo(listOf("newer"), "template-1"))
        assertFalse(journal.canUndo(listOf("older"), "template-1"))
    }
}
