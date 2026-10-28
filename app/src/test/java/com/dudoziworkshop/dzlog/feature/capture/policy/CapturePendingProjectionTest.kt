package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellKind
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgressPolicy
import com.dudoziworkshop.dzlog.domain.phrase.PhraseResolver
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.domain.table.applyPatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturePendingProjectionTest {
    private val counter = TableCellState(
        rowIndex = 0, colIndex = 0, kind = TableCellKind.INPUT,
        dataType = TableCellDataType.COUNTER, cellId = "counter",
        typedValue = CellValue.CounterSeed(1)
    )
    private val phrase = TableCellState(
        rowIndex = 0, colIndex = 1, kind = TableCellKind.INPUT,
        dataType = TableCellDataType.ROTATING_TEXT, cellId = "phrase",
    )
    private val template = TableTemplateState(rows = 1, cols = 2, cells = listOf(counter, phrase))
    private val scope = CaptureReservationScope("Pictures/DZlog/", "Test")

    private fun shot(id: String, number: Int, phraseIndex: Int, completedShots: Int = 0) =
        CapturePendingProjection.Shot(
            id, scope, number, TablePatch(
                updatesByCellId = mapOf("counter" to (number + 1).toString()),
                phraseProgressByCellId = mapOf("phrase" to CellPhraseProgress(phraseIndex, completedShots)),
            )
        )

    @Test fun speculativePreviewAdvancesWithoutMutatingSavedTemplate() {
        val projection = CapturePendingProjection()
        projection.add(shot("first", 1, 1))
        projection.add(shot("second", 2, 2))
        val preview = projection.previewTemplate(template)
        assertEquals(1, (template.cells[0].typedValue as CellValue.CounterSeed).start)
        assertEquals(3, (preview.cells[0].typedValue as CellValue.CounterSeed).start)
        assertEquals(2, preview.cells[1].phraseIndex)
        assertEquals(3, projection.nextCounter(scope, 1))
    }

    @Test fun pendingNumbersFromPreviousPhraseCannotAdvanceNewPhraseCounter() {
        val projection = CapturePendingProjection()
        val firstPhrase = scope.copy(streamPrefix = "phrase-one")
        val secondPhrase = scope.copy(streamPrefix = "phrase-two")
        projection.add(shot("one", 1, 0).copy(scope = firstPhrase))
        projection.add(shot("two", 2, 0).copy(scope = firstPhrase))
        projection.add(shot("three", 3, 1).copy(scope = firstPhrase))

        assertEquals(4, projection.nextCounter(firstPhrase, 1))
        assertEquals(1, projection.nextCounter(secondPhrase, 1))
    }

    @Test fun templateSwitchClearsOnlySafeCommittedPreviewCache() {
        val projection = CapturePendingProjection()
        assertTrue(projection.bindTemplate("template-a"))
        val advanced = template.applyPatch(shot("first", 1, 1).templatePatch)
        projection.recordCommittedTemplate(advanced)
        assertEquals(advanced, projection.previewTemplate(template))

        // Switching to a different template after the queue drains must not
        // overlay the old template's phrase/counter on the new selection.
        assertTrue(projection.bindTemplate("template-b"))
        assertEquals(template, projection.previewTemplate(template))
        projection.add(shot("second", 2, 2))
        assertTrue(!projection.bindTemplate("template-c"))
        assertEquals(1, projection.pendingCount())
    }

    @Test fun rejectedDuplicateCounterDoesNotAlterPendingList() {
        val projection = CapturePendingProjection()
        projection.add(shot("first", 1, 1))
        assertThrows(IllegalStateException::class.java) {
            projection.add(shot("second", 1, 2))
        }
        assertEquals(1, projection.pendingCount())
    }

    @Test fun rejectsOutOfOrderCompletion() {
        val projection = CapturePendingProjection()
        projection.add(shot("first", 1, 1))
        projection.add(shot("second", 2, 2))
        assertThrows(IllegalStateException::class.java) {
            projection.completeFirst("second")
        }
        assertEquals(2, projection.pendingCount())
        assertEquals(1, projection.completeFirst("first").phraseProgressByCellId["phrase"]?.index)
        assertTrue(projection.hasPending())
    }

    @Test fun threeCapturesPerPhraseDoNotRewindWhileCatalogUpdatesLag() {
        val projection = CapturePendingProjection()
        val shots = listOf(
            shot("one", 1, 0, 1),
            shot("two", 2, 0, 2),
            shot("three", 3, 1, 0),
            shot("four", 4, 1, 1),
            shot("five", 5, 1, 2),
            shot("six", 6, 2, 0),
        )

        // Three quick shutters advance the NEXT preview to phrase 2.
        shots.take(3).forEach(projection::add)
        assertEquals(1, projection.previewTemplate(template).cells[1].phraseIndex)

        // Simulate the Compose catalog still supplying the old base on every
        // completed receipt. Removing a reservation must never rewind phrase 2.
        shots.take(3).forEach { shot ->
            val base = projection.committedBase(template)
            val applied = base.applyPatch(projection.completeFirst(shot.id))
            projection.recordCommittedTemplate(applied)
            assertEquals(1, projection.previewTemplate(template).cells[1].phraseIndex)
        }

        // The 4th and 5th captures must remain on phrase 2, not phrase 1.
        projection.add(shots[3])
        assertEquals(1, projection.previewTemplate(template).cells[1].phraseIndex)
        projection.add(shots[4])
        assertEquals(1, projection.previewTemplate(template).cells[1].phraseIndex)
        projection.add(shots[5])
        assertEquals(2, projection.previewTemplate(template).cells[1].phraseIndex)

        // Once the catalog receives the committed base it becomes authoritative.
        val committed = projection.committedBase(template)
        assertEquals(committed, projection.committedBase(committed))
    }
    @Test fun nineRapidRotatingCapturesStayOnCorrectPhraseAndScopedCounter() {
        val rotation = RotatingPhraseSet(
            id = "rotation",
            name = "3 shots each",
            items = listOf("ONE", "TWO", "THREE"),
            defaultEvery = 3,
        )
        val initial = template.copy(
            cells = listOf(counter, phrase.copy(phraseSetId = rotation.id)),
            phraseSets = listOf(rotation),
        )
        val projection = CapturePendingProjection()
        val outstanding = mutableListOf<CapturePendingProjection.Shot>()
        val persistedNext = mutableMapOf<CaptureReservationScope, Int>()

        // MediaStore sees only committed shots, while the projection also sees
        // reservations. Simulate a slow worker with up to 3 outstanding captures.
        fun commitOldest() {
            val shot = outstanding.removeAt(0)
            val committed = projection.committedBase(initial)
                .applyPatch(projection.completeFirst(shot.id))
            projection.recordCommittedTemplate(committed)
            persistedNext[shot.scope] = shot.counter + 1
        }

        val observed = mutableListOf<Pair<String, Int>>()
        repeat(10) { shotIndex ->
            if (outstanding.size == 3) commitOldest()
            val before = projection.previewTemplate(initial)
            val text = PhraseResolver.resolveSelectedTextByCellId(
                cells = before.cells,
                phraseSets = before.phraseSets,
                progressCursor = 1,
            ).getValue("phrase")
            val scoped = CaptureReservationScope("Pictures/DZlog/", text)
            val next = projection.nextCounter(scoped, persistedNext[scoped] ?: 1)
            observed += text to next

            val shot = CapturePendingProjection.Shot(
                id = "shot-${shotIndex + 1}",
                scope = scoped,
                counter = next,
                templatePatch = TablePatch(
                    updatesByCellId = mapOf("counter" to (next + 1).toString()),
                    phraseProgressByCellId = CellPhraseProgressPolicy.captureUpdates(
                        before.cells, before.phraseSets,
                    ),
                ),
            )
            projection.add(shot)
            outstanding += shot
        }
        assertEquals(
            listOf(
                "ONE" to 1, "ONE" to 2, "ONE" to 3,
                "TWO" to 1, "TWO" to 2, "TWO" to 3,
                "THREE" to 1, "THREE" to 2, "THREE" to 3,
                "ONE" to 4,
            ),
            observed,
        )
        while (outstanding.isNotEmpty()) commitOldest()
        val after = projection.previewTemplate(initial)
        assertEquals(0, after.cells[1].phraseIndex)
        assertEquals(1, after.cells[1].phraseCompletedShots)
    }


    @Test fun configurableRepeatCountsRespectRapidCaptureBoundaries() {
        for (repeatEvery in listOf(1, 2, 5, 12, 30)) {
            val set = RotatingPhraseSet(
                id = "set", name = "User configuration",
                items = listOf("A", "B", "C"), defaultEvery = repeatEvery,
            )
            val start = template.copy(
                cells = listOf(counter, phrase.copy(phraseSetId = set.id)),
                phraseSets = listOf(set),
            )
            val projection = CapturePendingProjection()
            val outstanding = mutableListOf<CapturePendingProjection.Shot>()
            val observed = mutableListOf<String>()
            var nextCounter = 1

            fun commitOldest() {
                val done = outstanding.removeAt(0)
                val committed = projection.committedBase(start)
                    .applyPatch(projection.completeFirst(done.id))
                projection.recordCommittedTemplate(committed)
            }
            repeat(repeatEvery * 3 + 1) { index ->
                if (outstanding.size == 3) commitOldest()
                val before = projection.previewTemplate(start)
                observed += PhraseResolver.resolveSelectedTextByCellId(
                    before.cells, before.phraseSets, 1
                ).getValue("phrase")
                val shot = CapturePendingProjection.Shot(
                    id = "shot-$index", scope = scope,
                    counter = nextCounter++,
                    templatePatch = TablePatch(
                        updatesByCellId = emptyMap(),
                        phraseProgressByCellId = CellPhraseProgressPolicy.captureUpdates(
                            before.cells, before.phraseSets
                        )
                    )
                )
                projection.add(shot)
                outstanding.add(shot)
            }
            val expected = List(repeatEvery) { "A" } +
                List(repeatEvery) { "B" } +
                List(repeatEvery) { "C" } + "A"
            assertEquals("repeat interval $repeatEvery", expected, observed)
            while (outstanding.isNotEmpty()) commitOldest()
        }
    }
    @Test fun sameTemplateExternalEditReplacesPreviousCaptureCache() {
        val projection = CapturePendingProjection()
        assertTrue(projection.bindTemplate("table-a"))
        val old = template.copy(phraseSets = listOf(
            RotatingPhraseSet("phrases", "Set", listOf("OLD", "OTHER"))
        ), cells = listOf(counter, phrase.copy(phraseSetId = "phrases")))
        val captured = old.applyPatch(shot("old", 1, 1).templatePatch)
        projection.recordCommittedTemplate(captured)
        val edited = old.copy(
            phraseSets = listOf(RotatingPhraseSet("phrases", "Set", listOf("NEW", "NEXT"))),
            cells = listOf(counter, phrase.copy(phraseSetId = "phrases", phraseIndex = 0)),
        )
        assertTrue(projection.canSaveEditedTemplate("table-a"))
        assertTrue(projection.invalidateCommittedAfterExternalEdit("table-a"))
        assertEquals(edited, projection.previewTemplate(edited))
        assertEquals(
            "NEW",
            PhraseResolver.resolveSelectedTextByCellId(
                edited.cells, edited.phraseSets, 1
            ).getValue("phrase"),
        )
    }

    @Test fun pendingCapturePreventsExternalEditInvalidation() {
        val projection = CapturePendingProjection()
        assertTrue(projection.bindTemplate("table-a"))
        projection.add(shot("one", 1, 1))
        assertTrue(!projection.canSaveEditedTemplate("table-a"))
        assertTrue(!projection.invalidateCommittedAfterExternalEdit("table-a"))
        assertEquals(1, projection.pendingCount())
        assertEquals(1, projection.previewTemplate(template).cells[1].phraseIndex)
        projection.completeFirst("one")
        assertTrue(projection.canSaveEditedTemplate("table-a"))
    }

    @Test fun editingOtherTemplateDoesNotResetCurrentCaptureProgress() {
        val projection = CapturePendingProjection()
        assertTrue(projection.bindTemplate("table-a"))
        val captured = template.applyPatch(shot("one", 1, 1).templatePatch)
        projection.recordCommittedTemplate(captured)
        assertTrue(projection.canSaveEditedTemplate("table-b"))
        assertTrue(!projection.invalidateCommittedAfterExternalEdit("table-b"))
        assertEquals(captured, projection.previewTemplate(template))
    }

}
