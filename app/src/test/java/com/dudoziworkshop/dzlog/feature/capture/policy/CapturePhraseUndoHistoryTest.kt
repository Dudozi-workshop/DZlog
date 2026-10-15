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
        val entry = requireNotNull(journal.deleted(listOf("original")))
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
}
