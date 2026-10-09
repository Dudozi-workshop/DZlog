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
}
