package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.domain.model.RotatingCounterProgressMode
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RotatingPhrasePersistenceTest {
    @Test
    fun both_modes_round_trip_with_phrase_contents_and_order() {
        RotatingCounterProgressMode.entries.forEach { mode ->
            val set = RotatingPhraseSet("set", "처리구", listOf("A", "B", "C"), 2, mode)
            val template = newBlankTableTemplateState(1, 1).copy(phraseSets = listOf(set))
            assertEquals(template, tableTemplateStateFromJson(template.toJsonString()))
        }
    }

    @Test
    fun absent_unknown_and_null_modes_preserve_legacy_per_phrase_policy() {
        val template = newBlankTableTemplateState(1, 1).copy(
            phraseSets = listOf(RotatingPhraseSet("set", "처리구", listOf("A", "B"))),
        )
        listOf(null, "FUTURE_MODE", JSONObject.NULL).forEach { value ->
            val root = JSONObject(template.toJsonString())
            val set = root.getJSONArray("phraseSets").getJSONObject(0)
            if (value == null) set.remove("counterProgressMode") else set.put("counterProgressMode", value)
            assertEquals(template, tableTemplateStateFromJson(root.toString()))
        }
    }
    @Test fun cell_progress_round_trips_and_legacy_templates_start_at_zero() {
        val template = newBlankTableTemplateState(1, 1).let { t ->
            t.copy(cells = t.cells.map { it.copy(phraseIndex = 2, phraseCompletedShots = 7) })
        }
        assertEquals(template, tableTemplateStateFromJson(template.toJsonString()))
        val root = JSONObject(template.toJsonString())
        root.getJSONArray("cells").getJSONObject(0).apply {
            remove("phraseIndex"); remove("phraseCompletedShots")
        }
        val restored = requireNotNull(tableTemplateStateFromJson(root.toString())).cells.single()
        assertEquals(0, restored.phraseIndex)
        assertEquals(0, restored.phraseCompletedShots)
    }

    @Test
    fun inactive_individual_period_survives_serialization_and_reordering() {
        val set = RotatingPhraseSet(
            "s", "세트", listOf("A", "B", "C"), defaultEvery = 5,
            itemEveryOverrides = listOf(null, null, 2),
            itemEveryInactiveOverrides = listOf(null, 3, 2),
        )
        val template = newBlankTableTemplateState(1, 1).copy(phraseSets = listOf(set))
        val restored = requireNotNull(tableTemplateStateFromJson(template.toJsonString()))
        assertEquals(template, restored)

        val reordered = com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseSetItemPolicy
            .moveItem(restored.phraseSets.single(), 1, 0)
        assertEquals(listOf("B", "A", "C"), reordered.items)
        assertEquals(listOf(3, null, 2), reordered.itemEveryInactiveOverrides)
        assertEquals(listOf(null, null, 2), reordered.itemEveryOverrides)
    }

    @Test
    fun missing_inactive_periods_remain_compatible_with_old_templates() {
        val set = RotatingPhraseSet(
            "s", "세트", listOf("A", "B"), defaultEvery = 4,
            itemEveryOverrides = listOf(null, 2),
        )
        val template = newBlankTableTemplateState(1, 1).copy(phraseSets = listOf(set))
        val json = JSONObject(template.toJsonString())
        json.getJSONArray("phraseSets").getJSONObject(0).remove("itemEveryInactiveOverrides")
        assertEquals(template, tableTemplateStateFromJson(json.toString()))
    }

}
