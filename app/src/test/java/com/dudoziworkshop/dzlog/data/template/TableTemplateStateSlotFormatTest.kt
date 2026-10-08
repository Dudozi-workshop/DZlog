package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TableTemplateStateSlotFormatTest {

    @Test
    fun naming_slot_format_pattern_round_trips_through_json() {
        val dateSlot = TableEditorSlotDraft(
            kind = "FORMAT",
            label = "날짜",
            formatType = "DATE",
            formatPattern = "yyyy-MM-dd",
        )
        val initial = newBlankTableTemplateState(rows = 1, cols = 1).copy(
            fileNameSlotDrafts = listOf(dateSlot) +
                List((FILE_NAME_SLOT_COUNT - 1).coerceAtLeast(0)) { null },
        )

        val restored = tableTemplateStateFromJson(initial.toJsonString())

        assertNotNull(restored)
        assertEquals("DATE", restored!!.fileNameSlotDrafts.first()?.formatType)
        assertEquals("yyyy-MM-dd", restored.fileNameSlotDrafts.first()?.formatPattern)
    }
}
