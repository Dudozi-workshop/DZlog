package com.dudoziworkshop.dzlog.domain.phrase

import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.domain.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RotatingPhrasePerItemCoreTest {
    private val base = RotatingPhraseSet("set", "생육 조사",
        listOf("초기", "중간", "최종", "처리 A", "처리 B"),
        defaultEvery = 5,
        itemEveryOverrides = listOf(null, null, null, 3, 3))

    @Test fun mixedCycleAdvancesAtEachItemsOwnPeriodAndWraps() {
        var cell = TableCellState(0, 0, cellId = "cell",
            dataType = TableCellDataType.ROTATING_TEXT, phraseSetId = base.id)
        val expected = listOf(5, 5, 5, 3, 3)
        expected.forEachIndexed { index, period ->
            assertEquals(index, cell.phraseIndex)
            assertEquals(period, base.everyForItem(index))
            repeat(period - 1) { shot ->
                cell = next(cell, base)
                assertEquals(index, cell.phraseIndex)
                assertEquals(shot + 1, cell.phraseCompletedShots)
            }
            cell = next(cell, base)
            assertEquals((index + 1) % 5, cell.phraseIndex)
            assertEquals(0, cell.phraseCompletedShots)
        }
        assertEquals(0, cell.phraseIndex)
    }

    @Test fun explicitCellPeriodOverridesEveryPhraseAndResetReturnsToSet() {
        val fixed = TableCellState(0, 0, cellId = "fixed",
            dataType = TableCellDataType.ROTATING_TEXT, phraseSetId = base.id,
            everyOverride = 4, phraseIndex = 3, phraseCompletedShots = 3)
        assertEquals(4, CellPhraseProgressPolicy.current(fixed, base).completedShots + 1)
        assertEquals(CellPhraseProgress(4, 0), CellPhraseProgressPolicy.afterSuccessfulCapture(fixed, base))
        val inherited = fixed.copy(everyOverride = null)
        assertEquals(CellPhraseProgress(4, 0),
            CellPhraseProgressPolicy.afterSuccessfulCapture(inherited, base))
        assertEquals(3, base.everyForItem(3))
    }

    @Test fun movingOrDeletingDuplicatePhrasesKeepsTheirOwnOverrides() {
        val duplicate = base.copy(items = listOf("같음", "같음", "다름"),
            itemEveryOverrides = listOf(2, 7, null))
        val moved = RotatingPhraseSetItemPolicy.moveItem(duplicate, 1, 0)
        assertEquals(listOf("같음", "같음", "다름"), moved.items)
        assertEquals(listOf(7, 2, null), moved.itemEveryOverrides)
        val removed = RotatingPhraseSetItemPolicy.removeItem(moved, 1)
        assertEquals(listOf("같음", "다름"), removed.items)
        assertEquals(listOf(7, null), removed.itemEveryOverrides)
        val inserted = RotatingPhraseSetItemPolicy.addItem(removed, "신규")
        assertEquals(listOf(7, null, null), inserted.itemEveryOverrides)
        val renamed = RotatingPhraseSetItemPolicy.renameItem(inserted, 0, "수정")
        assertEquals(7, renamed.itemEveryOverrides[0])
        assertEquals("수정", renamed.items[0])
    }

    @Test fun editingIndividualPeriodAndResetToSetDefault() {
        val changed = RotatingPhraseSetItemPolicy.setEvery(base, 1, 2)
        assertEquals(2, changed.everyForItem(1))
        assertEquals(5, changed.everyForItem(0))
        val cleared = RotatingPhraseSetItemPolicy.setEvery(changed, 1, null)
        assertEquals(5, cleared.everyForItem(1))
        assertEquals(null, cleared.itemEveryOverrides[1])
        assertSame(base, RotatingPhraseSetItemPolicy.setEvery(base, -1, 2))
        assertSame(base, RotatingPhraseSetItemPolicy.setEvery(base, 2, 0))
        assertEquals(base, RotatingPhraseSetItemPolicy.moveItem(base, 9, 0))
    }

    @Test fun oldTemplatesRoundTripWithoutNewKeyOrChangedMeaning() {
        val old = RotatingPhraseSet("legacy", "기존", listOf("A", "B"), 3)
        val cell = TableCellState(0, 0, cellId = "cell",
            dataType = TableCellDataType.ROTATING_TEXT, phraseSetId = old.id)
        val template = TableTemplateState(1, 1, listOf(cell), phraseSets = listOf(old))
        val encoded = template.toJsonString()
        assertFalse(JSONObject(encoded).getJSONArray("phraseSets").getJSONObject(0)
            .has("itemEveryOverrides"))
        val restored = requireNotNull(tableTemplateStateFromJson(encoded))
        assertEquals(template, restored)
        assertEquals(3, restored.phraseSets.single().everyForItem(1))
    }

    @Test fun continuousAddStopsAtSharedCsvAndBackupLimit() {
        val initial = RotatingPhraseSet(
            "capacity", "대량", (1..999).map { "문구 $it" }, defaultEvery = 2,
        )
        val thousand = RotatingPhraseSetItemPolicy.addItem(initial, "마지막")
        assertEquals(RotatingPhraseSet.MAX_ITEMS, thousand.items.size)
        assertEquals(2, thousand.everyForItem(999))
        assertSame(thousand, RotatingPhraseSetItemPolicy.addItem(thousand, "초과"))
        assertSame(initial, RotatingPhraseSetItemPolicy.addItem(initial, ""))
        assertSame(initial, RotatingPhraseSetItemPolicy.addItem(initial, "x".repeat(501)))
        assertSame(initial, RotatingPhraseSetItemPolicy.addItem(initial, "a\nb"))
        assertSame(initial, RotatingPhraseSetItemPolicy.renameItem(initial, 0, "x".repeat(501)))
        val changed = RotatingPhraseSetItemPolicy.renameItem(initial, 0, " 수정 ")
        assertEquals("수정", changed.items.first())
        assertSame(changed, RotatingPhraseSetItemPolicy.renameItem(changed, 0, "수정"))
        assertEquals(thousand.items.size, thousand.itemEveryOverrides.size)
    }

    @Test fun bulkIntervalChangesOnlySelectedRowsWithoutTouchingNamesOrOrders() {
        val source = RotatingPhraseSet(
            "bulk", "조사", (1..250).map { "문구 $it" }, defaultEvery = 5,
        )
        val changed = RotatingPhraseSetItemPolicy.setEveryForItems(
            source, listOf(0, 5, 200, 249, 5), 3,
        )
        assertEquals(source.items, changed.items)
        assertEquals(3, changed.everyForItem(0))
        assertEquals(3, changed.everyForItem(5))
        assertEquals(3, changed.everyForItem(200))
        assertEquals(3, changed.everyForItem(249))
        assertEquals(5, changed.everyForItem(199))
        assertEquals(5, source.everyForItem(5))
        assertSame(changed, RotatingPhraseSetItemPolicy.setEveryForItems(
            changed, listOf(0, 5, 200, 249), 3,
        ))
        val reverted = RotatingPhraseSetItemPolicy.setEveryForItems(
            changed, listOf(0, 200), null,
        )
        assertEquals(5, reverted.everyForItem(0))
        assertEquals(3, reverted.everyForItem(5))
        assertEquals(5, reverted.everyForItem(200))
        assertEquals(3, reverted.everyForItem(249))
    }

    @Test fun addingOnlyInheritedPhrasesSurvivesSaveAndReloadExactly() {
        val original = RotatingPhraseSet("id", "기본", listOf("시작"), defaultEvery = 5)
        val added = RotatingPhraseSetItemPolicy.addItem(original, "새 문구")
        assertEquals(listOf(null, null), added.itemEveryOverrides)
        val template = TableTemplateState(
            1, 1, listOf(TableCellState(0, 0, cellId = "cell")),
            phraseSets = listOf(added),
        )
        val restored = requireNotNull(tableTemplateStateFromJson(template.toJsonString()))
        assertEquals(template.phraseSets, restored.phraseSets)
        assertEquals(5, restored.phraseSets.single().everyForItem(1))
    }

    @Test fun mixedPeriodsPersistAndRestoreWithNulls() {
        val cell = TableCellState(0, 0, cellId = "cell",
            dataType = TableCellDataType.ROTATING_TEXT, phraseSetId = base.id,
            phraseIndex = 3, phraseCompletedShots = 2)
        val template = TableTemplateState(1, 1, listOf(cell), phraseSets = listOf(base))
        val json = template.toJsonString()
        val arr = JSONObject(json).getJSONArray("phraseSets").getJSONObject(0)
            .getJSONArray("itemEveryOverrides")
        assertTrue(arr.isNull(0))
        assertEquals(3, arr.getInt(3))
        val restored = requireNotNull(tableTemplateStateFromJson(json))
        assertEquals(template, restored)
        assertEquals(CellPhraseProgress(4, 0),
            CellPhraseProgressPolicy.afterSuccessfulCapture(restored.cells.single(), restored.phraseSets.single()))
    }

    private fun next(cell: TableCellState, set: RotatingPhraseSet): TableCellState {
        val after = CellPhraseProgressPolicy.afterSuccessfulCapture(cell, set)
        return cell.copy(phraseIndex = after.index, phraseCompletedShots = after.completedShots)
    }
}
